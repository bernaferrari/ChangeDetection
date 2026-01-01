package com.bernaferrari.changedetection.util

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringEncodingForName
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun decodeByteArray(bytes: ByteArray, charsetName: String): String {
    val encoding = NSString.stringEncodingForName(charsetName.ifBlank { "UTF-8" })
        .takeIf { it != 0UL }
        ?: NSUTF8StringEncoding

    return bytes.decodeToString()
}
