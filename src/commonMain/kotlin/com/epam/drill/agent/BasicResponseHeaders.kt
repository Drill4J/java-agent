package com.epam.drill.agent

expect object BasicResponseHeaders {
    fun adminAddressHeader(): String?
    fun retrieveAdminAddress(): String?
    fun idHeaderConfigKey(): String?
    fun idHeaderConfigValue(): String?
}
