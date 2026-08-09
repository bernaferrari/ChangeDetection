/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.difflib

import com.bernaferrari.difflib.patch.ChangeDelta
import com.bernaferrari.difflib.patch.Chunk
import com.bernaferrari.difflib.patch.Patch

/** Small multiplatform line diff used by the comparison UI on Android, iOS, and Wasm. */
object DiffUtils {
    private const val MaxLcsCells = 2_000_000L

    fun <T> diff(original: List<T>, revised: List<T>): Patch<T> {
        if (original == revised) return Patch()
        return if (original.size.toLong() * revised.size <= MaxLcsCells) {
            lcsDiff(original, revised)
        } else {
            boundedDiff(original, revised)
        }
    }

    private fun <T> lcsDiff(original: List<T>, revised: List<T>): Patch<T> {
        val rows = original.size + 1
        val columns = revised.size + 1
        val lcs = Array(rows) { IntArray(columns) }
        for (oldIndex in original.lastIndex downTo 0) {
            for (newIndex in revised.lastIndex downTo 0) {
                lcs[oldIndex][newIndex] = if (original[oldIndex] == revised[newIndex]) {
                    lcs[oldIndex + 1][newIndex + 1] + 1
                } else {
                    maxOf(lcs[oldIndex + 1][newIndex], lcs[oldIndex][newIndex + 1])
                }
            }
        }

        val patch = Patch<T>()
        var oldIndex = 0
        var newIndex = 0
        while (oldIndex < original.size || newIndex < revised.size) {
            if (oldIndex < original.size && newIndex < revised.size && original[oldIndex] == revised[newIndex]) {
                oldIndex++
                newIndex++
                continue
            }

            val oldStart = oldIndex
            val newStart = newIndex
            val removed = mutableListOf<T>()
            val added = mutableListOf<T>()
            while (
                oldIndex < original.size || newIndex < revised.size
            ) {
                if (oldIndex < original.size && newIndex < revised.size && original[oldIndex] == revised[newIndex]) break
                val shouldAdd = newIndex < revised.size &&
                    (oldIndex == original.size || lcs[oldIndex][newIndex + 1] >= lcs[oldIndex + 1][newIndex])
                if (shouldAdd) {
                    added += revised[newIndex++]
                } else {
                    removed += original[oldIndex++]
                }
            }
            patch.addDelta(ChangeDelta(Chunk(oldStart, removed), Chunk(newStart, added)))
        }
        return patch
    }

    private fun <T> boundedDiff(original: List<T>, revised: List<T>): Patch<T> {
        var prefix = 0
        while (prefix < original.size && prefix < revised.size && original[prefix] == revised[prefix]) prefix++
        var suffix = 0
        while (
            suffix < original.size - prefix &&
            suffix < revised.size - prefix &&
            original[original.lastIndex - suffix] == revised[revised.lastIndex - suffix]
        ) {
            suffix++
        }
        return Patch<T>().apply {
            addDelta(
                ChangeDelta(
                    Chunk(prefix, original.subList(prefix, original.size - suffix)),
                    Chunk(prefix, revised.subList(prefix, revised.size - suffix)),
                ),
            )
        }
    }
}
