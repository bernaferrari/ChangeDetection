package com.bernaferrari.changedetection

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bernaferrari.changedetection.error.ErrorContext
import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.error.NetworkError
import com.bernaferrari.changedetection.error.SyncError
import com.bernaferrari.changedetection.error.toErrorResult
import com.bernaferrari.changedetection.notifications.NotificationHelper
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.util.ContentHasher
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import com.bernaferrari.changedetection.repo.ComparisonMethod

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val syncEngine: com.bernaferrari.changedetection.sync.SyncEngine by inject()
    private val sitesRepository: SitesRepository by inject()
    private val dataStore: DataStore<Preferences> by inject()

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
            if (syncEngine.syncSite(siteId)) {
                Result.success()
            } else {
                Result.retry()
            }
        }
    }

    private suspend fun syncAllSites(): Result {
        val comparisonMethod = getComparisonMethod()

        if (comparisonMethod == ComparisonMethod.HTML) {
            // Sync all HTML sites via SyncEngine
            val successCount = syncEngine.syncAllSites()
            Napier.d("SyncEngine processed $successCount sites")
            return Result.success()
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

    private fun triggerScreenshotService(siteId: String) {
        val intent = android.content.Intent(
            applicationContext,
            com.bernaferrari.changedetection.sync.ScreenshotService::class.java
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
            ComparisonMethod.valueOf(stored ?: ComparisonMethod.HTML.name)
        }.getOrElse { ComparisonMethod.HTML }
    }
}
