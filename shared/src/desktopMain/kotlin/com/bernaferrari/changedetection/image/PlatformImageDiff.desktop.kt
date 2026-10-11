/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.image

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import com.bernaferrari.changedetection.detection.PixelDifference
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

internal actual object PlatformImageDiff {
    actual suspend fun compare(
        beforeBytes: ByteArray,
        afterBytes: ByteArray,
        threshold: Double,
        includeDiffImage: Boolean,
    ): SnapshotImageDiff = withContext(desktopImageDispatcher) {
        val pixelsDiffer = PixelDifference(threshold)
        val decodedBefore = decodeDesktopImage(beforeBytes, 1_000_000, 2048)
        val before = decodedBefore?.image
        currentCoroutineContext().ensureActive()
        val decodedAfter = decodeDesktopImage(afterBytes, 1_000_000, 2048)
        val after = decodedAfter?.image
        if (before == null || after == null) {
            return@withContext SnapshotImageDiff(
                match = false,
                mismatchPercentage = 100.0,
                differentPixels = 0,
                totalPixels = 0,
                beforeWidth = decodedBefore?.originalWidth ?: 0,
                beforeHeight = decodedBefore?.originalHeight ?: 0,
                afterWidth = decodedAfter?.originalWidth ?: 0,
                afterHeight = decodedAfter?.originalHeight ?: 0,
                diffImagePng = null,
                error = "Could not decode one of the captured screenshots.",
            )
        }

        if (decodedBefore.originalWidth != decodedAfter.originalWidth || decodedBefore.originalHeight != decodedAfter.originalHeight) {
            return@withContext SnapshotImageDiff(
                match = false,
                mismatchPercentage = 100.0,
                differentPixels = 0,
                totalPixels = 0,
                beforeWidth = decodedBefore.originalWidth,
                beforeHeight = decodedBefore.originalHeight,
                afterWidth = decodedAfter.originalWidth,
                afterHeight = decodedAfter.originalHeight,
                diffImagePng = null,
                dimensionMismatch = SnapshotDimensionMismatch(
                    beforeWidth = decodedBefore.originalWidth,
                    beforeHeight = decodedBefore.originalHeight,
                    afterWidth = decodedAfter.originalWidth,
                    afterHeight = decodedAfter.originalHeight,
                ),
            )
        }

        currentCoroutineContext().ensureActive()
        val beforePixels = IntArray(before.width * before.height)
        val afterPixels = IntArray(after.width * after.height)
        before.readPixels(beforePixels)
        after.readPixels(afterPixels)

        val diffPixels = if (includeDiffImage) IntArray(beforePixels.size) else null
        var differentPixels = 0L
        val bounds = ChangedBoundsTracker(before.width)
        val context = currentCoroutineContext()
        beforePixels.indices.forEach { index ->
            if (index % 4096 == 0) context.ensureActive()
            val changed = pixelsDiffer.changed(beforePixels[index], afterPixels[index])
            if (changed) {
                differentPixels++
                bounds.add(index)
            }
            if (diffPixels != null) diffPixels[index] = differenceDisplayColor(beforePixels[index], changed)
        }
        val totalPixels = beforePixels.size.toLong()

        return@withContext SnapshotImageDiff(
            match = differentPixels == 0L,
            mismatchPercentage = if (totalPixels == 0L) 0.0 else differentPixels * 100.0 / totalPixels,
            differentPixels = differentPixels,
            totalPixels = totalPixels,
            beforeWidth = decodedBefore.originalWidth,
            beforeHeight = decodedBefore.originalHeight,
            afterWidth = decodedAfter.originalWidth,
            afterHeight = decodedAfter.originalHeight,
            changedBounds = bounds.result(before.height),
            // Statistics use bounded sampled pixels; dimensions retain the original capture size.
            diffImagePng = diffPixels?.let { encodeDifference(before.width, before.height, it) },
        )
    }
}

private fun encodeDifference(width: Int, height: Int, pixels: IntArray): ByteArray {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    return try {
        image.setRGB(0, 0, width, height, pixels, 0, width)
        ByteArrayOutputStream().use { output ->
            check(ImageIO.write(image, "png", output)) { "Could not encode the screenshot difference" }
            output.toByteArray()
        }
    } finally {
        image.flush()
    }
}
