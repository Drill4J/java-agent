package com.epam.drill.core

import com.epam.drill.*
import com.epam.drill.agent.*
import com.epam.drill.agent.classloading.*
import com.epam.drill.common.*
import com.epam.drill.core.plugin.loader.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.request.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.serialization.builtins.*
import mu.*

@kotlin.native.concurrent.SharedImmutable
private val logger = KotlinLogging.logger("CallbackLogger")

@Suppress("unused")
@SharedImmutable
val CallbackRegister: Unit = run {
    getClassesByConfig = {
        runBlocking {
            when (waitForMultipleWebApps()) {
                null -> logger.warn {
                    "Apps: ${state.webApps.filterValues { !it }.keys} have not initialized in ${waitingTimeout}ms.. " +
                            "Please check the app names or increase the timeout"
                }
                else -> logger.info { "app is initialized" }
            }
            val packagesPrefixes = exec { agentConfig.packagesPrefixes }
            val (serviceClass, service) = dataService()
            val retrieveClassesData: jmethodID? =
                GetMethodID(serviceClass, "retrieveClassesData", "(Ljava/lang/String;)Ljava/lang/String;")
            val jsonClasses = CallObjectMethod(service, retrieveClassesData, NewStringUTF(packagesPrefixes))
            String.serializer().list parse (jsonClasses.toKString() ?: "[]")
        }
    }

    setPackagesPrefixes = { prefixes ->
        exec { agentConfig.packagesPrefixes = prefixes }
        val parsedPrefixes = (PackagesPrefixes.serializer() parse prefixes).packagesPrefixes
        state = state.copy(packagePrefixes = parsedPrefixes)
    }

    sessionStorage = RequestHolder::store
    drillRequest = RequestHolder::get

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
