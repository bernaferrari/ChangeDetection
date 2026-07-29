/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import android.graphics.Bitmap
import com.bernaferrari.kmpwright.core.DiffOptions
import com.bernaferrari.kmpwright.core.RgbaImage
import com.bernaferrari.kmpwright.core.ScreenshotResult
import com.bernaferrari.kmpwright.core.VisualDiff
import com.bernaferrari.kmpwright.core.toRgbaImage
import java.io.ByteArrayOutputStream

internal actual object PlatformImageDiff {
    actual suspend fun compare(
        beforeBytes: ByteArray,
        afterBytes: ByteArray,
        threshold: Double,
    ): SnapshotImageDiff {
        val beforeImage = ScreenshotResult(beforeBytes, width = 0, height = 0).toRgbaImage()
        val afterImage = ScreenshotResult(afterBytes, width = 0, height = 0).toRgbaImage()

        if (beforeImage == null || afterImage == null) {
            return SnapshotImageDiff(
                match = false,
                mismatchPercentage = 100.0,
                differentPixels = 0,
                totalPixels = 0,
                beforeWidth = beforeImage?.width ?: 0,
                beforeHeight = beforeImage?.height ?: 0,
                afterWidth = afterImage?.width ?: 0,
                afterHeight = afterImage?.height ?: 0,
                diffImagePng = null,
                error = "Could not decode one of the captured screenshots.",
            )
        }

        val diff =
            VisualDiff.compareRgba(
                expected = beforeImage,
                actual = afterImage,
                options = DiffOptions(threshold = threshold, generateDiffImage = true),
            )

        val mismatch =
            diff.dimensionMismatch?.let {
                SnapshotDimensionMismatch(
                    beforeWidth = it.expectedWidth,
                    beforeHeight = it.expectedHeight,
                    afterWidth = it.actualWidth,
                    afterHeight = it.actualHeight,
                )
            }

        return SnapshotImageDiff(
            match = diff.match,
            mismatchPercentage = diff.mismatchPercentage,
            differentPixels = diff.differentPixels,
            totalPixels = diff.totalPixels,
            beforeWidth = beforeImage.width,
            beforeHeight = beforeImage.height,
            afterWidth = afterImage.width,
            afterHeight = afterImage.height,
            diffImagePng = diff.diffImage?.let { pixels ->
                encodeRgbaAsPng(RgbaImage(beforeImage.width, beforeImage.height, pixels))
            },
            dimensionMismatch = mismatch,
        )
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
