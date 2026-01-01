package com.bernaferrari.changedetection.util

import java.nio.charset.Charset

actual fun decodeByteArray(bytes: ByteArray, charsetName: String): String {
    val charset = runCatching {
        charsetName.takeIf { it.isNotBlank() }?.let { Charset.forName(it) }
    }.getOrNull() ?: Charsets.UTF_8

    return runCatching { String(bytes, charset) }
        .getOrElse { String(bytes, Charsets.UTF_8) }
}
