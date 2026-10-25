package com.bernaferrari.changedetection.addedit

import com.bernaferrari.changedetection.detection.TextRegion
import com.bernaferrari.changedetection.detection.TextRegionException
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.sync.SyncEngine
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

class DesktopHttpCapturePolicyTest {
    @Test fun desktopTextUsesFreshHttpPreviewRatherThanASignedInLiveDom() = runTest {
        val state = mock<WebViewState>()
        val navigator = mock<WebViewNavigator>()
        assertNull(captureLivePagePreview(state, navigator, "https://example.test/page", CaptureMode.TEXT, TextRegion()))
        verifyNoInteractions(state, navigator)
    }
    @Test fun scriptOnlyRegionIsRejectedDuringDesktopSetupBeforeSavingAnInconsistentBaseline() = runTest {
        val engine = mock<SyncEngine>()
        val raw = "<html><body><p>Static page</p><script>document.body.insertAdjacentHTML('beforeend', '<p id=watched>Dynamic price</p>')</script></body></html>"
        whenever(engine.fetchFromServer(any())).thenReturn(SyncEngine.FetchResult.Success("text/html", raw.encodeToByteArray()))
        val failure = runCatching { HtmlCapturePreviewer(engine).capture("https://example.test/page", CaptureMode.TEXT, TextRegion("#watched")) }.exceptionOrNull()
        assertTrue(failure is TextRegionException)
        verify(engine, never()).establishFirstVersion(any(), any())
    }
}
