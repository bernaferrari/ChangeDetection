package com.bernaferrari.changedetection.addedit

import com.bernaferrari.changedetection.detection.TextRegion
import com.bernaferrari.changedetection.repo.CaptureMode
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.WebViewState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions

class LegacyHttpLiveCaptureTest {
    @Test fun rawHttpModesNeverReadOrSaveTheLoadedJavaScriptDom() = runTest {
        for (mode in listOf(CaptureMode.HTML, CaptureMode.SOURCE, CaptureMode.DEFAULT)) {
            val state = mock<WebViewState>()
            val navigator = mock<WebViewNavigator>()
            assertNull(captureLivePagePreview(state, navigator, "https://example.test/page", mode, TextRegion()))
            verifyNoInteractions(state, navigator)
        }
    }
}
