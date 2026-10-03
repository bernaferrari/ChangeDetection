/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.image

import com.bernaferrari.changedetection.detection.PixelDifference

/** Compact pixel input; platform decoders keep its size bounded before comparison. */
internal data class RgbaImage(val width: Int, val height: Int, val pixels: ByteArray) {
    init {
        require(width > 0 && height > 0 && width.toLong() * height * 4 == pixels.size.toLong())
    }
}

internal data class RgbaDifference(
    val match: Boolean,
    val mismatchPercentage: Double,
    val differentPixels: Long,
    val totalPixels: Long,
    val diffImage: ByteArray?,
)

internal object RgbaDiff {
    /** Same normalized RGBA distance as the previous adapter, without retaining browser machinery. */
    fun compare(expected: RgbaImage, actual: RgbaImage, threshold: Double, includeDiffImage: Boolean = true): RgbaDifference {
        val pixelsDiffer = PixelDifference(threshold)
        if (expected.width != actual.width || expected.height != actual.height) return RgbaDifference(false, 100.0, 0, 0, null)
        val total = expected.width.toLong() * expected.height
        val result = if (includeDiffImage) ByteArray(expected.pixels.size) else null
        var differences = 0L
        for (index in expected.pixels.indices step 4) {
            val before = expected.pixels.argbAt(index)
            val after = actual.pixels.argbAt(index)
            val changed = pixelsDiffer.changed(before, after)
            if (changed) differences++
            if (result != null) {
                val display = differenceDisplayColor(before, changed)
                result[index] = (display ushr 16).toByte()
                result[index + 1] = (display ushr 8).toByte()
                result[index + 2] = display.toByte()
                result[index + 3] = 255.toByte()
            }
        }
        return RgbaDifference(differences == 0L, differences * 100.0 / total, differences, total, result)
    }
}

private fun ByteArray.argbAt(index: Int): Int = ((this[index + 3].toInt() and 255) shl 24) or
    ((this[index].toInt() and 255) shl 16) or
    ((this[index + 1].toInt() and 255) shl 8) or
    (this[index + 2].toInt() and 255)

/** The same red highlights and muted baseline on Android and Desktop. */
internal fun differenceDisplayColor(before: Int, changed: Boolean): Int {
    if (changed) return 0xffff0000.toInt()
    val gray = (((before ushr 16) and 255) + ((before ushr 8) and 255) + (before and 255)) / 6
    return 0xff000000.toInt() or (gray shl 16) or (gray shl 8) or gray
}
