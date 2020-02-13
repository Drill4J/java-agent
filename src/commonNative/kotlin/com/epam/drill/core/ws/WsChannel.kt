package com.epam.drill.core.ws

import kotlinx.coroutines.channels.*
import kotlin.native.ThreadLocal
import kotlin.native.concurrent.*

@SharedImmutable
private val worker = Worker.start(true)

@ThreadLocal
private val messageChannel = Channel<String>()

val msChannel: Channel<String>
    get() = worker.execute(TransferMode.UNSAFE, {}) { messageChannel }.result
