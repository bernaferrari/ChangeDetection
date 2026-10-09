/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory

internal actual suspend fun decodeArgbAtWidth(bytes: ByteArray, targetWidth: Int): Triple<IntArray, Int, Int>? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= targetWidth) sample *= 2
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return@runCatching null
    val width = minOf(targetWidth, decoded.width)
    val height = (decoded.height.toLong() * width / decoded.width).toInt().coerceIn(1, 12_000)
    val scaled = if (decoded.width == width && decoded.height == height) decoded else Bitmap.createScaledBitmap(decoded, width, height, false)
    val pixels = IntArray(width * height)
    scaled.getPixels(pixels, 0, width, 0, 0, width, height)
    if (scaled !== decoded) scaled.recycle()
    decoded.recycle()
    Triple(pixels, width, height)
}.getOrNull()
