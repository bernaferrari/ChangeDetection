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
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bernaferrari.changedetection.background.*
import com.bernaferrari.changedetection.sync.SyncEngine
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    private val syncEngine: SyncEngine,
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID = "sync_channel"
        private val PENDING_BATCH = stringSetPreferencesKey("monitoring_batch_remaining")
        private val BATCH_RETRY = booleanPreferencesKey("monitoring_batch_retry")
        private val BATCH_CURSOR = stringPreferencesKey("monitoring_batch_cursor")
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
            com.bernaferrari.changedetection.diagnostics.DiagnosticLog.appEvent(com.bernaferrari.changedetection.diagnostics.AppEventKind.BACKGROUND_FAILURE, e, stage = com.bernaferrari.changedetection.diagnostics.AppEventStage.WORKER_CHECK)
            Result.retry()
        }
    }

    private suspend fun syncSingleSite(siteId: String): Result {
        val site = sitesRepository.getSiteById(siteId) ?: return Result.success()
        val foreground = foregroundRequested || site.captureMode.resolve(getComparisonMethod()) == ComparisonMethod.SCREENSHOT
        return boundedCheck(site, foreground, 0, 1).toWorkResult()
    }

    private suspend fun syncAllSites(): Result {
        val sites = sitesRepository.getSites().filter { it.isSyncEnabled }
        val preferences = dataStore.data.first()
        val checkpoint = BatchCheckpoint(preferences[PENDING_BATCH].orEmpty(), preferences[BATCH_RETRY] ?: false, preferences[BATCH_CURSOR])
        val defaultMethod = getComparisonMethod()
        val showProgress = foregroundRequested || sites.any { it.captureMode.resolve(defaultMethod) == ComparisonMethod.SCREENSHOT }
        val retry = runMonitoringBatch(sites, checkpoint, save = { state ->
            dataStore.edit {
                if (state.remaining.isEmpty()) {
                    it.remove(PENDING_BATCH);it.remove(BATCH_RETRY);it.remove(BATCH_CURSOR)
                } else {
                    it[PENDING_BATCH] = state.remaining
                    it[BATCH_RETRY] = state.retryNeeded
                    state.cursor?.let { cursor -> it[BATCH_CURSOR] = cursor } ?: it.remove(BATCH_CURSOR)
                }
            }
        }, elapsedMillis = android.os.SystemClock::elapsedRealtime) { site, index, total ->
            boundedCheck(site, showProgress, index, total)
        }
        return if (retry) Result.retry() else Result.success()
    }

    /** Includes time queued behind manual/Turbo checks, not only time inside a browser. */
    private suspend fun boundedCheck(site: com.bernaferrari.changedetection.repo.Site, foreground: Boolean, completed: Int, total: Int): SyncResult = try {
        withTimeout(90_000) {
            checkAfterForeground(prepare = {
                if (foreground) setForeground(createForegroundInfo(pageName = site.title?.takeIf { it.isNotBlank() } ?: site.url, completed = completed, total = total))
            }, check = { checkRunner.check(site.id) }, denied = { error ->
                com.bernaferrari.changedetection.diagnostics.DiagnosticLog.appEvent(com.bernaferrari.changedetection.diagnostics.AppEventKind.BACKGROUND_FAILURE, error, stage = com.bernaferrari.changedetection.diagnostics.AppEventStage.WORKER_FOREGROUND)
                syncEngine.processFetchResult(site, SyncEngine.FetchResult.Error("Background check could not start", error), site.threshold)
            })
        }
    } catch (timeout: TimeoutCancellationException) {
        currentCoroutineContext().ensureActive()
        syncEngine.processFetchResult(site, SyncEngine.FetchResult.Error("Page check timed out", timeout), site.threshold)
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
