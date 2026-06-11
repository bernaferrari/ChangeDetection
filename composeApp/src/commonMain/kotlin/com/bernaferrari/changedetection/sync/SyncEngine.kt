/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.error.*
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import com.bernaferrari.changedetection.util.ContentHasher
import com.bernaferrari.changedetection.util.FileStorage
import io.github.aakira.napier.Napier
import com.fleeksoft.ksoup.Ksoup
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Result of a sync operation with detailed error information.
 */
sealed class SyncResult {
    data class Success(
        val siteId: String,
        val hadChanges: Boolean,
        val snapId: String? = null,
        val contentSize: Int = 0,
    ) : SyncResult()

    data class Failed(
        val siteId: String,
        val error: ErrorResult,
        val exception: Throwable? = null,
    ) : SyncResult()

    data class Skipped(
        val siteId: String,
        val reason: String,
    ) : SyncResult()
}

/**
 * Shared Sync Engine for fetching website content.
 *
 * This class contains the core sync logic using:
 * - Ktor for networking (multiplatform)
 * - Ksoup for HTML parsing (multiplatform)
 * - FileStorage for content persistence (platform-specific)
 */
@OptIn(ExperimentalUuidApi::class)
class SyncEngine(
    private val httpClient: HttpClient,
    private val sitesDao: SitesDao,
    private val snapsDao: SnapsDao,
    private val fileStorage: FileStorage,
    private val errorHandler: ErrorHandler,
) {
    // Per-site mutexes to prevent concurrent syncs of the same site
    private val siteMutexes = mutableMapOf<String, Mutex>()
    private val mutexMapLock = Mutex()

    private suspend fun <T> withSiteLock(siteId: String, block: suspend () -> T): T {
        val mutex = mutexMapLock.withLock {
            siteMutexes.getOrPut(siteId) { Mutex() }
        }
        return mutex.withLock { block() }
    }

    /**
     * Check if a site is currently being synced.
     */
    suspend fun isSiteSyncing(siteId: String): Boolean = mutexMapLock.withLock {
        siteMutexes[siteId]?.isLocked == true
    }

    /**
     * Fetch result for internal use.
     */
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

        data class Error(
            val message: String,
            val throwable: Throwable? = null,
            val errorResult: ErrorResult? = null,
        ) : FetchResult()
    }

    /**
     * Sync a specific site by ID.
     * Uses per-site locking to prevent concurrent syncs of the same site.
     *
     * @return SyncResult with success/failure details
     */
    suspend fun syncSite(siteId: String): SyncResult = withSiteLock(siteId) {
        syncSiteInternal(siteId)
    }

    /**
     * Try to sync a site, returning immediately if already syncing.
     *
     * @return SyncResult with success/failure details, or Skipped if already syncing
     */
    suspend fun trySyncSite(siteId: String): SyncResult {
        val mutex = mutexMapLock.withLock {
            siteMutexes.getOrPut(siteId) { Mutex() }
        }

        if (!mutex.tryLock()) {
            return SyncResult.Skipped(siteId, "Already syncing")
        }

        return try {
            syncSiteInternal(siteId)
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun syncSiteInternal(siteId: String): SyncResult = withContext(Dispatchers.Default) {
        val site = sitesDao.getSiteById(siteId)

        if (site == null) {
            Napier.w("Site not found: $siteId")
            return@withContext SyncResult.Skipped(siteId, "Site not found")
        }

        if (!site.isSyncEnabled) {
            Napier.d("Sync disabled for site: ${site.title}")
            return@withContext SyncResult.Skipped(siteId, "Sync disabled")
        }

        Napier.i("Syncing: ${site.title ?: site.url}")

        val fetchResult = fetchFromServer(site)
        processFetchResult(site, fetchResult, site.threshold)
    }

    /**
     * Process fetch result and save if changed.
     * This is the main entry point for external fetchers (e.g., ScreenshotService).
     */
    suspend fun processFetchResult(
        site: Site,
        result: FetchResult,
        threshold: Int = 0,
    ): SyncResult = withContext(Dispatchers.Default) {
        when (result) {
            is FetchResult.Success -> {
                processSuccessResult(site, result, threshold)
            }
            is FetchResult.Error -> {
                processErrorResult(site, result)
            }
        }
    }

    private suspend fun processSuccessResult(
        site: Site,
        result: FetchResult.Success,
        threshold: Int,
    ): SyncResult {
        val lastSnap = snapsDao.getLastSnapForSiteId(site.id)
        val isHtml = result.contentType.contains("html", ignoreCase = true)

        val contentHash = if (isHtml) {
            ContentHasher.hashHtmlContent(result.data)
        } else {
            ContentHasher.hashContent(result.data)
        }

        val hasContentChanged = ContentHasher.contentChanged(
            result.data,
            lastSnap?.let { fileStorage.readBytes(it.snapId) } ?: ByteArray(0),
            result.contentType,
        )

        val meetsThreshold = threshold <= kotlin.math.abs(
            (lastSnap?.contentSize ?: 0) - result.data.size,
        )

        if (hasContentChanged && meetsThreshold || lastSnap == null) {
            return saveNewSnap(site, result, contentHash, lastSnap == null)
        } else {
            // No changes - update timestamp only
            sitesDao.updateSite(site.copy(timestamp = currentTimeMillis(), isSuccessful = true))
            Napier.d("No changes for ${site.url}")
            return SyncResult.Success(site.id, hadChanges = false)
        }
    }

    private suspend fun saveNewSnap(
        site: Site,
        result: FetchResult.Success,
        contentHash: String,
        isFirstSnap: Boolean,
    ): SyncResult {
        val snapId = Uuid.random().toString()
        var fileWritten = false

        return try {
            val snap = Snap(
                snapId = snapId,
                siteId = site.id,
                timestamp = currentTimeMillis(),
                contentType = result.contentType,
                contentCharset = "UTF-8",
                contentSize = result.data.size,
                contentHash = contentHash,
                processedContentHash = contentHash,
            )

            // Step 1: Save content to file storage first (can be cleaned up on failure)
            fileStorage.writeBytes(snapId, result.data)
            fileWritten = true

            // Step 2: Save metadata to database
            snapsDao.insertSnap(snap)

            // Step 3: Update site status
            sitesDao.updateSite(
                site.copy(timestamp = currentTimeMillis(), isSuccessful = true),
            )

            val changeReason = if (isFirstSnap) "Initial snapshot" else "Content changed"
            Napier.i("✓ Saved snap for ${site.url}: $changeReason (${result.data.size} bytes)")

            SyncResult.Success(
                siteId = site.id,
                hadChanges = true,
                snapId = snapId,
                contentSize = result.data.size,
            )
        } catch (e: Exception) {
            // Rollback: Clean up file if it was written but database operation failed
            if (fileWritten) {
                try {
                    fileStorage.deleteFile(snapId)
                    Napier.d("Rolled back file for failed snap: $snapId")
                } catch (cleanupError: Exception) {
                    Napier.w("Failed to clean up file for failed snap: $snapId", cleanupError)
                }
            }

            // Rollback: Try to delete snap metadata if it was inserted
            try {
                snapsDao.deleteSnapById(snapId)
            } catch (_: Exception) {
                // Ignore cleanup errors
            }

            val errorResult = errorHandler.handleError(
                DatabaseError(DatabaseError.Operation.WRITE, "Failed to save snapshot", e),
                ErrorContext.SYNC_WORKER,
                "save_snap",
                site.id,
            )
            SyncResult.Failed(site.id, errorResult, e)
        }
    }

    private suspend fun processErrorResult(site: Site, result: FetchResult.Error): SyncResult {
        val errorResult = result.errorResult ?: errorHandler.handleError(
            result.throwable ?: Exception(result.message),
            ErrorContext.SYNC_WORKER,
            "fetch_content",
            site.id,
        )

        // Update site as failed
        sitesDao.updateSite(
            site.copy(timestamp = currentTimeMillis(), isSuccessful = false),
        )

        Napier.e("✗ Sync failed for ${site.url}: ${result.message}", result.throwable)

        return SyncResult.Failed(site.id, errorResult, result.throwable)
    }

    /**
     * Sync all enabled sites.
     *
     * @return Pair of (successCount, results list)
     */
    suspend fun syncAllSites(): Pair<Int, List<SyncResult>> = withContext(Dispatchers.Default) {
        val sites = sitesDao.getSitesList()
        val results = mutableListOf<SyncResult>()
        var successCount = 0

        Napier.i("Starting sync for ${sites.size} sites")

        for (site in sites) {
            if (site.isSyncEnabled) {
                val result = syncSite(site.id)
                results.add(result)
                if (result is SyncResult.Success) {
                    successCount++
                }
            }
        }

        Napier.i("Sync complete: $successCount/${sites.size} succeeded")
        successCount to results
    }

    /**
     * Fetch content from a website URL using Ktor.
     */
    suspend fun fetchFromServer(site: Site): FetchResult = withContext(Dispatchers.Default) {
        try {
            val response: HttpResponse = httpClient.get(site.url) {
                headers {
                    append(HttpHeaders.UserAgent, USER_AGENT)
                    append(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                }
                timeout {
                    requestTimeoutMillis = REQUEST_TIMEOUT_MS
                    connectTimeoutMillis = CONNECT_TIMEOUT_MS
                }
            }

            if (!response.status.isSuccess()) {
                val errorResult = errorHandler.handleError(
                    NetworkError(response.status.value, "HTTP ${response.status.value}"),
                    ErrorContext.SYNC_WORKER,
                    "fetch_${site.id}",
                    site.id,
                )
                return@withContext FetchResult.Error(
                    "HTTP ${response.status.value}: ${response.status.description}",
                    errorResult = errorResult,
                )
            }

            val contentType = response.contentType()?.toString() ?: "text/html"

            // Check content size before loading
            val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0
            if (contentLength > MAX_CONTENT_SIZE) {
                val errorResult = errorHandler.handleError(
                    SyncError(SyncError.Reason.CONTENT_TOO_LARGE, "Content size: $contentLength"),
                    ErrorContext.SYNC_WORKER,
                    "fetch_${site.id}",
                    site.id,
                )
                return@withContext FetchResult.Error(
                    "Content too large (${contentLength / 1024 / 1024}MB)",
                    errorResult = errorResult,
                )
            }

            val bodyText = response.bodyAsText()
            FetchResult.Success(contentType, bodyText.encodeToByteArray())
        } catch (e: Exception) {
            val errorResult = errorHandler.handleError(
                e,
                ErrorContext.SYNC_WORKER,
                "fetch_${site.id}",
                site.id,
            )
            FetchResult.Error(
                e.message ?: "Unknown error",
                e,
                errorResult,
            )
        }
    }

    /**
     * Parse HTML content and extract text using Ksoup.
     */
    fun parseHtml(content: String): String = try {
        Ksoup.parse(content).text()
    } catch (e: Exception) {
        content
    }

    /**
     * Generate a meaningful description of what changed.
     */
    fun generateChangeDescription(newContent: ByteArray, contentType: String): String = try {
        if (contentType.contains("html", ignoreCase = true)) {
            val doc = Ksoup.parse(newContent.decodeToString())
            val textLength = doc.text().length

            when {
                textLength > 10000 -> "Major content update"
                textLength > 5000 -> "Significant changes"
                textLength > 1000 -> "Content updated"
                else -> "Minor update"
            }
        } else if (contentType.contains("image", ignoreCase = true)) {
            "Visual change"
        } else {
            val sizeKb = newContent.size / 1024
            when {
                sizeKb > 500 -> "Large update (${sizeKb}KB)"
                sizeKb > 100 -> "Update (${sizeKb}KB)"
                else -> "Content changed"
            }
        }
    } catch (e: Exception) {
        "Content changed"
    }

    private fun currentTimeMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()

    companion object {
        const val USER_AGENT = "Mozilla/5.0 (compatible; ChangeDetection/2.0)"
        const val REQUEST_TIMEOUT_MS = 30_000L
        const val CONNECT_TIMEOUT_MS = 15_000L
        const val MAX_CONTENT_SIZE = 10L * 1024 * 1024 // 10MB limit
    }
}
