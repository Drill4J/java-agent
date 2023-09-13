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
package com.epam.drill.test2code.coverage

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Data pool for getting objects by key and releasing them after use
 * @param K the key
 * @param V the object
 */
interface DataPool<K: Any, V> {
    /**
     * Get an object from the pool by the key if it is contained there, or put a default value into the pool
     * @param key a key
     * @param default a function that returns a default value
     * @return an object that is obtained by key
     */
    fun getOrPut(key: K, default: () -> V): V

    /**
     * Get an object from the pool by the key
     * @param key a key
     * @return object corresponding to the key
     */
    fun get(key: K): V?

    /**
     * Release the used object by key and move it into the released queue
     * @param key a key
     * @param value a used object
     */
    fun release(key: K)

    /**
     * Poll objects from the released queue
     * @return a sequence of released objects
     */
    fun pollReleased(): Sequence<V>
}

/**
 * Thread safety implementation of DataPool
 * @see DataPool
 */
class ConcurrentDataPool<K : Any, V> : DataPool<K, V> {
    private val dataMap = ConcurrentHashMap<K, V>()
    private val released = ConcurrentLinkedQueue<V>()

    override fun getOrPut(key: K, default: () -> V): V {
        return dataMap.getOrPut(key, default)
    }

    override fun release(key: K) {
        val value = dataMap[key]
        dataMap.remove(key)
        value?.apply { released.add(value) }
    }

    override fun get(key: K): V? {
        return dataMap[key]
    }

    override fun pollReleased(): Sequence<V> {
        return generateSequence { released.poll() }
    }
}