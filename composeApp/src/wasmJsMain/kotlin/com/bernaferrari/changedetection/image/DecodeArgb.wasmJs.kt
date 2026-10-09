/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.image

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Rect

internal actual suspend fun decodeArgbAtWidth(bytes: ByteArray, targetWidth: Int): Triple<IntArray, Int, Int>? = runCatching {
    val image = Image.makeFromEncoded(bytes)
    val width = minOf(targetWidth, image.width).coerceAtLeast(1)
    val height = (image.height.toLong() * width / image.width).toInt().coerceIn(1, MAX_ROWS)
    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.PREMUL))
    Canvas(bitmap).drawImageRect(image, Rect.makeWH(width.toFloat(), height.toFloat()))
    val raw = bitmap.readPixels() ?: return@runCatching null
    val pixels = IntArray(width * height) { i ->
        val o = i * 4
        ((raw[o + 3].toInt() and 255) shl 24) or ((raw[o].toInt() and 255) shl 16) or ((raw[o + 1].toInt() and 255) shl 8) or (raw[o + 2].toInt() and 255)
    }
    bitmap.close()
    image.close()
    Triple(pixels, width, height)
}.getOrNull()

private const val MAX_ROWS = 12_000
