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
package com.epam.drill.agent.ttl.forkjoin

import com.epam.drill.agent.*
import com.epam.drill.agent.ttl.TtlCallable
import com.epam.drill.agent.ttl.TtlRunnable
import com.epam.drill.agent.ttl.testmodel.Call
import com.epam.drill.agent.ttl.testmodel.Task
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.ForkJoinPool

private val pool = ForkJoinPool()

class ForkJoinPool4RunnableCallableTest : AnnotationSpec() {

    @Test
    fun test_Runnable() {
        val ttlInstances = createParentTtlInstances()

        val task = Task("1", ttlInstances)
        val ttlRunnable = if (noTtlAgentRun()) TtlRunnable.get(task) else task

        if (noTtlAgentRun()) {
            // create after new Task, won't see parent value in in task!
            createParentTtlInstancesAfterCreateChild(ttlInstances)
        }
        val submit = pool.submit(ttlRunnable)
        if (!noTtlAgentRun()) {
            // create after new Task, won't see parent value in in task!
            createParentTtlInstancesAfterCreateChild(ttlInstances)
        }


        submit.get()


        // child Inheritable
        assertChildTtlValues("1", task.copied)

        // child do not effect parent
        assertParentTtlValues(copyTtlValues(ttlInstances))
    }

    @Test
    fun test_Callable() {
        val ttlInstances = createParentTtlInstances()

        val call = Call("1", ttlInstances)
        val ttlCallable = if (noTtlAgentRun()) TtlCallable.get(call) else call

        if (noTtlAgentRun()) {
            // create after new Task, won't see parent value in in task!
            createParentTtlInstancesAfterCreateChild(ttlInstances)
        }
        val future = pool.submit(ttlCallable)
        if (!noTtlAgentRun()) {
            // create after new Task, won't see parent value in in task!
            createParentTtlInstancesAfterCreateChild(ttlInstances)
        }

        future.get() shouldBe "ok"


        // child Inheritable
        assertChildTtlValues("1", call.copied)

        // child do not effect parent
        assertParentTtlValues(copyTtlValues(ttlInstances))
    }
}
