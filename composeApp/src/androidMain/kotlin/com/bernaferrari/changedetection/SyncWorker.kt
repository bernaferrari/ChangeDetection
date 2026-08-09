/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bernaferrari.changedetection.repo.ComparisonMethod
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.sync.SyncEngine
import com.bernaferrari.changedetection.sync.SyncResult
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.first
import org.koin.android.annotation.KoinWorker

@KoinWorker
class SyncWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val syncEngine: SyncEngine,
    private val sitesRepository: SitesRepository,
    private val dataStore: DataStore<Preferences>,
) : CoroutineWorker(context, workerParams) {

    init {
        Napier.d("SyncWorker created successfully with Koin")
    }

    override suspend fun doWork(): Result {
        Napier.d("SyncWorker.doWork() starting")
        return try {
            val siteId = inputData.getString("id")

            if (siteId != null) {
                // Sync specific site
                syncSingleSite(siteId)
            } else {
                // Sync all sites
                syncAllSites()
            }
        } catch (e: Exception) {
            Napier.e("Sync work failed", e)
            Result.retry()
        }
    }

    private suspend fun syncSingleSite(siteId: String): Result {
        val site = sitesRepository.getSiteById(siteId) ?: return Result.failure()
        val comparisonMethod = getComparisonMethod()

        return if (comparisonMethod == ComparisonMethod.SCREENSHOT) {
            triggerScreenshotService(siteId)
            Result.success()
        } else {
            syncEngine.syncSite(siteId).toWorkResult()
        }
    }

    private suspend fun syncAllSites(): Result {
        val comparisonMethod = getComparisonMethod()

        if (comparisonMethod == ComparisonMethod.HTML) {
            // Sync all HTML sites via SyncEngine
            val (successCount, results) = syncEngine.syncAllSites()
            Napier.d("SyncEngine processed $successCount sites")
            return if (results.any { it is SyncResult.Failed && it.error.isRetryable }) {
                Result.retry()
            } else {
                Result.success()
            }
        }

        // Screenshot mode: use service for every enabled site
        val sites = sitesRepository.getSites()
        for (site in sites) {
            if (site.isSyncEnabled) {
                triggerScreenshotService(site.id)
            }
        }

        return Result.success()
    }

    private fun SyncResult.toWorkResult(): Result = when (this) {
        is SyncResult.Success -> Result.success()
        is SyncResult.Skipped -> Result.success()
        is SyncResult.Failed -> if (error.isRetryable) Result.retry() else Result.failure()
    }

    private fun triggerScreenshotService(siteId: String) {
        val intent =
            android.content
                .Intent(
                    applicationContext,
                    com.bernaferrari.changedetection.sync.ScreenshotService::class.java,
                ).apply {
                    putExtra(com.bernaferrari.changedetection.sync.ScreenshotService.EXTRA_SITE_ID, siteId)
                }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
    }

    private suspend fun getComparisonMethod(): ComparisonMethod {
        val preferences = dataStore.data.first()
        val stored = preferences[SettingsViewModel.COMPARISON_METHOD_KEY]
        return runCatching {
            ComparisonMethod.valueOf(stored ?: ComparisonMethod.SCREENSHOT.name)
        }.getOrElse { ComparisonMethod.SCREENSHOT }
    }
}
