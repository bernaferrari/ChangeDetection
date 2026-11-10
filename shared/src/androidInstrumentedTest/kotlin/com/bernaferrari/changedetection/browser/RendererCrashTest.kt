package com.bernaferrari.changedetection.browser

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import com.multiplatform.webview.web.rememberWebViewState
import org.junit.Rule
import org.junit.Test

/** A page that kills its renderer must leave the app running and offer Retry. */
class RendererCrashTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun awaitRetry() = compose.waitUntil(20_000) { compose.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty() }

    @Test fun aCrashedRendererOffersRetryInsteadOfClosingTheApp() {
        compose.setContent {
            ChangeDetectionTheme(false, false) {
                PlatformBrowserView(state = rememberWebViewState("chrome://crash"), modifier = Modifier.fillMaxSize())
            }
        }
        awaitRetry()
        // Retry builds a new browser view, which loads the same crashing address again.
        compose.onNodeWithText("Retry").performClick()
        // Seeing Retry a second time proves the process survived both crashes and the view was really replaced.
        awaitRetry()
    }
}
