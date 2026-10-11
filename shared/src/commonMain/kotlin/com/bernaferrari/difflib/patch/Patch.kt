/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.difflib.patch

data class Chunk<T>(
    val position: Int,
    val lines: List<T>,
    val changePosition: List<Int> = emptyList(),
) {
    fun size(): Int = lines.size
}

abstract class AbstractDelta<T>(
    val source: Chunk<T>,
    val target: Chunk<T>,
)

class ChangeDelta<T>(
    source: Chunk<T>,
    target: Chunk<T>,
) : AbstractDelta<T>(source, target)

class Patch<T> {
    private val mutableDeltas = mutableListOf<AbstractDelta<T>>()
    val deltas: List<AbstractDelta<T>> get() = mutableDeltas

    fun addDelta(delta: AbstractDelta<T>) {
        mutableDeltas += delta
    }
}
