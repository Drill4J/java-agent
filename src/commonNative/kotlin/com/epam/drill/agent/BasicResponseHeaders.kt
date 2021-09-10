package com.epam.drill.agent

import com.epam.drill.core.transport.*

actual object BasicResponseHeaders {
    private val idHeaderPair = idHeaderPairFromConfig()

    actual fun adminAddressHeader(): String? = "drill-admin-url"

    actual fun retrieveAdminAddress(): String? = retrieveAdminUrl()

    actual fun idHeaderConfigKey(): String? = idHeaderPair.first

    actual fun idHeaderConfigValue(): String? = idHeaderPair.second
}
