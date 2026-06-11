/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

import java.util.LinkedHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Android/JVM implementation using LinkedHashMap with LRU ordering
 * and ReentrantReadWriteLock for thread-safety.
 */
actual class SynchronizedCache<K : Any, V : Any> actual constructor(
    private val maxSize: Int,
) {
    private val cache =
        object : LinkedHashMap<K, V>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean = size > maxSize
        }

    private val lock = ReentrantReadWriteLock()

    actual operator fun get(key: K): V? = lock.read {
        cache[key]
    }

    actual operator fun set(
        key: K,
        value: V,
    ) = lock.write {
        cache[key] = value
    }

    actual fun clear() = lock.write {
        cache.clear()
    }

    actual val size: Int
        get() = lock.read { cache.size }
}
