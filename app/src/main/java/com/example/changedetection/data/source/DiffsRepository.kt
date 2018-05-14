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

package com.example.changedetection.data.source

import com.example.changedetection.data.Diff
import com.example.changedetection.data.Task

import java.util.ArrayList
import java.util.LinkedHashMap

/**
 * Concrete implementation to load tasks from the data sources into a cache.
 *
 *
 * For simplicity, this implements a dumb synchronisation between locally persisted data and data
 * obtained from the server, by using the remote data source only if the local database doesn't
 * exist or is empty.
 *
 * //TODO: Implement this class using LiveData.
 */
class DiffsRepository // Prevent direct instantiation.
private constructor(
    tasksLocalDataSource: DiffsDataSource
) : DiffsDataSource {

    override fun getDiffs(taskId: String, callback: DiffsDataSource.LoadDiffsCallback) {
        checkNotNull(callback)

        // Respond immediately with cache if available and not dirty
        if (mCachedTasks != null && !mCacheIsDirty) {
            callback.onDiffsLoaded(ArrayList(mCachedTasks!!.values))
            return
        }

        // Query the local storage if available. If not, query the network.
        mTasksLocalDataSource.getDiffs(taskId, object : DiffsDataSource.LoadDiffsCallback {
            override fun onDiffsLoaded(diffs: List<Diff>) {
                refreshCache(diffs)
                callback.onDiffsLoaded(ArrayList(mCachedTasks!!.values))
            }

            override fun onDataNotAvailable() {
                callback.onDataNotAvailable()
            }
        })
    }

    override fun getDiff(diffId: String, callback: DiffsDataSource.GetDiffCallback) {
        checkNotNull(diffId)
        checkNotNull(callback)

        val cachedTask = getDiffWithId(diffId)

        // Respond immediately with cache if available
        if (cachedTask != null) {
            callback.onDiffLoaded(cachedTask)
            return
        }

        // Is the task in the local data source? If not, query the network.
        mTasksLocalDataSource.getDiff(diffId, object : DiffsDataSource.GetDiffCallback {

            override fun onDiffLoaded(diff: Diff) {
                // Do in memory cache update to keep the app UI up to date
                if (mCachedTasks == null) {
                    mCachedTasks = LinkedHashMap()
                }
                mCachedTasks!![diff.valueId] = diff

                callback.onDiffLoaded(diff)
            }

            override fun onDataNotAvailable() {
                callback.onDataNotAvailable()
            }
        })
    }

    override fun refreshDiffs() {
        mCacheIsDirty = true
    }

    override fun deleteAllDiffs() {
        mTasksLocalDataSource.deleteAllDiffs()

        if (mCachedTasks == null) {
            mCachedTasks = LinkedHashMap()
        }
        mCachedTasks!!.clear()
    }

    override fun deleteDiff(diffId: String) {
        mTasksLocalDataSource.deleteDiff(checkNotNull(diffId))
        mCachedTasks!!.remove(diffId)
    }

    private val mTasksLocalDataSource: DiffsDataSource = checkNotNull(tasksLocalDataSource)

    /**
     * This variable has package local visibility so it can be accessed from tests.
     */
    internal var mCachedTasks: MutableMap<String, Diff>? = null

    /**
     * Marks the cache as invalid, to force an update the next time data is requested. This variable
     * has package local visibility so it can be accessed from tests.
     */
    private var mCacheIsDirty = false

    override fun saveDiff(diff: Diff, callback: DiffsDataSource.GetDiffCallback) {
        checkNotNull(diff)
        mTasksLocalDataSource.saveDiff(diff, object : DiffsDataSource.GetDiffCallback {
            override fun onDiffLoaded(diff: Diff) {
                callback.onDiffLoaded(diff)
            }

            override fun onDataNotAvailable() {
                callback.onDataNotAvailable()
            }
        })

        // Do in memory cache update to keep the app UI up to date
        if (mCachedTasks == null) {
            mCachedTasks = LinkedHashMap()
        }
        mCachedTasks!![diff.valueId] = diff
    }

    private fun refreshCache(diffs: List<Diff>) {
        if (mCachedTasks == null) {
            mCachedTasks = LinkedHashMap()
        }
        mCachedTasks!!.clear()
        for (diff in diffs) {
            mCachedTasks!![diff.valueId] = diff
        }
        mCacheIsDirty = false
    }

//    private fun refreshLocalDataSource(diffs: List<Diff>) {
//        mTasksLocalDataSource.deleteAllDiffs()
//        for (diff in diffs) {
//            mTasksLocalDataSource.saveDiff(diff)
//        }
//    }

    private fun getDiffWithId(id: String): Diff? {
        checkNotNull(id)
        return if (mCachedTasks == null || mCachedTasks!!.isEmpty()) {
            null
        } else {
            mCachedTasks!![id]
        }
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
