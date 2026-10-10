/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** Region decoders for the last captures; each crop then reads only its own rows. */
private val decoders = LinkedHashMap<ByteArray, BitmapRegionDecoder>()

internal actual suspend fun decodeCropPlatform(bytes: ByteArray, left: Float, top: Float, right: Float, bottom: Float, targetWidth: Int): ImageBitmap? {
    val decoder = synchronized(decoders) {
        decoders[bytes] ?: run {
            @Suppress("DEPRECATION")
            BitmapRegionDecoder.newInstance(bytes, 0, bytes.size, false)
        }?.also {
            decoders[bytes] = it
            while (decoders.size > 2) decoders.remove(decoders.keys.first())?.recycle()
        }
    } ?: return null
    val w = decoder.width
    val h = decoder.height
    val rect = Rect((left * w).toInt().coerceIn(0, w - 1), (top * h).toInt().coerceIn(0, h - 1), (right * w).toInt().coerceIn(1, w), (bottom * h).toInt().coerceIn(1, h))
    if (rect.width() <= 0 || rect.height() <= 0) return null
    var sample = 1
    while (rect.width() / (sample * 2) >= targetWidth) sample *= 2
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    // Region decoders are not thread safe.
    val bitmap = synchronized(decoder) { decoder.decodeRegion(rect, options) } ?: return null
    return bitmap.asImageBitmap()
}
