/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.image

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual object PlatformImageDiff {
    actual suspend fun compare(
        beforeBytes: ByteArray,
        afterBytes: ByteArray,
        threshold: Double,
        includeDiffImage: Boolean,
    ): SnapshotImageDiff = withContext(Dispatchers.Default) {
        val before = decodeRgba(beforeBytes)
        val after = decodeRgba(afterBytes)
        val beforeImage = before?.image
        val afterImage = after?.image

        if (beforeImage == null || afterImage == null) {
            return@withContext SnapshotImageDiff(
                match = false,
                mismatchPercentage = 100.0,
                differentPixels = 0,
                totalPixels = 0,
                beforeWidth = before?.originalWidth ?: 0,
                beforeHeight = before?.originalHeight ?: 0,
                afterWidth = after?.originalWidth ?: 0,
                afterHeight = after?.originalHeight ?: 0,
                diffImagePng = null,
                error = "Could not decode one of the captured screenshots.",
            )
        }

        val diff = RgbaDiff.compare(beforeImage, afterImage, threshold, includeDiffImage)

        return@withContext SnapshotImageDiff(
            match = diff.match && before.originalWidth == after.originalWidth && before.originalHeight == after.originalHeight,
            mismatchPercentage = diff.mismatchPercentage,
            differentPixels = diff.differentPixels,
            totalPixels = diff.totalPixels,
            beforeWidth = before.originalWidth,
            beforeHeight = before.originalHeight,
            afterWidth = after.originalWidth,
            afterHeight = after.originalHeight,
            diffImagePng = diff.diffImage?.let { pixels ->
                encodeRgbaAsPng(RgbaImage(beforeImage.width, beforeImage.height, pixels))
            },
            dimensionMismatch = if (
                before.originalWidth != after.originalWidth || before.originalHeight != after.originalHeight
            ) {
                SnapshotDimensionMismatch(
                    beforeWidth = before.originalWidth,
                    beforeHeight = before.originalHeight,
                    afterWidth = after.originalWidth,
                    afterHeight = after.originalHeight,
                )
            } else {
                null
            },
        )
    }
}

private data class DecodedRgba(
    val image: RgbaImage,
    val originalWidth: Int,
    val originalHeight: Int,
)

private fun decodeRgba(bytes: ByteArray): DecodedRgba? {
    val sampled = decodeSampledBitmap(
        bytes = bytes,
        maxPixels = MAX_DIFF_PIXELS,
        maxDimension = MAX_DIFF_DIMENSION,
    ) ?: return null
    val bitmap = sampled.bitmap
    return try {
        val argb = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(argb, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val rgba = ByteArray(argb.size * 4)
        var byteIndex = 0
        argb.forEach { pixel ->
            rgba[byteIndex++] = ((pixel shr 16) and 0xFF).toByte()
            rgba[byteIndex++] = ((pixel shr 8) and 0xFF).toByte()
            rgba[byteIndex++] = (pixel and 0xFF).toByte()
            rgba[byteIndex++] = ((pixel ushr 24) and 0xFF).toByte()
        }
        DecodedRgba(
            image = RgbaImage(bitmap.width, bitmap.height, rgba),
            originalWidth = sampled.originalWidth,
            originalHeight = sampled.originalHeight,
        )
    } finally {
        bitmap.recycle()
    }
}

private fun encodeRgbaAsPng(image: RgbaImage): ByteArray? {
    if (image.width <= 0 || image.height <= 0) return null

    val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(image.width * image.height)

    var byteIndex = 0
    for (pixelIndex in pixels.indices) {
        val red = image.pixels[byteIndex].toInt() and 0xFF
        val green = image.pixels[byteIndex + 1].toInt() and 0xFF
        val blue = image.pixels[byteIndex + 2].toInt() and 0xFF
        val alpha = image.pixels[byteIndex + 3].toInt() and 0xFF
        pixels[pixelIndex] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
        byteIndex += 4
    }

    bitmap.setPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
    return try {
        ByteArrayOutputStream().use { output ->
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                output.toByteArray()
            } else {
                null
            }
        }
    } finally {
        bitmap.recycle()
    }
}

private const val MAX_DIFF_PIXELS = 1_000_000
private const val MAX_DIFF_DIMENSION = 3_000
