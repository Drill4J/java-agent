package com.epam.drill.agent

fun String.matches(others: Iterable<String>, thisOffset: Int = 0): Boolean = others.any {
    regionMatches(thisOffset, it, 0, it.length)
} && others.none {
    it.startsWith('!') && regionMatches(thisOffset, it, 1, it.length - 1)
}
