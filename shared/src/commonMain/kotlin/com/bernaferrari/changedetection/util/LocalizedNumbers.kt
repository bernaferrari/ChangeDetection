/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

internal fun localizedInteger(value: Number): String = localizedNumber(value.toDouble(), maxFractionDigits = 0)

/** Accept digits produced by local keyboards consistently on JVM, iOS and Wasm. Reject signs, decimals and overflow. */
internal fun parseLocalizedWholeNumber(text: String): Long? {
    val digits = text.trim()
    if (digits.isEmpty()) return null
    val normalized = StringBuilder(digits.length)
    for (character in digits) {
        val digit = character.digitToIntOrNull() ?: return null
        normalized.append('0' + digit)
    }
    return normalized.toString().toLongOrNull()
}
