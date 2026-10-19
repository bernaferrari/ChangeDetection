package com.bernaferrari.changedetection.image

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.ensureActive
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

/** Same rectangle consumers share a cancellable build; only completed results enter the LRU. */
internal object CropCache {
    private val cache = CropBitmapCache()
    suspend fun get(bytes: ByteArray, left: Float, top: Float, right: Float, bottom: Float, targetWidth: Int): ImageBitmap? = cache.get(bytes, left, top, right, bottom, targetWidth)
}

internal class CropBitmapCache(
    private val maxPixels: Long = 8_000_000,
    private val decode: suspend (ByteArray, Float, Float, Float, Float, Int) -> ImageBitmap? = ::decodeCropPlatform,
) {
    private val lock = Mutex()
    private val builds = com.bernaferrari.changedetection.sync.PageCheckLocks()
    private val entries = LinkedHashMap<String, ImageBitmap>()
    private var pixels = 0L

    suspend fun get(bytes: ByteArray, left: Float, top: Float, right: Float, bottom: Float, targetWidth: Int): ImageBitmap? = withContext(Dispatchers.Default) {
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        val width = (((targetWidth.coerceIn(1, 2048) + 63) / 64) * 64)
        // A sampled hash could return another page's crop. Hash the complete encoded capture off Main.
        val key = "${com.bernaferrari.changedetection.util.CryptoUtils.sha256(bytes)}:$left:$top:$right:$bottom:$width"
        suspend fun cached(): ImageBitmap? = lock.withLock {
            entries.remove(key)?.also { entries[key] = it }
        }
        cached()?.let { return@withContext it }
        builds.withLock(key) {
            cached()?.let { return@withLock it }
            val bitmap = try {
                decode(bytes, left, top, right, bottom, width)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            } ?: return@withLock null
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            val cost = bitmap.width.toLong() * bitmap.height
            lock.withLock {
                if (cost <= maxPixels) {
                    entries[key] = bitmap
                    pixels += cost
                    while (pixels > maxPixels || entries.size > 48) {
                        val oldest = entries.remove(entries.keys.first())!!
                        pixels -= oldest.width.toLong() * oldest.height
                    }
                }
            }
            bitmap
        }
    }

    internal suspend fun retainedPixels(): Long = lock.withLock { pixels }
}
