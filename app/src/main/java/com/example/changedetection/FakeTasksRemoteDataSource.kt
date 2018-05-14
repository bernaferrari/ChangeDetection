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
import com.example.changedetection.data.Diff
import com.example.changedetection.data.Task
import com.example.changedetection.data.source.TasksDataSource
import com.example.changedetection.data.source.local.SiteAndLastDiff
import java.util.LinkedHashMap

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

    override fun saveTask(task: Task) {
        TASKS_SERVICE_DATA[task.id] = task
    }

    override fun completeTask(task: Task) {
        val completedTask = Task(task.title, task.url, task.timestamp, task.id, true)
        TASKS_SERVICE_DATA[task.id] = completedTask
    }

    override fun completeTask(taskId: String) {
        // Not required for the remote data source.
    }

    override fun activateTask(task: Task) {
        val activeTask = Task(task.title, task.url, task.timestamp, task.id)
        TASKS_SERVICE_DATA[task.id] = activeTask
    }

    override fun activateTask(taskId: String) {
        // Not required for the remote data source.
    }

    override fun clearCompletedTasks() {
        val it = TASKS_SERVICE_DATA.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            if (entry.value.isCompleted) {
                it.remove()
            }
        }
    }

    override fun refreshTasks() {
        // Not required because the {@link TasksRepository} handles the logic of refreshing the
        // tasks from all the available data sources.
    }

    override fun deleteTask(taskId: String) {
        TASKS_SERVICE_DATA.remove(taskId)
    }

    override fun deleteAllTasks() {
        TASKS_SERVICE_DATA.clear()
    }

    @VisibleForTesting
    fun addTasks(vararg tasks: Task) {
        if (tasks != null) {
            for (task in tasks) {
                TASKS_SERVICE_DATA[task.id] = task
            }
        }
    }

    companion object {

        private var INSTANCE: FakeTasksRemoteDataSource? = null

        private val TASKS_SERVICE_DATA = LinkedHashMap<String, Task>()

        val instance: FakeTasksRemoteDataSource
            get() {
                if (INSTANCE == null) {
                    INSTANCE = FakeTasksRemoteDataSource()
                }
                return INSTANCE!!
            }
    }
}
