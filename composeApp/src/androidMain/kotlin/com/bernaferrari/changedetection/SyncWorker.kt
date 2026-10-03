/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bernaferrari.changedetection.repo.ComparisonMethod
import com.bernaferrari.changedetection.library.R
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.sync.SyncEngine
import com.bernaferrari.changedetection.sync.SyncResult
import com.bernaferrari.changedetection.sync.AndroidScreenshotSyncer
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import org.koin.android.annotation.KoinWorker

@KoinWorker
class SyncWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val checkRunner: com.bernaferrari.changedetection.sync.CheckRunner,
    private val sitesRepository: SitesRepository,
    private val dataStore: DataStore<Preferences>,
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val NOTIFICATION_ID = 2002
        private const val CHANNEL_ID = "sync_channel"
    }

    init {
        Napier.d("SyncWorker created successfully with Koin")
    }

    // Expedited work on API 30-30 requires getForegroundInfo() or it crashes when
    // out-of-quota work falls back to expedited execution.
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Sync Service",
                    NotificationManager.IMPORTANCE_LOW,
                )
            notificationManager.createNotificationChannel(channel)
        }

        val notification =
            NotificationCompat
                .Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setContentTitle(applicationContext.getString(R.string.app_name))
                .setContentText(applicationContext.getString(R.string.status_sync_in_progress))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .setOngoing(true)
                .build()

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
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
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            Napier.e("Sync work failed", e)
            Result.retry()
        }
    }

    private suspend fun syncSingleSite(siteId: String): Result {
        val site = sitesRepository.getSiteById(siteId) ?: return Result.success()
        if (site.captureMode.resolve(getComparisonMethod()) == ComparisonMethod.SCREENSHOT) setForeground(getForegroundInfo())
        return checkRunner.check(siteId).toWorkResult()
    }

    private suspend fun syncAllSites(): Result {
        val sites = sitesRepository.getSites().filter { it.isSyncEnabled }
        if (sites.isEmpty()) return Result.success()
        val defaultMethod = getComparisonMethod()
        if (sites.any { it.captureMode.resolve(defaultMethod) == ComparisonMethod.SCREENSHOT }) setForeground(getForegroundInfo())
        val results = sites.map { checkRunner.check(it.id) }
        return if (results.any { it is SyncResult.Failed && it.error.isRetryable }) Result.retry() else Result.success()
    }

    private fun SyncResult.toWorkResult(): Result = when (this) {
        is SyncResult.Success -> Result.success()
        is SyncResult.Skipped -> Result.success()
        is SyncResult.Failed -> if (error.isRetryable) Result.retry() else Result.failure()
    }
    private suspend fun getComparisonMethod(): ComparisonMethod {
        val preferences = dataStore.data.first()
        val stored = preferences[SettingsViewModel.COMPARISON_METHOD_KEY]
        return runCatching {
            ComparisonMethod.valueOf(stored ?: ComparisonMethod.SCREENSHOT.name)
        }.getOrElse { ComparisonMethod.SCREENSHOT }
    }
}
