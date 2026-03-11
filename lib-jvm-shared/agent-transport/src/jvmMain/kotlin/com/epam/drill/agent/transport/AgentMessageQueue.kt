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
import java.util.concurrent.TimeUnit

/**
 * A queue interface for storing serialized messages when transport in unavailable state.
 * Messages are stored with corresponding [AgentMessageDestination] object.
 * It has the same method signatures as [java.util.Queue] interface.
 *
 * It's used to store messages after serialization by [AgentMessageSerializer] and
 * destination mapping by [AgentMessageDestinationMapper] before sending by [AgentMessageTransport].
 *
 * @see AgentMessageDestination
 * @see AgentMessageSerializer
 * @see AgentMessageDestinationMapper
 * @see AgentMessageTransport
 * @see java.util.Queue
 */
interface AgentMessageQueue<T> {

    /**
     * Inserts the specified element into this queue if it is possible to do so
     * immediately without violating capacity restrictions, returning
     * `true` upon success and throwing an `IllegalStateException`
     * if no space is currently available.
     *
     * @param e the element to add
     * @return `true` (as specified by [java.util.Collection.add])
     * @throws IllegalStateException if the element cannot be added at this
     * time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     * prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null and
     * this queue does not permit null elements
     * @throws IllegalArgumentException if some property of this element
     * prevents it from being added to this queue
     */
    fun add(e: Pair<AgentMessageDestination, T>): Boolean

    /**
     * Inserts the specified element into this queue if it is possible to do
     * so immediately without violating capacity restrictions.
     * When using a capacity-restricted queue, this method is generally
     * preferable to [.add], which can fail to insert an element only
     * by throwing an exception.
     *
     * @param e the element to add
     * @return `true` if the element was added to this queue, else
     * `false`
     * @throws ClassCastException if the class of the specified element
     * prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null and
     * this queue does not permit null elements
     * @throws IllegalArgumentException if some property of this element
     * prevents it from being added to this queue
     */
    fun offer(e: Pair<AgentMessageDestination, T>): Boolean

    /**
     * Retrieves and removes the head of this queue.  This method differs
     * from [poll][.poll] only in that it throws an exception if this
     * queue is empty.
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    fun remove(): Pair<AgentMessageDestination, T>

    /**
     * Retrieves and removes the head of this queue,
     * or returns `null` if this queue is empty.
     *
     * @return the head of this queue, or `null` if this queue is empty
     */
    fun poll(): Pair<AgentMessageDestination, T>?

    /**
     * Retrieves and removes the head of this queue, waiting up to the specified wait time if necessary
     */
    fun poll(timeout: Long, unit: TimeUnit): Pair<AgentMessageDestination, T>?

    /**
     * Retrieves, but does not remove, the head of this queue,
     * or returns `null` if this queue is empty.
     *
     * @return the head of this queue, or `null` if this queue is empty
     */
    fun peek(): Pair<AgentMessageDestination, T>?

    /**
     * Returns the number of elements in this collection.  If this collection
     * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of elements in this collection
     */
    fun size(): Int

}
