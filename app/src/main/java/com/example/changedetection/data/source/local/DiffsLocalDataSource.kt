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
import com.example.changedetection.cleanUpHtml
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

    override fun deleteAllDiffsForSite(siteId: String) {
        val runnable = Runnable {
            mDiffsDao.deleteAllDiffsForSite(siteId)
        }

        mAppExecutors.diskIO().execute(runnable)
    }

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
                    Logger.d("ReturningTask ${diffs.site}")
                    Logger.d("ReturningCount $diffs - ${diffs.diffs.size}")
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

    override fun deleteAllDiffsForSite() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteDiff(diffId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveDiff(diff: Diff, callback: DiffsDataSource.GetDiffCallback) {
        val saveRunnable = Runnable {
            val getDiffByid = mDiffsDao.getDiffBySiteId(diff.siteId)

            val wasSuccessful =
                if (getDiffByid?.value?.cleanUpHtml() != diff.value.cleanUpHtml()) {
                    Logger.d("Difference detected! Id went from ${getDiffByid?.fileId} to ${diff.fileId}")
                    Logger.d("Difference detected! Size went from ${getDiffByid?.value?.count()} to ${diff.value.count()}")
                    mDiffsDao.insertDiff(diff.copy())

                    Logger.d("getAllDiffs: ${mDiffsDao.getAllDiffs().count()}")

                    mDiffsDao.getAllDiffs().forEach {
                        Logger.d("fileId: ${it.fileId} - siteId: ${it.siteId}")
                    }
                    true
                    // We don't want to show a change when there is only one diff. It won't be a change when user just puts the website.
//                    getDiffByid != null
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

//
//    /**
//     * Note: [GetTaskCallback.onDataNotAvailable] is fired if the [Site] isn't
//     * found.
//     */
//    override fun getSite(taskId: String, callback: TasksDataSource.GetTaskCallback) {
//        val runnable = Runnable {
//            val site = mDiffsDao.getTaskById(taskId)
//
//            mAppExecutors.mainThread().execute {
//                if (site != null) {
//                    callback.onTaskLoaded(site)
//                } else {
//                    callback.onDataNotAvailable()
//                }
//            }
//        }
//
//        mAppExecutors.diskIO().execute(runnable)
//    }
//
//    override fun saveTask(site: Site) {
//        checkNotNull(site)
//        val saveRunnable = Runnable { mDiffsDao.insertTask(site) }
//        mAppExecutors.diskIO().execute(saveRunnable)
//    }
//
//    override fun completeTask(site: Site) {
//        val completeRunnable = Runnable { mDiffsDao.updateCompleted(site.id, true) }
//
//        mAppExecutors.diskIO().execute(completeRunnable)
//    }
//
//    override fun completeTask(taskId: String) {
//        // Not required for the local data source because the {@link TasksRepository} handles
//        // converting from a {@code taskId} to a {@link site} using its cached data.
//    }
//
//    override fun activateTask(site: Site) {
//        val activateRunnable = Runnable { mDiffsDao.updateCompleted(site.id, false) }
//        mAppExecutors.diskIO().execute(activateRunnable)
//    }
//
//    override fun activateTask(taskId: String) {
//        // Not required for the local data source because the {@link TasksRepository} handles
//        // converting from a {@code taskId} to a {@link site} using its cached data.
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
//        // sites from all the available data sources.
//    }
//
//    override fun deleteAllTasks() {
//        val deleteRunnable = Runnable { mDiffsDao.deleteTasks() }
//
//        mAppExecutors.diskIO().execute(deleteRunnable)
//    }
//
//    override fun deleteSite(taskId: String) {
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
