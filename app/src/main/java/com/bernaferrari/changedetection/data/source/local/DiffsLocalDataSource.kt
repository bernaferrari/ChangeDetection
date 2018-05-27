/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bernaferrari.changedetection.data.source.local

import android.arch.paging.DataSource
import android.support.annotation.VisibleForTesting
import com.bernaferrari.changedetection.data.Diff
import com.bernaferrari.changedetection.data.DiffWithoutValue
import com.bernaferrari.changedetection.data.source.DiffsDataSource
import com.bernaferrari.changedetection.extensions.cleanUpHtml
import com.bernaferrari.changedetection.util.AppExecutors
import com.orhanobut.logger.Logger

/**
 * Concrete implementation of a data source as a db.
 */
class DiffsLocalDataSource// Prevent direct instantiation.
private constructor(
    private val mAppExecutors: AppExecutors,
    private val mDiffsDao: DiffsDao
) : DiffsDataSource {

    override fun getCheese(id: String): DataSource.Factory<Int, DiffWithoutValue> {
        return mDiffsDao.allDiffsBySiteId(id)
    }

    override fun getDiffStorage(
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

    /**
     * Note: [LoadTasksCallback.onDataNotAvailable] is fired if the database doesn't exist
     * or the table is empty.
     */
    fun getDiffs(siteId: String, callback: DiffsDataSource.LoadDiffsCallback) {
        val runnable = Runnable {
            val diffs = mDiffsDao.getDiffsForSiteWithLimit(siteId)
            mAppExecutors.mainThread().execute {
                if (diffs == null) {
                    // This will be called if the table is new or just empty.
                    callback.onDataNotAvailable()
                } else {
                    Logger.d("ReturningCount $diffs - ${diffs.size}")
                    callback.onDiffsLoaded(diffs)
                }
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    override fun saveDiff(diff: Diff, callback: DiffsDataSource.GetDiffCallback) {
        val saveRunnable = Runnable {
            val getDiffByid = mDiffsDao.getDiffBySiteId(diff.siteId)

// Uncomment for testing.
// mDiffsDao.insertDiff(diff.copy(value = diff.value.plus(UUID.randomUUID().toString())))
//            val wasSuccessful = true
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
