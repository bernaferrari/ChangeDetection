package com.bernaferrari.changedetection.repo.source

import kotlinx.coroutines.flow.Flow
import com.bernaferrari.changedetection.repo.ContentTypeInfo
import com.bernaferrari.changedetection.repo.Snap

/**
 * Concrete implementation to load snaps from the data sources into a cache.
 * KMP Version.
 */
class SnapsRepository(
    private val mSnapsLocalDataSource: SnapsDataSource
) : SnapsDataSource {
    override suspend fun getSingleSnapPair(id: String): ByteArray {
        return mSnapsLocalDataSource.getSingleSnapPair(id)
    }

    // override fun getSnapForPaging(siteId: String, filter: String): PagingSource<Int, Snap> {
    //    return mSnapsLocalDataSource.getSnapForPaging(siteId, filter)
    // }

    override suspend fun deleteAllSnaps(siteId: String) {
        return mSnapsLocalDataSource.deleteAllSnaps(siteId)
    }

    override suspend fun deleteSnapsForSiteIdAndContentType(siteId: String, contentType: String) {
        return mSnapsLocalDataSource.deleteSnapsForSiteIdAndContentType(siteId, contentType)
    }

    override suspend fun getContentTypeInfo(siteId: String): List<ContentTypeInfo> {
        return mSnapsLocalDataSource.getContentTypeInfo(siteId)
    }

    override suspend fun getMostRecentSnap(siteId: String): Snap? {
        return mSnapsLocalDataSource.getMostRecentSnap(siteId)
    }

    override suspend fun getAllMostRecentSnaps(): Map<String, Snap> {
        return mSnapsLocalDataSource.getAllMostRecentSnaps()
    }

    override suspend fun getSnapById(snapId: String): Snap? {
        return mSnapsLocalDataSource.getSnapById(snapId)
    }

    override fun getSnaps(siteId: String): Flow<List<Snap>> {
        return mSnapsLocalDataSource.getSnaps(siteId)
    }

    override fun getSnapsFiltered(siteId: String, filter: String): Flow<List<Snap>> {
        return mSnapsLocalDataSource.getSnapsFiltered(siteId, filter)
    }

    override suspend fun getSnapContent(snapId: String): ByteArray {
        return mSnapsLocalDataSource.getSnapContent(snapId)
    }

    override suspend fun getSnapPair(
        originalId: String,
        newId: String
    ): Pair<Pair<Snap, ByteArray>, Pair<Snap, ByteArray>>? {
        return mSnapsLocalDataSource.getSnapPair(originalId, newId)
    }

    override suspend fun getLastNSnapsForSiteId(siteId: String, limit: Int): List<Snap>? {
        return mSnapsLocalDataSource.getLastNSnapsForSiteId(siteId, limit)
    }

    override suspend fun saveSnap(snap: Snap, content: ByteArray, threshold: Int): Result<Snap> {
        return mSnapsLocalDataSource.saveSnap(snap, content, threshold)
    }

    override suspend fun deleteSnap(snapId: String) {
        mSnapsLocalDataSource.deleteSnap(snapId)
    }

    override suspend fun pruneSnaps(siteId: String) {
        mSnapsLocalDataSource.pruneSnaps(siteId)
    }

    override suspend fun getAllSnapsForSite(siteId: String): List<Snap> {
        return mSnapsLocalDataSource.getAllSnapsForSite(siteId)
    }

    override suspend fun getLastContentHash(siteId: String): String? {
        return mSnapsLocalDataSource.getLastContentHash(siteId)
    }

    override suspend fun getLastProcessedContentHash(siteId: String): String? {
        return mSnapsLocalDataSource.getLastProcessedContentHash(siteId)
    }
}
