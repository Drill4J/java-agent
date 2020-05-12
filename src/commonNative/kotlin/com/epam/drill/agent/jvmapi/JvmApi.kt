package com.epam.drill.agent.jvmapi

import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*

inline fun <reified T : Any> instance(): Pair<jclass?, jobject?> {
    val name = T::class.qualifiedName!!.replace(".", "/")
    return instance(name)
}

fun instance(name: String): Pair<jclass?, jobject?> {
    val requestHolderClass = FindClass(name)
    val selfMethodId: jfieldID? = GetStaticFieldID(requestHolderClass, "INSTANCE", "L$name;")
    val requestHolder: jobject? = GetStaticObjectField(requestHolderClass, selfMethodId)
    return Pair(requestHolderClass, requestHolder)
}

fun jbyteArray?.readBytes() = this?.let { jbytes ->
    val length = GetArrayLength(jbytes)
    val buffer: COpaquePointer? = GetPrimitiveArrayCritical(jbytes, null)
    try {
        buffer?.readBytes(length)
    } finally {
        ReleasePrimitiveArrayCritical(jbytes, buffer, JNI_ABORT)
    }

}

inline fun withJSting(block: JStingConverter.() -> Unit) {
    val jStingConverter = JStingConverter()
    block(jStingConverter)
    jStingConverter.localStrings.forEach { (x, y) ->
        jni.ReleaseStringUTFChars!!(env, x, y)
    }
}

class JStingConverter {
    val localStrings = mutableMapOf<jstring, CPointer<ByteVar>?>()
    fun jstring.toKString(): String {
        val nativeString = jni.GetStringUTFChars!!(env, this, null)
        localStrings[this] = nativeString
        return nativeString?.toKString()!!
    }
}

fun jclass.signature(): String = memScoped {
    val ptrVar = alloc<CPointerVar<ByteVar>>()
    GetClassSignature(this@signature, ptrVar.ptr, null)
    ptrVar.value!!.toKString()
}

fun jclass.status(): UInt = memScoped {
    val alloc = alloc<jintVar>()
    GetClassStatus(this@status, alloc.ptr)
    alloc.value.toUInt()
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T : CPointer<*>> CPointer<CPointerVarOf<T>>.sequenceOf(count: Int): Sequence<T> {
    var current = 0
    return generateSequence<T> {
        if (current == count) null
        else this[current++]
    }
}
