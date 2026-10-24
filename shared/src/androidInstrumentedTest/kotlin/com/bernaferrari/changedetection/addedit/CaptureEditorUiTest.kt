package com.bernaferrari.changedetection.addedit

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureEditorUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun regionPresetsAndCustomSelectorsWorkAtLargeText() {
        var region by mutableStateOf(com.bernaferrari.changedetection.detection.TextRegion())
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 2f)) {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        TextRegionFields(region, error = false, enabled = true, onChoose = { region = it })
                    }
                }
            }
        }
        compose.onNodeWithText("Main content").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(MAIN_TEXT_REGION, region.include) }
        compose.onNodeWithText("CSS selectors").performScrollTo().performClick()
        compose.onNodeWithText("Include CSS selector (optional)").performScrollTo().performTextReplacement("#price")
        compose.onNodeWithText("Ignore CSS selector (optional)").performScrollTo().performTextReplacement(".timer")
        compose.runOnIdle { assertEquals(com.bernaferrari.changedetection.detection.TextRegion("#price", ".timer"), region) }
        compose.onNodeWithText("Whole page").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(com.bernaferrari.changedetection.detection.TextRegion(), region) }
    }

    @Test fun legacyFilterExplanationAndRemovalWorkAtLargeText() {
        var removals = 0
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 2f)) {
                ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                    LegacyThresholdNotice(512, enabled = true, onRemove = { removals++ })
                }
            }
        }
        compose.onNodeWithText("Legacy byte filter: 512 bytes").assertIsDisplayed()
        compose.onNodeWithText("Remove filter on save").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, removals) }
    }

    @Test fun readableAndSourceRulesRemainSelectableAtLargeText() {
        var chosen by mutableStateOf(CaptureMode.TEXT)
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 2f)) {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    CompareChoice(chosen, { chosen = it }, enabled = true, screenshotAvailable = true, isEditing = false)
                }
            }
        }
        compose.onNodeWithText("Code").performClick()
        compose.runOnIdle { assertEquals(CaptureMode.HTML, chosen) }
        compose.onNodeWithText("Any source change").performClick()
        compose.runOnIdle { assertEquals(CaptureMode.SOURCE, chosen) }
        compose.onNodeWithText("Any source change").performClick()
        compose.runOnIdle { assertEquals(CaptureMode.SOURCE, chosen) }
        compose.onNodeWithText("Page HTML").performClick()
        compose.runOnIdle { assertEquals(CaptureMode.HTML, chosen) }
    }

    @Test fun realScreenshotCanExpandAndReturnToTheUnsavedEditor() {
        val html = """<meta name="viewport" content="width=device-width,initial-scale=1"><body style="margin:0;background:white;color:#182226;font-family:sans-serif"><h1>Camera restock</h1><p>12 cameras available today</p><div style="height:1800px;background:#eef3f8">Delivery details</div><h2>End of captured page</h2></body>"""
        com.bernaferrari.changedetection.browser.TestPageServer(200, html).use { server ->
            val bytes = kotlinx.coroutines.runBlocking {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
                    val browser = com.bernaferrari.changedetection.sync.AndroidScreenshotBrowserFactory(
                        androidx.test.core.app.ApplicationProvider.getApplicationContext(),
                    ).create()
                    try {
                        val result = browser.capture(previewSite(server.url), 300)
                        (result as com.bernaferrari.changedetection.sync.ScreenshotCaptureOutcome.Success).bytes
                    } finally {
                        browser.close()
                    }
                }
            }
            val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
            java.io.File(context.getExternalFilesDir(null), "capture-source.png").writeBytes(bytes)
            var saved = false
            compose.setContent {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    AddEditConfirmScreen(
                        url = server.url, urlError = null, title = "Camera restock", isEditing = false,
                        isSaving = false, saveError = null, captureMode = CaptureMode.SCREENSHOT, onCaptureModeChange = {},
                        onUrlChange = {}, onTitleChange = {}, onNavigateUp = {}, onSave = { saved = true },
                        preview = CapturePreviewState.Ready(CapturePreview.Image(bytes)),
                        onPreview = {}, onClosePreview = {},
                    )
                }
            }
            compose.onNodeWithText("Open page").assertIsDisplayed().performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithText("Watch this page").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Watch this page").performClick()
            // The review step slides in; on a busy device it can take more than a frame.
            compose.waitUntil(5_000) { compose.onAllNodesWithText("View full screenshot").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("View full screenshot").performScrollTo().performClick()
            compose.onNodeWithText("Done").assertIsDisplayed()
            compose.waitUntil(5_000) {
                compose.onAllNodes(hasContentDescription("Capture preview") and hasAnyAncestor(isDialog())).fetchSemanticsNodes().isNotEmpty()
            }
            android.os.ParcelFileDescriptor.AutoCloseInputStream(
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
                    .executeShellCommand("screencap -p /sdcard/Download/capture-expanded.png"),
            ).use { it.readBytes() }
            val scroll = compose.onNode(hasScrollAction() and hasAnyAncestor(isDialog()))
            scroll.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) { it(0f, 100_000f) }
            compose.waitForIdle()
            val range = scroll.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange]
            compose.waitUntil(5_000) { range.maxValue() > 0f && range.value() >= range.maxValue() - 1f }
            android.os.ParcelFileDescriptor.AutoCloseInputStream(
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
                    .executeShellCommand("screencap -p /sdcard/Download/capture-expanded-bottom.png"),
            ).use { it.readBytes() }
            compose.onNodeWithText("Done").performClick()
            compose.onNodeWithText("Start watching").assertIsDisplayed()
            compose.runOnIdle { assertEquals(false, saved) }
        }
    }

    @Test fun duplicateFeedbackIsVisibleInTheReviewStepAndBackIsConsumedWhileSaving() {
        var saving by mutableStateOf(false)
        var duplicate by mutableStateOf<String?>(null)
        var back = 0
        var opened: String? = null
        compose.setContent {
            CompositionLocalProvider(LocalBrowserAvailable provides false) {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    com.bernaferrari.changedetection.util.BackHandler { back++ }
                    AddEditConfirmScreen(
                        url = "http://localhost", urlError = null, title = "Page", isEditing = false,
                        isSaving = saving, saveError = null, captureMode = CaptureMode.TEXT, onCaptureModeChange = {},
                        onUrlChange = {}, onTitleChange = {}, onNavigateUp = { back++ },
                        onSave = { duplicate = "existing" }, duplicateSiteId = duplicate,
                        onOpenDuplicate = { opened = it }, onDismissDuplicate = { duplicate = null },
                        preview = CapturePreviewState.Ready(CapturePreview.Html("Available", 9)),
                        onPreview = {}, onClosePreview = {},
                    )
                }
            }
        }
        compose.onNodeWithText("Open page").assertIsEnabled().performClick()
        compose.onNodeWithText("Start watching").performClick()
        compose.onNodeWithText("You're already monitoring this page").assertExists()
        compose.onNodeWithText("Open").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("existing", opened)
            saving = true
        }
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Saving…").assertExists()
        compose.runOnIdle { assertEquals(0, back) }
    }

    @Test fun editorOffersCaptureChoiceAndKeepsSaveReachable() {
        var mode by mutableStateOf(CaptureMode.DEFAULT)
        var saved: CaptureMode? = null
        var preview by mutableStateOf<CapturePreviewState>(CapturePreviewState.Idle)
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                AddEditConfirmScreen(
                    url = "https://example.com", urlError = null, title = "Example page", isEditing = true,
                    isSaving = false, saveError = null, captureMode = mode, onCaptureModeChange = { mode = it },
                    onUrlChange = {}, onTitleChange = {}, onNavigateUp = {}, onSave = { saved = mode },
                    preview = preview,
                    onPreview = { preview = CapturePreviewState.Ready(CapturePreview.Html("Camera restock: available today", 42)) },
                    onClosePreview = { preview = CapturePreviewState.Idle },
                )
            }
        }
        compose.onNodeWithText("Code").performScrollTo().performClick()
        compose.onNodeWithText("Changing what’s watched starts fresh from the next check. Your history stays, and the switch itself won’t send an alert.").assertExists()
        compose.onNodeWithText("Camera restock: available today").performScrollTo().assertIsDisplayed()
        compose.waitForIdle()
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("screencap -p /sdcard/Download/capture-editor.png"),
        ).use { it.readBytes() }
        compose.onNodeWithText("Save").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(CaptureMode.HTML, saved) }
    }
}
