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
    private var threadLocalContext: ThreadLocal<ContextKey> = ThreadLocal() // TODO maybe assign that in setContext call (I'm not sure if this really gona be thread-local)

    override fun setContext(sessionId: String?, testId: String?) {
        this.threadLocalContext.set(ContextKey(sessionId, testId))
    }

    override fun releaseContext(sessionId: String?, testId: String?) {
        execDataPool.release(ContextKey(sessionId, testId))
        this.threadLocalContext.remove()
    }

    // Coverage polling
    override fun poll(): Sequence<ExecDatum> {
        execDataPool.release(CONTEXT_AMBIENT)
        return execDataPool
            .pollReleased()
            .flatMap { it.values }
            .filter { it.probes.containCovered() }
            // TODO this looks redundant, since we already create fresh sequence in pollReleased impl
            .map { datum ->
                datum.copy(
                    probes = AgentProbes(
                        values = datum.probes.values.copyOf()
                    )
                ).also {
                    datum.probes.values.fill(false)
                }
            }
    }

    // IExecDataProvider
    override fun invoke(
        id: Long,
        num: Int,
        name: String,
        probeCount: Int,
    ): AgentProbes = this.getProbesForClass(id)

    private fun getProbesForClass(classId: ClassId): AgentProbes {
        val context = threadLocalContext.get() ?: CONTEXT_AMBIENT;
        val execData = execDataPool.getOrPut(context) {
            ExecData()
        }
        val classDescriptor = classDescriptorsProvider.get(classId)!! // TODO this will crash AUT, I don't like this
        val execDatum = execData.getOrPut(classId) {
            ExecDatum(
                id = classDescriptor.id,
                name = classDescriptor.name,
                probes = AgentProbes(classDescriptor.probeCount),
                sessionId = context.sessionId,
                testId = context.testId
            )
        }
        return  execDatum.probes
    }
}
