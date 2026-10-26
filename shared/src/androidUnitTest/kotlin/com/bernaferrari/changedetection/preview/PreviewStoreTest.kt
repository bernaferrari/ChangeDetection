package com.bernaferrari.changedetection.preview

import com.bernaferrari.changedetection.image.ChangedBounds
import com.bernaferrari.changedetection.image.VisualRegion
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.util.FileStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PreviewStoreTest {
    private class MemoryFiles : FileStorage {
        val files = mutableMapOf<String, ByteArray>()
        override fun writeBytes(fileName: String, bytes: ByteArray) { files[fileName] = bytes }
        override fun readBytes(fileName: String): ByteArray = files.getValue(fileName)
        override fun deleteFile(fileName: String): Boolean = files.remove(fileName) != null
        override fun fileExists(fileName: String): Boolean = fileName in files
    }

    private val crop = ChangePreview.Crop(
        beforeSnapId = "a",
        afterSnapId = "b",
        bounds = ChangedBounds(0f, 0.1f, 1f, 0.2f),
        percent = 1.5,
        sizeChanged = false,
        regions = listOf(VisualRegion(VisualRegion.Kind.Changed, 0f, 1f, 0.1f, 0.2f, 0.1f, 0.2f)),
    )
    private val words = ChangePreview.Words("Key dates", listOf(EditSegment("31 October", EditSegment.Kind.Removed), EditSegment("14 November", EditSegment.Kind.Added)), 1, false, true)

    @Test fun previewsSurviveARelaunch() = runBlocking {
        val files = MemoryFiles()
        val store = PreviewStore(files, scope = CoroutineScope(SupervisorJob() + Dispatchers.Default))
        store.put("a>b", crop)
        store.put("c>d", words)
        store.put("e>f", ChangePreview.NothingVisible)
        waitForWrite(files)

        val relaunched = PreviewStore(files)
        assertEquals(crop, relaunched.get("a>b"))
        assertEquals(words, relaunched.get("c>d"))
        assertEquals(ChangePreview.NothingVisible, relaunched.get("e>f"))
    }

    @Test fun keepsOnlyTheNewestPairs() = runBlocking {
        val files = MemoryFiles()
        val store = PreviewStore(files, capacity = 2)
        store.put("1", words)
        store.put("2", words)
        store.put("3", words)
        assertNull(store.get("1"))
        assertEquals(words, store.get("3"))
    }

    @Test fun anUnreadableFileIsIgnored() = runBlocking {
        val files = MemoryFiles()
        files.files["change-previews.json"] = "{not json".encodeToByteArray()
        assertNull(PreviewStore(files).get("a>b"))
    }

    @Test fun aStoredPreviewIsNotWorkedOutAgain() = runBlocking {
        val files = MemoryFiles()
        PreviewStore(files).apply {
            put("before>after", words)
            waitForWrite(files)
        }
        val snaps = mock<SnapsRepository>()
        val before = Snap("before", "page", 1L, "text/html", "UTF-8", 20)
        val after = before.copy(snapId = "after", timestamp = 2L)
        whenever(snaps.getSnapContent("before")).thenReturn("<p>x</p>".encodeToByteArray())
        val previews = ChangePreviewRepository(snaps, PreviewStore(files))
        assertEquals(words, previews.between(before, after))
        verify(snaps, never()).getSnapContent("before")
        Unit
    }

    private suspend fun waitForWrite(files: MemoryFiles) {
        repeat(50) {
            if (files.fileExists("change-previews.json")) return
            delay(100)
        }
        error("The store never wrote its file")
    }
}
