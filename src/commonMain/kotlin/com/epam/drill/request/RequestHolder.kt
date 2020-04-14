@file:Suppress("unused")

package com.epam.drill.request

expect object RequestHolder {

    fun drillRequest(): Any?
    fun storeRequest(rawRequest: String, pattern: String?)

}
