package com.bernaferrari.changedetection.data.source

import android.arch.paging.DataSource
import com.bernaferrari.changedetection.data.Diff
import com.bernaferrari.changedetection.data.MinimalDiff

/**
 *
 * For simplicity, this implements a dumb synchronisation between locally persisted data and data
 * obtained from the server, by using the remote data source only if the local database doesn't
 * exist or is empty.
 *
 * Inspired from Architecture Components MVVM sample app
 */
class DiffsRepository // Prevent direct instantiation.
private constructor(
    diffsLocalDataSource: DiffsDataSource
) : DiffsDataSource {

    override fun getMostRecentMinimalDiffs(
        siteId: String,
        callback: DiffsDataSource.GetMinimalDiffCallback
    ) {
        mDiffsLocalDataSource.getMostRecentMinimalDiffs(
            siteId,
            object : DiffsDataSource.GetMinimalDiffCallback {
                override fun onMinimalDiffLoaded(minimalDiffList: List<Int>?) {
                    callback.onMinimalDiffLoaded(minimalDiffList)
                }
            }
        )
    }

    override fun getDiffPair(
        originalId: String,
        newId: String,
        callback: DiffsDataSource.GetPairCallback
    ) {
        mDiffsLocalDataSource.getDiffPair(
            originalId,
            newId,
            callback = object : DiffsDataSource.GetPairCallback {
                override fun onDiffLoaded(pair: Pair<Diff, Diff>) {
                    callback.onDiffLoaded(pair)
                }

                override fun onDataNotAvailable() {
                    callback.onDataNotAvailable()
                }
            }
        )
    }

    override fun getDiffForPaging(id: String): DataSource.Factory<Int, MinimalDiff> {
        return mDiffsLocalDataSource.getDiffForPaging(id)
    }

    override fun deleteAllDiffsForSite(siteId: String) {
        mDiffsLocalDataSource.deleteAllDiffsForSite(siteId)
    }

    override fun getDiff(diffId: String, callback: DiffsDataSource.GetDiffCallback) {
        checkNotNull(diffId)
        checkNotNull(callback)

        // Is the site in the local data source? If not, query the network.
        mDiffsLocalDataSource.getDiff(diffId, object : DiffsDataSource.GetDiffCallback {
            override fun onDiffLoaded(diff: Diff) {
                callback.onDiffLoaded(diff)
            }

            override fun onDataNotAvailable() {
                callback.onDataNotAvailable()
            }
        })
    }

    override fun deleteDiff(diffId: String) {
        mDiffsLocalDataSource.deleteDiff(diffId)
    }

    private val mDiffsLocalDataSource: DiffsDataSource = checkNotNull(diffsLocalDataSource)

    override fun saveDiff(diff: Diff, callback: DiffsDataSource.GetDiffCallback) {
        checkNotNull(diff)
        mDiffsLocalDataSource.saveDiff(diff, object : DiffsDataSource.GetDiffCallback {
            override fun onDiffLoaded(diff: Diff) {
                callback.onDiffLoaded(diff)
            }

            override fun onDataNotAvailable() {
                callback.onDataNotAvailable()
            }
        })
    }

    companion object {

        @Volatile
        private var INSTANCE: DiffsRepository? = null

        /**
         * Returns the single instance of this class, creating it if necessary.
         *
         * @param tasksRemoteDataSource the backend data source
         * @param tasksLocalDataSource  the device storage data source
         * @return the [DiffsRepository] instance
         */
        fun getInstance(
            diffsLocalDataSource: DiffsDataSource
        ): DiffsRepository {
            if (INSTANCE == null) {
                synchronized(DiffsRepository::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = DiffsRepository(diffsLocalDataSource)
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
