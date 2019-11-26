package com.epam.drill.agent

import com.alibaba.ttl.threadpool.agent.*
import java.lang.instrument.*

fun premain(args: String?, instrumentation: Instrumentation) {
    TtlAgent.premain(args ?: "", instrumentation)
    println("Ttl agent is attached")
}