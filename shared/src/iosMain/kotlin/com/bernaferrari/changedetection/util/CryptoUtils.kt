@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.bernaferrari.changedetection.util

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.CoreCrypto.*

actual object CryptoUtils {
    actual fun sha256(bytes: ByteArray): String = memScoped {
        val digest = allocArray<UByteVar>(CC_SHA256_DIGEST_LENGTH)

        CC_SHA256(
            bytes.refTo(0),
            bytes.size.toUInt(),
            digest,
        )

        val result = StringBuilder()
        for (i in 0 until CC_SHA256_DIGEST_LENGTH) {
            val b = digest[i].toInt() and 0xFF
            result.append(b.toString(16).padStart(2, '0'))
        }
        result.toString()
    }
}
