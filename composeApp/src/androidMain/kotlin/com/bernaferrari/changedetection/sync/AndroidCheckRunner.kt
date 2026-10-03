/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bernaferrari.changedetection.repo.ComparisonMethod
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout

/** One awaited check shared by manual, scheduled and Turbo entry points. */
class AndroidCheckRunner(
    private val sites: SitesRepository,
    private val engine: SyncEngine,
    private val screenshots: AndroidScreenshotSyncer,
    private val preferences: DataStore<Preferences>,
) : CheckRunner {
    // Serialize before capture, not merely at commit. Finished pages release
    // their lock entries; the capacity limit allows at most two live WebViews.
    private val locks = PageCheckLocks()
    private val capacity = Semaphore(2)

    override suspend fun check(siteId: String): SyncResult = locks.withLock(siteId) {
        capacity.withPermit {
            val site = sites.getSiteById(siteId) ?: return@withPermit SyncResult.Skipped(siteId, "Page deleted")
            if (!site.isSyncEnabled) return@withPermit SyncResult.Skipped(siteId, "Page paused")
            try {
                withTimeout(90_000) {
                    val stored = preferences.data.first()[SettingsViewModel.COMPARISON_METHOD_KEY]
                    val defaultMethod = ComparisonMethod.entries.firstOrNull { it.name == stored } ?: ComparisonMethod.SCREENSHOT
                    if (site.captureMode.resolve(defaultMethod) == ComparisonMethod.HTML) {
                        engine.syncSite(siteId)
                    } else {
                        screenshots.syncSites(listOf(site)).single()
                    }
                }
            } catch (timeout: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                engine.processFetchResult(site, SyncEngine.FetchResult.Error("Page check timed out", timeout), site.threshold)
            }
        }
    }
}
