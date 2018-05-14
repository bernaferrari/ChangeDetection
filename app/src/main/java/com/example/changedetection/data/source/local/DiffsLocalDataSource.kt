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
import com.example.changedetection.data.Diff

import com.example.changedetection.data.source.DiffsDataSource
import com.example.changedetection.util.AppExecutors
import com.orhanobut.logger.Logger


/**
 * Concrete implementation of a data source as a db.
 */
class DiffsLocalDataSource// Prevent direct instantiation.
private constructor(
    private val mAppExecutors: AppExecutors,
    private val mDiffsDao: DiffsDao
) : DiffsDataSource {

    /**
     * Note: [LoadTasksCallback.onDataNotAvailable] is fired if the database doesn't exist
     * or the table is empty.
     */
    override fun getDiffs(taskId: String, callback: DiffsDataSource.LoadDiffsCallback) {
        val runnable = Runnable {
            val diffs = mDiffsDao.getDiffsForUser(taskId)
            mAppExecutors.mainThread().execute {
                if (diffs == null) {
                    // This will be called if the table is new or just empty.
                    callback.onDataNotAvailable()
                } else {
                    Logger.d("Returning $diffs - ${diffs.task}")
                    callback.onDiffsLoaded(diffs.diffs.sortedByDescending { it.timestamp })
                }
            }
        }

        mAppExecutors.diskIO().execute(runnable)
        }

    override fun getDiff(diffId: String, callback: DiffsDataSource.GetDiffCallback) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun refreshDiffs() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteAllDiffs() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteDiff(diffId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun saveDiff(diff: Diff, callback: DiffsDataSource.GetDiffCallback) {
        val saveRunnable = Runnable {
            val wasSuccessful = //try {
                if (mDiffsDao.getLastDiff(diff.owner)?.firstOrNull()?.value != diff.value) {
                    mDiffsDao.insertDiff(diff)
                    true
                } else {
                    Logger.d("Beep beep! No difference detected!")
                    false
                }
//            } catch (e: Exception){
//                false
//            }

            mAppExecutors.mainThread().execute {
                if (wasSuccessful){
                    callback.onDiffLoaded(diff)
                } else {
                    callback.onDataNotAvailable()
                }
            }
        }

        mAppExecutors.diskIO().execute(saveRunnable)
    }

//
//    /**
//     * Note: [GetTaskCallback.onDataNotAvailable] is fired if the [Task] isn't
//     * found.
//     */
//    override fun getTask(taskId: String, callback: TasksDataSource.GetTaskCallback) {
//        val runnable = Runnable {
//            val task = mDiffsDao.getTaskById(taskId)
//
//            mAppExecutors.mainThread().execute {
//                if (task != null) {
//                    callback.onTaskLoaded(task)
//                } else {
//                    callback.onDataNotAvailable()
//                }
//            }
//        }
//
//        mAppExecutors.diskIO().execute(runnable)
//    }
//
//    override fun saveTask(task: Task) {
//        checkNotNull(task)
//        val saveRunnable = Runnable { mDiffsDao.insertTask(task) }
//        mAppExecutors.diskIO().execute(saveRunnable)
//    }
//
//    override fun completeTask(task: Task) {
//        val completeRunnable = Runnable { mDiffsDao.updateCompleted(task.id, true) }
//
//        mAppExecutors.diskIO().execute(completeRunnable)
//    }
//
//    override fun completeTask(taskId: String) {
//        // Not required for the local data source because the {@link TasksRepository} handles
//        // converting from a {@code taskId} to a {@link task} using its cached data.
//    }
//
//    override fun activateTask(task: Task) {
//        val activateRunnable = Runnable { mDiffsDao.updateCompleted(task.id, false) }
//        mAppExecutors.diskIO().execute(activateRunnable)
//    }
//
//    override fun activateTask(taskId: String) {
//        // Not required for the local data source because the {@link TasksRepository} handles
//        // converting from a {@code taskId} to a {@link task} using its cached data.
//    }
//
//    override fun clearCompletedTasks() {
//        val clearTasksRunnable = Runnable { mDiffsDao.deleteCompletedTasks() }
//
//        mAppExecutors.diskIO().execute(clearTasksRunnable)
//    }
//
//    override fun refreshTasks() {
//        // Not required because the {@link TasksRepository} handles the logic of refreshing the
//        // tasks from all the available data sources.
//    }
//
//    override fun deleteAllTasks() {
//        val deleteRunnable = Runnable { mDiffsDao.deleteTasks() }
//
//        mAppExecutors.diskIO().execute(deleteRunnable)
//    }
//
//    override fun deleteTask(taskId: String) {
//        val deleteRunnable = Runnable { mDiffsDao.deleteTaskById(taskId) }
//
//        mAppExecutors.diskIO().execute(deleteRunnable)
//    }

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
