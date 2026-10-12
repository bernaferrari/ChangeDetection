package com.bernaferrari.changedetection.addedit

import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import com.bernaferrari.changedetection.sync.ChangeNotifier
import com.bernaferrari.changedetection.sync.SyncEngine
import com.bernaferrari.changedetection.util.FileStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions

class CapturePreviewerTest {
    @Test fun regionPreviewUsesTheSameSelectedContentAsCapture() = runTest {
        val html = "<nav>Ad</nav><div id='price'>100<span class='timer'>10s</span></div>"
        val client = HttpClient(MockEngine { respond(html, headers = headersOf("Content-Type", "text/html")) })
        try {
            val previewer = HtmlCapturePreviewer(SyncEngine(client, mock(), mock(), mock(), mock(), ErrorHandler(), mock()))
            val region = com.bernaferrari.changedetection.detection.TextRegion("#price", ".timer")
            val preview = previewer.capture("https://example.com/product", CaptureMode.TEXT, region) as CapturePreview.Html
            val captured = com.bernaferrari.changedetection.detection.prepareTextCapture(html.toByteArray(), "text/html", region)
            assertEquals("100", preview.text)
            assertEquals(com.bernaferrari.changedetection.util.readableSnapshotText(captured.data.decodeToString(), true), preview.text)
            val missing = runCatching { previewer.capture("https://example.com/product", CaptureMode.TEXT, region.copy(include = "#missing")) }.exceptionOrNull()
            assertTrue(missing is com.bernaferrari.changedetection.detection.TextRegionException)
        } finally {
            client.close()
        }
    }

    @Test fun htmlPreviewFetchesRealContentWithoutTouchingPersistenceOrAlerts() = runTest {
        val html = "<html><body><h1>Available today</h1><p>${"Details ".repeat(2_000)}</p></body></html>"
        val client = HttpClient(MockEngine { respond(html, headers = headersOf("Content-Type", "text/html")) })
        val database = mock<ChangeDatabase>()
        val sites = mock<SitesDao>()
        val snaps = mock<SnapsDao>()
        val files = mock<FileStorage>()
        val notifier = mock<ChangeNotifier>()
        try {
            val previewer = HtmlCapturePreviewer(SyncEngine(client, database, sites, snaps, files, ErrorHandler(), notifier))
            val result = previewer.capture("https://example.com/product", CaptureMode.HTML) as CapturePreview.Html
            assertTrue(result.text.startsWith("Available today"))
            assertEquals(12_000, result.text.length)
            assertEquals(html.toByteArray().size, result.totalBytes)
            verifyNoInteractions(database, sites, snaps, files, notifier)
        } finally {
            client.close()
        }
    }
}
