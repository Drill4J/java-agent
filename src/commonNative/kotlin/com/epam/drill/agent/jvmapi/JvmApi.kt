package com.epam.drill.agent.jvmapi

import com.epam.drill.jvmapi.gen.*

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