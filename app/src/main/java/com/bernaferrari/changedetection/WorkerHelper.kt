package com.bernaferrari.changedetection

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.settings.SettingsViewModel
import com.orhanobut.logger.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class WorkerHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
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

    // Add instance property for easier access
    val UNIQUEWORK = Companion.UNIQUEWORK

    suspend fun updateWorkerWithConstraints(
        dataStore: DataStore<Preferences>,
        cancelCurrentWork: Boolean = true
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

    open fun cancelWork() {
        WorkManager.getInstance(context).cancelAllWorkByTag(UNIQUEWORK)
    }

    private fun schedulePeriodicWork(
        intervalMinutes: Long,
        constraints: WorkConstraints
    ) {
        Logger.d("Scheduling periodic work with interval: $intervalMinutes minutes")

        val workerConstraints = Constraints.Builder().apply {
            setRequiredNetworkType(if (constraints.wifi) NetworkType.UNMETERED else NetworkType.CONNECTED)
            if (constraints.batteryNotLow) setRequiresBatteryNotLow(true)
            if (constraints.charging) setRequiresCharging(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && constraints.deviceIdle) {
                setRequiresDeviceIdle(true)
            }
        }.build()

        val inputData = workDataOf(
            WIFI to constraints.wifi,
            CHARGING to constraints.charging,
            BATTERYNOTLOW to constraints.batteryNotLow,
            IDLE to constraints.deviceIdle
        )


        val periodicWork = PeriodicWorkRequestBuilder<SyncWorker>(intervalMinutes, TimeUnit.MINUTES)
            .addTag(UNIQUEWORK)
            .setConstraints(workerConstraints)
            .setInputData(inputData)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUEWORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWork
        )

        Logger.d("Periodic work scheduled successfully")
    }

    fun getWorkInfoFlow() = WorkManager.getInstance(context).getWorkInfosByTagFlow(UNIQUEWORK)

    // Add method to get work info for specific site
    fun getSiteWorkInfoFlow(siteId: String) =
        WorkManager.getInstance(context).getWorkInfosByTagFlow(siteId)

    fun reloadSite(site: Site) {
        Logger.d("🔄 Scheduling one-time work for site: ${site.id}")

        val workerClass = SyncWorker::class.java

        val work = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(workDataOf("id" to site.id, "isSingle" to true))
            .addTag("singleEvent")
            .addTag("reload_${site.id}")
            .addTag(site.id)
            .addTag(UNIQUEWORK)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context)
            .beginUniqueWork("reload_${site.id}", ExistingWorkPolicy.REPLACE, work)
            .enqueue()

        Logger.d("✅ One-time work scheduled for site: ${site.id}")
    }
    
    fun reloadSiteById(siteId: String) {
        Logger.d("🔄 Scheduling one-time work for site ID: $siteId")
        scheduleOneTimeWork(siteId)
    }

    private fun scheduleOneTimeWork(siteId: String) {
        val work = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(workDataOf("id" to siteId, "isSingle" to true))
            .addTag(siteId)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueue(work)
    }

    fun startQuickMonitor(siteIds: List<String>, intervalSeconds: Int = 15) {
        Logger.d("⚡ Starting Quick Monitor for ${siteIds.size} sites: $siteIds (${intervalSeconds}s interval)")
        
        try {
            // Start the foreground service for quick monitoring
            Logger.d("📱 About to start QuickMonitorService...")
            
            // Ensure we have valid site IDs
            if (siteIds.isEmpty()) {
                Logger.e("❌ Cannot start Quick Monitor with empty site list")
                return
            }
            
            // Log context info
            Logger.d("📱 Starting from context: ${context.javaClass.simpleName}")
            
            QuickMonitorService.start(context, siteIds, intervalSeconds)
            Logger.d("✅ QuickMonitorService.start() called successfully")
            
            // Also trigger immediate work for the sites
            siteIds.forEach { siteId ->
                Logger.d("🔄 Scheduling immediate work for site: $siteId")
                scheduleOneTimeWork(siteId)
            }
        } catch (e: Exception) {
            Logger.e("❌ Failed to start QuickMonitorService: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun scheduleQuickMonitorWork(siteId: String, intervalSeconds: Int) {
        val work = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(workDataOf("id" to siteId, "isSingle" to true))
            .addTag("quick_monitor")
            .addTag(siteId)
            .addTag(UNIQUEWORK)
            .setInitialDelay(intervalSeconds.toLong(), TimeUnit.SECONDS)
            .build()
        
        WorkManager.getInstance(context)
            .beginUniqueWork(
                "quick_monitor_$siteId",
                ExistingWorkPolicy.REPLACE,
                work
            )
            .enqueue()
    }
    
    fun stopQuickMonitor() {
        Logger.d("⏹️ Stopping Quick Monitor")
        WorkManager.getInstance(context).cancelAllWorkByTag("quick_monitor")
        QuickMonitorService.stop(context)
    }
    
}

data class WorkConstraints(
    val wifi: Boolean = false,
    val charging: Boolean = false,
    val batteryNotLow: Boolean = false,
    val deviceIdle: Boolean = false
) {
    companion object {
        suspend fun fromDataStore(dataStore: DataStore<Preferences>): WorkConstraints {
            val preferences = dataStore.data.first()
            return WorkConstraints(
                wifi = preferences[SettingsViewModel.REQUIRE_WIFI_KEY] ?: false,
                charging = preferences[SettingsViewModel.REQUIRE_CHARGING_KEY] ?: false,
                batteryNotLow = preferences[SettingsViewModel.REQUIRE_BATTERY_NOT_LOW_KEY] ?: false,
                deviceIdle = preferences[SettingsViewModel.REQUIRE_DEVICE_IDLE_KEY] ?: false
            )
        }
    }
}
