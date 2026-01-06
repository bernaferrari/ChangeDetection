package com.bernaferrari.changedetection.util

import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.String

fun decodeByteArray(bytes: ByteArray, charsetName: String): String {
    val charset = runCatching {
        charsetName.takeIf { it.isNotBlank() }?.let { Charset.forName(it) }
    }.getOrNull() ?: Charsets.UTF_8

    return String(bytes, charset = charset)
}
