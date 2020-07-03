package com.epam.drill.logger

import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.logger.api.*


@Suppress("UNUSED", "UNUSED_PARAMETER")
@CName("Java_com_epam_drill_logger_NativeApi_getLogLevel")
fun getLogLevel(env: JNIEnv, thiz: jobject): jint = Logging.logLevel.code

@Suppress("UNUSED", "UNUSED_PARAMETER")
@CName("Java_com_epam_drill_logger_NativeApi_setLogLevel")
fun setLogLevel(env: JNIEnv, thiz: jobject, code: jint) {
    Logging.logLevel = code.toLogLevel()
}

@Suppress("UNUSED", "UNUSED_PARAMETER")
@CName("Java_com_epam_drill_logger_NativeApi_setFilename")
fun setFilename(env: JNIEnv, thiz: jobject, filename: jstring?) = withJSting {
    Logging.filename = filename?.toKString()
}

@Suppress("UNUSED", "UNUSED_PARAMETER")
@CName("Java_com_epam_drill_logger_NativeApi_output")
fun output(env: JNIEnv, thiz: jobject, message: jstring) = withJSting {
    Logging.output(message.toKString())
}
