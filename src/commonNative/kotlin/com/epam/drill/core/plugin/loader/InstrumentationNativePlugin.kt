package com.epam.drill.core.plugin.loader

import com.epam.drill.common.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.plugin.api.processing.*
import kotlinx.cinterop.*

class InstrumentationNativePlugin(
    pluginId: String,
    pluginApiClass: jclass,
    userPlugin: jobject,
    pluginConfig: PluginMetadata,
    internal val qs: jmethodID? = GetMethodID(pluginApiClass, "instrument", "(Ljava/lang/String;[B)[B")
) : GenericNativePlugin(pluginId, pluginApiClass, userPlugin, pluginConfig), Instrumenter {

    override fun instrument(className: String, initialBytes: ByteArray) = memScoped<ByteArray?> {
        val classDataLen = initialBytes.size
        val newByteArray: jbyteArray? = NewByteArray(classDataLen)
        initialBytes.usePinned {
            SetByteArrayRegion(newByteArray, 0, classDataLen, it.addressOf(0))
        }
        val bytes = CallObjectMethod(userPlugin, qs, NewStringUTF(className), newByteArray) ?: return null
        val length = GetArrayLength(bytes)
        val buffer: COpaquePointer? = GetPrimitiveArrayCritical(bytes, null)
        try {
            return ByteArray(length).apply {
                usePinned { destination ->
                    platform.posix.memcpy(
                        destination.addressOf(0),
                        buffer,
                        length.convert()
                    )
                }
            }
        } finally { ReleasePrimitiveArrayCritical(bytes, buffer, JNI_ABORT) }
    }

}
