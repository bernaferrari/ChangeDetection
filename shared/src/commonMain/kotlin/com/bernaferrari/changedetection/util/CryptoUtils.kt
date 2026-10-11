/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

expect object CryptoUtils {
    fun sha256(bytes: ByteArray): String
}
