/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.image

import org.jetbrains.compose.resources.decodeToImageBitmap
import com.bernaferrari.changedetection.detection.PixelDifference

internal actual object PlatformImageDiff {
    actual suspend fun compare(
        beforeBytes: ByteArray,
        afterBytes: ByteArray,
        threshold: Double,
        includeDiffImage: Boolean,
    ): SnapshotImageDiff {
        val pixelsDiffer = PixelDifference(threshold)
        val before = runCatching { beforeBytes.decodeToImageBitmap() }.getOrNull()
        val after = runCatching { afterBytes.decodeToImageBitmap() }.getOrNull()
        if (before == null || after == null) {
            return SnapshotImageDiff(
                match = false,
                mismatchPercentage = 100.0,
                differentPixels = 0,
                totalPixels = 0,
                beforeWidth = before?.width ?: 0,
                beforeHeight = before?.height ?: 0,
                afterWidth = after?.width ?: 0,
                afterHeight = after?.height ?: 0,
                diffImagePng = null,
                error = "Could not decode one of the captured screenshots.",
            )
        }

        if (before.width != after.width || before.height != after.height) {
            return SnapshotImageDiff(
                match = false,
                mismatchPercentage = 100.0,
                differentPixels = 0,
                totalPixels = 0,
                beforeWidth = before.width,
                beforeHeight = before.height,
                afterWidth = after.width,
                afterHeight = after.height,
                diffImagePng = null,
                dimensionMismatch = SnapshotDimensionMismatch(
                    beforeWidth = before.width,
                    beforeHeight = before.height,
                    afterWidth = after.width,
                    afterHeight = after.height,
                ),
            )
        }

        val beforePixels = IntArray(before.width * before.height)
        val afterPixels = IntArray(after.width * after.height)
        before.readPixels(beforePixels)
        after.readPixels(afterPixels)

        var differentPixels = 0L
        val bounds = ChangedBoundsTracker(before.width)
        beforePixels.indices.forEach { index ->
            if (pixelsDiffer.changed(beforePixels[index], afterPixels[index])) {
                differentPixels++
                bounds.add(index)
            }
        }
        val totalPixels = beforePixels.size.toLong()

        return SnapshotImageDiff(
            match = differentPixels == 0L,
            mismatchPercentage = if (totalPixels == 0L) 0.0 else differentPixels * 100.0 / totalPixels,
            differentPixels = differentPixels,
            totalPixels = totalPixels,
            beforeWidth = before.width,
            beforeHeight = before.height,
            afterWidth = after.width,
            afterHeight = after.height,
            changedBounds = bounds.result(before.height),
            // Wasm reports accurate statistics and uses the interactive swipe view as its visual overlay.
            diffImagePng = null,
        )
    }
}
