/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.test2code.coverage

import com.epam.drill.jacoco.AgentProbes

/**
 * Provides boolean array for the probe.
 * Implementations must be kotlin singleton objects.
 */
typealias IProbesProxy = (ClassId, Int, String, Int) -> AgentProbes

// TODO extending w/o explicit reason is not the most straightforward solution
// reason for extension: to allow CoverageManager to use abstract ExecDataProvider as IProbesProxy
interface IExecDataProvider: IProbesProxy {
    fun setContext(sessionId: String?, testId: String?)
    fun releaseContext(sessionId: String?, testId: String?)
    fun poll(): Sequence<ExecDatum>
}

const val SESSION_CONTEXT_NONE = "SESSION_CONTEXT_NONE"
const val TEST_CONTEXT_NONE = "TEST_CONTEXT_NONE"
const val SESSION_CONTEXT_AMBIENT = "GLOBAL"

data class ContextKey(
    private val _sessionId: SessionId? = null, // TODO there must be a better way to avoid nullability and assign defaults
    private val _testId: TestId? = null
) {
    val sessionId: SessionId = _sessionId ?: SESSION_CONTEXT_NONE
    val testId: TestId = _testId ?: TEST_CONTEXT_NONE
}

private val CONTEXT_AMBIENT = ContextKey(SESSION_CONTEXT_AMBIENT)

/**
 * Implements :
 * - ThreadLocal storage for code execution context
 * - coverage storage in ExecData/ExecDatum with "lazy" creation
 * - access to class probes for instrumented code via IExecDataProvider
 * - coverage polling

 * Note #1:
 *  TODO this implementation wont work with AUTs relying on async request handling (1 request = multiple handler threads)
 *  the context will either be lost or wrong when request will get passed from one thread to the other
 *  FIX: we might employ TransmittableThreadLocal storage (see EPMDJ-8256 and «com.alibaba.ttl.TransmittableThreadLocal»)

 * Note #1:
 *  Since ThreadExecDataProvider implements IExecDataProvider it _must_ be a Kotlin singleton object
 *  otherwise the instrumented probe calls will fail
 */
class ThreadExecDataProvider(
    private val classDescriptorsProvider: IClassDescriptorsProvider,
    private val execDataPool: DataPool<ContextKey, ExecData> = ConcurrentDataPool(),
): IExecDataProvider {

    // Context management
    private val context: ThreadLocal<ContextKey> = ThreadLocal()
    private val execData: ThreadLocal<ExecData> = ThreadLocal()
    private lateinit var globalExecData: ExecData

    init {
        addGlobalExecDataToPool()
    }

    private fun addGlobalExecDataToPool(): ExecData {
        globalExecData = execDataPool.getOrPut(
            CONTEXT_AMBIENT,
            default = { ExecData() }
        )
        return globalExecData
    }

    private fun releaseGlobalExecDataFromPool() {
        execDataPool.release(CONTEXT_AMBIENT, globalExecData)
        // TODO cannot set this.globalExecData = null, since it still can be used by instrumented code
        //      it also means some coverage might be lost when coverage polling happens before instrumented method returns
    }

    override fun setContext(sessionId: String?, testId: String?) {
        val ctx = ContextKey(sessionId, testId)
        this.context.set(ctx)
        this.execData.set(execDataPool.getOrPut(
            ctx,
            default = { ExecData() }
        ))
    }

    override fun releaseContext(sessionId: String?, testId: String?) {
        execDataPool.release(ContextKey(sessionId, testId), this.execData.get())
        this.execData.remove()
        this.context.remove()
    }

    // Coverage polling
    override fun poll(): Sequence<ExecDatum> {
        releaseGlobalExecDataFromPool()
        return execDataPool
            .pollReleased()
            .flatMap { it.values }
            .filter { it.probes.containCovered() }
    }

    // IExecDataProvider
    override fun invoke(
        id: Long,
        num: Int,
        name: String,
        probeCount: Int,
    ): AgentProbes = this.getProbesForClass(id)

    private fun getProbesForClass(classId: ClassId): AgentProbes {
        val currentContext: ContextKey
        val currentExecData: ExecData

        val requestContext = this.context.get()
        if (requestContext != null) {
            currentContext = requestContext
            currentExecData = execDataPool.get(currentContext)!!
        } else {
            currentContext = CONTEXT_AMBIENT
            currentExecData = addGlobalExecDataToPool()
        }

        val classDescriptor = classDescriptorsProvider.get(classId)
        val execDatum = currentExecData.getOrPut(classId) {
            ExecDatum(
                id = classDescriptor.id,
                name = classDescriptor.name,
                probes = AgentProbes(classDescriptor.probeCount),
                sessionId = currentContext.sessionId,
                testId = currentContext.testId
            )
        }
        return  execDatum.probes
    }
}
