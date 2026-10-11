/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

/**
 * Thread-safe LRU cache interface for KMP.
 * Uses expect/actual for platform-specific implementations.
 */
expect class SynchronizedCache<K : Any, V : Any>(
    maxSize: Int,
) {
    operator fun get(key: K): V?

    operator fun set(
        key: K,
        value: V,
    )

    fun clear()

    val size: Int
}
