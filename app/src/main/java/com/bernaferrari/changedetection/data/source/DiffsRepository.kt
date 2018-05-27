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

package com.bernaferrari.changedetection.data.source

import android.arch.paging.DataSource
import com.bernaferrari.changedetection.data.Diff
import com.bernaferrari.changedetection.data.DiffWithoutValue

/**
 *
 *
 * For simplicity, this implements a dumb synchronisation between locally persisted data and data
 * obtained from the server, by using the remote data source only if the local database doesn't
 * exist or is empty.
 *
 */
class DiffsRepository // Prevent direct instantiation.
private constructor(
    diffsLocalDataSource: DiffsDataSource
) : DiffsDataSource {

    override fun getDiffStorage(
        originalId: String,
        newId: String,
        callback: DiffsDataSource.GetPairCallback
    ) {
        mDiffsLocalDataSource.getDiffStorage(
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

    override fun getCheese(id: String): DataSource.Factory<Int, DiffWithoutValue> {
        return mDiffsLocalDataSource.getCheese(id)
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
