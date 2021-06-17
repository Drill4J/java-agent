package com.epam.drill.bootstrap

import platform.posix.*

actual fun agentLoad(path: String): Any? = dlopen(path, RTLD_LAZY)?.let { handle -> dlsym(handle, "Agent_OnLoad") }
