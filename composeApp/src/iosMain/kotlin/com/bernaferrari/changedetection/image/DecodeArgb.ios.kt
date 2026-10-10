/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.image

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

/** Decoded in strips of rows and area-averaged down to [targetWidth]; memory stays bounded for tall pages. */
internal actual suspend fun decodeArgbAtWidth(bytes: ByteArray, targetWidth: Int): Triple<IntArray, Int, Int>? = runCatching {
    val image = Image.makeFromEncoded(bytes)
    try {
        val sourceWidth = image.width
        val sourceHeight = minOf(image.height, (MAX_ROWS.toLong() * sourceWidth / minOf(targetWidth, sourceWidth).coerceAtLeast(1)).toInt())
        if (sourceWidth <= 0 || sourceHeight <= 0) return@runCatching null
        val scaler = BoxDownscaler(sourceWidth, sourceHeight, targetWidth)
        val strip = Bitmap()
        strip.allocPixels(ImageInfo(sourceWidth, STRIP_ROWS, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL))
        val row = IntArray(sourceWidth)
        try {
            var top = 0
            while (top < sourceHeight) {
                val rows = minOf(STRIP_ROWS, sourceHeight - top)
                check(image.readPixels(strip, 0, top)) { "Could not read the capture" }
                val raw = strip.readPixels() ?: error("Could not read the capture")
                for (r in 0 until rows) {
                    val base = r * sourceWidth * 4
                    for (x in 0 until sourceWidth) {
                        val o = base + x * 4
                        row[x] = (0xFF shl 24) or ((raw[o].toInt() and 255) shl 16) or ((raw[o + 1].toInt() and 255) shl 8) or (raw[o + 2].toInt() and 255)
                    }
                    scaler.addRow(top + r, row)
                }
                top += rows
            }
        } finally {
            strip.close()
        }
        Triple(scaler.result(), scaler.width, scaler.height)
    } finally {
        image.close()
    }
}.getOrNull()

private const val MAX_ROWS = 12_000
private const val STRIP_ROWS = 512
