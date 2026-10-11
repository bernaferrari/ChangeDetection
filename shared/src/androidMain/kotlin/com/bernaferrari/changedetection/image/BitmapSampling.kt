/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory

internal data class SampledBitmap(
    val bitmap: Bitmap,
    val originalWidth: Int,
    val originalHeight: Int,
)

internal fun decodeSampledBitmap(
    bytes: ByteArray,
    maxPixels: Int,
    maxDimension: Int,
): SampledBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val sampleSize = calculateInSampleSize(
        width = bounds.outWidth,
        height = bounds.outHeight,
        maxPixels = maxPixels,
        maxDimension = maxDimension,
    )
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
    return SampledBitmap(bitmap, bounds.outWidth, bounds.outHeight)
}

internal fun calculateInSampleSize(
    width: Int,
    height: Int,
    maxPixels: Int,
    maxDimension: Int,
): Int {
    if (width <= 0 || height <= 0) return 1

    var sampleSize = 1
    while (
        width / sampleSize > maxDimension ||
        height / sampleSize > maxDimension ||
        width.toLong() * height.toLong() / (sampleSize.toLong() * sampleSize) > maxPixels
    ) {
        sampleSize *= 2
    }
    return sampleSize
}
