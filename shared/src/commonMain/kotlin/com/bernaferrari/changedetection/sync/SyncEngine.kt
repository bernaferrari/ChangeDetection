package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.error.*
import com.bernaferrari.changedetection.repo.capturePolicy
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import com.bernaferrari.changedetection.util.ContentHasher
import com.bernaferrari.changedetection.util.FileStorage
import com.bernaferrari.changedetection.extensions.findCharset
import com.bernaferrari.changedetection.util.decodeByteArray
import io.github.aakira.napier.Napier
import com.fleeksoft.ksoup.Ksoup
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.room3.withWriteTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
open class SyncEngine(
    private val httpClient: HttpClient,
    private val database: ChangeDatabase,
    private val sitesDao: SitesDao,
    private val snapsDao: SnapsDao,
    private val fileStorage: FileStorage,
    private val errorHandler: ErrorHandler,
    private val changeNotifier: ChangeNotifier = NoOpChangeNotifier,
    private val progressTracker: SyncProgressTracker = SyncProgressTracker(),
    private val maintenance: com.bernaferrari.changedetection.storage.SnapshotMaintenance? = null,
) {
    private val pageLocks = PageCheckLocks()

    /**
     * Finds the best icon for pages that have none yet (added before icons were captured, or screenshot pages
     * not checked since). One light request per page, straight to the page's own site.
     */
    suspend fun resolveMissingIcons() {
        sitesDao.getSitesList().filter { it.iconUrl.isBlank() }.forEach { site ->
            val result = fetchFromServer(site) as? FetchResult.Success ?: return@forEach
            if (!result.contentType.contains("html", true)) return@forEach
            runCatching { com.bernaferrari.changedetection.addedit.htmlIcon(result.data.decodeToString(), site.url) }
                .getOrNull()?.let { runCatching { sitesDao.updateIcon(site.id, it) } }
        }
    }

    private suspend fun <T> withSiteLock(siteId: String, block: suspend () -> T): T = pageLocks.withLock(siteId) {
        progressTracker.track(siteId, block)
    }

    suspend fun isSiteSyncing(siteId: String): Boolean = pageLocks.isLocked(siteId)

    /**
     * Fetch result for internal use.
     */
    sealed class FetchResult {
        data class Success(val contentType: String, val data: ByteArray, val capturedWithBrowser: Boolean = false) : FetchResult() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || this::class != other::class) return false
                other as Success
                if (contentType != other.contentType) return false
                if (capturedWithBrowser != other.capturedWithBrowser) return false
                if (!data.contentEquals(other.data)) return false
                return true
            }

            override fun hashCode(): Int {
                var result = contentType.hashCode()
                result = 31 * result + data.contentHashCode()
                result = 31 * result + capturedWithBrowser.hashCode()
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
    suspend fun trySyncSite(siteId: String): SyncResult = pageLocks.tryWithLock(
        siteId,
        onBusy = { SyncResult.Skipped(siteId, "Already syncing") },
    ) {
        progressTracker.track(siteId) { syncSiteInternal(siteId) }
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

        if (!com.bernaferrari.changedetection.repo.supportsVisualMonitoring && site.captureMode == com.bernaferrari.changedetection.repo.CaptureMode.SCREENSHOT) {
            val error = ContentError(ContentError.Reason.CAPTURE_UNAVAILABLE, "Screenshot monitoring is unavailable on this platform")
            return@withContext processErrorResult(site, FetchResult.Error(error.message.orEmpty(), error))
        }

        Napier.i("Syncing: ${site.title ?: site.url}")

        val fetchResult = fetchFromServer(site)
        processFetchResultUnlocked(site, fetchResult, site.threshold)
    }

    suspend fun processFetchResult(
        site: Site,
        result: FetchResult,
        threshold: Int = 0,
    ): SyncResult = withSiteLock(site.id) {
        withContext(Dispatchers.Default) {
            processFetchResultUnlocked(site, result, threshold)
        }
    }

    /**
     * Core change-detection path. Callers MUST hold the per-site lock
     * (via [processFetchResult], [syncSite] or [trySyncSite]).
     */
    private suspend fun processFetchResultUnlocked(
        site: Site,
        result: FetchResult,
        threshold: Int = 0,
    ): SyncResult = withContext(Dispatchers.Default) {
        val currentSite = sitesDao.getSiteById(site.id)
            ?: return@withContext SyncResult.Skipped(site.id, "Page deleted during check")
        if (currentSite.url != site.url || currentSite.captureMode != site.captureMode || currentSite.threshold != site.threshold || currentSite.visualChangeThresholdBps != site.visualChangeThresholdBps || currentSite.capturePolicy() != site.capturePolicy() || !currentSite.isSyncEnabled) {
            return@withContext SyncResult.Skipped(site.id, "Page changed or paused during check")
        }
        try {
            maintenance?.beforeCheck()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Napier.w("Orphan cleanup will retry on a later check", error)
        }
        when (result) {
            is FetchResult.Success -> {
                if (result.data.isEmpty()) {
                    processErrorResult(currentSite, FetchResult.Error("The page returned no content"))
                } else if (site.captureMode == com.bernaferrari.changedetection.repo.CaptureMode.TEXT && result.contentType.startsWith("image/", true)) {
                    processErrorResult(currentSite, FetchResult.Error("This address returns an image. Choose screenshots or source monitoring."))
                } else {
                    try {
                        processSuccessResult(currentSite, result, threshold)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        processErrorResult(currentSite, FetchResult.Error("Could not read the saved baseline", DatabaseError(DatabaseError.Operation.READ, "Could not read the saved baseline", error)))
                    }
                }
            }

            is FetchResult.Error -> {
                processErrorResult(currentSite, result)
            }
        }
    }

    /** Detection uses pixel evidence; filtered regions are only an explanation for the reader. */
    private suspend fun visiblyChanged(before: ByteArray, after: ByteArray): Boolean {
        val difference = com.bernaferrari.changedetection.image.PlatformImageDiff.compare(before, after, includeDiffImage = false)
        check(difference.error == null && (difference.totalPixels > 0 || difference.dimensionMismatch != null)) { "Could not compare screenshots" }
        return difference.dimensionMismatch != null || difference.differentPixels > 0
    }

    private suspend fun processSuccessResult(
        site: Site,
        incoming: FetchResult.Success,
        threshold: Int,
    ): SyncResult {
        // Pages added before icons were captured get their real (high-resolution) icon from the HTML they already served.
        if (site.iconUrl.isBlank() && incoming.contentType.contains("html", true)) {
            runCatching { com.bernaferrari.changedetection.addedit.htmlIcon(incoming.data.decodeToString(), site.url) }
                .getOrNull()?.let { runCatching { sitesDao.updateIcon(site.id, it) } }
        }
        val prepared = try {
            if (site.captureMode == com.bernaferrari.changedetection.repo.CaptureMode.TEXT) {
                com.bernaferrari.changedetection.detection.prepareTextCapture(incoming.data, incoming.contentType, site.textRegion())
            } else {
                null
            }
        } catch (selection: com.bernaferrari.changedetection.detection.TextRegionException) {
            return processErrorResult(site, FetchResult.Error(selection.message.orEmpty(), selection))
        }
        val result = if (prepared != null) incoming.copy(data = prepared.data) else incoming
        val lastSnap = snapsDao.getLastSnapForSiteId(site.id)
        val contentHash = if (result.contentType.startsWith("image/", true)) {
            // A new browser renderer establishes a silent baseline, rather than a website-change alert.
            val renderer = if (result.capturedWithBrowser) "image-v3" else "image-v2"
            "$renderer:${site.capturePolicy().imageIdentity(result.capturedWithBrowser)}:${ContentHasher.hashContent(result.data)}"
        } else {
            prepared?.contentHash ?: ContentHasher.detectionHash(result.data, result.contentType, site.captureMode)
        }
        val isFirstSnap = lastSnap == null ||
            lastSnap.contentType.startsWith("image/", true) != result.contentType.startsWith("image/", true) ||
            ContentHasher.detectionPolicy(lastSnap.contentHash) != ContentHasher.detectionPolicy(contentHash)

        // A stored normalized hash avoids loading and parsing the previous document
        // on every 30-second check. Legacy rows have no hash, so compare their bytes.
        val hasContentChanged = when {
            isFirstSnap -> true
            lastSnap.contentHash.isNotEmpty() -> lastSnap.contentHash != contentHash
            else -> ContentHasher.contentChanged(result.data, fileStorage.readBytes(lastSnap.snapId), result.contentType)
        }
        val explicitRule = site.captureMode == com.bernaferrari.changedetection.repo.CaptureMode.TEXT || site.captureMode == com.bernaferrari.changedetection.repo.CaptureMode.SOURCE
        val visualThreshold = site.visualFilterBasisPoints().takeIf { result.contentType.startsWith("image/", true) } ?: 0
        val meetsThreshold = if (visualThreshold > 0 && !isFirstSnap && hasContentChanged) {
            try {
                val difference = com.bernaferrari.changedetection.image.PlatformImageDiff.compare(
                    fileStorage.readBytes(requireNotNull(lastSnap).snapId),
                    result.data,
                    includeDiffImage = false,
                )
                if (difference.error != null || (difference.totalPixels <= 0 && difference.dimensionMismatch == null)) {
                    return processErrorResult(site, FetchResult.Error("Could not compare screenshots", ContentError(ContentError.Reason.UNKNOWN)))
                }
                difference.dimensionMismatch != null || difference.differentPixels * 10_000 >= difference.totalPixels * visualThreshold
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                return processErrorResult(site, FetchResult.Error("Could not compare screenshots", error))
            }
        } else if (result.contentType.startsWith("image/", true) && !isFirstSnap && hasContentChanged && !explicitRule) {
            // Encoding changes and pixel differences below the perceptual tolerance do not create a version.
            // Region trimming must never discard the underlying pixel evidence.
            visiblyChanged(fileStorage.readBytes(requireNotNull(lastSnap).snapId), result.data)
        } else {
            visualThreshold > 0 || explicitRule || isFirstSnap || threshold <= 0 || !hasContentChanged ||
                ContentHasher.changedByteCount(result.data, fileStorage.readBytes(lastSnap!!.snapId), result.contentType) >= threshold
        }

        if (hasContentChanged && meetsThreshold) {
            return saveNewSnap(site, result, contentHash, lastSnap?.snapId, isFirstSnap)
        } else {
            val updated = updateStatusIfCurrent(site, successful = true, isBrowser = result.contentType.contains("image", ignoreCase = true), expectedSnapshot = lastSnap)
            if (!updated) return SyncResult.Skipped(site.id, "Page changed or paused during check")
            Napier.d("No changes for ${site.url}")
            return SyncResult.Success(site.id, hadChanges = false)
        }
    }

    private suspend fun saveNewSnap(
        site: Site,
        result: FetchResult.Success,
        contentHash: String,
        previousSnapId: String?,
        isFirstSnap: Boolean,
    ): SyncResult {
        val snapId = Uuid.random().toString()
        var committed = false
        val notificationSite: Site
        try {
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
            fileStorage.writeBytes(snapId, result.data)
            currentCoroutineContext().ensureActive()

            // Complete this short commit even if Stop arrives at the suspension boundary.
            // The committed flag must be set before cancellation can be delivered on return.
            notificationSite = withContext(NonCancellable) {
                val savedSite = inWriteTransaction {
                    val current = currentCaptureSite(site) ?: return@inWriteTransaction null
                    // A fresh start or history edit invalidates the comparison we just made.
                    if (snapsDao.getLastSnapForSiteId(site.id)?.snapId != previousSnapId) return@inWriteTransaction null
                    snapsDao.insertSnap(snap)
                    sitesDao.updateSyncMetadata(
                        siteId = site.id,
                        timestamp = snap.timestamp,
                        successful = true,
                        isBrowser = result.contentType.contains("image", ignoreCase = true),
                        isRead = if (!isFirstSnap) {
                            false
                        } else if (previousSnapId == null) {
                            true
                        } else {
                            null
                        },
                    )
                    current
                }
                committed = savedSite != null
                savedSite
            } ?: return SyncResult.Skipped(site.id, "Page or history changed during check")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val errorResult = errorHandler.handleError(
                if (error is SnapshotStorageError) error else DatabaseError(DatabaseError.Operation.WRITE, "Failed to save snapshot", error),
                ErrorContext.SYNC_WORKER,
                "save_snap",
                site.id,
            )
            try {
                updateStatusIfCurrent(site, successful = false, isBrowser = site.isBrowser, failureCode = CheckFailure.from(errorResult, error).name)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (statusError: Exception) {
                Napier.w("Could not record failed snapshot status for ${site.id}", statusError)
            }
            return SyncResult.Failed(site.id, errorResult, error)
        } finally {
            if (!committed) {
                // Room rolls back failed transactions. Never delete a committed row here.
                // A failed write can leave a partial file even when writeBytes did not return.
                withContext(NonCancellable) {
                    runCatching {
                        if (!fileStorage.deleteFile(snapId) && fileStorage.fileExists(snapId)) {
                            if (!fileStorage.deleteFile(snapId)) Napier.w("Orphan cleanup will retry snapshot $snapId")
                        }
                    }.onFailure { Napier.w("Could not clean up incomplete snapshot $snapId", it) }
                }
            }
        }

        // Side effects after the commit can never remove durable history.
        if (!isFirstSnap && notificationSite.isNotificationEnabled) {
            try {
                changeNotifier.notifyChange(notificationSite, snapId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Napier.w("Saved change, but notification delivery failed for ${site.id}", error)
            }
        }
        try {
            maintenance?.afterSavedVersion()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Napier.w("Saved version retained; history maintenance will retry", error)
        }
        return SyncResult.Success(site.id, hadChanges = !isFirstSnap, snapId = snapId, contentSize = result.data.size)
    }

    /** Must be read under the same write transaction as the status or snapshot commit. */
    private suspend fun currentCaptureSite(site: Site): Site? = sitesDao.getSiteById(site.id)?.takeIf {
        it.url == site.url && it.isSyncEnabled && it.threshold == site.threshold && it.visualChangeThresholdBps == site.visualChangeThresholdBps && it.captureMode == site.captureMode && it.capturePolicy() == site.capturePolicy()
    }

    private suspend fun updateStatusIfCurrent(site: Site, successful: Boolean, isBrowser: Boolean, expectedSnapshot: Snap? = null, failureCode: String? = null): Boolean = inWriteTransaction {
        currentCaptureSite(site) ?: return@inWriteTransaction false
        if (expectedSnapshot != null && snapsDao.getLastSnapForSiteId(site.id)?.snapId != expectedSnapshot.snapId) return@inWriteTransaction false
        sitesDao.updateSyncMetadata(site.id, currentTimeMillis(), successful, isBrowser, isRead = null, failureCode = failureCode)
        true
    }

    private suspend fun processErrorResult(site: Site, result: FetchResult.Error): SyncResult {
        val errorResult = result.errorResult ?: errorHandler.handleError(
            result.throwable ?: Exception(result.message),
            ErrorContext.SYNC_WORKER,
            "fetch_content",
            site.id,
        )

        if (!updateStatusIfCurrent(site, successful = false, isBrowser = site.isBrowser, failureCode = CheckFailure.from(errorResult, result.throwable).name)) {
            return SyncResult.Skipped(site.id, "Page changed or paused during check")
        }
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

    /** Checks only the active pages whose last check is older than [intervalMinutes]; used when the app comes back. */
    suspend fun syncDueSites(intervalMinutes: Long): Int = withContext(Dispatchers.Default) {
        val now = currentTimeMillis()
        val due = sitesDao.getSitesList().filter { it.isSyncEnabled && now - it.timestamp >= intervalMinutes * 60_000L }
        due.forEach { trySyncSite(it.id) }
        due.size
    }

    /**
     * Fetch content from a website URL using Ktor.
     */
    suspend fun fetchFromServer(site: Site): FetchResult = withContext(Dispatchers.Default) {
        try {
            httpClient.prepareGet(site.url) {
                headers {
                    append(HttpHeaders.UserAgent, USER_AGENT)
                    append(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                }
                timeout {
                    requestTimeoutMillis = REQUEST_TIMEOUT_MS
                    connectTimeoutMillis = CONNECT_TIMEOUT_MS
                }
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    val errorResult = errorHandler.handleError(
                        NetworkError(response.status.value, "HTTP ${response.status.value}"),
                        ErrorContext.SYNC_WORKER,
                        "fetch_${site.id}",
                        site.id,
                    )
                    return@execute FetchResult.Error(
                        "HTTP ${response.status.value}: ${response.status.description}",
                        throwable = NetworkError(response.status.value, "HTTP ${response.status.value}"),
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
                    return@execute FetchResult.Error(
                        "Content too large (${contentLength / 1024 / 1024}MB)",
                        throwable = SyncError(SyncError.Reason.CONTENT_TOO_LARGE),
                        errorResult = errorResult,
                    )
                }
                // Stream the body so the size cap is enforced even without a
                // Content-Length header, and keep binary payloads intact
                // (decoding everything as text corrupted images and other binaries).
                // Extract the charset label from the Content-Type header text
                // (multiplatform-safe; response.charset() is JVM-only).
                val charsetName = contentType.findCharset()?.takeIf { it.isNotBlank() } ?: "UTF-8"
                val isText = contentType.contains("text/", ignoreCase = true) ||
                    contentType.contains("html", ignoreCase = true) ||
                    contentType.contains("xml", ignoreCase = true) ||
                    contentType.contains("json", ignoreCase = true)

                val bodyBytes = readCappedBody(response.bodyAsChannel())
                val data = if (isText) {
                    // Normalize text payloads to UTF-8 bytes (decodeByteArray falls
                    // back to UTF-8 when the label is unknown).
                    decodeByteArray(bodyBytes, charsetName).encodeToByteArray()
                } else {
                    bodyBytes
                }
                FetchResult.Success(contentType, data)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
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

    /**
     * Reads the response body enforcing MAX_CONTENT_SIZE during the read, so
     * chunked responses without a Content-Length header cannot balloon memory.
     */
    private suspend fun readCappedBody(channel: ByteReadChannel): ByteArray {
        val source = channel.readRemaining(MAX_CONTENT_SIZE + 1)
        val bytes = source.readByteArray()
        if (bytes.size > MAX_CONTENT_SIZE) {
            throw SyncError(
                SyncError.Reason.CONTENT_TOO_LARGE,
                "Response exceeded ${MAX_CONTENT_SIZE / 1024 / 1024}MB",
            )
        }
        return bytes
    }

    /**
     * Runs [block] inside a database write transaction. Extracted as an open
     * hook so tests can substitute a no-op transaction around mocked DAOs
     * (Room's withWriteTransaction extension cannot be stubbed by Mockito).
     */
    protected open suspend fun <T> inWriteTransaction(block: suspend () -> T): T = database.withWriteTransaction { block() }

    private fun currentTimeMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()

    companion object {
        const val USER_AGENT = "Mozilla/5.0 (compatible; ChangeDetection/2.0)"
        const val REQUEST_TIMEOUT_MS = 30_000L
        const val CONNECT_TIMEOUT_MS = 15_000L
        const val MAX_CONTENT_SIZE = 10L * 1024 * 1024 // 10MB limit
    }
}
