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
import com.example.changedetection.data.source.local.SiteAndLastDiff

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
class TasksRepository // Prevent direct instantiation.
private constructor(
    tasksRemoteDataSource: TasksDataSource,
    tasksLocalDataSource: TasksDataSource
) : TasksDataSource {

    private val mTasksRemoteDataSource: TasksDataSource = checkNotNull(tasksRemoteDataSource)
    private val mTasksLocalDataSource: TasksDataSource = checkNotNull(tasksLocalDataSource)

    /**
     * This variable has package local visibility so it can be accessed from tests.
     */
    internal var mCachedTasks: MutableMap<String, Task>? = null

    /**
     * Marks the cache as invalid, to force an update the next time data is requested. This variable
     * has package local visibility so it can be accessed from tests.
     */
    private var mCacheIsDirty = false

    override fun getTaskAndDiffs(callback: (MutableList<SiteAndLastDiff>) -> Unit) {
        mTasksLocalDataSource.getTaskAndDiffs {
            callback.invoke(it)
        }
    }

    /**
     * Gets tasks from cache, local data source (SQLite) or remote data source, whichever is
     * available first.
     *
     *
     * Note: [LoadTasksCallback.onDataNotAvailable] is fired if all data sources fail to
     * get the data.
     */
    override fun getTasks(callback: TasksDataSource.LoadTasksCallback) {
        checkNotNull(callback)

        // Respond immediately with cache if available and not dirty
        if (mCachedTasks != null && !mCacheIsDirty) {
            callback.onTasksLoaded(ArrayList(mCachedTasks!!.values))
            return
        }

//        EspressoIdlingResource.increment() // App is busy until further notice

        if (mCacheIsDirty) {
            // If the cache is dirty we need to fetch new data from the network.
            getTasksFromRemoteDataSource(callback)
        } else {
            // Query the local storage if available. If not, query the network.
            mTasksLocalDataSource.getTasks(object : TasksDataSource.LoadTasksCallback {
                override fun onTasksLoaded(tasks: List<Task>) {
                    refreshCache(tasks)

//                    EspressoIdlingResource.decrement() // Set app as idle.
                    callback.onTasksLoaded(ArrayList(mCachedTasks!!.values))
                }

                override fun onDataNotAvailable() {
                    getTasksFromRemoteDataSource(callback)
                }
            })
        }
    }

    override fun saveTask(task: Task) {
        checkNotNull(task)
        mTasksRemoteDataSource.saveTask(task)
        mTasksLocalDataSource.saveTask(task)

        // Do in memory cache update to keep the app UI up to date
        if (mCachedTasks == null) {
            mCachedTasks = LinkedHashMap()
        }
        mCachedTasks!![task.id] = task
    }

    override fun completeTask(task: Task) {
        checkNotNull(task)
        mTasksRemoteDataSource.completeTask(task)
        mTasksLocalDataSource.completeTask(task)

        val completedTask = Task(task.title, task.url, task.timestamp, task.id, true)

        // Do in memory cache update to keep the app UI up to date
        if (mCachedTasks == null) {
            mCachedTasks = LinkedHashMap()
        }
        mCachedTasks!![task.id] = completedTask
    }

    override fun completeTask(taskId: String) {
        checkNotNull(taskId)
        completeTask(getTaskWithId(taskId)!!)
    }

    override fun activateTask(task: Task) {
        checkNotNull(task)
        mTasksRemoteDataSource.activateTask(task)
        mTasksLocalDataSource.activateTask(task)

        val activeTask = Task(task.title, task.url, task.timestamp, task.id)

        // Do in memory cache update to keep the app UI up to date
        if (mCachedTasks == null) {
            mCachedTasks = LinkedHashMap()
        }
        mCachedTasks!![task.id] = activeTask
    }

    override fun activateTask(taskId: String) {
        checkNotNull(taskId)
        activateTask(getTaskWithId(taskId)!!)
    }

    override fun clearCompletedTasks() {
        mTasksRemoteDataSource.clearCompletedTasks()
        mTasksLocalDataSource.clearCompletedTasks()

        // Do in memory cache update to keep the app UI up to date
        if (mCachedTasks == null) {
            mCachedTasks = LinkedHashMap()
        }
        val it = mCachedTasks!!.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            if (entry.value.isCompleted) {
                it.remove()
            }
        }
    }

    /**
     * Gets tasks from local data source (sqlite) unless the table is new or empty. In that case it
     * uses the network data source. This is done to simplify the sample.
     *
     *
     * Note: [GetTaskCallback.onDataNotAvailable] is fired if both data sources fail to
     * get the data.
     */
    override fun getTask(taskId: String, callback: TasksDataSource.GetTaskCallback) {
        checkNotNull(taskId)
        checkNotNull(callback)

        val cachedTask = getTaskWithId(taskId)

        // Respond immediately with cache if available
        if (cachedTask != null) {
            callback.onTaskLoaded(cachedTask)
            return
        }

//        EspressoIdlingResource.increment() // App is busy until further notice

        // Load from server/persisted if needed.

        // Is the task in the local data source? If not, query the network.
        mTasksLocalDataSource.getTask(taskId, object : TasksDataSource.GetTaskCallback {
            override fun onTaskLoaded(task: Task) {
                // Do in memory cache update to keep the app UI up to date
                if (mCachedTasks == null) {
                    mCachedTasks = LinkedHashMap()
                }
                mCachedTasks!![task.id] = task

//                EspressoIdlingResource.decrement() // Set app as idle.

                callback.onTaskLoaded(task)
            }

            override fun onDataNotAvailable() {
                mTasksRemoteDataSource.getTask(taskId, object : TasksDataSource.GetTaskCallback {
                    override fun onTaskLoaded(task: Task) {
                        if (task == null) {
                            onDataNotAvailable()
                            return
                        }
                        // Do in memory cache update to keep the app UI up to date
                        if (mCachedTasks == null) {
                            mCachedTasks = LinkedHashMap()
                        }
                        mCachedTasks!![task.id] = task
//                        EspressoIdlingResource.decrement() // Set app as idle.

                        callback.onTaskLoaded(task)
                    }

                    override fun onDataNotAvailable() {
//                        EspressoIdlingResource.decrement() // Set app as idle.

                        callback.onDataNotAvailable()
                    }
                })
            }
        })
    }

    override fun refreshTasks() {
        mCacheIsDirty = true
    }

    override fun deleteAllTasks() {
        mTasksRemoteDataSource.deleteAllTasks()
        mTasksLocalDataSource.deleteAllTasks()

        if (mCachedTasks == null) {
            mCachedTasks = LinkedHashMap()
        }
        mCachedTasks!!.clear()
    }

    override fun deleteTask(taskId: String) {
        mTasksRemoteDataSource.deleteTask(checkNotNull(taskId))
        mTasksLocalDataSource.deleteTask(checkNotNull(taskId))

        mCachedTasks!!.remove(taskId)
    }

    private fun getTasksFromRemoteDataSource(callback: TasksDataSource.LoadTasksCallback) {
        mTasksRemoteDataSource.getTasks(object : TasksDataSource.LoadTasksCallback {
            override fun onTasksLoaded(tasks: List<Task>) {
                refreshCache(tasks)
                refreshLocalDataSource(tasks)

//                EspressoIdlingResource.decrement() // Set app as idle.
                callback.onTasksLoaded(ArrayList(mCachedTasks!!.values))
            }

            override fun onDataNotAvailable() {

//                EspressoIdlingResource.decrement() // Set app as idle.
                callback.onDataNotAvailable()
            }
        })
    }

    private fun refreshCache(tasks: List<Task>) {
        if (mCachedTasks == null) {
            mCachedTasks = LinkedHashMap()
        }
        mCachedTasks!!.clear()
        for (task in tasks) {
            mCachedTasks!![task.id] = task
        }
        mCacheIsDirty = false
    }

    private fun refreshLocalDataSource(tasks: List<Task>) {
        mTasksLocalDataSource.deleteAllTasks()
        for (task in tasks) {
            mTasksLocalDataSource.saveTask(task)
        }
    }

    private fun getTaskWithId(id: String): Task? {
        checkNotNull(id)
        return if (mCachedTasks == null || mCachedTasks!!.isEmpty()) {
            null
        } else {
            mCachedTasks!![id]
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
            tasksRemoteDataSource: TasksDataSource,
            tasksLocalDataSource: TasksDataSource
        ): TasksRepository {
            if (INSTANCE == null) {
                synchronized(TasksRepository::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = TasksRepository(tasksRemoteDataSource, tasksLocalDataSource)
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
