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

package com.example.changedetection.data.source.remote

import android.os.Handler
import com.example.changedetection.data.Diff

import com.example.changedetection.data.Task
import com.example.changedetection.data.source.TasksDataSource
import com.example.changedetection.data.source.local.SiteAndLastDiff
import java.util.LinkedHashMap

/**
 * Implementation of the data source that adds a latency simulating network.
 */
class TasksRemoteDataSource// Prevent direct instantiation.
private constructor() : TasksDataSource {
    override fun getTaskAndDiffs(callback: (MutableList<SiteAndLastDiff>) -> Unit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    /**
     * Note: [LoadTasksCallback.onDataNotAvailable] is never fired. In a real remote data
     * source implementation, this would be fired if the server can't be contacted or the server
     * returns an error.
     */
    override fun getTasks(callback: TasksDataSource.LoadTasksCallback) {
        // Simulate network by delaying the execution.
        val handler = Handler()
        handler.postDelayed(
            { callback.onTasksLoaded(TASKS_SERVICE_DATA.values.toMutableList()) },
            SERVICE_LATENCY_IN_MILLIS.toLong()
        )
    }

    /**
     * Note: [GetTaskCallback.onDataNotAvailable] is never fired. In a real remote data
     * source implementation, this would be fired if the server can't be contacted or the server
     * returns an error.
     */
    override fun getTask(taskId: String, callback: TasksDataSource.GetTaskCallback) {
        val task = TASKS_SERVICE_DATA[taskId]!!

        // Simulate network by delaying the execution.
        val handler = Handler()
        handler.postDelayed({ callback.onTaskLoaded(task) }, SERVICE_LATENCY_IN_MILLIS.toLong())
    }

    override fun saveTask(task: Task) {
        TASKS_SERVICE_DATA[task.id] = task
    }

    override fun completeTask(task: Task) {
        val completedTask = Task(task.title, task.url, task.timestamp, task.id, true)
        TASKS_SERVICE_DATA[task.id] = completedTask
    }

    override fun completeTask(taskId: String) {
        // Not required for the remote data source because the {@link TasksRepository} handles
        // converting from a {@code taskId} to a {@link task} using its cached data.
    }

    override fun activateTask(task: Task) {
        val activeTask = Task(task.title, task.url, task.timestamp, task.id)
        TASKS_SERVICE_DATA[task.id] = activeTask
    }

    override fun activateTask(taskId: String) {
        // Not required for the remote data source because the {@link TasksRepository} handles
        // converting from a {@code taskId} to a {@link task} using its cached data.
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

    override fun deleteAllTasks() {
        TASKS_SERVICE_DATA.clear()
    }

    override fun deleteTask(taskId: String) {
        TASKS_SERVICE_DATA.remove(taskId)
    }

    companion object {

        private var INSTANCE: TasksRemoteDataSource? = null

        private val SERVICE_LATENCY_IN_MILLIS = 2000

        private val TASKS_SERVICE_DATA: MutableMap<String, Task>

        init {
            TASKS_SERVICE_DATA = LinkedHashMap(2)
            addTask("Build tower in Pisa", "Ground looks good, no foundation work required.", 0,"0")
            addTask("Finish bridge in Tacoma", "Found awesome girders at half the cost!", 1,"1")
            addTask("Finish bridge in Tacoma", "Found awesome girders at half the cost!", 0,"2")
            addTask("Finish bridge in Tacoma", "Found awesome girders at half the cost!", 0,"3")
            addTask("Finish bridge in Tacoma", "Found awesome girders at half the cost!", 0,"4")
            addTask("Finish bridge in Tacoma", "Found awesome girders at half the cost!", 0,"5")
            addTask("Finish bridge in Tacoma", "Found awesome girders at half the cost!", 0,"6")
            addTask("Finish bridge in Tacoma", "Found awesome girders at half the cost!", 0,"7")
            addTask("Finish bridge in Tacoma", "Found awesome girders at half the cost!", 0,"8")
            addTask("Finish bridge in Tacoma", "Found awesome girders at half the cost!", 0,"12")
            addTask("Finish bridge in Tacoma", "Found awesome girders at half the cost!", 0,"13")
            addTask("Finish bridge in Tacoma", "Found awesome girders at half the cost!", 0,"14")
            addTask("Finish bridge in Tacoma", "Found awesome girders at half the cost!", 0,"15")
            addTask("Finish bridge in Tacoma", "Found awesome girders at half the cost!", 0,"16")
            addTask("Finish bridge in Tacoma", "Found awesome girders at half the cost!", 0,"17")
            addTask("Finish bridge in Tacoma", "Found awesome girders at half the cost!", 0,"18")
        }

        val instance: TasksRemoteDataSource
            get() {
                if (INSTANCE == null) {
                    INSTANCE = TasksRemoteDataSource()
                }
                return INSTANCE!!
            }

        private fun addTask(title: String, description: String, timestamp: Long, id: String) {
            val newTask = Task(title, description, timestamp, id)
            TASKS_SERVICE_DATA[newTask.id] = newTask
        }
    }
}
