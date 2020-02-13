package com.epam.drill.core

import com.epam.drill.common.*
import com.epam.drill.common.ws.*
import com.epam.drill.plugin.api.processing.*
import kotlin.native.concurrent.*

class DI {
    lateinit var adminAddress: URL
    lateinit var secureAdminAddress: URL
    lateinit var agentConfig: AgentConfig
    lateinit var drillInstallationDir: String
    var requestPattern: String? = null

    var pstorage: MutableMap<String, AgentPart<*, *>> = mutableMapOf()
    val pl = mutableMapOf<String, PluginMetadata>()

}

inline fun <reified T> exec(noinline what: DI.() -> T) = work.execute(TransferMode.UNSAFE, { what }) {
    it(dsa)
}.result


@SharedImmutable
val work = Worker.start(true)

@ThreadLocal
val dsa = DI()

