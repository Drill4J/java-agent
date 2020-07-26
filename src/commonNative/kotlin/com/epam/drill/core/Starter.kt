package com.epam.drill.core

import com.epam.drill.*
import com.epam.drill.agent.*
import com.epam.drill.core.callbacks.classloading.*
import com.epam.drill.core.callbacks.vminit.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.logger.*
import com.epam.drill.transport.common.ws.*
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.native.concurrent.*
import com.epam.drill.kni.*
import kotlin.test.*

@SharedImmutable
private val logger = Logging.logger("MainLogger")

object Agent: JvmtiAgent {
    override fun agentOnLoad(options: String): Int{
        try {
        val agentParameters = options.asAgentParams().validate()
            performAgentInitialization(agentParameters)
            setUnhandledExceptionHook({ error: Throwable ->
                logger.error(error) { "unhandled event $error" }
            }.freeze())

            memScoped {
                val jvmtiCapabilities = alloc<jvmtiCapabilities>()
                jvmtiCapabilities.can_retransform_classes = 1.toUInt()
                jvmtiCapabilities.can_maintain_original_method_order = 1.toUInt()
                AddCapabilities(jvmtiCapabilities.ptr)
            }
            AddToBootstrapClassLoaderSearch("$drillInstallationDir/drillRuntime.jar")
            callbackRegister()

            logger.info { "The native agent was loaded" }
            logger.info { "Pid is: " + getpid() }
        } catch (ex: Throwable) {
            logger.error { "Can't load the agent. Ex: ${ex.message}" }
            JNI_ERR
        }
        return JNI_OK
    }
    override fun agentOnUnload(){
        logger.info { "Agent_OnUnload" }
    }
}

fun String?.asAgentParams(): AgentParameters {
    if (this.isNullOrEmpty()) return mutableMapOf()
    return try {
        this.split(",").associate {
            val (key, value) = it.split("=")
            key to value
        }
    } catch (parseException: Exception) {
        throw IllegalArgumentException("wrong agent parameters: $this")
    }
}

private fun callbackRegister() = memScoped {
    val alloc = alloc<jvmtiEventCallbacks>()
    alloc.VMInit = staticCFunction(::jvmtiEventVMInitEvent)
    alloc.VMDeath = staticCFunction(::vmDeathEvent)
    alloc.ClassFileLoadHook = staticCFunction(::classLoadEvent)
    SetEventCallbacks(alloc.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, null)
}

@Suppress("UNUSED_PARAMETER")
fun vmDeathEvent(jvmtiEnv: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?) {
    logger.info { "vmDeathEvent" }
}

private typealias AgentParameters = Map<String, String>

private fun AgentParameters.validate(): AgentParameters = apply {
    check("agentId" in this) { "Please set 'agentId' as agent parameters e.g. -agentpath:/path/to/agent=agentId={your ID}" }
    val adminAddress = get("adminAddress")
    checkNotNull(adminAddress) { "Please set 'adminAddress' as agent parameters e.g. -agentpath:/path/to/agent=adminAddress={hostname:port}" }
    try {
        URL("ws://$adminAddress")
    } catch (parseException: RuntimeException) {
        fail("Please check 'adminAddress' parameter. It should be a valid address to the admin service without schema and any additional paths, e.g. localhost:8090")
    }
}
