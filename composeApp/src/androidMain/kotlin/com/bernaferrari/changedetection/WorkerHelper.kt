/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.background.SyncJobInfo
import com.bernaferrari.changedetection.background.SyncState
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class WorkerHelper(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
) : MonitoringManager {
    companion object {
        const val UNIQUEWORK = "work"
        const val TURBO_WORK = "turbo_work"
        const val WIFI = "wifi"
        const val CHARGING = "charging"
        const val BATTERYNOTLOW = "batteryNotLow"
        const val IDLE = "idle"
        const val DELAY = "delay"
        const val BACKGROUND_SYNC = "backgroundSync"
    }

    override suspend fun updateWorkerWithConstraints(
        dataStore: DataStore<Preferences>,
        cancelCurrentWork: Boolean,
    ) {
        val preferences = dataStore.data.first()
        val constraints = WorkConstraints.fromDataStore(dataStore)

        if (cancelCurrentWork) {
            cancelWork()
        }

        if (preferences[SettingsViewModel.BACKGROUND_SYNC_KEY] == true) {
            val delay = preferences[SettingsViewModel.SYNC_INTERVAL_KEY] ?: 30L
            schedulePeriodicWork(delay, constraints)
        }
    }

    override fun cancelWork() {
        WorkManager.getInstance(context).cancelAllWorkByTag(UNIQUEWORK)
    }

    private fun schedulePeriodicWork(
        intervalMinutes: Long,
        constraints: WorkConstraints,
    ) {
        Napier.d("Scheduling periodic work with interval: $intervalMinutes minutes")

        val requiresDeviceIdle = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && constraints.deviceIdle

        val workerConstraints =
            Constraints
                .Builder()
                .apply {
                    setRequiredNetworkType(if (constraints.wifi) NetworkType.UNMETERED else NetworkType.CONNECTED)
                    if (constraints.batteryNotLow) setRequiresBatteryNotLow(true)
                    if (constraints.charging) setRequiresCharging(true)
                    if (requiresDeviceIdle) {
                        // Device Idle constraint causes a crash with PeriodicWorkRequest because it has default backoff criteria
                        // which cannot be unset and is incompatible with idle mode jobs.
                        // We must ignore this constraint for periodic work to prevent the crash.
                        Napier.w("Ignoring Device Idle constraint for Periodic Work to prevent crash (incompatible with backoff).")
                    }
                }.build()

        val inputData =
            workDataOf(
                WIFI to constraints.wifi,
                CHARGING to constraints.charging,
                BATTERYNOTLOW to constraints.batteryNotLow,
                IDLE to constraints.deviceIdle,
            )

        val periodicWorkBuilder =
            PeriodicWorkRequestBuilder<SyncWorker>(intervalMinutes, TimeUnit.MINUTES)
                .addTag(UNIQUEWORK)
                .setConstraints(workerConstraints)
                .setInputData(inputData)

        periodicWorkBuilder.setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUEWORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkBuilder.build(),
        )

        Napier.d("Periodic work scheduled successfully")
    }

    fun getWorkInfoFlow() = WorkManager.getInstance(context).getWorkInfosByTagFlow(UNIQUEWORK)

    fun getSiteWorkInfoFlow(siteId: String) = WorkManager.getInstance(context).getWorkInfosByTagFlow(siteId)

    override fun reloadSite(site: Site) {
        Napier.d("Scheduling one-time work for site: ${site.id}")

        val work =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(workDataOf("id" to site.id, "isSingle" to true))
                .addTag("singleEvent")
                .addTag("reload_${site.id}")
                .addTag(site.id)
                .addTag(UNIQUEWORK)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

        WorkManager
            .getInstance(context)
            .beginUniqueWork("reload_${site.id}", ExistingWorkPolicy.REPLACE, work)
            .enqueue()

        Napier.d("One-time work scheduled for site: ${site.id}")
    }

    fun reloadSiteById(siteId: String) {
        Napier.d("Scheduling one-time work for site ID: $siteId")
        scheduleOneTimeWork(siteId)
    }

    private fun scheduleOneTimeWork(siteId: String) {
        val work =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(workDataOf("id" to siteId, "isSingle" to true))
                .addTag(siteId)
                .addTag(UNIQUEWORK)
                .addTag("quick_monitor")
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

        WorkManager.getInstance(context).enqueue(work)
    }

    override fun startQuickMonitor(
        siteIds: List<String>,
        intervalSeconds: Int,
    ) {
        Napier.d("Starting Quick Monitor for ${siteIds.size} sites: $siteIds (${intervalSeconds}s interval)")

        try {
            if (siteIds.isEmpty()) {
                Napier.e("Cannot start Quick Monitor with empty site list")
                return
            }

            QuickMonitorService.start(context, siteIds, intervalSeconds)
            Napier.d("QuickMonitorService.start() called successfully")

            // Also trigger immediate work for the sites
            siteIds.forEach { siteId ->
                Napier.d("Scheduling immediate work for site: $siteId")
                scheduleOneTimeWork(siteId)
            }
        } catch (e: Exception) {
            Napier.e("Failed to start QuickMonitorService: ${e.message}")
        }
    }

    override fun stopQuickMonitor() {
        Napier.d("Stopping Quick Monitor")
        WorkManager.getInstance(context).cancelAllWorkByTag("quick_monitor")
        QuickMonitorService.stop(context)
    }

    override fun getSyncStatusFlow(): Flow<List<SyncJobInfo>> = getWorkInfoFlow().map { workInfoList ->
        workInfoList.map { workInfo ->
            SyncJobInfo(
                state =
                when (workInfo.state) {
                    androidx.work.WorkInfo.State.ENQUEUED -> SyncState.ENQUEUED
                    androidx.work.WorkInfo.State.RUNNING -> SyncState.RUNNING
                    androidx.work.WorkInfo.State.SUCCEEDED -> SyncState.SUCCEEDED
                    androidx.work.WorkInfo.State.FAILED -> SyncState.FAILED
                    androidx.work.WorkInfo.State.BLOCKED -> SyncState.BLOCKED
                    androidx.work.WorkInfo.State.CANCELLED -> SyncState.CANCELLED
                },
                tags = workInfo.tags,
            )
        }
    }
}

data class WorkConstraints(
    val wifi: Boolean = false,
    val charging: Boolean = false,
    val batteryNotLow: Boolean = false,
    val deviceIdle: Boolean = false,
) {
    companion object {
        suspend fun fromDataStore(dataStore: DataStore<Preferences>): WorkConstraints {
            val preferences = dataStore.data.first()
            return WorkConstraints(
                wifi = preferences[SettingsViewModel.REQUIRE_WIFI_KEY] ?: false,
                charging = preferences[SettingsViewModel.REQUIRE_CHARGING_KEY] ?: false,
                batteryNotLow = preferences[SettingsViewModel.REQUIRE_BATTERY_NOT_LOW_KEY] ?: false,
                deviceIdle = preferences[SettingsViewModel.REQUIRE_DEVICE_IDLE_KEY] ?: false,
            )
        }
    }
}
