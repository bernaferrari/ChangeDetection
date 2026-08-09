/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

actual class SynchronizedCache<K : Any, V : Any> actual constructor(
    private val maxSize: Int,
) {
    private val cache = mutableMapOf<K, V>()
    private val accessOrder = mutableListOf<K>()

    actual operator fun get(key: K): V? = cache[key]?.also {
        accessOrder.remove(key)
        accessOrder += key
    }

    actual operator fun set(key: K, value: V) {
        cache[key] = value
        accessOrder.remove(key)
        accessOrder += key
        while (cache.size > maxSize) cache.remove(accessOrder.removeAt(0))
    }

    actual fun clear() {
        cache.clear()
        accessOrder.clear()
    }

    actual val size: Int get() = cache.size
}
