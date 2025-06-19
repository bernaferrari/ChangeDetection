package com.bernaferrari.changedetection

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.orhanobut.logger.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val okHttpClient: OkHttpClient,
    private val sitesRepository: SitesRepository,
    private val snapsRepository: SnapsRepository
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
                    val snap = com.bernaferrari.changedetection.repo.Snap(
                        siteId = siteId,
                        timestamp = System.currentTimeMillis(),
                        contentType = fetchResult.contentType,
                        contentSize = fetchResult.data.size,
                        contentCharset = "UTF-8"
                    )

                    snapsRepository.saveSnap(snap, fetchResult.data, 0)
                    sitesRepository.updateSite(site.copy(isSuccessful = true))

                    Logger.d("Sync successful for site: $siteId")
                    Result.success()
                }

                is FetchResult.Error -> {
                    sitesRepository.updateSite(site.copy(isSuccessful = false))
                    Logger.e("Sync failed for site: $siteId - ${fetchResult.message}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Logger.e("Sync work failed: ${e.localizedMessage}")
            Result.retry()
        }
    }

    private suspend fun fetchFromServer(site: Site): FetchResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(site.url)
                .addHeader("User-Agent", "ChangeDetection/1.0")
                .build()

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

                // Extract element content if element monitoring is enabled
                val finalContent = if (site.elementMonitoring && site.cssSelector.isNotEmpty()) {
                    extractElementContent(fullContent, site.cssSelector, site.elementXPath)
                } else {
                    fullContent
                }

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

    private fun extractElementContent(html: String, cssSelector: String, xpath: String): String {
        return try {
            val doc = org.jsoup.Jsoup.parse(html)

            val elements = doc.select(cssSelector)
            if (elements.isNotEmpty()) {
                val element = elements.first()

                buildString {
                    appendLine("<!-- Element: $cssSelector -->")
                    appendLine("<!-- Text: ${element?.text()?.take(100)} -->")
                    appendLine("<!-- HTML: -->")
                    append(element?.outerHtml())
                }
            } else {
                "<!-- Element not found: $cssSelector -->"
            }
        } catch (e: Exception) {
            Logger.e("Error extracting element content: ${e.message}")
            "<!-- Error extracting element: ${e.message} -->"
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
