package com.bernaferrari.changedetection

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.repo.zapRules
import com.bernaferrari.changedetection.repo.customFilters
import com.bernaferrari.changedetection.repo.smartFilterType
import com.bernaferrari.changedetection.monitoring.SmartMonitoringEngine
import com.bernaferrari.changedetection.monitoring.ContentFilter
import com.bernaferrari.changedetection.monitoring.ZapRule
import com.bernaferrari.changedetection.monitoring.SmartFilterType
import com.bernaferrari.changedetection.util.ContentHasher
import com.bernaferrari.changedetection.util.isChangedFrom
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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
    private val smartMonitoringEngine: SmartMonitoringEngine,
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
                        if (site.isNotificationEnabled) {
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
                .addHeader("User-Agent", site.userAgent.ifEmpty { "ChangeDetection/1.0" })

            // Apply captured authentication data
            applyAuthentication(requestBuilder, site)
            
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
                val finalContent = processContentWithSmartEngine(fullContent, site)

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

    private suspend fun processContentWithSmartEngine(content: String, site: Site): String {
        return try {
            // Step 1: Apply element zapping if configured
            val zappedContent = if (site.zapRules.isNotEmpty()) {
                smartMonitoringEngine.zapElements(content, site.zapRules)
            } else {
                content
            }
            
            // Step 2: Apply smart filters based on site configuration
            val filters = createFiltersForSite(site, zappedContent)
            
            // Step 3: Get previous content for change detection
            val previousSnap = snapsRepository.getMostRecentSnap(site.id)
            val previousContent = previousSnap?.let { 
                String(snapsRepository.getSnapContent(it.snapId) ?: byteArrayOf())
            }
            
            // Step 4: Process with smart monitoring engine
            val processedResult = smartMonitoringEngine.processContent(
                content = zappedContent,
                filters = filters,
                previousContent = previousContent
            )
            
            // Step 5: Extract element content if element monitoring is enabled
            val finalContent = if (site.elementMonitoring && site.cssSelector.isNotEmpty()) {
                extractElementContent(processedResult.filteredContent, site.cssSelector, site.elementXPath)
            } else {
                processedResult.filteredContent
            }
            
            // Log processing results for debugging
            Logger.d("Smart processing completed for ${site.url}")
            Logger.d("- Importance Score: ${processedResult.importanceScore}")
            Logger.d("- Content Type: ${processedResult.contentType}")
            Logger.d("- Changes Detected: ${processedResult.changes.size}")
            Logger.d("- Word Count: ${processedResult.metadata.wordCount}")
            
            finalContent
            
        } catch (e: Exception) {
            Logger.e("Error in smart content processing: ${e.message}")
            // Fallback to basic element extraction
            if (site.elementMonitoring && site.cssSelector.isNotEmpty()) {
                extractElementContent(content, site.cssSelector, site.elementXPath)
            } else {
                content
            }
        }
    }
    
    private suspend fun createFiltersForSite(site: Site, content: String): List<ContentFilter> {
        val filters = mutableListOf<ContentFilter>()
        
        // Only apply smart monitoring if enabled for this site
        if (!site.smartMonitoringEnabled) {
            return filters
        }
        
        // Add base filters based on site's smart filter type
        filters.addAll(smartMonitoringEngine.createSmartFilters(content, site.smartFilterType))
        
        // Add custom filters from site configuration if available
        site.customFilters?.let { customFilters ->
            filters.addAll(customFilters)
        }
        
        return filters
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

    private fun extractElementContent(html: String, cssSelector: String, xpath: String): String {
        return try {
            val document = org.jsoup.Jsoup.parse(html)
            if (cssSelector.isBlank()) {
                document.outerHtml()
            } else {
                val elements = document.select(cssSelector)
                if (elements.isEmpty()) {
                    "<!-- Element not found: $cssSelector -->"
                } else {
                    buildString {
                        append("<!-- Extracted elements for selector(s): $cssSelector -->\n")
                        elements.forEachIndexed { index, element ->
                            append("<!-- Element #${index + 1} text: ${element.text().take(100)} -->\n")
                            append(element.outerHtml()).append('\n')
                            append("<!-- ====== -->\n")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e("Error extracting element content: ${e.message}")
            "<!-- Error extracting element: ${e.message} -->"
        }
    }

    private fun parseCookieData(cookieData: String): String {
        if (cookieData.isBlank()) return ""

        val trimmed = cookieData.trim()
        return try {
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                val cookieMap = Json.decodeFromString<Map<String, String>>(trimmed)
                cookieMap.entries.joinToString("; ") { (name, value) -> "$name=$value" }
            } else {
                trimmed
            }
        } catch (e: Exception) {
            Logger.e("Failed to parse cookie data: ${'$'}{e.message}")
            trimmed
        }
    }
    
    /**
     * Apply authentication data captured from the browser session
     */
    private fun applyAuthentication(requestBuilder: Request.Builder, site: Site) {
        try {
            // Apply cookies if available
            if (site.cookieData.isNotBlank()) {
                // Parse and apply cookies
                val cookies = parseCookieData(site.cookieData)
                if (cookies.isNotBlank()) {
                    requestBuilder.addHeader("Cookie", cookies)
                    Logger.d("🍪 Applied cookies for ${site.title}")
                }
            }
            
            // Apply custom headers if available
            if (site.customHeadersJson.isNotBlank()) {
                applyCustomHeaders(requestBuilder, site.customHeadersJson)
            }
            
            // Apply authentication method specific headers
            when (site.authenticationMethod) {
                com.bernaferrari.changedetection.repo.AuthenticationMethod.BEARER_TOKEN -> {
                    if (site.oauthTokenData.isNotBlank()) {
                        requestBuilder.addHeader("Authorization", "Bearer ${site.oauthTokenData}")
                        Logger.d("🔑 Applied Bearer token for ${site.title}")
                    }
                }
                
                com.bernaferrari.changedetection.repo.AuthenticationMethod.BASIC -> {
                    if (site.encryptedCredentials.isNotBlank()) {
                        try {
                            // Decrypt credentials from Android Keystore encryption
                            val credentials = com.bernaferrari.changedetection.security.CredentialManager.decryptCredentials(site.encryptedCredentials)
                            if (credentials != null && credentials.username.isNotBlank()) {
                                val credentialString = "${credentials.username}:${credentials.password}"
                                val encodedCredentials = java.util.Base64.getEncoder()
                                    .encodeToString(credentialString.toByteArray(Charsets.UTF_8))
                                requestBuilder.addHeader("Authorization", "Basic $encodedCredentials")
                                Logger.d("🔐 Applied Basic auth for ${site.title}")
                            } else {
                                Logger.w("⚠️ Failed to decrypt credentials for ${site.title}")
                            }
                        } catch (e: Exception) {
                            Logger.w("⚠️ Failed to apply Basic auth for ${site.title}: ${e.message}")
                        }
                    } else {
                        Logger.w("⚠️ Basic auth requested but no credentials available for ${site.title}")
                    }
                }
                
                com.bernaferrari.changedetection.repo.AuthenticationMethod.COOKIE_AUTH -> {
                    Logger.d("🍪 Using cookie-based authentication for ${site.title}")
                }
                
                com.bernaferrari.changedetection.repo.AuthenticationMethod.CUSTOM_HEADER -> {
                    Logger.d("📋 Using custom header authentication for ${site.title}")
                }
                
                else -> {
                    // No authentication or other methods
                }
            }
            
        } catch (e: Exception) {
            Logger.e("❌ Failed to apply authentication for ${site.title}: ${e.message}")
        }
    }
    
    /**
     * Apply custom headers from JSON configuration
     */
    private fun applyCustomHeaders(requestBuilder: Request.Builder, customHeadersJson: String) {
        try {
            if (customHeadersJson.isNotBlank()) {
                customHeadersJson.split(";").forEach { header ->
                    val parts = header.split(":", limit = 2)
                    if (parts.size == 2) {
                        requestBuilder.addHeader(parts[0].trim(), parts[1].trim())
                        Logger.d("📋 Applied custom header: ${parts[0].trim()}")
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e("Failed to apply custom headers: ${e.message}")
        }
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
