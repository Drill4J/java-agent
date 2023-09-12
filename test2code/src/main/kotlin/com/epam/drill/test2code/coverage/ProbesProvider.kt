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

const val SESSION_ID_NO_CONTEXT = "SESSION_NO_CONTEXT"
const val TEST_ID_NO_CONTEXT = "TEST_NO_CONTEXT"
private val AMBIENT_CONTEXT_KEY = SessionTestKey("SESSION_CONTEXT_AMBIENT", "TEST_CONTEXT_AMBIENT")

// TODO extending w/o explicit reason is not the most straightforward solution
// reason for extension: to allow CoverageManager to use abstract ExecDataProvider as IProbesProxy
interface IExecDataProvider: IProbesProxy {
    fun setContext(sessionId: String?, testId: String?)
    fun clearContext(sessionId: String?, testId: String?)
    fun getExecData(sessionId: String?, testId: String?): ExecData?
    fun getExecDatumProbes(classId: ClassId): AgentProbes
}

class ThreadExecDataProvider(
    private val execDataPool: DataPool<SessionTestKey, ExecData>,
    private val probesDescriptorProvider: IClassDescriptorProvider,
): IExecDataProvider {

    override fun invoke(
        id: Long,
        num: Int,
        name: String,
        probeCount: Int,
    ): AgentProbes = this.getExecDatumProbes(id)

    // TODO ! wont work with AUTs implementing async request handling
    // WHY: the context will either be lost or wrong when request handling will pass from one thread to the other)
    // FIX: we might employ TransmittableThreadLocal storage (see EPMDJ-8256 and «com.alibaba.ttl.TransmittableThreadLocal»)
    private var threadLocalContext: ThreadLocal<SessionTestKey> = ThreadLocal() // TODO maybe assign that in setContext call (I'm not sure if this really gona be thread-local)

    init {
        resetContext()
    }

    private fun resetContext() {
        this.threadLocalContext.set(AMBIENT_CONTEXT_KEY)
    }

    override fun setContext(sessionId: String?, testId: String?) {
        this.threadLocalContext.set(
            SessionTestKey(
                sessionId ?: SESSION_ID_NO_CONTEXT,
                testId ?: TEST_ID_NO_CONTEXT)
            // TODO Admin Backend relies on testId, rather than _context_
            // because of that, it _will_ mix up contexts with the following keys:
            //      SESSION_ID_NO_CONTEXT:TEST_ID_NO_CONTEXT
            //      vs
            //      *sessionId*:TEST_ID_NO_CONTEXT
        )
    }

    override fun clearContext(sessionId: String?, testId: String?) {
        resetContext()
    }

    override fun getExecData(sessionId: String?, testId: String?): ExecData {
        TODO("Not yet implemented")
    }

    override fun getExecDatumProbes(classId: ClassId): AgentProbes {
        val context = threadLocalContext.get();

        // !!!!!!!!!!!!!!!!!!!!! two getOrPut calls might introduce concurrency issue !!!!!!!!!!!!!!!!!!!!!
        val execData = execDataPool.getOrPut(context) {
            ExecData()
        }
        val classDescriptor = probesDescriptorProvider.get(classId)!! // TODO this will crash AUT, I don't like this
        val (sessionId, testId) = context // TODO relies too much on the field order definition. FIX: store context as Map

        // !!!!!!!!!!!!!!!!!!!!! two getOrPut calls might introduce concurrency issue !!!!!!!!!!!!!!!!!!!!!
        val execDatum = execData.getOrPut(classId) {
            ExecDatum(
                id = classDescriptor.id,
                name = classDescriptor.name,
                probes = AgentProbes(classDescriptor.probeCount),
                sessionId = sessionId,
                testId = testId
            )
        }
        return  execDatum.probes
    }

}
