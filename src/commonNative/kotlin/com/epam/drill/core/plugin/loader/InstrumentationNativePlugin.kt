package com.epam.drill.core.plugin.loader

import com.epam.drill.common.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.plugin.api.processing.*
import io.ktor.utils.io.bits.*
import kotlinx.cinterop.*
import kotlin.test.*

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
        SetByteArrayRegion(newByteArray, 0, classDataLen, getBytes(newByteArray, initialBytes))
        val callObjectMethod = CallObjectMethod(userPlugin, qs, NewStringUTF(className), newByteArray) ?: return null
        val getByteArrayElements: CPointer<ByteVarOf<jbyte>>? = GetByteArrayElements(callObjectMethod, null)
        val readBytes = ByteArray(GetArrayLength(callObjectMethod)).apply {
            Memory.of(getByteArrayElements!!, classDataLen).loadByteArray(0, this)
        }
        DeleteLocalRef(newByteArray)
        ReleaseByteArrayElements(callObjectMethod, getByteArrayElements, JNI_ABORT)
        readBytes
    }

    private fun getBytes(newByteArray: jbyteArray?, classData: ByteArray): CPointer<jbyteVar>? {
        val bytess: CPointer<jbyteVar> = GetByteArrayElements(newByteArray, null) ?: fail("Can't alloc array")
        classData.useMemory(0, classData.size) {
            it.copyTo(Memory.of(bytess, classData.size), 0, classData.size, 0)
        }
        return bytess
    }
}
