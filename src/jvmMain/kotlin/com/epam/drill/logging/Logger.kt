package com.epam.drill.logging

import java.util.logging.*

operator fun Logger.invoke(level: Level, msg: () -> String) {
    if (isLoggable(level)) {
        log(level, msg())
    }
}
