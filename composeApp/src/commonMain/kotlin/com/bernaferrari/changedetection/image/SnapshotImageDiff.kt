/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.image

const val DEFAULT_IMAGE_DIFF_THRESHOLD = 0.1

data class SnapshotDimensionMismatch(
    val beforeWidth: Int,
    val beforeHeight: Int,
    val afterWidth: Int,
    val afterHeight: Int,
)

/** Where pixels changed, as fractions of the compared image (0..1), so any scaled copy can be cropped. */
data class ChangedBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/** Accumulates the bounding box of changed pixels while a diff loop runs. */
internal class ChangedBoundsTracker(private val width: Int) {
    private var minX = Int.MAX_VALUE
    private var minY = Int.MAX_VALUE
    private var maxX = -1
    private var maxY = -1

    fun add(pixelIndex: Int) {
        val x = pixelIndex % width
        val y = pixelIndex / width
        if (x < minX) minX = x
        if (x > maxX) maxX = x
        if (y < minY) minY = y
        if (y > maxY) maxY = y
    }

    fun result(height: Int): ChangedBounds? = if (maxX < 0 || width <= 0 || height <= 0) {
        null
    } else {
        ChangedBounds(minX.toFloat() / width, minY.toFloat() / height, (maxX + 1f) / width, (maxY + 1f) / height)
    }
}

data class SnapshotImageDiff(
    val match: Boolean,
    val mismatchPercentage: Double,
    val differentPixels: Long,
    val totalPixels: Long,
    val beforeWidth: Int,
    val beforeHeight: Int,
    val afterWidth: Int,
    val afterHeight: Int,
    val diffImagePng: ByteArray?,
    val dimensionMismatch: SnapshotDimensionMismatch? = null,
    val error: String? = null,
    val changedBounds: ChangedBounds? = null,
) {
    val isSampled: Boolean
        get() = totalPixels > 0 && totalPixels < beforeWidth.toLong() * beforeHeight

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SnapshotImageDiff) return false
        if (match != other.match) return false
        if (mismatchPercentage != other.mismatchPercentage) return false
        if (differentPixels != other.differentPixels) return false
        if (totalPixels != other.totalPixels) return false
        if (beforeWidth != other.beforeWidth) return false
        if (beforeHeight != other.beforeHeight) return false
        if (afterWidth != other.afterWidth) return false
        if (afterHeight != other.afterHeight) return false
        if (diffImagePng != null && other.diffImagePng != null) {
            if (!diffImagePng.contentEquals(other.diffImagePng)) return false
        } else if (diffImagePng !== other.diffImagePng) {
            return false
        }
        if (dimensionMismatch != other.dimensionMismatch) return false
        if (changedBounds != other.changedBounds) return false
        return error == other.error
    }

    override fun hashCode(): Int {
        var result = match.hashCode()
        result = 31 * result + mismatchPercentage.hashCode()
        result = 31 * result + differentPixels.hashCode()
        result = 31 * result + totalPixels.hashCode()
        result = 31 * result + beforeWidth
        result = 31 * result + beforeHeight
        result = 31 * result + afterWidth
        result = 31 * result + afterHeight
        result = 31 * result + (diffImagePng?.contentHashCode() ?: 0)
        result = 31 * result + (dimensionMismatch?.hashCode() ?: 0)
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }
}

internal expect object PlatformImageDiff {
    suspend fun compare(
        beforeBytes: ByteArray,
        afterBytes: ByteArray,
        threshold: Double = DEFAULT_IMAGE_DIFF_THRESHOLD,
        includeDiffImage: Boolean = true,
    ): SnapshotImageDiff
}
