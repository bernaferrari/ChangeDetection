package com.bernaferrari.changedetection.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * The last two captures (before and after), decoded once. PNG cannot be decoded from the middle, so cutting
 * every crop from one decoded image is far faster than decoding each region on its own.
 */
private val decoded = LinkedHashMap<ByteArray, Bitmap>()

private fun decodedCapture(bytes: ByteArray): Bitmap? = synchronized(decoded) {
    decoded[bytes]?.let { return it }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
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
    ) ?: return null
    decoded[bytes] = bitmap
    while (decoded.size > 2) decoded.remove(decoded.keys.first())
    bitmap
}

internal actual suspend fun decodeCropPlatform(bytes: ByteArray, left: Float, top: Float, right: Float, bottom: Float, targetWidth: Int): ImageBitmap? {
    val source = decodedCapture(bytes) ?: return null
    val w = source.width
    val h = source.height
    val x = (left * w).toInt().coerceIn(0, w - 1)
    val y = (top * h).toInt().coerceIn(0, h - 1)
    val cw = ((right * w).toInt() - x).coerceIn(1, w - x)
    val ch = ((bottom * h).toInt() - y).coerceIn(1, h - y)
    // A copy of just this rectangle (scaled down only when it is much wider than needed).
    val scale = minOf(1f, targetWidth.toFloat() / cw)
    val crop = if (scale < 0.75f) {
        Bitmap.createScaledBitmap(Bitmap.createBitmap(source, x, y, cw, ch), (cw * scale).toInt().coerceAtLeast(1), (ch * scale).toInt().coerceAtLeast(1), true)
    } else {
        Bitmap.createBitmap(source, x, y, cw, ch)
    }
    return crop.asImageBitmap()
}
