/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

import java.security.MessageDigest

actual object CryptoUtils {
    private val hexChars = "0123456789abcdef".toCharArray()

    actual fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        val result = CharArray(hash.size * 2)
        hash.forEachIndexed { index, byte ->
            val value = byte.toInt() and 0xFF
            result[index * 2] = hexChars[value ushr 4]
            result[index * 2 + 1] = hexChars[value and 0x0F]
        }
        return result.concatToString()
    }
}
