package com.bernaferrari.changedetection.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageInputStream
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import kotlinx.coroutines.Dispatchers

/** Shared by previews and comparisons so obsolete native decodes cannot flood the CPU pool. */
internal val desktopImageDispatcher = Dispatchers.Default.limitedParallelism(2)

internal data class DesktopDecodedImage(val image: ImageBitmap, val originalWidth: Int, val originalHeight: Int)

/** Inspect dimensions before allocating pixels; large supported images decode with subsampling. */
internal fun decodeDesktopImage(bytes: ByteArray, maxPixels: Int, maxDimension: Int): DesktopDecodedImage? {
    require(maxPixels > 0 && maxDimension > 0)
    return runCatching {
        val dimensions = Data.makeFromBytes(bytes).use { data -> Codec.makeFromData(data).use { it.width to it.height } }
        val (width, height) = dimensions
        if (width <= 0 || height <= 0 || width > 100_000 || height > 100_000 || width.toLong() * height > 100_000_000L) return null
        if (width <= maxDimension && height <= maxDimension && width.toLong() * height <= maxPixels) {
            return DesktopDecodedImage(bytes.decodeToImageBitmap(), width, height)
        }
        var sample = 1
        fun sampled(size: Int) = (size.toLong() + sample - 1) / sample
        while (sampled(width) > maxDimension || sampled(height) > maxDimension || sampled(width) * sampled(height) > maxPixels) sample++
        MemoryCacheImageInputStream(ByteArrayInputStream(bytes)).use { input ->
            val readers = ImageIO.getImageReaders(input)
            if (!readers.hasNext()) return null
            val reader = readers.next()
            try {
                reader.setInput(input, true, true)
                val parameters = reader.defaultReadParam.apply { setSourceSubsampling(sample, sample, 0, 0) }
                val bitmap = reader.read(0, parameters) ?: return null
                try {
                    if (bitmap.width > maxDimension || bitmap.height > maxDimension || bitmap.width.toLong() * bitmap.height > maxPixels) return null
                    DesktopDecodedImage(bitmap.toComposeImageBitmap(), width, height)
                } finally {
                    bitmap.flush()
                }
            } finally {
                reader.dispose()
            }
        }
    }.getOrNull()
}
