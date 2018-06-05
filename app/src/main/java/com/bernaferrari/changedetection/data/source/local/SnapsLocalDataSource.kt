package com.bernaferrari.changedetection.data.source.local

import android.arch.paging.DataSource
import android.support.annotation.VisibleForTesting
import com.bernaferrari.changedetection.data.MinimalSnap
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.data.source.SnapsDataSource
import com.bernaferrari.changedetection.extensions.cleanUpHtml
import com.bernaferrari.changedetection.util.AppExecutors
import com.orhanobut.logger.Logger

/**
 * Concrete implementation of a data source as a db.
 * Inspired from Architecture Components MVVM sample app
 */
class SnapsLocalDataSource// Prevent direct instantiation.
private constructor(
    private val mAppExecutors: AppExecutors,
    private val mSnapsDao: SnapsDao
) : SnapsDataSource {

    /**
     * get the most recent diffs without value.
     *
     * @param siteId the site id for filtering the diffs.
     */
    override fun getMostRecentMinimalSnaps(
        siteId: String,
        callback: SnapsDataSource.GetMinimalSnapCallback
    ) {
        val runnable = Runnable {
            val original = mSnapsDao.getLastSnapsSize(siteId)

            mAppExecutors.mainThread().execute {
                callback.onLoaded(original)
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    /**
     * get diffs for Paging Adapter.
     *
     * @param siteId the site id for filtering the diffs.
     */
    override fun getSnapForPaging(siteId: String): DataSource.Factory<Int, MinimalSnap> {
        return mSnapsDao.getAllSnapsForSiteIdForPaging(siteId)
    }

    override fun getSnapPair(
        originalId: String,
        newId: String,
        callback: SnapsDataSource.GetPairCallback
    ) {
        val runnable = Runnable {
            val original = mSnapsDao.getSnapById(originalId)
            val new = mSnapsDao.getSnapById(newId)

            mAppExecutors.mainThread().execute {
                if (original != null && new != null) {
                    callback.onSnapsLoaded(Pair(original, new))
                } else {
                    callback.onDataNotAvailable()
                }
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    override fun getSnap(snapId: String, callback: SnapsDataSource.GetSnapsCallback) {
        val runnable = Runnable {
            val diff = mSnapsDao.getSnapById(snapId)

            mAppExecutors.mainThread().execute {
                if (diff != null) {
                    callback.onSnapsLoaded(diff)
                } else {
                    callback.onDataNotAvailable()
                }
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    override fun saveSnap(snap: Snap, callback: SnapsDataSource.GetSnapsCallback) {
        val saveRunnable = Runnable {
            val getDiffByid = mSnapsDao.getLastSnapForSiteId(snap.siteId)

            // Uncomment for testing.
            // mSnapsDao.insertSnap(minimalSnap.copy(value = minimalSnap.value.plus(UUID.randomUUID().toString())))
            val wasSuccessful =
                if (snap.value.isNotBlank() && getDiffByid?.value?.cleanUpHtml() != snap.value.cleanUpHtml()) {
                    Logger.d("Difference detected! Size went from ${getDiffByid?.value?.count()} to ${snap.value.count()}")
                    mSnapsDao.insertSnap(snap)
                    true
                } else {
                    Logger.d("Beep beep! No difference detected!")
                    false
                }

            mAppExecutors.mainThread().execute {
                if (wasSuccessful) {
                    callback.onSnapsLoaded(snap)
                } else {
                    callback.onDataNotAvailable()
                }
            }
        }

        mAppExecutors.diskIO().execute(saveRunnable)
    }

    override fun deleteSnap(snapId: String) {
        val deleteRunnable = Runnable { mSnapsDao.deleteSnapById(snapId) }
        mAppExecutors.diskIO().execute(deleteRunnable)
    }

    override fun deleteAllSnapsForSite(siteId: String) {
        val runnable = Runnable {
            mSnapsDao.deleteAllSnapsForSite(siteId)
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    companion object {
        @Volatile
        private var INSTANCE: SnapsLocalDataSource? = null

        fun getInstance(
            appExecutors: AppExecutors,
            snapsDao: SnapsDao
        ): SnapsLocalDataSource {
            if (INSTANCE == null) {
                synchronized(SnapsLocalDataSource::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = SnapsLocalDataSource(appExecutors, snapsDao)
                    }
                }
            }
            return INSTANCE!!
        }

        @VisibleForTesting
        internal fun clearInstance() {
            INSTANCE = null
        }
    }
}
