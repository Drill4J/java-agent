package com.epam.drill.request

expect object RequestHolder {
    fun init(isAsync: Boolean)
    fun store(drillRequest: ByteArray)
    fun dump(): ByteArray?
    fun closeSession()
}
