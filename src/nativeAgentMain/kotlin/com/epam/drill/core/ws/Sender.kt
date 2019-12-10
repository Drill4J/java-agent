package com.epam.drill.core.ws

import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.native.concurrent.SharedImmutable

@SharedImmutable
private val dispatcher = newSingleThreadContext("sender coroutine")

object Sender : CoroutineScope {

    operator fun invoke(block: suspend () -> Unit) {
        launch { block() }
    }

    fun send(message: String) {
        launch {
            msChannel.send(message)
        }
    }

    override val coroutineContext: CoroutineContext = dispatcher

}