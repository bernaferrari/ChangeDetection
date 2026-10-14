package com.bernaferrari.changedetection.repo.source

import com.bernaferrari.changedetection.repo.ContentTypeInfo
import com.bernaferrari.changedetection.repo.Snap
import kotlinx.coroutines.flow.Flow

/**
 * Main entry point for accessing snaps data.
 * Inspired from Architecture Components MVVM sample app
 */
interface SnapsDataSource {
    fun observeStorageBytes(): Flow<Long>

    /** Bytes held by versions older than each page's newest pair, i.e. what [clearOlderVersions] would free. */
    fun observeOlderVersionBytes(): Flow<Long>

    /** Keeps each page's newest two versions (its current change) and deletes everything older. */
    suspend fun clearOlderVersions()

    suspend fun deleteSiteWithSnaps(siteId: String)

    suspend fun getContentTypeInfo(siteId: String): List<ContentTypeInfo>

    suspend fun getMostRecentSnap(siteId: String): Snap?

    suspend fun getAllMostRecentSnaps(): Map<String, Snap>

    /** Number of saved versions per page, loaded in one query where supported. */
    suspend fun getSnapshotCounts(): Map<String, Int> = emptyMap()

    fun getSnaps(siteId: String): Flow<List<Snap>>

    /** Re-emits whenever any row in the snaps table changes (Room invalidation probe). */
    fun observeSnapsTable(): Flow<Boolean>

    fun getSnapsFiltered(
        siteId: String,
        filter: String,
    ): Flow<List<Snap>>

    suspend fun getAllSnapsForSite(siteId: String): List<Snap>

    // fun getSnapForPaging(siteId: String, filter: String): PagingSource<Int, Snap>

    suspend fun getSnapById(snapId: String): Snap?

    suspend fun getSnapContent(snapId: String): ByteArray

    suspend fun getLastNSnapsForSiteId(
        siteId: String,
        limit: Int,
    ): List<Snap>?

    suspend fun getSnapPair(
        originalId: String,
        newId: String,
    ): Pair<Pair<Snap, ByteArray>, Pair<Snap, ByteArray>>?

    suspend fun deleteSnap(snapId: String)

    suspend fun pruneSnaps(siteId: String)

    suspend fun getSingleSnapPair(id: String): ByteArray

    // Fast content hash comparison methods
    suspend fun getLastContentHash(siteId: String): String?

    suspend fun getLastProcessedContentHash(siteId: String): String?
}
