package com.bernaferrari.changedetection.data.source.local

import android.arch.paging.DataSource
import android.support.annotation.VisibleForTesting
import com.bernaferrari.changedetection.data.Diff
import com.bernaferrari.changedetection.data.MinimalDiff
import com.bernaferrari.changedetection.data.source.DiffsDataSource
import com.bernaferrari.changedetection.extensions.cleanUpHtml
import com.bernaferrari.changedetection.util.AppExecutors
import com.orhanobut.logger.Logger

/**
 * Concrete implementation of a data source as a db.
 * Inspired from Architecture Components MVVM sample app
 */
class DiffsLocalDataSource// Prevent direct instantiation.
private constructor(
    private val mAppExecutors: AppExecutors,
    private val mDiffsDao: DiffsDao
) : DiffsDataSource {

    override fun getMostRecentMinimalDiffs(
        siteId: String,
        callback: DiffsDataSource.GetMinimalDiffCallback
    ) {
        val runnable = Runnable {
            val original = mDiffsDao.getLastDiffsSize(siteId)

            mAppExecutors.mainThread().execute {
                callback.onMinimalDiffLoaded(original)
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    override fun getDiffForPaging(id: String): DataSource.Factory<Int, MinimalDiff> {
        return mDiffsDao.allDiffsBySiteId(id)
    }

    override fun getDiffPair(
        originalId: String,
        newId: String,
        callback: DiffsDataSource.GetPairCallback
    ) {
        val runnable = Runnable {
            val original = mDiffsDao.getDiffById(originalId)
            val new = mDiffsDao.getDiffById(newId)

            mAppExecutors.mainThread().execute {
                if (original != null && new != null) {
                    callback.onDiffLoaded(Pair(original, new))
                } else {
                    callback.onDataNotAvailable()
                }
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    override fun getDiff(diffId: String, callback: DiffsDataSource.GetDiffCallback) {
        val runnable = Runnable {
            val diff = mDiffsDao.getDiffById(diffId)

            mAppExecutors.mainThread().execute {
                if (diff != null) {
                    callback.onDiffLoaded(diff)
                } else {
                    callback.onDataNotAvailable()
                }
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    override fun saveDiff(diff: Diff, callback: DiffsDataSource.GetDiffCallback) {
        val saveRunnable = Runnable {
            val getDiffByid = mDiffsDao.getDiffBySiteId(diff.siteId)

            // Uncomment for testing.
            // mDiffsDao.insertDiff(minimalDiff.copy(value = minimalDiff.value.plus(UUID.randomUUID().toString())))
            val wasSuccessful =
                if (diff.value.isNotBlank() && getDiffByid?.value?.cleanUpHtml() != diff.value.cleanUpHtml()) {
                    Logger.d("Difference detected! Size went from ${getDiffByid?.value?.count()} to ${diff.value.count()}")
                    mDiffsDao.insertDiff(diff)
                    true
                } else {
                    Logger.d("Beep beep! No difference detected!")
                    false
                }

            mAppExecutors.mainThread().execute {
                if (wasSuccessful) {
                    callback.onDiffLoaded(diff)
                } else {
                    callback.onDataNotAvailable()
                }
            }
        }

        mAppExecutors.diskIO().execute(saveRunnable)
    }

    override fun deleteDiff(diffId: String) {
        val deleteRunnable = Runnable { mDiffsDao.deleteDiffById(diffId) }
        mAppExecutors.diskIO().execute(deleteRunnable)
    }

    override fun deleteAllDiffsForSite(siteId: String) {
        val runnable = Runnable {
            mDiffsDao.deleteAllDiffsForSite(siteId)
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    companion object {
        @Volatile
        private var INSTANCE: DiffsLocalDataSource? = null

        fun getInstance(
            appExecutors: AppExecutors,
            diffsDao: DiffsDao
        ): DiffsLocalDataSource {
            if (INSTANCE == null) {
                synchronized(DiffsLocalDataSource::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = DiffsLocalDataSource(appExecutors, diffsDao)
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
