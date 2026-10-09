/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bernaferrari.changedetection.browser.IosPageCapture
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.ComparisonMethod
import com.bernaferrari.changedetection.sync.SyncEngine
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.first

/** Previews exactly what a check will save: page text over HTTP, or a WebKit screenshot. */
class IosCapturePreviewer(engine: SyncEngine, private val preferences: DataStore<Preferences>) : CapturePreviewer {
    private val html = HtmlCapturePreviewer(engine)

    override suspend fun capture(url: String, mode: CaptureMode): CapturePreview = capture(url, mode, com.bernaferrari.changedetection.detection.TextRegion())

    override suspend fun capture(url: String, mode: CaptureMode, region: com.bernaferrari.changedetection.detection.TextRegion): CapturePreview {
        val settings = preferences.data.first()
        val defaultMethod = ComparisonMethod.entries.firstOrNull { it.name == settings[SettingsViewModel.COMPARISON_METHOD_KEY] } ?: ComparisonMethod.SCREENSHOT
        if (mode.resolve(defaultMethod) == ComparisonMethod.HTML) return html.capture(url, mode, region)
        val png = IosPageCapture.capture(url, settings[SettingsViewModel.BROWSER_DELAY_KEY] ?: 2_500L, hide = region.exclude, only = region.include)
        return CapturePreview.Image(png, IosPageCapture.lastTitle?.let { cleanTitle(it) })
    }
}
