package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import io.github.aakira.napier.Napier
import com.fleeksoft.ksoup.Ksoup
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Shared Sync Engine for fetching website content.
 * This class contains the core logic that was previously in SyncWorker,
 * now made multiplatform using Ktor (networking) and Ksoup (HTML parsing).
 */
class SyncEngine(
    private val httpClient: HttpClient,
    private val sitesDao: SitesDao,
    private val snapsDao: SnapsDao
) {


    sealed class FetchResult {
        data class Success(val contentType: String, val data: ByteArray) : FetchResult() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || this::class != other::class) return false
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

        data class Error(val message: String, val throwable: Throwable? = null) : FetchResult()
    }

    /**
     * Sync a specific site by ID.
     * Fetches content, checks for changes, and saves a new snap if changed.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun syncSite(siteId: String): Boolean = withContext(Dispatchers.Default) {
        val site = sitesDao.getSiteById(siteId) ?: run {
            Napier.e("Site not found: $siteId")
            return@withContext false
        }

        if (!site.isSyncEnabled) {
            Napier.d("Sync disabled for site: ${site.title} (${site.url})")
            return@withContext false
        }

        Napier.i("Starting sync for: ${site.title ?: site.url}")
        val result = fetchFromServer(site)

        when (result) {
            is FetchResult.Success -> {
                val lastSnap = snapsDao.getLastSnapForSiteId(siteId)
                val contentHash = if (result.contentType.contains("html", ignoreCase = true)) {
                    hashHtmlContent(result.data)
                } else {
                    hashContent(result.data)
                }

                val hasChanged = hasContentChanged(result.data, lastSnap?.contentHash, result.contentType)

                if (hasChanged || lastSnap == null) {
                    val changeReason = if (lastSnap == null) "Initial snapshot" else "Content changed"
                    Napier.i("Changes detected for ${site.url}: $changeReason")

                    val snap = Snap(
                        snapId = Uuid.random().toString(),
                        siteId = siteId,
                        timestamp = currentTimeMillis(),
                        contentType = result.contentType,
                        contentCharset = "UTF-8",
                        contentSize = result.data.size,
                        contentHash = contentHash,
                        processedContentHash = contentHash
                    )
                    snapsDao.insertSnap(snap)
                } else {
                    Napier.d("No changes for ${site.url}")
                }

                // Update site timestamp and success status
                sitesDao.updateSite(
                    site.copy(
                        timestamp = currentTimeMillis(),
                        isSuccessful = true
                    )
                )

                true
            }

            is FetchResult.Error -> {
                Napier.e("Sync failed for ${site.url}: ${result.message}", result.throwable)

                // Update site as failed
                sitesDao.updateSite(
                    site.copy(
                        timestamp = currentTimeMillis(),
                        isSuccessful = false
                    )
                )
                false
            }
        }
    }

    /**
     * Sync all enabled sites.
     */
    suspend fun syncAllSites(): Int = withContext(Dispatchers.Default) {
        val sites = sitesDao.getSitesList()
        var successCount = 0

        Napier.i("Starting sync for all sites (count: ${sites.size})")

        for (site in sites) {
            if (site.isSyncEnabled) {
                if (syncSite(site.id)) {
                    successCount++
                }
            }
        }

        Napier.i("Sync finished. Success count: $successCount/${sites.size}")
        successCount
    }

    /**
     * Fetch content from a website URL using Ktor.
     */
    suspend fun fetchFromServer(site: Site): FetchResult = withContext(Dispatchers.Default) {
        try {
            val response: HttpResponse = httpClient.get(site.url) {
                headers {
                    append(HttpHeaders.UserAgent, "ChangeDetection/1.0")
                }
                // Set a reasonable timeout for individual requests
                timeout {
                    requestTimeoutMillis = 30_000 // 30 seconds
                    connectTimeoutMillis = 15_000 // 15 seconds
                }
            }

            if (!response.status.isSuccess()) {
                val errorMsg = "HTTP ${response.status.value}: ${response.status.description}"
                return@withContext FetchResult.Error(errorMsg)
            }

            val contentType = response.contentType()?.toString() ?: "text/html"
            val bodyText = response.bodyAsText()

            FetchResult.Success(contentType, bodyText.encodeToByteArray())
        } catch (e: Exception) {
            val message = when (e) {
                is HttpRequestTimeoutException -> "Request timed out: ${site.url}"
                is ResponseException -> "HTTP Error: ${e.response.status.value}"
                else -> e.message ?: "Unknown error fetching: ${site.url}"
            }
            FetchResult.Error(message, e)
        }
    }

    /**
     * Parse HTML content and extract text using Ksoup.
     */
    fun parseHtml(content: String): String {
        return try {
            val doc = Ksoup.parse(content)
            doc.text()
        } catch (e: Exception) {
            content
        }
    }

    /**
     * Generate a meaningful description of what changed.
     */
    fun generateChangeDescription(newContent: ByteArray, contentType: String): String {
        return try {
            if (contentType.contains("html", ignoreCase = true)) {
                val content = newContent.decodeToString()
                val doc = Ksoup.parse(content)
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

    /**
     * Check for content changes using simple hash comparison.
     * Returns true if content has changed.
     */
    fun hasContentChanged(
        newContent: ByteArray,
        lastContentHash: String?,
        contentType: String
    ): Boolean {
        if (newContent.isEmpty()) return false
        if (lastContentHash == null) return true

        val newHash = if (contentType.contains("html", ignoreCase = true)) {
            hashHtmlContent(newContent)
        } else {
            hashContent(newContent)
        }

        return newHash != lastContentHash
    }

    /**
     * Simple hash for raw content.
     */
    fun hashContent(content: ByteArray): String {
        // Simple hash based on content - can be replaced with proper SHA-256 if needed
        return content.contentHashCode().toString(16)
    }

    /**
     * Hash for HTML content (after parsing to text).
     */
    fun hashHtmlContent(content: ByteArray): String {
        val textContent = parseHtml(content.decodeToString())
        return textContent.hashCode().toString(16)
    }

    /**
     * Platform-independent way to get current time in milliseconds.
     */
    private fun currentTimeMillis(): Long = System.currentTimeMillis()
}
