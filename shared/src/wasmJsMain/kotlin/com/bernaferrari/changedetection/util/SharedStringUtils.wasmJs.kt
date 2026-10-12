package com.bernaferrari.changedetection.util

actual fun decodeByteArray(bytes: ByteArray, charsetName: String): String = bytes.decodeToString()
