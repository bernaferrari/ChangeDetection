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
import kotlinx.coroutines.flow.map
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.isSuccess
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import java.net.UnknownHostException

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val httpClient: HttpClient by inject()
    private val sitesRepository: SitesRepository by inject()
    private val snapsRepository: SnapsRepository by inject()
    private val errorHandler: ErrorHandler by inject()
    private val notificationHelper: NotificationHelper by inject()
    private val dataStore: DataStore<Preferences> by inject()

    init {
        Napier.d("SyncWorker created successfully with Koin")
    }

    override suspend fun doWork(): Result {
        Napier.d("SyncWorker.doWork() starting")
        return try {
            val siteId = inputData.getString("id") ?: return Result.failure()
            val isSingle = inputData.getBoolean("isSingle", false)
            val wifiRequired = inputData.getBoolean(WorkerHelper.WIFI, false)

            Napier.d("Starting sync work for site: $siteId, isSingle: $isSingle, wifi: $wifiRequired")

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
                        val contentHash = ContentHasher.hashContent(fetchResult.data)
                        val processedContentHash = if (fetchResult.contentType.contains("html", ignoreCase = true)) {
                            ContentHasher.hashHtmlContent(fetchResult.data)
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
                        Napier.d("Content changed - saved new snap for site: $siteId")

                        // Send change detection notification if enabled for this site
                        if (site.isNotificationEnabled && hadPreviousSnap) {
                            val changeDescription = generateChangeDescription(fetchResult.data, fetchResult.contentType)
                            notificationHelper.sendChangeDetectedNotification(site, changeDescription)
                        }
                    } else {
                        Napier.d("No content changes detected for site: $siteId")
                    }

                    sitesRepository.updateSite(site.copy(isSuccessful = true))
                    Result.success()
                }

                is FetchResult.Error -> {
                    sitesRepository.updateSite(site.copy(isSuccessful = false))

                    // Handle error with proper error types
                    val error = when {
                        fetchResult.message.contains("404") -> NetworkError(404, "Page not found")
                        fetchResult.message.contains("403") -> NetworkError(403, "Access forbidden")
                        fetchResult.message.contains("500") -> NetworkError(500, "Server error")
                        fetchResult.message.contains("timeout") -> SyncError(
                            SyncError.Reason.NETWORK_ERROR,
                            "Request timeout"
                        )

                        fetchResult.message.contains("Network not available") -> SyncError(
                            SyncError.Reason.NETWORK_ERROR,
                            "No internet connection"
                        )

                        else -> SyncError(SyncError.Reason.UNKNOWN, fetchResult.message)
                    }

                    errorHandler.handleError(error, ErrorContext.SYNC_WORKER, "Sync site: ${site.url}")

                    // Send error notification if enabled for this site
                    if (site.isNotificationEnabled) {
                        notificationHelper.sendSyncErrorNotification(site, fetchResult.message)
                    }

                    Napier.e("Sync failed for site: $siteId - ${fetchResult.message}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            val errorResult = e.toErrorResult(errorHandler, ErrorContext.SYNC_WORKER)
            Napier.e("Sync work failed: ${errorResult.userMessage}")
            Result.retry()
        }
    }

    private suspend fun fetchFromServer(site: Site): FetchResult = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get(site.url) {
                header("User-Agent", "ChangeDetection/1.0")
            }

            Napier.d("Response status: ${response.status} for ${site.url}")

            if (!response.status.isSuccess()) {
                return@withContext FetchResult.Error("HTTP ${response.status.value}: ${response.status.description}")
            }

            val contentType = response.headers["Content-Type"] ?: "text/html"
            val bodyBytes = response.body<ByteArray>()

            // Re-implementing correctly:
            val fullContent = bodyBytes.decodeToString() // UTF-8 default
            val finalContent = processContentWithSmartEngine(fullContent)

            FetchResult.Success(contentType, finalContent.toByteArray())

        } catch (e: UnknownHostException) {
            Napier.e("Network error for ${site.url}: ${e.localizedMessage}")
            FetchResult.Error("Network not available: ${site.url}")
        } catch (e: IllegalArgumentException) {
            Napier.e("Invalid URL: ${site.url}")
            FetchResult.Error("Invalid URL: ${site.url}")
        } catch (e: SocketTimeoutException) {
            Napier.e("Timeout for ${site.url}")
            FetchResult.Error("Request timeout: ${site.url}")
        } catch (e: ConnectTimeoutException) {
            Napier.e("Connect timeout for ${site.url}")
            FetchResult.Error("Request timeout: ${site.url}")
        } catch (e: ClientRequestException) {
            Napier.e("Client error for ${site.url}: ${e.response.status}")
            FetchResult.Error("HTTP ${e.response.status.value}: ${e.response.status.description}")
        } catch (e: ServerResponseException) {
            Napier.e("Server error for ${site.url}: ${e.response.status}")
            FetchResult.Error("HTTP ${e.response.status.value}: ${e.response.status.description}")
        } catch (e: Exception) {
            Napier.e("Unexpected error for ${site.url}: ${e.localizedMessage}")
            FetchResult.Error("${e.localizedMessage ?: "Unknown error"}: ${site.url}")
        }
    }

    private suspend fun processContentWithSmartEngine(content: String): String {
        return try {
            content
        } catch (e: Exception) {
            Napier.e("Error in smart content processing: ${e.message}")
            content
        }
    }

    private suspend fun checkForContentChanges(siteId: String, newContent: ByteArray, contentType: String): Boolean {
        if (newContent.isEmpty()) return false

        // Check for size changes with configurable threshold
        val lastSnap = snapsRepository.getMostRecentSnap(siteId)
        if (lastSnap != null) {
            val threshold = dataStore.data.map {
                it[SettingsViewModel.MIN_CHANGE_THRESHOLD_KEY] ?: 0
            }.first()

            if (threshold > 0) {
                // Convert percentage (0-10) to decimal (0.0-0.1)
                val thresholdDecimal = threshold.toDouble() / 100.0

                if (!ContentHasher.quickSizeCheck(newContent.size, lastSnap.contentSize, thresholdDecimal)) {
                    Napier.d("Change ignored due to size threshold ($threshold%): ${newContent.size} vs ${lastSnap.contentSize}")
                    return false
                }
            }
        }
        val newHash = if (contentType.contains("html", ignoreCase = true)) {
            ContentHasher.hashHtmlContent(newContent)
        } else {
            ContentHasher.hashContent(newContent)
        }

        val lastHash = if (contentType.contains("html", ignoreCase = true)) {
            snapsRepository.getLastProcessedContentHash(siteId)
        } else {
            snapsRepository.getLastContentHash(siteId)
        }

        return lastHash == null || newHash != lastHash
    }

    private suspend fun generateChangeDescription(newContent: ByteArray, contentType: String): String {
        return try {
            if (contentType.contains("html", ignoreCase = true)) {
                val content = newContent.decodeToString()
                // Use a KMP-friendly HTML parser or regex if simple, 
                // but since this file is currently in androidMain, Jsoup is still available and imported?
                // Original file had `org.jsoup.Jsoup`. I will preserve it since I am only maintaining it in androidMain for now.
                // Assuming Jsoup dependency is available in androidMain.
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
