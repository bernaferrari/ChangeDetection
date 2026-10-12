package com.bernaferrari.changedetection.util

import java.nio.charset.Charset

actual fun decodeByteArray(
    bytes: ByteArray,
    charsetName: String,
): String = runCatching {
    bytes.toString(Charset.forName(charsetName))
}.getOrElse {
    bytes.decodeToString()
}
