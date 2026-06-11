/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

/**
 * Decodes a ByteArray to a String.
 * Note: In KMP commonMain, only UTF-8 is reliably supported.
 * Non-UTF-8 charsets require platform-specific implementations.
 */
fun decodeByteArray(
    bytes: ByteArray,
    charsetName: String,
): String {
    // In common code, we can only reliably use UTF-8
    // If specific charset handling is needed, use expect/actual pattern
    return bytes.decodeToString()
}
