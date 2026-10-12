package com.bernaferrari.changedetection.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSLock

/**
 * iOS implementation using insertion/access order tracked explicitly.
 */
@OptIn(ExperimentalForeignApi::class)
actual class SynchronizedCache<K : Any, V : Any> actual constructor(
    private val maxSize: Int,
) {
    private val cache = mutableMapOf<K, V>()
    private val accessOrder = mutableListOf<K>()
    private val lock = NSLock()

    actual operator fun get(key: K): V? {
        lock.lock()
        try {
            val value = cache[key] ?: return null
            markUsed(key)
            return value
        } finally {
            lock.unlock()
        }
    }

    actual operator fun set(
        key: K,
        value: V,
    ) {
        lock.lock()
        try {
            cache[key] = value
            markUsed(key)
            trimToMaxSize()
        } finally {
            lock.unlock()
        }
    }

    actual fun clear() {
        lock.lock()
        try {
            cache.clear()
            accessOrder.clear()
        } finally {
            lock.unlock()
        }
    }

    actual val size: Int
        get() {
            lock.lock()
            try {
                return cache.size
            } finally {
                lock.unlock()
            }
        }

    private fun markUsed(key: K) {
        accessOrder.remove(key)
        accessOrder.add(key)
    }

    private fun trimToMaxSize() {
        while (cache.size > maxSize && accessOrder.isNotEmpty()) {
            cache.remove(accessOrder.removeAt(0))
        }
    }
}
