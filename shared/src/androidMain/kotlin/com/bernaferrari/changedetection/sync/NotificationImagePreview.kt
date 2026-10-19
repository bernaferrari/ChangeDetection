package com.bernaferrari.changedetection.sync

import android.content.Context
import android.graphics.*
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bernaferrari.changedetection.library.R
import com.bernaferrari.changedetection.preview.ChangePreview
import com.bernaferrari.changedetection.preview.ChangePreviewRepository
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream

internal data class PreparedAndroidAlert(val summary: String?, val image: Bitmap? = null)

internal suspend fun prepareAndroidAlert(context: Context, site: Site, snapId: String): PreparedAndroidAlert {
    val koin = org.koin.mp.KoinPlatformTools.defaultContext().getOrNull()
    val snaps = koin?.getOrNull<SnapsRepository>()
    val previews = koin?.getOrNull<ChangePreviewRepository>()
    val summary = if (koin == null) null else changeAlertText(site, snapId, snaps, previews).second
    val enabled = koin?.getOrNull<DataStore<Preferences>>()?.data?.first()?.get(NotificationPreferences.IMAGE_PREVIEWS) ?: false
    if (!enabled || site.captureMode != CaptureMode.SCREENSHOT || snaps == null || previews == null) return PreparedAndroidAlert(summary)
    val image = try {
        val snap = snaps.getSnapById(snapId) ?: return PreparedAndroidAlert(summary)
        val preview = previews.forVersion(snap) as? ChangePreview.Crop ?: return PreparedAndroidAlert(summary)
        val before = readNotificationSnapshot(context, preview.beforeSnapId) ?: return PreparedAndroidAlert(summary)
        val after = readNotificationSnapshot(context, preview.afterSnapId) ?: return PreparedAndroidAlert(summary)
        renderNotificationPreview(before, after, preview, context.getString(R.string.change_notification_before), context.getString(R.string.change_notification_now))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
    return PreparedAndroidAlert(summary, image)
}

/** No fetch: bounded reads of saved snapshots. Oversized captures fall back to the ordinary alert. */
internal suspend fun readNotificationSnapshot(context: Context, name: String): ByteArray? = withContext(Dispatchers.IO) {
    if (name.isBlank() || name.any { it == '/' || it == '\\' }) return@withContext null
    context.openFileInput(name).use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16_384)
        while (true) {
            currentCoroutineContext().ensureActive()
            val count = input.read(buffer)
            if (count < 0) break
            if (output.size() + count > MAX_PREVIEW_INPUT_BYTES) return@withContext null
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    }
}
internal const val MAX_PREVIEW_INPUT_BYTES = 2 * 1024 * 1024

/** A single local changed region, not a full-page thumbnail. Result is 720×300 (<1 MiB ARGB). */
internal suspend fun renderNotificationPreview(before: ByteArray, after: ByteArray, change: ChangePreview.Crop, beforeLabel: String, afterLabel: String): Bitmap? = withContext(Dispatchers.Default) {
    if (before.size > MAX_PREVIEW_INPUT_BYTES || after.size > MAX_PREVIEW_INPUT_BYTES) return@withContext null
    val region = change.regions.maxByOrNull { it.weight }
    val bounds = change.bounds
    if (region == null && bounds == null) return@withContext null
    suspend fun panel(bytes: ByteArray, old: Boolean): Bitmap? {
        currentCoroutineContext().ensureActive()
        val dimensions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, dimensions)
        val w = dimensions.outWidth
        val h = dimensions.outHeight
        if (w <= 0 || h <= 0 || w.toLong() * h > 50_000_000) return null
        val l = region?.left ?: bounds!!.left
        val right = region?.right ?: bounds!!.right
        val top = region?.let { if (old) it.beforeTop else it.afterTop } ?: bounds!!.top
        val bottom = region?.let { if (old) it.beforeBottom else it.afterBottom } ?: bounds!!.bottom
        if (listOf(l, right, top, bottom).any { !it.isFinite() }) return null
        val x = (l * w).toInt().coerceIn(0, w - 1)
        val cw = ((right * w).toInt() - x).coerceIn(1, w - x)
        val y = ((top * h).toInt() - 8).coerceIn(0, h - 1)
        // Limit tall changes to their leading portion; tapping still opens the complete comparison.
        val ch = minOf(((bottom * h).toInt() - y).coerceAtLeast(32), (cw * 0.75f).toInt().coerceAtLeast(1), h - y)
        var sample = 1
        while (cw.toLong() * ch / (sample.toLong() * sample) > 256_000 || maxOf(cw, ch) / sample > 720) sample *= 2
        @Suppress("DEPRECATION")
        val decoder = BitmapRegionDecoder.newInstance(bytes, 0, bytes.size, false)
        return try {
            currentCoroutineContext().ensureActive()
            decoder.decodeRegion(Rect(x, y, x + cw, y + ch), BitmapFactory.Options().apply { inSampleSize = sample })
        } finally {
            decoder.recycle()
        }
    }
    val first = panel(before, true) ?: return@withContext null
    try {
        val second = panel(after, false) ?: return@withContext null
        try {
            currentCoroutineContext().ensureActive()
            val result = Bitmap.createBitmap(720, 300, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(result)
                canvas.drawColor(Color.WHITE)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                paint.color = Color.DKGRAY
                paint.textSize = 28f
                paint.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText(beforeLabel, 16f, 34f, paint)
                canvas.drawText(afterLabel, 376f, 34f, paint)
                fun drawPanel(bitmap: Bitmap, x: Float) {
                    val scale = minOf(328f / bitmap.width, 236f / bitmap.height)
                    canvas.drawBitmap(bitmap, null, RectF(x, 48f, x + bitmap.width * scale, 48f + bitmap.height * scale), paint)
                }
                drawPanel(first, 16f)
                drawPanel(second, 376f)
                paint.color = Color.rgb(220, 185, 0)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                canvas.drawRect(376f, 48f, 704f, 284f, paint)
                currentCoroutineContext().ensureActive()
                result
            } catch (error: Throwable) {
                result.recycle()
                throw error
            }
        } finally {
            second.recycle()
        }
    } finally {
        first.recycle()
    }
}
