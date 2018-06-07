package com.bernaferrari.changedetection.data.source

import android.arch.lifecycle.LiveData
import android.arch.paging.DataSource
import com.bernaferrari.changedetection.data.MinimalSnap
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

    override fun getMinimalSnaps(
        siteId: String
    ): LiveData<List<MinimalSnap>> = mSnapsLocalDataSource.getMinimalSnaps(siteId)

    override fun getMostRecentMinimalSnaps(siteId: String, callback: (List<Int>) -> Unit) {
        mSnapsLocalDataSource.getMostRecentMinimalSnaps(
            siteId
        ) {
            callback.invoke(it)
        }
    }

    override fun getSnapPair(
        originalId: String,
        newId: String,
        callback: SnapsDataSource.GetPairCallback
    ) {
        mSnapsLocalDataSource.getSnapPair(
            originalId,
            newId,
            callback = object : SnapsDataSource.GetPairCallback {
                override fun onSnapsLoaded(pair: Pair<Snap, Snap>) {
                    callback.onSnapsLoaded(pair)
                }

                override fun onDataNotAvailable() {
                    callback.onDataNotAvailable()
                }
            }
        )
    }

    override fun getHeavySnapForPaging(siteId: String): DataSource.Factory<Int, Snap> {
        return mSnapsLocalDataSource.getHeavySnapForPaging(siteId)
    }

    override fun getSnapForPaging(siteId: String): DataSource.Factory<Int, MinimalSnap> {
        return mSnapsLocalDataSource.getSnapForPaging(siteId)
    }

    override fun deleteAllSnapsForSite(siteId: String) {
        mSnapsLocalDataSource.deleteAllSnapsForSite(siteId)
    }

    override fun getSnap(snapId: String, callback: SnapsDataSource.GetSnapsCallback) {
        checkNotNull(snapId)
        checkNotNull(callback)

        // Is the site in the local data source? If not, query the network.
        mSnapsLocalDataSource.getSnap(snapId, object : SnapsDataSource.GetSnapsCallback {
            override fun onSnapsLoaded(snap: Snap) {
                callback.onSnapsLoaded(snap)
            }

            override fun onDataNotAvailable() {
                callback.onDataNotAvailable()
            }
        })
    }

    override fun deleteSnap(snapId: String) {
        mSnapsLocalDataSource.deleteSnap(snapId)
    }

    private val mSnapsLocalDataSource: SnapsDataSource = checkNotNull(snapsLocalDataSource)

    override fun saveSnap(snap: Snap, callback: SnapsDataSource.GetSnapsCallback) {
        checkNotNull(snap)
        mSnapsLocalDataSource.saveSnap(snap, object : SnapsDataSource.GetSnapsCallback {
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
