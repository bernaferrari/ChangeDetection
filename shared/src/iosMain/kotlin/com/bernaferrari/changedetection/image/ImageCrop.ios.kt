package com.bernaferrari.changedetection.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.sync.withLock
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface

/** The last decoded captures, so cropping many areas from one page decodes it once. */
private val decoded = LinkedHashMap<ByteArray, Image>()
private val decodeLock = kotlinx.coroutines.sync.Mutex()

internal actual suspend fun decodeCropPlatform(bytes: ByteArray, left: Float, top: Float, right: Float, bottom: Float, targetWidth: Int): ImageBitmap? = decodeLock.withLock {
    val image = cachedImage(bytes) ?: return@withLock null
    val src = Rect.makeLTRB(left * image.width, top * image.height, right * image.width, bottom * image.height)
    if (src.width <= 0f || src.height <= 0f) return@withLock null
    val width = minOf(targetWidth, (src.width * 3).toInt()).coerceAtLeast(1)
    val height = (src.height * width / src.width).toInt().coerceIn(1, 4_000)
    val surface = Surface.makeRasterN32Premul(width, height)
    surface.canvas.drawImageRect(image, src, Rect.makeWH(width.toFloat(), height.toFloat()))
    val out = surface.makeImageSnapshot().toComposeImageBitmap()
    surface.close()
    out
}

private fun cachedImage(bytes: ByteArray): Image? = run {
    decoded[bytes] ?: runCatching { Image.makeFromEncoded(bytes) }.getOrNull()?.also {
        decoded[bytes] = it
        while (decoded.size > 2) decoded.remove(decoded.keys.first())?.close()
    }
}
