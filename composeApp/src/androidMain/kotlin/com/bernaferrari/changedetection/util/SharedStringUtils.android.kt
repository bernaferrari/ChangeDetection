/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
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
