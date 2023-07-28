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
@file:Suppress("unused")

package com.epam.drill.agent

import com.epam.drill.*
import com.epam.drill.common.*
import com.epam.drill.kni.*
import com.epam.drill.logger.*
import com.epam.drill.logger.api.*
import com.epam.drill.plugin.*
import com.epam.drill.plugin.api.processing.*
import com.epam.drill.request.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.protobuf.*
import java.util.jar.*
import kotlin.reflect.jvm.*
import kotlin.time.*

@Serializable
class ByteArrayListWrapper(val bytesList: List<ByteArray>)

@ExperimentalStdlibApi
@Kni
actual object DataService {
    private val logger = Logging.logger(DataService::class.jvmName)

    actual fun createAgentPart(id: String, jarPath: String): Any? = run {
        val agentPartClass = retrieveApiClass(jarPath)!!
        val constructor = agentPartClass.getConstructor(
            String::class.java,
            AgentContext::class.java,
            Sender::class.java,
            LoggerFactory::class.java
        )
        constructor.newInstance(id, RequestHolder.agentContext, PluginSender, Logging)
    }

    actual fun doRawActionBlocking(
        agentPart: Any,
        data: String,
    ): Any = with(agentPart as AgentPart<*>) {
        runBlocking { doRawAction(data) }
    }

    private fun retrieveApiClass(jarPath: String): Class<AgentPart<*>>? = JarFile(jarPath).use { jf ->
        val result = retrieveApiClass(
            AgentPart::class.java, jf.entries().iterator().asSequence().toSet(),
            ClassLoader.getSystemClassLoader()
        )
        @Suppress("UNCHECKED_CAST")
        result as Class<AgentPart<*>>?
    }
}
