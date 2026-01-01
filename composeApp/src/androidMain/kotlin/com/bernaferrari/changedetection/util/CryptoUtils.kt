package com.bernaferrari.changedetection.util

import java.security.MessageDigest

actual object CryptoUtils {
    actual fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }
}
