package com.bernaferrari.changedetection.addedit

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.ComparisonMethod
import com.bernaferrari.changedetection.sync.AndroidScreenshotBrowserFactory
import com.bernaferrari.changedetection.sync.ScreenshotCaptureOutcome
import com.bernaferrari.changedetection.sync.SyncEngine
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class AndroidCapturePreviewer(context: Context, engine: SyncEngine, private val preferences: DataStore<Preferences>) : CapturePreviewer {
    private val html = HtmlCapturePreviewer(engine)
    private val browsers = AndroidScreenshotBrowserFactory(context)
    private val singlePreview = Mutex()

    override suspend fun capture(url: String, mode: CaptureMode): CapturePreview = capture(url, mode, com.bernaferrari.changedetection.detection.TextRegion())

    override suspend fun keepAsFirstVersion(site: com.bernaferrari.changedetection.repo.Site, preview: CapturePreview): Boolean = html.keepAsFirstVersion(site, preview)

    override suspend fun capture(url: String, mode: CaptureMode, region: com.bernaferrari.changedetection.detection.TextRegion): CapturePreview = singlePreview.withLock {
        withTimeout(90_000) {
            val settings = preferences.data.first()
            val defaultMethod = ComparisonMethod.entries.firstOrNull { it.name == settings[SettingsViewModel.COMPARISON_METHOD_KEY] } ?: ComparisonMethod.SCREENSHOT
            if (mode != CaptureMode.TEXT && mode.resolve(defaultMethod) == ComparisonMethod.HTML) {
                html.capture(url, mode, region)
            } else {
                withContext(Dispatchers.Main.immediate) {
                    val browser = browsers.create()
                    try {
                        when (val result = browser.capture(previewSite(url).copy(captureMode = mode, textIncludeSelector = region.include, textExcludeSelector = region.exclude), settings[SettingsViewModel.BROWSER_DELAY_KEY] ?: 5_000L)) {
                            // Same capture and hand-off as AndroidScreenshotSyncer, so it can stand in for the first check.
                            is ScreenshotCaptureOutcome.Success -> {
                                val capture = SyncEngine.FetchResult.Success(result.contentType, result.bytes, capturedWithBrowser = true)
                                if (mode == CaptureMode.TEXT) {
                                    previewFromCapture(capture, url, mode, region)
                                } else {
                                    CapturePreview.Image(result.bytes, result.title?.let { cleanTitle(it) }, result.icon, capture)
                                }
                            }

                            is ScreenshotCaptureOutcome.Failure -> error(result.message)
                        }
                    } finally {
                        browser.close()
                    }
                }
            }
        }
    }
}
