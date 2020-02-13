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
    private val qs: jmethodID? = GetMethodID(pluginApiClass, "instrument", "(Ljava/lang/String;[B)[B")
) : GenericNativePlugin(pluginId, pluginApiClass, userPlugin, pluginConfig),
    InstrumentationPlugin {


    override fun instrument(className: String, initialBytes: ByteArray) = memScoped {
        val classDataLen = initialBytes.size
        val newByteArray: jbyteArray? = NewByteArray(classDataLen)
        SetByteArrayRegion(
            newByteArray, 0, classDataLen,
            getBytes(newByteArray, initialBytes)
        )

        val callObjectMethod = CallObjectMethod(userPlugin, qs, NewStringUTF(className), newByteArray) ?: return null

        val size = GetArrayLength(callObjectMethod)
        val getByteArrayElements: CPointer<ByteVarOf<jbyte>>? = GetByteArrayElements(callObjectMethod, null)
        val readBytes = getByteArrayElements?.readBytes(size)
        DeleteLocalRef(newByteArray)
        readBytes
    }

    override fun retransform() {
        CallVoidMethodA(userPlugin, GetMethodID(pluginApiClass, "retransform", "()V"), null)
    }

    private fun getBytes(
        newByteArray: jbyteArray?,
        classData: ByteArray
    ): CPointer<jbyteVar>? {
        val bytess: CPointer<jbyteVar>? = GetByteArrayElements(newByteArray, null)
        classData.forEachIndexed { index, byte ->
            bytess!![index] = byte
        }
        return bytess
    }
}
