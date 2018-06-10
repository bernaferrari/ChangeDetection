package com.bernaferrari.changedetection.data.source

import android.arch.lifecycle.LiveData
import android.arch.paging.DataSource
import com.bernaferrari.changedetection.data.Snap

/**
 *
 * For simplicity, this implements a dumb synchronisation between locally persisted data and data
 * obtained from the server, by using the remote data source only if the local database doesn't
 * exist or is empty.
 *
 * Inspired from Architecture Components MVVM sample app
 */
class SnapsRepository // Prevent direct instantiation.
private constructor(
    snapsLocalDataSource: SnapsDataSource
) : SnapsDataSource {

    override fun getSnaps(
        siteId: String
    ): LiveData<List<Snap>> = mSnapsLocalDataSource.getSnaps(siteId)

    override fun getMostRecentSnaps(siteId: String, callback: (List<Int>) -> Unit) {
        mSnapsLocalDataSource.getMostRecentSnaps(
            siteId
        ) {
            callback.invoke(it)
        }
    }

    override fun getSnapPair(
        originalId: String,
        newId: String,
        callback: ((Pair<Pair<Snap, ByteArray>, Pair<Snap, ByteArray>>) -> (Unit))
    ) {
        mSnapsLocalDataSource.getSnapPair(
            originalId,
            newId
        ) {
            callback.invoke(it)
        }
    }

    override fun getHeavySnapForPaging(siteId: String): DataSource.Factory<Int, Snap> {
        return mSnapsLocalDataSource.getHeavySnapForPaging(siteId)
    }

    override fun getSnapForPaging(siteId: String): DataSource.Factory<Int, Snap> {
        return mSnapsLocalDataSource.getSnapForPaging(siteId)
    }

    override fun deleteAllSnapsForSite(siteId: String) {
        mSnapsLocalDataSource.deleteAllSnapsForSite(siteId)
    }

    override fun getSnapContent(snapId: String, callback: ((ByteArray) -> (Unit))) {
        checkNotNull(snapId)
        checkNotNull(callback)

        // Is the site in the local data source? If not, query the network.
        mSnapsLocalDataSource.getSnapContent(snapId) {
            callback.invoke(it)
        }
    }

    override fun deleteSnap(snapId: String) {
        mSnapsLocalDataSource.deleteSnap(snapId)
    }

    private val mSnapsLocalDataSource: SnapsDataSource = checkNotNull(snapsLocalDataSource)

    override fun saveSnap(
        snap: Snap,
        content: ByteArray,
        callback: SnapsDataSource.GetSnapsCallback
    ) {
        checkNotNull(snap)
        mSnapsLocalDataSource.saveSnap(snap, content, object : SnapsDataSource.GetSnapsCallback {
            override fun onSnapsLoaded(snap: Snap) {
                callback.onSnapsLoaded(snap)
            }

            override fun onDataNotAvailable() {
                callback.onDataNotAvailable()
            }
        })
    }

    companion object {

        @Volatile
        private var INSTANCE: SnapsRepository? = null

        /**
         * Returns the single instance of this class, creating it if necessary.
         *
         * @param tasksRemoteDataSource the backend data source
         * @param tasksLocalDataSource  the device storage data source
         * @return the [SnapsRepository] instance
         */
        fun getInstance(
            snapsLocalDataSource: SnapsDataSource
        ): SnapsRepository {
            if (INSTANCE == null) {
                synchronized(SnapsRepository::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = SnapsRepository(snapsLocalDataSource)
                    }
                }
            }
            return INSTANCE!!
        }

        /**
         * Used to force [.getInstance] to create a new instance
         * next time it's called.
         */
        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
