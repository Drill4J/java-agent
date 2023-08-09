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
package com.epam.drill.plugins.test2code.coverage

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

interface DataPool<K, V> {
    fun getOrPut(key: K, default: () -> V): V
    fun release(key: K, value: V)
    fun pollReleased(): Sequence<V>
}

class ConcurrentDataPool<K, V> : DataPool<K, V> {
    private val dataMap = ConcurrentHashMap<K, V>()
    private val released = ConcurrentLinkedQueue<V>()

    override fun getOrPut(key: K, default: () -> V): V {
        return dataMap.getOrPut(key, default)
    }

    override fun release(key: K, value: V) {
        dataMap.remove(key)
        released.add(value)
    }

    override fun pollReleased(): Sequence<V> {
        return generateSequence { released.poll() }
    }
}