package com.bernaferrari.changedetection.image

import com.bernaferrari.changedetection.detection.PixelDifference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual object PlatformImageDiff {
    /** Statistics are computed on a bounded-width copy, like the other platforms' sampled diffs. */
    private const val STATS_WIDTH = 540

    actual suspend fun compare(
        beforeBytes: ByteArray,
        afterBytes: ByteArray,
        threshold: Double,
        includeDiffImage: Boolean,
    ): SnapshotImageDiff = withContext(Dispatchers.Default) {
        val before = decodeArgbAtWidth(beforeBytes, STATS_WIDTH)
        val after = decodeArgbAtWidth(afterBytes, STATS_WIDTH)
        if (before == null || after == null) {
            return@withContext SnapshotImageDiff(false, 100.0, 0, 0, before?.second ?: 0, before?.third ?: 0, after?.second ?: 0, after?.third ?: 0, null, error = "Could not decode one of the captured screenshots.")
        }
        val (beforePixels, width, beforeHeight) = before
        val (afterPixels, afterWidth, afterHeight) = after
        val visual = computeVisualChange(beforeBytes, afterBytes, threshold)
        if (width != afterWidth || beforeHeight != afterHeight) {
            return@withContext SnapshotImageDiff(
                match = false,
                mismatchPercentage = ((visual?.changedShare ?: 1f) * 100.0),
                differentPixels = 0,
                totalPixels = 0,
                beforeWidth = width,
                beforeHeight = beforeHeight,
                afterWidth = afterWidth,
                afterHeight = afterHeight,
                diffImagePng = null,
                dimensionMismatch = SnapshotDimensionMismatch(width, beforeHeight, afterWidth, afterHeight),
                visualChange = visual,
            )
        }
        val pixels = PixelDifference(threshold)
        val bounds = ChangedBoundsTracker(width)
        var different = 0L
        for (index in beforePixels.indices) {
            if (pixels.changed(beforePixels[index], afterPixels[index])) {
                different++
                bounds.add(index)
            }
        }
        val total = beforePixels.size.toLong()
        SnapshotImageDiff(
            match = different == 0L,
            mismatchPercentage = different * 100.0 / total,
            differentPixels = different,
            totalPixels = total,
            beforeWidth = width,
            beforeHeight = beforeHeight,
            afterWidth = afterWidth,
            afterHeight = afterHeight,
            diffImagePng = if (includeDiffImage) encodeDifference(beforePixels, afterPixels, width, beforeHeight, pixels) else null,
            changedBounds = bounds.result(beforeHeight),
            visualChange = visual,
        )
    }

    private fun encodeDifference(before: IntArray, after: IntArray, width: Int, height: Int, pixels: PixelDifference): ByteArray? = runCatching {
        val rgba = ByteArray(width * height * 4)
        for (i in before.indices) {
            val display = differenceDisplayColor(before[i], pixels.changed(before[i], after[i]))
            val o = i * 4
            rgba[o] = (display ushr 16).toByte()
            rgba[o + 1] = (display ushr 8).toByte()
            rgba[o + 2] = display.toByte()
            rgba[o + 3] = 255.toByte()
        }
        val info = org.jetbrains.skia.ImageInfo(width, height, org.jetbrains.skia.ColorType.RGBA_8888, org.jetbrains.skia.ColorAlphaType.UNPREMUL)
        org.jetbrains.skia.Image.makeRaster(info, rgba, width * 4).encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)?.bytes
    }.getOrNull()
}
