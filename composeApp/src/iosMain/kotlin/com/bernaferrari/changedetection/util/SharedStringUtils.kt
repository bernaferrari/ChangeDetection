package com.bernaferrari.changedetection.util

actual fun decodeByteArray(bytes: ByteArray, charsetName: String): String {
    // TODO: Implement proper charset support for iOS using NSString encodings if needed
    return bytes.decodeToString()
}
