package com.epam.drill.core.concurrency

import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.native.concurrent.*

@SharedImmutable
private val dispatcher = newSingleThreadContext("BackgroundThread coroutine")

object BackgroundThread : CoroutineScope {

    operator fun <T> invoke(block: suspend () -> T) = launch { block() }

    override val coroutineContext: CoroutineContext = dispatcher

}