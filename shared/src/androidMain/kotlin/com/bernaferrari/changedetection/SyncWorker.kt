package com.bernaferrari.changedetection

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import com.bernaferrari.changedetection.sync.SyncResult
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
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
        private const val CHANNEL_ID = "sync_channel"
    }

    init {
        appLog.d("SyncWorker created successfully with Koin")
    }

    @Volatile
    private var foregroundRequested = false

    // WorkManager also requests foreground info for expedited work before Android 12.
    override suspend fun getForegroundInfo(): ForegroundInfo {
        foregroundRequested = true
        return createForegroundInfo()
    }

    private fun createForegroundInfo(
        pageName: String? = null,
        completed: Int = 0,
        total: Int = 0,
    ): ForegroundInfo {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    applicationContext.getString(R.string.sync_notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                )
            notificationManager.createNotificationChannel(channel)
        }

        val title = when {
            total > 1 -> applicationContext.getString(R.string.status_checking_page_progress, completed + 1, total)
            pageName != null -> applicationContext.getString(R.string.status_checking_page)
            else -> applicationContext.getString(R.string.status_checking_pages)
        }
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivityProvider.getMainActivityClass()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification =
            NotificationCompat
                .Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_change)
                .setContentTitle(title)
                .setContentText(pageName ?: applicationContext.getString(R.string.status_preparing_page_checks))
                .setContentIntent(contentIntent)
                // Count completed attempts; the page currently loading is not complete yet.
                .setProgress(total, completed, total <= 1)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .setOngoing(true)
                .build()

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ForegroundInfo(
                id.hashCode(),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(id.hashCode(), notification)
        }
    }

    override suspend fun doWork(): Result {
        appLog.d("SyncWorker.doWork() starting")
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
            appLog.e("Sync work failed", e)
            com.bernaferrari.changedetection.diagnostics.DiagnosticLog.appEvent(com.bernaferrari.changedetection.diagnostics.AppEventKind.BACKGROUND_FAILURE, e)
            Result.retry()
        }
    }

    private suspend fun syncSingleSite(siteId: String): Result {
        val site = sitesRepository.getSiteById(siteId) ?: return Result.success()
        if (foregroundRequested || site.captureMode.resolve(getComparisonMethod()) == ComparisonMethod.SCREENSHOT) {
            setForeground(createForegroundInfo(pageName = site.title?.takeIf { it.isNotBlank() } ?: site.url, total = 1))
        }
        return checkRunner.check(siteId).toWorkResult()
    }

    private suspend fun syncAllSites(): Result {
        val sites = sitesRepository.getSites().filter { it.isSyncEnabled }
        if (sites.isEmpty()) return Result.success()
        val defaultMethod = getComparisonMethod()
        val showProgress = foregroundRequested || sites.any { it.captureMode.resolve(defaultMethod) == ComparisonMethod.SCREENSHOT }
        val results = sites.mapIndexed { index, site ->
            if (showProgress) {
                setForeground(
                    createForegroundInfo(
                        pageName = site.title?.takeIf { it.isNotBlank() } ?: site.url,
                        completed = index,
                        total = sites.size,
                    ),
                )
            }
            checkRunner.check(site.id)
        }
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

private val appLog = com.bernaferrari.changedetection.logging.AppLog.tagged("SyncWorker")
