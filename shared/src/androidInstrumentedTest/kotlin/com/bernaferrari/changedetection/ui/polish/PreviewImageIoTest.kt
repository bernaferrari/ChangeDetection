package com.bernaferrari.changedetection.ui.polish

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.bernaferrari.changedetection.preview.ChangePreviewRepository
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.storage.SnapshotFileCleanup
import com.bernaferrari.changedetection.util.FileStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger

class PreviewImageIoTest {
    @Test fun eachScreenshotIsReadOnceForPixelAndRegionAnalysis() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "preview-io-${java.util.UUID.randomUUID()}.db"
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(context, context.getDatabasePath(name).absolutePath), BundledSQLiteDriver())
        val content = mutableMapOf("old" to png(Color.RED), "new" to png(Color.BLUE))
        val reads = AtomicInteger()
        val files = object : FileStorage {
            override fun readBytes(fileName: String): ByteArray {
                reads.incrementAndGet()
                return content.getValue(fileName)
            }
            override fun writeBytes(fileName: String, bytes: ByteArray) {
                content[fileName] = bytes
            }
            override fun fileExists(fileName: String) = fileName in content
            override fun deleteFile(fileName: String) = content.remove(fileName) != null
        }
        try {
            val repository = SnapsRepository(SnapsLocalDataSource(AppExecutors(), db.snapsDao(), db.siteDao(), files, SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files)))
            val previews = ChangePreviewRepository(repository)
            val before = Snap("old", "page", 1L, "image/png", "", content.getValue("old").size)
            val after = before.copy(snapId = "new", timestamp = 2L)
            val preview = previews.between(before, after)
            assertEquals("Pixel comparison and region analysis must reuse the same bytes", 2, reads.get())
            assertSame(preview, previews.between(before, after))
            assertEquals(2, reads.get())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
    private fun png(color: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        return ByteArrayOutputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            bitmap.recycle()
            it.toByteArray()
        }
    }
}
