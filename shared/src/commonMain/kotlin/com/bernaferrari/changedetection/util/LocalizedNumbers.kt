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
        val zero = when (character) {
            in '0'..'9' -> '0'
            in '०'..'९' -> '०'
            in '০'..'৯' -> '০'
            in '０'..'９' -> '０'
            in '٠'..'٩' -> '٠'
            in '۰'..'۹' -> '۰'
            else -> return null
        }
        normalized.append('0' + (character - zero))
    }
    return normalized.toString().toLongOrNull()
}
