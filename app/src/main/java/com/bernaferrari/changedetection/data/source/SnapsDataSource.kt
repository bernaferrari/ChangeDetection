package com.bernaferrari.changedetection.data.source

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import com.bernaferrari.changedetection.data.ContentTypeInfo
import com.bernaferrari.changedetection.data.Snap

/**
 * Main entry point for accessing snaps data.
 * Inspired from Architecture Components MVVM sample app
 */
interface SnapsDataSource {

    suspend fun getContentTypeInfo(siteId: String): List<ContentTypeInfo>

    suspend fun getMostRecentSnap(siteId: String): Snap?

    suspend fun getSnaps(siteId: String): LiveData<List<Snap>>

    suspend fun getSnapsFiltered(siteId: String, filter: String): LiveData<List<Snap>>

    suspend fun getSnapForPaging(siteId: String, filter: String): DataSource.Factory<Int, Snap>

    suspend fun getSnapContent(snapId: String): ByteArray

    suspend fun getSnapPair(
        originalId: String,
        newId: String
    ): Pair<Pair<Snap, ByteArray>, Pair<Snap, ByteArray>>

    suspend fun saveSnap(snap: Snap, content: ByteArray): Result<Snap>

    suspend fun deleteAllSnaps(siteId: String)

    suspend fun deleteSnap(snapId: String)

    suspend fun deleteSnapsForSiteIdAndContentType(siteId: String, contentType: String)

    suspend fun pruneSnaps(siteId: String)
}
