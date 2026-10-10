/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bernaferrari.changedetection.browser.IosPageCapture
import com.bernaferrari.changedetection.repo.ComparisonMethod
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/** One check on iOS: screenshot pages are captured with WebKit, text pages are fetched over HTTP. */
class IosCheckRunner(
    private val sites: SitesRepository,
    private val engine: SyncEngine,
    private val preferences: DataStore<Preferences>,
) : CheckRunner {
    override suspend fun check(siteId: String): SyncResult {
        val site = sites.getSiteById(siteId) ?: return SyncResult.Skipped(siteId, "Page deleted")
        if (!site.isSyncEnabled) return SyncResult.Skipped(siteId, "Page paused")
        val settings = preferences.data.first()
        val defaultMethod = ComparisonMethod.entries.firstOrNull { it.name == settings[SettingsViewModel.COMPARISON_METHOD_KEY] } ?: ComparisonMethod.SCREENSHOT
        if (site.captureMode.resolve(defaultMethod) == ComparisonMethod.HTML) return engine.syncSite(siteId)
        return try {
            val png = IosPageCapture.capture(site.url, settings[SettingsViewModel.BROWSER_DELAY_KEY] ?: DEFAULT_SETTLE_MS, hide = site.textExcludeSelector, only = site.textIncludeSelector)
            IosPageCapture.lastIcon?.let { sites.updateIcon(site.id, it) }
            engine.processFetchResult(site, SyncEngine.FetchResult.Success("image/png", png), site.threshold)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            engine.processFetchResult(site, SyncEngine.FetchResult.Error(error.message ?: "Could not capture the page", error), site.threshold)
        }
    }

    /** Every active page, one at a time (WebKit captures are heavy). */
    suspend fun checkAll(onlyDueAfterMinutes: Long? = null): Int {
        val now = com.bernaferrari.changedetection.util.getCurrentTimeMillis()
        val due = sites.getSites().filter { site ->
            site.isSyncEnabled && (onlyDueAfterMinutes == null || now - site.timestamp >= onlyDueAfterMinutes * 60_000L)
        }
        due.forEach { runCatching { check(it.id) } }
        return due.size
    }

    private companion object {
        const val DEFAULT_SETTLE_MS = 2_500L
    }
}
