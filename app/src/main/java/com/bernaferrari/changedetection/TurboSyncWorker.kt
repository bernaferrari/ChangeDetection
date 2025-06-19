package com.bernaferrari.changedetection

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.orhanobut.logger.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@HiltWorker
class TurboSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val okHttpClient: OkHttpClient,
    private val sitesRepository: SitesRepository,
    private val snapsRepository: SnapsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Logger.d("🚀 TurboSyncWorker starting")

        return try {
            val siteId = inputData.getString("siteId") ?: return Result.failure()

            // Get site
            val site = sitesRepository.getSiteById(siteId) ?: return Result.failure()

            // Perform sync
            when (val result = fetchFromServer(site.url)) {
                is FetchResult.Success -> {
                    val snap = com.bernaferrari.changedetection.repo.Snap(
                        siteId = siteId,
                        timestamp = System.currentTimeMillis(),
                        contentType = result.contentType,
                        contentSize = result.data.size,
                        contentCharset = "UTF-8"
                    )

                    snapsRepository.saveSnap(snap, result.data, 0)
                    sitesRepository.updateSite(site.copy(isSuccessful = true))

                    Logger.d("✅ Turbo sync successful")
                    Result.success()
                }

                is FetchResult.Error -> {
                    sitesRepository.updateSite(site.copy(isSuccessful = false))
                    Logger.e("❌ Turbo sync failed: ${result.message}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Logger.e("❌ TurboSyncWorker error: ${e.message}")
            Result.retry()
        }
    }

    private suspend fun fetchFromServer(url: String): FetchResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "ChangeDetection-Turbo/1.0")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext FetchResult.Error("HTTP ${response.code}: ${response.message}")
                }

                val contentType = response.header("content-type", "text/html") ?: "text/html"
                val bytes =
                    response.body?.bytes() ?: throw IllegalStateException("Response body is null")

                FetchResult.Success(contentType, bytes)
            }
        } catch (e: Exception) {
            Logger.e("❌ Fetch error: ${e.message}")
            FetchResult.Error(e.message ?: "Unknown error")
        }
    }
}
