/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.bernaferrari.changedetection.util

import kotlinx.cinterop.*
import platform.Foundation.*

actual fun decodeByteArray(
    bytes: ByteArray,
    charsetName: String,
): String = NSString.create(bytes.toNSData(), charsetName.toNSStringEncoding())?.toString() ?: bytes.decodeToString()

private fun String.toNSStringEncoding(): NSStringEncoding = when (uppercase().replace('_', '-').replace(" ", "")) {
    "US-ASCII", "ASCII" -> NSASCIIStringEncoding
    "ISO-8859-1", "ISO8859-1", "LATIN-1", "LATIN1" -> NSISOLatin1StringEncoding
    "WINDOWS-1252", "CP1252" -> NSWindowsCP1252StringEncoding
    else -> NSUTF8StringEncoding
}

private fun ByteArray.toNSData(): NSData = memScoped {
    if (isEmpty()) return NSData.data()

    val data = NSMutableData.dataWithCapacity(size.toULong())!!
    data.appendBytes(allocArrayOf(this@toNSData), size.toULong())
    data
}
