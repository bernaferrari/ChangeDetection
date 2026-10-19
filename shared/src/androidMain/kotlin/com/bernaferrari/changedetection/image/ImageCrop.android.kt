package com.bernaferrari.changedetection.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * The last two captures (before and after), decoded once. PNG cannot be decoded from the middle, so cutting
 * every crop from one decoded image is far faster than decoding each region on its own.
 */
private val decoded = LinkedHashMap<ByteArray, Bitmap>()

private val decodeLock = Mutex()

private suspend fun decodedCapture(bytes: ByteArray): Bitmap? = decodeLock.withLock {
    currentCoroutineContext().ensureActive()
    decoded[bytes]?.let { return@withLock it }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withLock null
    // Keep memory bounded on very large captures (about 40 MB per capture at most).
    var sample = 1
    while (bounds.outWidth.toLong() * bounds.outHeight / (sample.toLong() * sample) > 10_000_000L) sample *= 2
    val bitmap = BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        },
    ) ?: return@withLock null
    try {
        currentCoroutineContext().ensureActive()
    } catch (cancelled: CancellationException) {
        bitmap.recycle()
        throw cancelled
    }
    decoded[bytes] = bitmap
    while (decoded.size > 2) decoded.remove(decoded.keys.first())
    bitmap
}

internal actual suspend fun decodeCropPlatform(bytes: ByteArray, left: Float, top: Float, right: Float, bottom: Float, targetWidth: Int): ImageBitmap? = withContext(Dispatchers.Default) {
    val source = decodedCapture(bytes) ?: return@withContext null
    currentCoroutineContext().ensureActive()
    val w = source.width
    val h = source.height
    val x = (left * w).toInt().coerceIn(0, w - 1)
    val y = (top * h).toInt().coerceIn(0, h - 1)
    val cw = ((right * w).toInt() - x).coerceIn(1, w - x)
    val ch = ((bottom * h).toInt() - y).coerceIn(1, h - y)
    // Draw directly into the bounded result; do not allocate an intermediate full-height crop.
    val scale = minOf(1f, targetWidth.coerceIn(1, 2048).toFloat() / cw, 4000f / ch, kotlin.math.sqrt(1_000_000.0 / (cw.toDouble() * ch)).toFloat())
    val crop = Bitmap.createBitmap((cw * scale).toInt().coerceAtLeast(1), (ch * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    try {
        Canvas(crop).drawBitmap(source, Rect(x, y, x + cw, y + ch), RectF(0f, 0f, crop.width.toFloat(), crop.height.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
        currentCoroutineContext().ensureActive()
        crop.asImageBitmap()
    } catch (error: Throwable) {
        crop.recycle()
        throw error
    }
}
