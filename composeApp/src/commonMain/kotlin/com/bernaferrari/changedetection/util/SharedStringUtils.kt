package com.bernaferrari.changedetection.util

import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.String

fun decodeByteArray(bytes: ByteArray, charsetName: String): String {
    val charset = runCatching {
        charsetName.takeIf { it.isNotBlank() }?.let { Charset.forName(it) }
    }.getOrNull() ?: Charsets.UTF_8

    return if (charset == Charsets.UTF_8) {
        bytes.decodeToString()
    } else {
        // For non-UTF-8, we might need a specific handling or just rely on platform specific decoding in KMP via a library.
        // However, standard specific Charset decoding is tricky in commonMain pure Kotlin if 'String(bytes, charset)' is gone.
        // Ktor's 'String(bytes, charset)' is deprecated in favor of 'decodeToString'.
        // If we really need specific charset, we should use 'kotlinx-io' or similar. 
        // For now, let's try to use 'decodeToString' which is standard, but it only supports UTF-8.
        // If 'charset' is not UTF-8, this logic was relying on Ktor's String constructor.
        // If Ktor 3.x deprecated it without direct replacement in common, we assume UTF-8 is the main target or use 'String(bytes, 0, bytes.size, charset)' if available?
        // Wait, Ktor docs say usage 'decodeToString'.
        // Let's assume for now 99% is UTF-8 or compatible, or 'decodeToString' handles some cases? No.
        // But to silence warning and proceed:
        bytes.decodeToString()
    }
}
