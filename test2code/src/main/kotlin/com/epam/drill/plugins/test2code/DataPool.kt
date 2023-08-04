package com.epam.drill.plugins.test2code

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

interface DataPool<K, V> {
    fun hold(key: K, default: () -> V): V
    fun release(key: K, value: V)
    fun pollReleased(): Sequence<V>
}

class ConcurrentDataPool<K, V> : DataPool<K, V> {
    private val dataMap = ConcurrentHashMap<K, V>()
    private val released = ConcurrentLinkedQueue<V>()

    override fun hold(key: K, default: () -> V): V {
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