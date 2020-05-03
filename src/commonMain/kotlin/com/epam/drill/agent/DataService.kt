package com.epam.drill.agent

expect object DataService {

    fun retrieveClassesData(config: String): ByteArray
}
