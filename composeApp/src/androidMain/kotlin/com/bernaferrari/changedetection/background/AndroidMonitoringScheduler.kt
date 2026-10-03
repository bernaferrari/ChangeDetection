/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.background

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.bernaferrari.changedetection.SyncWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

/** Reconciles durable preferences with Android's durable job, including completion/failure. */
internal class AndroidMonitoringScheduler(
    private val workManager: WorkManager,
    private val dataStore: DataStore<Preferences>,
) {
    private val reconciliation = Mutex()

    suspend fun reconcile() = reconciliation.withLock {
        // Read inside the lock: queued callers must apply the latest saved preferences.
        val schedule = MonitoringSchedule.from(dataStore.data.first())
        if (!schedule.enabled) {
            workManager.cancelUniqueWork(WORK_NAME).await()
        } else {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (schedule.unmeteredOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresCharging(schedule.chargingOnly)
                .setRequiresBatteryNotLow(schedule.batteryNotLow)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(schedule.intervalMinutes, TimeUnit.MINUTES)
                .addTag(WORK_NAME)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build()
            workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request).await()
        }
        Unit
    }

    companion object {
        const val WORK_NAME = "work"
    }
}
