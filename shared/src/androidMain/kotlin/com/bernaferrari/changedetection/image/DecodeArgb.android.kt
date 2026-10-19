package com.bernaferrari.changedetection.image

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect

/** Decoded in strips of rows and area-averaged down to [targetWidth]; memory stays bounded for tall pages. */
internal actual suspend fun decodeArgbAtWidth(bytes: ByteArray, targetWidth: Int): Triple<IntArray, Int, Int>? = runCatching {
    val context = currentCoroutineContext()
    context.ensureActive()
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val sourceWidth = bounds.outWidth
    if (sourceWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
    val sourceHeight = minOf(bounds.outHeight, (MAX_ROWS.toLong() * sourceWidth / minOf(targetWidth, sourceWidth).coerceAtLeast(1)).toInt())
    val scaler = BoxDownscaler(sourceWidth, sourceHeight, targetWidth)

    @Suppress("DEPRECATION")
    val decoder = BitmapRegionDecoder.newInstance(bytes, 0, bytes.size, false) ?: return@runCatching null
    val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
    val pixels = IntArray(sourceWidth * STRIP_ROWS)
    try {
        var top = 0
        while (top < sourceHeight) {
            context.ensureActive()
            val rows = minOf(STRIP_ROWS, sourceHeight - top)
            val strip = decoder.decodeRegion(Rect(0, top, sourceWidth, top + rows), options) ?: return@runCatching null
            strip.getPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, rows)
            strip.recycle()
            for (r in 0 until rows) scaler.addRow(top + r, pixels, r * sourceWidth)
            top += rows
        }
    } finally {
        decoder.recycle()
    }
    Triple(scaler.result(), scaler.width, scaler.height)
}.getOrElse { if (it is CancellationException) throw it else null }

private const val MAX_ROWS = 12_000
private const val STRIP_ROWS = 512
