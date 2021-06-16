package com.epam.drill.bootstrap

import platform.posix.*

fun agentLoad(path: String): Any? = dlopen(path, RTLD_LAZY)?.let { handle -> dlsym(handle, "Agent_OnLoad") }
