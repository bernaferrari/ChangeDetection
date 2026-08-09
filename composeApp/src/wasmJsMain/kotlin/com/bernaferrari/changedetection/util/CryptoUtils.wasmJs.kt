/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

import okio.ByteString.Companion.toByteString

actual object CryptoUtils {
    actual fun sha256(bytes: ByteArray): String = bytes.toByteString().sha256().hex()
}
