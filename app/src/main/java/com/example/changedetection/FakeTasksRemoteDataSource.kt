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

package com.example.changedetection;

import android.support.annotation.VisibleForTesting
import com.example.changedetection.data.Site
import com.example.changedetection.data.source.TasksDataSource
import com.example.changedetection.data.source.local.SiteAndLastDiff
import java.util.*

/**
 * Implementation of a remote data source with static access to the data for easy testing.
 */
class FakeTasksRemoteDataSource// Prevent direct instantiation.
private constructor() : TasksDataSource {
    override fun getTaskAndDiffs(callback: (MutableList<SiteAndLastDiff>) -> Unit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTasks(callback: TasksDataSource.LoadTasksCallback) {
        callback.onTasksLoaded(TASKS_SERVICE_DATA.values.toMutableList())
    }

    override fun getTask(taskId: String, callback: TasksDataSource.GetTaskCallback) {
        val task = TASKS_SERVICE_DATA[taskId]!!
        callback.onTaskLoaded(task)
    }

    override fun saveTask(site: Site) {
        TASKS_SERVICE_DATA[site.id] = site
    }

    override fun completeTask(site: Site) {
        val completedTask = Site(site.title, site.url, site.timestamp, site.id, true, false)
        TASKS_SERVICE_DATA[site.id] = completedTask
    }

    override fun completeTask(taskId: String) {
        // Not required for the remote data source.
    }

    override fun activateTask(site: Site) {
        val activeTask = Site(site.title, site.url, site.timestamp, site.id)
        TASKS_SERVICE_DATA[site.id] = activeTask
    }

    override fun activateTask(taskId: String) {
        // Not required for the remote data source.
    }

    override fun clearCompletedTasks() {
        val it = TASKS_SERVICE_DATA.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            if (entry.value.successful) {
                it.remove()
            }
        }
    }

    override fun refreshTasks() {
        // Not required because the {@link TasksRepository} handles the logic of refreshing the
        // sites from all the available data sources.
    }

    override fun deleteSite(taskId: String) {
        TASKS_SERVICE_DATA.remove(taskId)
    }

    override fun deleteAllTasks() {
        TASKS_SERVICE_DATA.clear()
    }

    @VisibleForTesting
    fun addTasks(vararg sites: Site) {
        if (sites != null) {
            for (task in sites) {
                TASKS_SERVICE_DATA[task.id] = task
            }
        }
    }

    companion object {

        private var INSTANCE: FakeTasksRemoteDataSource? = null

        private val TASKS_SERVICE_DATA = LinkedHashMap<String, Site>()

        val instance: FakeTasksRemoteDataSource
            get() {
                if (INSTANCE == null) {
                    INSTANCE = FakeTasksRemoteDataSource()
                }
                return INSTANCE!!
            }
    }
}
