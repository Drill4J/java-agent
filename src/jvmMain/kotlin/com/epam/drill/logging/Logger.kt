package com.epam.drill.logging

enum class Level {
    INFO
}

fun log(level: Level, msg: () -> String) {
    println("[DRILL][${level.name}]: ${msg()}")
}
