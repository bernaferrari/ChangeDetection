package com.bernaferrari.changedetection.data.source

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import com.bernaferrari.changedetection.data.ContentTypeInfo
import com.bernaferrari.changedetection.data.Snap
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * For simplicity, this implements a dumb synchronisation between locally persisted data and data
 * obtained from the server, by using the remote data source only if the local database doesn't
 * exist or is empty.
 *
 * Inspired from Architecture Components MVVM sample app
 */
@Singleton
class SnapsRepository @Inject constructor(
    private val mSnapsLocalDataSource: SnapsDataSource
) : SnapsDataSource {

    override suspend fun getSnapForPaging(
        siteId: String,
        filter: String
    ): DataSource.Factory<Int, Snap> {
        return mSnapsLocalDataSource.getSnapForPaging(siteId, filter)
    }

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

    override suspend fun getSnaps(siteId: String): LiveData<List<Snap>> {
        return mSnapsLocalDataSource.getSnaps(siteId)
    }

    override suspend fun getSnapsFiltered(siteId: String, filter: String): LiveData<List<Snap>> {
        return mSnapsLocalDataSource.getSnapsFiltered(siteId, filter)
    }

    override suspend fun getSnapContent(snapId: String): ByteArray {
        return mSnapsLocalDataSource.getSnapContent(snapId)
    }

    override suspend fun getSnapPair(
        originalId: String,
        newId: String
    ): Pair<Pair<Snap, ByteArray>, Pair<Snap, ByteArray>> {
        return mSnapsLocalDataSource.getSnapPair(originalId, newId)
    }

    override suspend fun saveSnap(snap: Snap, content: ByteArray): Result<Snap> {
        return mSnapsLocalDataSource.saveSnap(snap, content)
    }

    override suspend fun deleteSnap(snapId: String) {
        mSnapsLocalDataSource.deleteSnap(snapId)
    }

    override suspend fun pruneSnaps(siteId: String) {
        mSnapsLocalDataSource.pruneSnaps(siteId)
    }
}
