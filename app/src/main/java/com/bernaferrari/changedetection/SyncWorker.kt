package com.bernaferrari.changedetection

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.util.ContentHasher
import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.error.ErrorContext
import com.bernaferrari.changedetection.error.NetworkError
import com.bernaferrari.changedetection.error.SyncError
import com.bernaferrari.changedetection.error.toErrorResult
import com.bernaferrari.changedetection.notifications.NotificationHelper
import com.orhanobut.logger.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.OkHttpClient
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val okHttpClient: OkHttpClient,
    private val sitesRepository: SitesRepository,
    private val snapsRepository: SnapsRepository,
    private val contentHasher: ContentHasher,
    private val errorHandler: ErrorHandler,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    init {
        Logger.d("🔧 SyncWorker created successfully with Hilt")
    }

    override suspend fun doWork(): Result {
        Logger.d("🚀 SyncWorker.doWork() starting")
        return try {
            val siteId = inputData.getString("id") ?: return Result.failure()
            val isSingle = inputData.getBoolean("isSingle", false)
            val wifiRequired = inputData.getBoolean(WorkerHelper.WIFI, false)

            Logger.d("Starting sync work for site: $siteId, isSingle: $isSingle, wifi: $wifiRequired")

            // Get the site from database
            val site = sitesRepository.getSiteById(siteId) ?: return Result.failure()

            // Update site timestamp to indicate sync attempt
            sitesRepository.updateSite(site.copy(timestamp = System.currentTimeMillis()))

            // Fetch content from server
            when (val fetchResult = fetchFromServer(site)) {
                is FetchResult.Success -> {
                    // Check for changes using content hashing before saving
                    val hadPreviousSnap = snapsRepository.getMostRecentSnap(siteId) != null
                    val hasChanged = checkForContentChanges(siteId, fetchResult.data, fetchResult.contentType)
                    
                    if (hasChanged) {
                        // Generate content hashes
                        val contentHash = contentHasher.hashContent(fetchResult.data)
                        val processedContentHash = if (fetchResult.contentType.contains("html", ignoreCase = true)) {
                            contentHasher.hashHtmlContent(fetchResult.data)
                        } else {
                            contentHash
                        }
                        
                        val snap = com.bernaferrari.changedetection.repo.Snap(
                            siteId = siteId,
                            timestamp = System.currentTimeMillis(),
                            contentType = fetchResult.contentType,
                            contentSize = fetchResult.data.size,
                            contentCharset = "UTF-8",
                            contentHash = contentHash,
                            processedContentHash = processedContentHash
                        )

                        snapsRepository.saveSnap(snap, fetchResult.data, 0)
                        Logger.d("Content changed - saved new snap for site: $siteId")
                        
                        // Send change detection notification if enabled for this site
                        if (site.isNotificationEnabled && hadPreviousSnap) {
                            val changeDescription = generateChangeDescription(fetchResult.data, fetchResult.contentType)
                            notificationHelper.sendChangeDetectedNotification(site, changeDescription)
                        }
                    } else {
                        Logger.d("No content changes detected for site: $siteId")
                    }
                    
                    sitesRepository.updateSite(site.copy(isSuccessful = true))
                    Result.success()
                }

                is FetchResult.Error -> {
                    sitesRepository.updateSite(site.copy(isSuccessful = false))
                    
                    // Handle error with proper error types
                    val error = when {
                        fetchResult.message.contains("HTTP 404") -> NetworkError(404, "Page not found")
                        fetchResult.message.contains("HTTP 403") -> NetworkError(403, "Access forbidden")
                        fetchResult.message.contains("HTTP 5") -> NetworkError(500, "Server error")
                        fetchResult.message.contains("timeout") -> SyncError(SyncError.Reason.NETWORK_ERROR, "Request timeout")
                        fetchResult.message.contains("Network not available") -> SyncError(SyncError.Reason.NETWORK_ERROR, "No internet connection")
                        else -> SyncError(SyncError.Reason.UNKNOWN, fetchResult.message)
                    }
                    
                    errorHandler.handleError(error, ErrorContext.SYNC_WORKER, "Sync site: ${site.url}")
                    
                    // Send error notification if enabled for this site
                    if (site.isNotificationEnabled) {
                        notificationHelper.sendSyncErrorNotification(site, fetchResult.message)
                    }
                    
                    Logger.e("Sync failed for site: $siteId - ${fetchResult.message}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            val errorResult = e.toErrorResult(errorHandler, ErrorContext.SYNC_WORKER)
            Logger.e("Sync work failed: ${errorResult.userMessage}")
            Result.retry()
        }
    }

    private suspend fun fetchFromServer(site: Site): FetchResult = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url(site.url)
                .addHeader("User-Agent", "ChangeDetection/1.0")
            
            val request = requestBuilder.build()

            okHttpClient.newCall(request).execute().use { response ->
                Logger.d("Response successful: ${response.isSuccessful} for ${site.url}")

                if (!response.isSuccessful) {
                    return@withContext FetchResult.Error("HTTP ${response.code}: ${response.message}")
                }

                val contentType = response.header("content-type", "text/html") ?: "text/html"
                val fullContent = response.body?.use { body ->
                    if (contentType.contains("text", ignoreCase = true)) {
                        body.string()
                    } else {
                        body.string()
                    }
                } ?: throw IllegalStateException("Response body is null")

                // Process content with SmartMonitoringEngine
                val finalContent = processContentWithSmartEngine(fullContent)

                FetchResult.Success(contentType, finalContent.toByteArray())
            }
        } catch (e: UnknownHostException) {
            Logger.e("Network error for ${site.url}: ${e.localizedMessage}")
            FetchResult.Error("Network not available: ${site.url}")
        } catch (e: IllegalArgumentException) {
            Logger.e("Invalid URL: ${site.url}")
            FetchResult.Error("Invalid URL: ${site.url}")
        } catch (e: SocketTimeoutException) {
            Logger.e("Timeout for ${site.url}")
            FetchResult.Error("Request timeout: ${site.url}")
        } catch (e: Exception) {
            Logger.e("Unexpected error for ${site.url}: ${e.localizedMessage}")
            FetchResult.Error("${e.localizedMessage ?: "Unknown error"}: ${site.url}")
        }
    }

    private suspend fun processContentWithSmartEngine(content: String): String {
        return try {
            content
        } catch (e: Exception) {
            Logger.e("Error in smart content processing: ${e.message}")
            content
        }
    }
    
    /**
     * Fast content change detection using hashes
     */
    private suspend fun checkForContentChanges(siteId: String, newContent: ByteArray, contentType: String): Boolean {
        if (newContent.isEmpty()) return false
        
        // Quick size-based check first
        val lastSnap = snapsRepository.getMostRecentSnap(siteId)
        if (lastSnap != null && !contentHasher.quickSizeCheck(newContent.size, lastSnap.contentSize)) {
            return false
        }
        
        // Hash-based comparison
        val newHash = if (contentType.contains("html", ignoreCase = true)) {
            contentHasher.hashHtmlContent(newContent)
        } else {
            contentHasher.hashContent(newContent)
        }
        
        val lastHash = if (contentType.contains("html", ignoreCase = true)) {
            snapsRepository.getLastProcessedContentHash(siteId)
        } else {
            snapsRepository.getLastContentHash(siteId)
        }
        
        return lastHash == null || newHash != lastHash
    }

    
    
    /**
     * Generate a meaningful description of what changed
     */
    private suspend fun generateChangeDescription(newContent: ByteArray, contentType: String): String {
        return try {
            if (contentType.contains("html", ignoreCase = true)) {
                val content = String(newContent, Charsets.UTF_8)
                val doc = org.jsoup.Jsoup.parse(content)
                val textLength = doc.text().length
                
                when {
                    textLength > 10000 -> "Major content update detected"
                    textLength > 5000 -> "Significant content changes detected"
                    textLength > 1000 -> "Content changes detected"
                    else -> "Minor content update detected"
                }
            } else {
                val sizeKb = newContent.size / 1024
                when {
                    sizeKb > 500 -> "Large file update (${sizeKb}KB)"
                    sizeKb > 100 -> "File update detected (${sizeKb}KB)"
                    else -> "Content changed"
                }
            }
        } catch (e: Exception) {
            "Content has changed"
        }
    }
}

sealed class FetchResult {
    data class Success(val contentType: String, val data: ByteArray) : FetchResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Success

            if (contentType != other.contentType) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = contentType.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    data class Error(val message: String) : FetchResult()
}
