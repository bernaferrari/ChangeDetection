package com.bernaferrari.changedetection.util

/**
 * Platform-specific function to decode a byte array to a string with the given charset.
 */
expect fun decodeByteArray(bytes: ByteArray, charsetName: String): String
