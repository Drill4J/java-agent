package com.epam.drill.agent.jvmapi

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

@Suppress("NOTHING_TO_INLINE")
inline fun <T : CPointer<*>> CPointer<CPointerVarOf<T>>.sequenceOf(count: Int): Sequence<T> {
    var current = 0
    return generateSequence<T> {
        if (current == count) null
        else this[current++]
    }
}