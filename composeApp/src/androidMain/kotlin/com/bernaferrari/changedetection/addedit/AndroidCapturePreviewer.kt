/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
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

    override suspend fun capture(url: String, mode: CaptureMode, region: com.bernaferrari.changedetection.detection.TextRegion): CapturePreview = singlePreview.withLock {
        withTimeout(90_000) {
            val settings = preferences.data.first()
            val defaultMethod = ComparisonMethod.entries.firstOrNull { it.name == settings[SettingsViewModel.COMPARISON_METHOD_KEY] } ?: ComparisonMethod.SCREENSHOT
            if (mode.resolve(defaultMethod) == ComparisonMethod.HTML) {
                html.capture(url, mode, region)
            } else {
                withContext(Dispatchers.Main.immediate) {
                    val browser = browsers.create()
                    try {
                        when (val result = browser.capture(previewSite(url), settings[SettingsViewModel.BROWSER_DELAY_KEY] ?: 5_000L)) {
                            is ScreenshotCaptureOutcome.Success -> CapturePreview.Image(result.bytes)
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
