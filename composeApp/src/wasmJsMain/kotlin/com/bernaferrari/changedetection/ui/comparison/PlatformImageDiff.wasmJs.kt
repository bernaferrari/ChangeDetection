/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.math.abs

internal actual object PlatformImageDiff {
    actual suspend fun compare(
        beforeBytes: ByteArray,
        afterBytes: ByteArray,
        threshold: Double,
    ): SnapshotImageDiff {
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

        val channelThreshold = (threshold.coerceIn(0.0, 1.0) * 255).toInt()
        var differentPixels = 0L
        beforePixels.indices.forEach { index ->
            val first = beforePixels[index]
            val second = afterPixels[index]
            val changed =
                abs(((first shr 16) and 0xFF) - ((second shr 16) and 0xFF)) > channelThreshold ||
                    abs(((first shr 8) and 0xFF) - ((second shr 8) and 0xFF)) > channelThreshold ||
                    abs((first and 0xFF) - (second and 0xFF)) > channelThreshold
            if (changed) differentPixels++
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
            // Wasm reports accurate statistics and uses the interactive swipe view as its visual overlay.
            diffImagePng = null,
        )
    }
}
