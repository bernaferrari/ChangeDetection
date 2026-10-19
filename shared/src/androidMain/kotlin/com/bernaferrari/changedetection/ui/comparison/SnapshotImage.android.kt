package com.bernaferrari.changedetection.ui.comparison

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.bernaferrari.changedetection.image.calculateInSampleSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Full-page captures are often much taller than the largest texture a GPU can draw (commonly 4096 px).
 * Shrinking them to fit made text unreadable, so the capture is decoded at full width as a stack of
 * strips that each fit, and drawn as one image with smooth filtering.
 */
@Composable
internal actual fun SnapshotImage(
    bytes: ByteArray,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    val state by produceState<ImageLoadState>(ImageLoadState.Loading, bytes) {
        // Keep the decoded viewport visible while its full-page replacement decodes.
        value = withContext(Dispatchers.Default) {
            runCatching { decodeStrips(bytes) }.getOrNull()?.let { ImageLoadState.Ready(it) } ?: ImageLoadState.Failed
        }
    }

    when (val current = state) {
        ImageLoadState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }

        is ImageLoadState.Ready -> {
            DisposableEffect(current.strips) {
                onDispose { current.strips.bitmaps.forEach(Bitmap::recycle) }
            }
            val painter = remember(current.strips) { StripsPainter(current.strips) }
            Image(painter = painter, contentDescription = contentDescription, modifier = modifier, contentScale = contentScale)
        }

        ImageLoadState.Failed -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private class Strips(val bitmaps: List<Bitmap>, val width: Int, val height: Int) {
    val images: List<ImageBitmap> = bitmaps.map { it.asImageBitmap() }
}

private fun decodeStrips(bytes: ByteArray): Strips? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val width = bounds.outWidth
    val height = bounds.outHeight
    if (width <= 0 || height <= 0) return null
    // Memory, not texture size, is the only reason left to shrink; strips handle the texture limit.
    val sample = calculateInSampleSize(width, height, MAX_DISPLAY_PIXELS, Int.MAX_VALUE)
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    if (height / sample <= STRIP_HEIGHT) {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        return Strips(listOf(bitmap), bitmap.width, bitmap.height)
    }
    @Suppress("DEPRECATION")
    val decoder = BitmapRegionDecoder.newInstance(bytes, 0, bytes.size, false) ?: return null
    try {
        val bitmaps = mutableListOf<Bitmap>()
        var top = 0
        val sourceStrip = STRIP_HEIGHT * sample
        while (top < height) {
            val bottom = minOf(height, top + sourceStrip)
            bitmaps += decoder.decodeRegion(Rect(0, top, width, bottom), options) ?: run {
                bitmaps.forEach(Bitmap::recycle)
                return null
            }
            top = bottom
        }
        return Strips(bitmaps, bitmaps.first().width, bitmaps.sumOf { it.height })
    } finally {
        decoder.recycle()
    }
}

/** Draws the strips stacked, scaled together to the destination size. */
private class StripsPainter(private val strips: Strips) : Painter() {
    override val intrinsicSize: Size = Size(strips.width.toFloat(), strips.height.toFloat())

    override fun DrawScope.onDraw() {
        val sx = size.width / strips.width
        val sy = size.height / strips.height
        var top = 0
        strips.images.forEach { image ->
            val dstTop = (top * sy).roundToInt()
            val dstBottom = ((top + image.height) * sy).roundToInt()
            if (dstBottom > dstTop) {
                drawImage(
                    image,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(image.width, image.height),
                    dstOffset = IntOffset(0, dstTop),
                    dstSize = IntSize((image.width * sx).roundToInt(), dstBottom - dstTop),
                    filterQuality = FilterQuality.Medium,
                )
            }
            top += image.height
        }
    }
}

private sealed interface ImageLoadState {
    data object Loading : ImageLoadState
    class Ready(val strips: Strips) : ImageLoadState
    data object Failed : ImageLoadState
}

/** About two full-page captures' worth (40 MB) in a comparison, without shrinking them. */
private const val MAX_DISPLAY_PIXELS = 10_000_000
private const val STRIP_HEIGHT = 2_048
