package com.epam.drill.core

import com.epam.drill.*
import com.epam.drill.agent.*
import com.epam.drill.common.*
import com.epam.drill.core.plugin.loader.*
import com.epam.drill.core.transport.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import mu.*

@kotlin.native.concurrent.SharedImmutable
private val logger = KotlinLogging.logger("CallbackLogger")

private const val waitingTimeout: Long = 90000 //move to config or admin
@Suppress("unused")
@SharedImmutable
val CallbackRegister: Unit = run {
    getClassesByConfig = {
        runBlocking {
            withTimeoutOrNull(waitingTimeout) {
                while (!state.isWebAppInitialized) {
                    delay(1500)
                    logger.debug { "Waiting for Web app initialization" }
                }
            }
            val packagesPrefixes = exec { agentConfig.packagesPrefixes }
            val (serviceClass, service) = dataService()
            val retrieveClassesData: jmethodID? =
                GetMethodID(serviceClass, "retrieveClassesData", "(Ljava/lang/String;)Ljava/lang/String;")
            val jsonClasses = CallObjectMethod(service, retrieveClassesData, NewStringUTF(packagesPrefixes))
            String.serializer().list parse (jsonClasses.toKString() ?: "[]")
        }
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

}