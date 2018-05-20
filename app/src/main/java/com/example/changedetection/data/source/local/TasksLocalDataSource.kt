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

package com.example.changedetection.data.source.local

import android.support.annotation.VisibleForTesting
import com.example.changedetection.data.Site
import com.example.changedetection.data.source.TasksDataSource
import com.example.changedetection.util.AppExecutors
import com.orhanobut.logger.Logger


/**
 * Concrete implementation of a data source as a db.
 */
class TasksLocalDataSource// Prevent direct instantiation.
private constructor(
    private val mAppExecutors: AppExecutors,
    private val mTasksDao: TasksDao
) : TasksDataSource {

    override fun getTaskAndDiffs(callback: (MutableList<SiteAndLastDiff>) -> Unit) {
        val runnable = Runnable {
            val tasks = mTasksDao.sites
            val list = mutableListOf<SiteAndLastDiff>()
            tasks.mapTo(list) { task ->
//                if (mTasksDao.getDiffsCountForUser(task.id) > 1) {
                    SiteAndLastDiff(task, mTasksDao.getDiffById(task.id))
//                } else {
//                    SiteAndLastDiff(task, null)
//                }
            }

            mAppExecutors.mainThread().execute {
                callback.invoke(list)
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    /**
     * Note: [LoadTasksCallback.onDataNotAvailable] is fired if the database doesn't exist
     * or the table is empty.
     */
    override fun getTasks(callback: TasksDataSource.LoadTasksCallback) {
        val runnable = Runnable {
            val tasks = mTasksDao.sites
            mAppExecutors.mainThread().execute {
                if (tasks.isEmpty()) {
                    // This will be called if the table is new or just empty.
                    callback.onDataNotAvailable()
                } else {
                    callback.onTasksLoaded(tasks)
                }
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    /**
     * Note: [GetTaskCallback.onDataNotAvailable] is fired if the [Site] isn't
     * found.
     */
    override fun getTask(taskId: String, callback: TasksDataSource.GetTaskCallback) {
        val runnable = Runnable {
            val task = mTasksDao.getTaskById(taskId)

            mAppExecutors.mainThread().execute {
                if (task != null) {
                    callback.onTaskLoaded(task)
                } else {
                    callback.onDataNotAvailable()
                }
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    override fun saveTask(site: Site) {
        checkNotNull(site)
        val saveRunnable = Runnable { mTasksDao.insertTask(site) }
        mAppExecutors.diskIO().execute(saveRunnable)
    }

    override fun completeTask(site: Site) {
        val completeRunnable = Runnable { mTasksDao.updateCompleted(site.id, true) }

        mAppExecutors.diskIO().execute(completeRunnable)
    }

    override fun completeTask(taskId: String) {
        // Not required for the local data source because the {@link TasksRepository} handles
        // converting from a {@code taskId} to a {@link site} using its cached data.
    }

    override fun activateTask(site: Site) {
        val activateRunnable = Runnable { mTasksDao.updateCompleted(site.id, false) }
        mAppExecutors.diskIO().execute(activateRunnable)
    }

    override fun activateTask(taskId: String) {
        // Not required for the local data source because the {@link TasksRepository} handles
        // converting from a {@code taskId} to a {@link site} using its cached data.
    }

    override fun clearCompletedTasks() {
        val clearTasksRunnable = Runnable { mTasksDao.deleteCompletedTasks() }

        mAppExecutors.diskIO().execute(clearTasksRunnable)
    }

    override fun refreshTasks() {
        // Not required because the {@link TasksRepository} handles the logic of refreshing the
        // sites from all the available data sources.
    }

    override fun deleteAllTasks() {
        val deleteRunnable = Runnable { mTasksDao.deleteTasks() }

        mAppExecutors.diskIO().execute(deleteRunnable)
    }

    override fun deleteSite(taskId: String) {
        val deleteRunnable = Runnable { mTasksDao.deleteTaskById(taskId) }

        mAppExecutors.diskIO().execute(deleteRunnable)
    }

    companion object {

        @Volatile
        private var INSTANCE: TasksLocalDataSource? = null

        fun getInstance(
            appExecutors: AppExecutors,
            tasksDao: TasksDao
        ): TasksLocalDataSource {
            if (INSTANCE == null) {
                synchronized(TasksLocalDataSource::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = TasksLocalDataSource(appExecutors, tasksDao)
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
