package com.bernaferrari.changedetection.diagnostics

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.bernaferrari.changedetection.error.NetworkError
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.ui.components.CheckFailureNotice
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DiagnosticUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val watch = "private-watch-${java.util.UUID.randomUUID()}?token=SECRET"
    private fun seed(): AndroidDiagnosticStore = AndroidDiagnostics.install(context, "debug", "abcdef123456").also {
        runBlocking {
            try {
                recordDiagnostic(CaptureMode.TEXT, false, false, DiagnosticTrigger.CHECK, watch) {
                    diagnosticStage(DiagnosticStage.NAVIGATE) {
                        diagnosticMetric(DiagnosticStage.NAVIGATE, DiagnosticMetric.HTTP_STATUS, 503)
                        throw NetworkError(503, "Cookie: SECRET <html>PERSONAL</html>")
                    }
                }
            } catch (_: NetworkError) { }
            assertEquals(1, it.report(watch).attemptCount)
        }
    }

    @Test fun failedCheckPreviewShareCancelAndRepeatedNavigationNeverSendAutomatically() {
        val store = seed()
        var details by mutableStateOf(true)
        compose.setContent {
            ChangeDetectionTheme(false, false) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    if (details) {
                        Text("Synthetic failed page")
                        CheckFailureNotice("SERVER")
                        DiagnosticsEntry(watch)
                        TextButton(onClick = { details = false }) { Text("Back to support") }
                    } else {
                        Text("Support settings")
                        DiagnosticsEntry()
                        TextButton(onClick = { details = true }) { Text("Return to page") }
                    }
                }
            }
        }
        compose.onNodeWithTag("export-diagnostics").performClick().performClick()
        compose.onAllNodesWithText("Diagnostics report").assertCountEquals(1)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("share-diagnostics").fetchSemanticsNodes().any { !it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) } }
        compose.onNodeWithTag("share-diagnostics").assertIsEnabled()
        compose.onNodeWithTag("diagnostic-report-content").performScrollToNode(hasText("HTTP_STATUS", substring = true))
        compose.onNodeWithText("HTTP_STATUS", substring = true).assertIsDisplayed()
        assertFalse(runBlocking { store.report(watch).json }.contains("SECRET"))
        compose.onNodeWithTag("share-diagnostics").performClick()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        fun foreground(): String = ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("dumpsys activity activities")).use { stream ->
            stream.bufferedReader().useLines { lines -> lines.firstOrNull { it.contains("topResumedActivity=") }.orEmpty() }
        }
        compose.waitUntil(5_000) { foreground().contains("ChooserActivity") }
        assertTrue("System chooser should be foreground; never select a recipient", foreground().contains("ChooserActivity"))
        // The chooser Activity can be resumed before its window accepts input.
        automation.waitForIdle(100, 5_000)
        assertTrue(automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK))
        compose.waitUntil(5_000) { runCatching { compose.onAllNodesWithText("Close").fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false) }
        compose.onNodeWithText("Close").assertIsEnabled().performClick()
        compose.onNodeWithText("Synthetic failed page").assertIsDisplayed()
        compose.onNodeWithText("Back to support").performClick()
        compose.onNodeWithTag("export-diagnostics").performClick()
        compose.onNodeWithText("Diagnostics report").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Return to page").performClick()
        compose.onNodeWithText("Synthetic failed page").assertIsDisplayed()
    }

    @Test fun largeTextReportKeepsShareAndCloseReachableAndCanBeDismissedDuringLoad() {
        seed()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                ChangeDetectionTheme(false, false) { Column(Modifier.width(320.dp)) { DiagnosticsEntry(watch) } }
            }
        }
        compose.onNodeWithTag("export-diagnostics").performClick()
        compose.onNodeWithText("Close").assertIsDisplayed().performClick()
        compose.onNodeWithTag("export-diagnostics").performClick()
        compose.onNodeWithTag("share-diagnostics").assertIsDisplayed()
        compose.onNodeWithText("Close").assertIsDisplayed().performClick()
        compose.onAllNodesWithText("Diagnostics report").assertCountEquals(0)
    }
}
