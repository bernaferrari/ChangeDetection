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

import com.example.changedetection.data.Site
import com.example.changedetection.data.source.local.SiteAndLastDiff

/**
 * Concrete implementation to load sites from the data sources into a cache.
 *
 *
 * For simplicity, this implements a dumb synchronisation between locally persisted data and data
 * obtained from the server, by using the remote data source only if the local database doesn't
 * exist or is empty.
 *
 * //TODO: Implement this class using LiveData.
 */
class TasksRepository // Prevent direct instantiation.
private constructor(
    tasksLocalDataSource: TasksDataSource
) : TasksDataSource {

    private val mTasksLocalDataSource: TasksDataSource = checkNotNull(tasksLocalDataSource)

    override fun getTaskAndDiffs(callback: (MutableList<SiteAndLastDiff>) -> Unit) {
        mTasksLocalDataSource.getTaskAndDiffs {
            callback.invoke(it)
        }
    }

    /**
     * Gets sites from cache, local data source (SQLite) or remote data source, whichever is
     * available first.
     *
     *
     * Note: [LoadTasksCallback.onDataNotAvailable] is fired if all data sources fail to
     * get the data.
     */
    override fun getTasks(callback: TasksDataSource.LoadTasksCallback) {
        checkNotNull(callback)

        // Query the local storage if available. If not, query the network.
        mTasksLocalDataSource.getTasks(object : TasksDataSource.LoadTasksCallback {
            override fun onTasksLoaded(sites: List<Site>) {
                callback.onTasksLoaded(sites)
            }

            override fun onDataNotAvailable() {
                callback.onDataNotAvailable()
            }
        })
    }

    override fun saveTask(site: Site) {
        checkNotNull(site)
        mTasksLocalDataSource.saveTask(site)
    }

    /**
     * Gets sites from local data source (sqlite) unless the table is new or empty. In that case it
     * uses the network data source. This is done to simplify the sample.
     *
     *
     * Note: [GetTaskCallback.onDataNotAvailable] is fired if both data sources fail to
     * get the data.
     */
    override fun getTask(taskId: String, callback: TasksDataSource.GetTaskCallback) {
        // Load from server/persisted if needed.

        // Is the site in the local data source? If not, query the network.
        mTasksLocalDataSource.getTask(taskId, object : TasksDataSource.GetTaskCallback {
            override fun onTaskLoaded(site: Site) {
                callback.onTaskLoaded(site)
            }

            override fun onDataNotAvailable() {
                callback.onDataNotAvailable()
            }
        })
    }

    override fun deleteAllTasks() {
        mTasksLocalDataSource.deleteAllTasks()
    }

    override fun deleteSite(taskId: String) {
        mTasksLocalDataSource.deleteSite(checkNotNull(taskId))
    }

    private fun refreshLocalDataSource(sites: List<Site>) {
        mTasksLocalDataSource.deleteAllTasks()
        for (task in sites) {
            mTasksLocalDataSource.saveTask(task)
        }
    }

    companion object {

        @Volatile
        private var INSTANCE: TasksRepository? = null

        /**
         * Returns the single instance of this class, creating it if necessary.
         *
         * @param tasksRemoteDataSource the backend data source
         * @param tasksLocalDataSource  the device storage data source
         * @return the [TasksRepository] instance
         */
        fun getInstance(
            tasksLocalDataSource: TasksDataSource
        ): TasksRepository {
            if (INSTANCE == null) {
                synchronized(TasksRepository::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = TasksRepository(tasksLocalDataSource)
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
