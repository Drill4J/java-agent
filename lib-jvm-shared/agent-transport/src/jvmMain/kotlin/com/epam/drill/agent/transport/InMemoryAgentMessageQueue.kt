/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.agent.transport

import com.epam.drill.agent.common.transport.AgentMessageDestination
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class InMemoryAgentMessageQueue(
    private val capacity: Long
) : AgentMessageQueue<ByteArray> {

    private val queue: BlockingQueue<Pair<AgentMessageDestination, ByteArray>> = LinkedBlockingQueue()
    private var bytesSize: Long = 0

    override fun add(e: Pair<AgentMessageDestination, ByteArray>): Boolean = e
        .takeIf(::isCapable)
        ?.also(::increaseSize)
        ?.run {
            queue.add(this)
        } ?: throw IllegalArgumentException("Queue is out of capacity")

    override fun offer(e: Pair<AgentMessageDestination, ByteArray>) = e
        .takeIf(::isCapable)
        ?.also(::increaseSize)
        ?.run {
            queue.offer(this)
        } ?: false

    override fun remove() = queue.remove()
        .also(::decreaseSize)

    override fun poll() = queue.poll()
        ?.also(::decreaseSize)

    override fun poll(timeout: Long, unit: TimeUnit) = queue.poll(timeout, unit)
        ?.also(::decreaseSize)

    override fun peek() = queue.peek()

    override fun size(): Int = queue.size

    fun bytesSize(): Long = bytesSize

    private fun sizeOf(e: Pair<AgentMessageDestination, ByteArray>) =
        e.first.type.length + e.first.target.length + e.second.size.toLong()

    private fun increaseSize(e: Pair<AgentMessageDestination, ByteArray>) {
        bytesSize += sizeOf(e)
    }

    private fun decreaseSize(e: Pair<AgentMessageDestination, ByteArray>) {
        bytesSize -= sizeOf(e)
    }

    private fun isCapable(e: Pair<AgentMessageDestination, ByteArray>) =
        bytesSize + sizeOf(e) <= capacity

}
