package com.bernaferrari.changedetection.data.source

import android.arch.lifecycle.LiveData
import android.arch.paging.DataSource
import com.bernaferrari.changedetection.data.ContentTypeInfo
import com.bernaferrari.changedetection.data.Snap

/**
 * Main entry point for accessing snaps data.
 * Inspired from Architecture Components MVVM sample app
 */
interface SnapsDataSource {

    interface GetSnapsCallback {

        fun onSnapsLoaded(snap: Snap)

        fun onDataNotAvailable()
    }

    fun getContentTypeInfo(siteId: String, callback: ((List<ContentTypeInfo>) -> Unit))

    fun getMostRecentSnap(siteId: String, callback: ((Snap?) -> (Unit)))

    fun getSnaps(siteId: String, callback: ((LiveData<List<Snap>>) -> Unit))

    fun getSnapForPaging(siteId: String): DataSource.Factory<Int, Snap>

    fun getSnapContent(snapId: String, callback: ((ByteArray) -> (Unit)))

    fun getSnapPair(
        originalId: String,
        newId: String,
        callback: ((Pair<Pair<Snap, ByteArray>, Pair<Snap, ByteArray>>) -> (Unit))
    )

    fun saveSnap(snap: Snap, content: ByteArray, callback: GetSnapsCallback)

    fun deleteAllSnaps(siteId: String)

    fun deleteSnap(snapId: String)
}
