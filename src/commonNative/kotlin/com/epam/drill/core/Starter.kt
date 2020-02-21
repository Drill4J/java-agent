package com.epam.drill.core

import com.epam.drill.*
import com.epam.drill.agent.*
import com.epam.drill.common.*
import com.epam.drill.core.callbacks.classloading.*
import com.epam.drill.core.callbacks.vminit.*
import com.epam.drill.core.plugin.loader.*
import com.epam.drill.core.transport.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*
import kotlinx.serialization.*
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
private fun initAgentGlobals(vmPointer: CPointer<JavaVMVar>) = memScoped {
    vmGlobal.value = vmPointer.freeze()
    jvmti.value = getJvmti(vmPointer.pointed).value.freeze()
}

private fun runAgent(options: String?) {

    getClassesByConfig = {
        val packagesPrefixes = exec { agentConfig.packagesPrefixes }
        val (serviceClass, service) = dataService()
        val retrieveClassesData: jmethodID? =
            GetMethodID(serviceClass, "retrieveClassesData", "(Ljava/lang/String;)Ljava/lang/String;")
        val jsonClasses = CallObjectMethod(service, retrieveClassesData, NewStringUTF(packagesPrefixes))
        String.serializer().list parse (jsonClasses.toKString() ?: "[]")
    }

    setPackagesPrefixes = { prefixes -> exec { agentConfig.packagesPrefixes = prefixes } }

    sessionStorage = ::fillRequestToHolder
    drillSessionId = ::sessionId

    loadPlugin = ::loadPluginForJvm
    nativePlugin = { _, _, _ ->
        memScoped {
            //            val callbacks: jvmtiEventCallbacks? = gjavaVMGlob?.pointed?.callbackss
//            val reinterpret =
//                initPlugin?.reinterpret<CFunction<(CPointer<ByteVar>, CPointer<com.epam.drill.jvmapi.gen.jvmtiEnvVar>?, CPointer<JavaVMVar>?, CPointer<jvmtiEventCallbacks>?, CPointer<CFunction<(pluginId: CPointer<ByteVar>, message: CPointer<ByteVar>) -> Unit>>) -> COpaquePointer>>()
//            val id = pluginId.cstr.getPointer(this)
//            val jvmti = gdata?.pointed?.jvmti
//            val jvm = gjavaVMGlob?.pointed?.jvm
//            val clb = callbacks?.ptr
//            reinterpret!!(
//                id,
//                jvmti,
//                jvm,
//                clb,
//                sender
//            )?.asStableRef<NativePart<*>>()?.get()
            null
        }

    }



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
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, null)
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, null)
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, null)
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


fun MemScope.getJvmti(vm: JavaVMVar): CPointerVar<jvmtiEnvVar> {
    val jvmtiEnvPtr = alloc<CPointerVar<jvmtiEnvVar>>()
    vm.value!!.pointed.GetEnv!!(vm.ptr, jvmtiEnvPtr.ptr.reinterpret(), JVMTI_VERSION.convert())
    return jvmtiEnvPtr
}