package com.epam.drill.agent

import com.epam.drill.kni.*

@Kni
actual object BasicResponseHeaders {
    actual external fun adminAddressHeader(): String?
    actual external fun retrieveAdminAddress(): String?
    actual external fun idHeaderConfigKey(): String?
    actual external fun idHeaderConfigValue(): String?
}
