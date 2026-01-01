package com.bernaferrari.changedetection.util

expect object CryptoUtils {
    fun sha256(bytes: ByteArray): String
}
