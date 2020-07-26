package com.epam.drill.request

expect object RequestHolder {
    fun store(drillRequest: ByteArray)
    fun dump(): ByteArray?
    fun closeSession()
}
