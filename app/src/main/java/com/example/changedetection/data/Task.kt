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

package com.example.changedetection.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey

import java.util.UUID

/**
 * Immutable model class for a Task.
 */
@Entity(tableName = "tasks")
data class Task
/**
 * Use this constructor to specify a completed Task if the Task already has an id (copy of
 * another Task).
 *
 * @param title       title of the task
 * @param url url of the task
 * @param id          id of the task
 * @param completed   true if the task is completed, false if it's active
 */
    (
    @field:ColumnInfo(name = "title")
    val title: String?,
    @field:ColumnInfo(name = "url")
    val url: String,
    @field:ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @field:PrimaryKey
    @field:ColumnInfo(name = "entryid")
    val id: String,
    @field:ColumnInfo(name = "completed")
    val isCompleted: Boolean
) {

    val titleForList: String?
        get() = if (!title.isNullOrEmpty()) {
            title
        } else {
            url
        }

    val isActive: Boolean
        get() = !isCompleted

    /**
     * Use this constructor to create a new active Task.
     *
     * @param title       title of the task
     * @param url url of the task
     */
    @Ignore
    constructor(title: String?, url: String, timestamp: Long) : this(
        title,
        url,
        timestamp,
        UUID.randomUUID().toString(),
        false
    )

    /**
     * Use this constructor to create an active Task if the Task already has an id (copy of another
     * Task).
     *
     * @param title       title of the task
     * @param url url of the task
     * @param id          id of the task
     */
    @Ignore
    constructor(title: String?, url: String, timestamp: Long, id: String) : this(
        title,
        url,
        timestamp,
        id,
        false
    )

    /**
     * Use this constructor to create a new completed Task.
     *
     * @param title       title of the task
     * @param description url of the task
     * @param completed   true if the task is completed, false if it's active
     */
    @Ignore
    constructor(title: String?, url: String, timestamp: Long, completed: Boolean) : this(
        title,
        url,
        timestamp,
        UUID.randomUUID().toString(),
        completed
    )

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val task = o as Task?

        return id == task?.id && title == task.title && url == task.url
    }

    override fun hashCode(): Int {
        return id.hashCode() + (title?.hashCode() ?: 0) + (url?.hashCode() ?: 0)
    }

    override fun toString(): String {
        return "Task with title " + title!!
    }
}
