@file:Suppress("unused")

package com.epam.drill.request

expect object RequestHolder {

    fun dump(): ByteArray?
    fun store(rawRequest: String, pattern: String?)

}
