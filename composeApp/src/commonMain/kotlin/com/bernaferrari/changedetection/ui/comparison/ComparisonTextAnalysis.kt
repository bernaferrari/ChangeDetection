/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

private val htmlTagPattern = Regex("<[^>]+>")
private val whitespacePattern = Regex("\\s+")

/**
 * A small, stable similarity signal for the rendered-HTML comparison header.
 *
 * This intentionally measures shared normalized words rather than attempting to duplicate the
 * full diff engine. The function remains pure so its behavior is easy to characterize in tests.
 */
internal fun calculateRenderedSimilarity(old: String, new: String): Int {
    if (old == new) return 100
    if (old.isEmpty() || new.isEmpty()) return 0

    val oldWords = old.normalizedComparisonWords()
    val newWords = new.normalizedComparisonWords()

    if (oldWords == newWords) return 100
    if (oldWords.isEmpty() || newWords.isEmpty()) return 0

    val intersection = oldWords.intersect(newWords).size
    val union = oldWords.union(newWords).size
    return ((intersection.toDouble() / union.toDouble()) * 100).toInt()
}

private fun String.normalizedComparisonWords(): Set<String> = replace(htmlTagPattern, " ")
    .replace(whitespacePattern, " ")
    .trim()
    .lowercase()
    .split(whitespacePattern)
    .filter(String::isNotBlank)
    .toSet()
