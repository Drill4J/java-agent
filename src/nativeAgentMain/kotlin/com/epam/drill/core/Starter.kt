package com.epam.drill.core

import com.epam.drill.api.*
import com.epam.drill.core.agent.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*
import mu.*
import platform.posix.*
import kotlin.native.concurrent.*


@Suppress("UNUSED_PARAMETER", "UNUSED")
@CName("Agent_OnLoad")
fun agentOnLoad(vmPointer: CPointer<JavaVMVar>, options: String, reservedPtr: Long): jint {
    try {
        initAgentGlobals(vmPointer)
        runAgent(options)
    } catch (ex: Throwable) {
        println("Can't load the agent. Ex: ${ex.message}")
    }
    return JNI_OK
}

@Jvmapi
private fun initAgentGlobals(vmPointer: CPointer<JavaVMVar>) {
    agentSetup(vmPointer.pointed.value)
    saveVmToGlobal(vmPointer)
}

private fun runAgent(options: String?) {
    options.asAgentParams().apply {
        val logger = KotlinLogging.logger("StartLogger")
        logger.info { "init params: $this" }
        performAgentInitialization(this)
        setUnhandledExceptionHook({ x: Throwable ->
            println("unhandled event $x")
        }.freeze())

        printAllowedCapabilities()
        memScoped {
            val alloc = alloc<jvmtiCapabilities>()
            alloc.can_retransform_classes = 1.toUInt()
            alloc.can_retransform_any_class = 1.toUInt()
            alloc.can_generate_native_method_bind_events = 1.toUInt()
            alloc.can_maintain_original_method_order = 1.toUInt()
            AddCapabilities(alloc.ptr)
        }
        AddToBootstrapClassLoaderSearch("$drillInstallationDir/drillRuntime.jar")
        SetNativeMethodPrefix("xxx_")
        callbackRegister()

        logger.info { "The native agent was loaded" }
        logger.info { "Pid is: " + getpid() }
    }
}

fun String?.asAgentParams(): Map<String, String> {
    if (this.isNullOrEmpty()) return mutableMapOf()
    return try {
        this.split(",").associate {
            val split = it.split("=")
            split[0] to split[1]
        }
    } catch (parseException: Exception) {
        throw IllegalArgumentException("wrong agent parameters: $this")
    }
}

private fun callbackRegister() {
    generateDefaultCallbacks().useContents {
        SetEventCallbacks(this.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
        null
    }
    SetEventCallbacks(gjavaVMGlob?.pointed?.callbackss?.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
    gjavaVMGlob?.pointed?.callbackss?.VMDeath = staticCFunction(::vmDeathEvent)
    enableJvmtiEventVmDeath()
    enableJvmtiEventVmInit()
    enableJvmtiEventClassFileLoadHook()
    enableJvmtiEventNativeMethodBind()
}


@Suppress("UNUSED_PARAMETER")
fun vmDeathEvent(jvmtiEnv: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?) {
    KotlinLogging.logger("vmDeathEvent").info { "vmDeathEvent" }
}


@Suppress("UNUSED_PARAMETER", "UNUSED")
@CName("Agent_OnUnload")
fun agentOnUnload(vmPointer: CPointer<JavaVMVar>) {
    KotlinLogging.logger("Agent_OnUnload").info { "Agent_OnUnload" }
}


val drillInstallationDir: String
    get() = exec { drillInstallationDir }