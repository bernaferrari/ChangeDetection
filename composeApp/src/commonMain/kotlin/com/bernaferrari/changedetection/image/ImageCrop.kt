/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.image

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Width and height from a PNG header (no decoding), or null for other formats. */
internal fun pngSize(bytes: ByteArray): Pair<Int, Int>? {
    if (bytes.size < 24 || bytes[0] != 0x89.toByte() || bytes[1] != 'P'.code.toByte() || bytes[2] != 'N'.code.toByte()) return null
    fun int(at: Int) = ((bytes[at].toInt() and 255) shl 24) or ((bytes[at + 1].toInt() and 255) shl 16) or ((bytes[at + 2].toInt() and 255) shl 8) or (bytes[at + 3].toInt() and 255)
    val w = int(16)
    val h = int(20)
    return if (w > 0 && h > 0) w to h else null
}

/**
 * One rectangle of a capture (fractions of its size), decoded at about [targetWidth] pixels wide. Only that
 * rectangle is decoded, so a list of changes from a 10,000-pixel page stays light and scrolls smoothly.
 */
internal expect suspend fun decodeCropPlatform(bytes: ByteArray, left: Float, top: Float, right: Float, bottom: Float, targetWidth: Int): ImageBitmap?

/** Small cache: the same crops come back as you scroll, flip or switch modes. */
internal object CropCache {
    private val lock = Mutex()
    private val entries = LinkedHashMap<String, ImageBitmap>()
    private const val MAX = 48

    suspend fun get(bytes: ByteArray, left: Float, top: Float, right: Float, bottom: Float, targetWidth: Int): ImageBitmap? {
        // Width is bucketed so tiny layout differences reuse the same crop.
        val width = ((targetWidth + 63) / 64) * 64
        val key = "${bytes.size}:${bytes.contentHashCodeSample()}:$left:$top:$right:$bottom:$width"
        lock.withLock { entries[key]?.let { entries.remove(key); entries[key] = it; return it } }
        val bitmap = withContext(Dispatchers.Default) { runCatching { decodeCropPlatform(bytes, left, top, right, bottom, width) }.getOrNull() } ?: return null
        lock.withLock {
            entries[key] = bitmap
            while (entries.size > MAX) entries.remove(entries.keys.first())
        }
        return bitmap
    }

    /** Hashing a few KB identifies a capture without reading megabytes on every lookup. */
    private fun ByteArray.contentHashCodeSample(): Int {
        var h = 1
        val step = maxOf(1, size / 4096)
        var i = 0
        while (i < size) {
            h = 31 * h + this[i]
            i += step
        }
        return h
    }
}
