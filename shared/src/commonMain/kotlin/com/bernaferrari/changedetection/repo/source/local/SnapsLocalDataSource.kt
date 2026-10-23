package com.bernaferrari.changedetection.repo.source.local

import com.bernaferrari.changedetection.repo.AppExecutors
import com.bernaferrari.changedetection.repo.ContentTypeInfo
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsDataSource
import com.bernaferrari.changedetection.util.FileStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of a data source as a db.
 * KMP Compatible version.
 */
class SnapsLocalDataSource constructor(
    private val mAppExecutors: AppExecutors,
    private val mSnapsDao: SnapsDao,
    private val sitesDao: SitesDao,
    private val fileStorage: FileStorage,
    private val cleanup: com.bernaferrari.changedetection.storage.SnapshotFileCleanup,
) : SnapsDataSource {
    override fun observeStorageBytes(): Flow<Long> = mSnapsDao.observeStorageBytes()

    override fun observeOlderVersionBytes(): Flow<Long> = mSnapsDao.observeOlderVersionBytes()

    override suspend fun clearOlderVersions() = withContext(mAppExecutors.ioContext) {
        mSnapsDao.siteIdsWithOlderVersions().forEach { pruneSnaps(it) }
    }

    /** Keep the comparison pair; each bounded transaction also queues its file cleanup. */
    override suspend fun pruneSnaps(siteId: String) = withContext(mAppExecutors.ioContext) {
        while (true) {
            val obsolete = mSnapsDao.removeOlderVersionsBatch(siteId)
            if (obsolete.isEmpty()) break
            cleanup.drain()
        }
    }

    /**
     * Atomically deletes a page and all its saved-version rows. Snap content files
     * are queued transactionally by SQLite and removed in bounded, retryable batches.
     */
    override suspend fun deleteSiteWithSnaps(siteId: String): Unit = withContext(mAppExecutors.ioContext) {
        sitesDao.deleteSiteById(siteId)
        cleanup.drain()
    }

    override suspend fun deleteSnap(snapId: String) {
        withContext(mAppExecutors.ioContext) {
            // The database trigger commits cleanup intent with the row deletion.
            mSnapsDao.deleteSnapById(snapId)
            cleanup.drain()
        }
    }

    override fun getSnaps(siteId: String): Flow<List<Snap>> = mSnapsDao.getAllSnapsForSiteIdWithFlow(siteId)

    /** Re-emits whenever any row in the snaps table changes (Room invalidation). */
    override fun observeSnapsTable(): Flow<Boolean> = mSnapsDao.observeSnapsTable()

    override fun getSnapsFiltered(
        siteId: String,
        filter: String,
    ): Flow<List<Snap>> = mSnapsDao.getSnapsWithChange(siteId, filter)

    override suspend fun getAllSnapsForSite(siteId: String): List<Snap> = withContext(mAppExecutors.ioContext) {
        mSnapsDao.getAllSnapsForSiteId(siteId)
    }

    override suspend fun getSnapById(snapId: String): Snap? = withContext(mAppExecutors.ioContext) {
        mSnapsDao.getSnapById(snapId)
    }

    override suspend fun getSnapContent(snapId: String): ByteArray = withContext(mAppExecutors.ioContext) {
        fileStorage.readBytes(snapId)
    }

    override suspend fun getContentTypeInfo(siteId: String): List<ContentTypeInfo> = withContext(mAppExecutors.ioContext) {
        mSnapsDao.getContentTypesWithCount(siteId).map {
            ContentTypeInfo(it.contentType, it.count)
        }
    }

    override suspend fun getMostRecentSnap(siteId: String): Snap? = withContext(mAppExecutors.ioContext) {
        mSnapsDao.getLastSnapForSiteId(siteId)
    }

    override suspend fun getAllMostRecentSnaps(): Map<String, Snap> = withContext(mAppExecutors.ioContext) {
        val allSnapshots = mSnapsDao.getAllMostRecentSnaps()
        allSnapshots.associateBy { it.siteId }
    }

    override suspend fun getSnapshotCounts(): Map<String, Int> = withContext(mAppExecutors.ioContext) {
        mSnapsDao.getSnapshotCounts().associate { it.siteId to it.count }
    }

    override suspend fun getLastNSnapsForSiteId(
        siteId: String,
        limit: Int,
    ): List<Snap> = withContext(mAppExecutors.ioContext) {
        mSnapsDao.getLastNSnapsForSiteId(siteId, limit)
    }

    override suspend fun getSnapPair(
        originalId: String,
        newId: String,
    ): Pair<Pair<Snap, ByteArray>, Pair<Snap, ByteArray>>? = withContext(mAppExecutors.ioContext) {
        val originalSnap = mSnapsDao.getSnapById(originalId)
        val newSnap = mSnapsDao.getSnapById(newId)

        if (originalSnap == null || newSnap == null) {
            appLog.w("⚠️ Snap not found: original=$originalSnap, new=$newSnap")
            return@withContext null
        }

        try {
            val originalContent = fileStorage.readBytes(originalId)
            val newContent = fileStorage.readBytes(newId)

            (originalSnap to originalContent) to (newSnap to newContent)
        } catch (e: Exception) {
            appLog.e("❌ Error reading snap content: ${e.message}")
            null
        }
    }

    override suspend fun getSingleSnapPair(id: String): ByteArray = withContext(mAppExecutors.ioContext) {
        fileStorage.readBytes(id)
    }

    override suspend fun getLastContentHash(siteId: String): String? = withContext(mAppExecutors.ioContext) {
        mSnapsDao.getLastContentHash(siteId)
    }

    override suspend fun getLastProcessedContentHash(siteId: String): String? = withContext(mAppExecutors.ioContext) {
        mSnapsDao.getLastProcessedContentHash(siteId)
    }
}

private val appLog = com.bernaferrari.changedetection.logging.AppLog.tagged("SnapsLocalDataSource")
