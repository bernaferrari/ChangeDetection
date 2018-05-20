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
 * Main entry point for accessing sites data.
 */
interface TasksDataSource {

    interface LoadTasksCallback {

        fun onTasksLoaded(sites: List<Site>)

        fun onDataNotAvailable()
    }

    interface GetTaskCallback {

        fun onTaskLoaded(site: Site)

        fun onDataNotAvailable()
    }

    fun getTaskAndDiffs(callback: (MutableList<SiteAndLastDiff>) -> (Unit))

    fun getTasks(callback: LoadTasksCallback)

    fun getTask(taskId: String, callback: GetTaskCallback)

    fun saveTask(site: Site)

    fun completeTask(site: Site)

    fun completeTask(taskId: String)

    fun activateTask(site: Site)

    fun activateTask(taskId: String)

    fun clearCompletedTasks()

    fun refreshTasks()

    fun deleteAllTasks()

    fun deleteSite(taskId: String)
}
