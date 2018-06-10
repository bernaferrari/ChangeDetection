package com.bernaferrari.changedetection.data.source

import android.arch.lifecycle.LiveData
import android.arch.paging.DataSource
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

    fun getMostRecentSnaps(siteId: String, callback: ((List<Int>) -> (Unit)))

    fun getSnaps(siteId: String): LiveData<List<Snap>>

    fun getSnapForPaging(siteId: String): DataSource.Factory<Int, Snap>

    fun getHeavySnapForPaging(siteId: String): DataSource.Factory<Int, Snap>

    fun getSnapContent(snapId: String, callback: ((ByteArray) -> (Unit)))

    fun getSnapPair(
        originalId: String,
        newId: String,
        callback: ((Pair<Pair<Snap, ByteArray>, Pair<Snap, ByteArray>>) -> (Unit))
    )

    fun saveSnap(snap: Snap, content: ByteArray, callback: GetSnapsCallback)

    fun deleteAllSnapsForSite(siteId: String)

    fun deleteSnap(snapId: String)
}
