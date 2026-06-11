/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

interface FileStorage {
    fun writeBytes(
        fileName: String,
        bytes: ByteArray,
    )

    fun readBytes(fileName: String): ByteArray

    fun deleteFile(fileName: String): Boolean

    fun fileExists(fileName: String): Boolean
}
