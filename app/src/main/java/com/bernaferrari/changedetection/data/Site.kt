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

package com.bernaferrari.changedetection.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import java.util.*

/**
 * Immutable model class for a Site.
 */
@Entity(tableName = "sites")
data class Site
/**
 * Use this constructor to specify a completed Site if the Site already has an id (copy of
 * another Site).
 *
 * @param title       title of the site
 * @param url url of the site
 * @param id          id of the site
 * @param completed   true if the site is completed, false if it's active
 */
    (
    @field:ColumnInfo(name = "title")
    val title: String?,
    @field:ColumnInfo(name = "url")
    val url: String,
    @field:ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @field:PrimaryKey
    @field:ColumnInfo(name = "siteId")
    val id: String,
    @field:ColumnInfo(name = "successful")
    val successful: Boolean,
    @field:ColumnInfo(name = "read")
    val read: Boolean
) {

    val titleForList: String?
        get() = if (!title.isNullOrEmpty()) {
            title
        } else {
            url
        }

    val isActive: Boolean
        get() = !successful

    /**
     * Use this constructor to create a new active Site.
     *
     * @param title       title of the site
     * @param url url of the site
     */
    @Ignore
    constructor(title: String?, url: String, timestamp: Long) : this(
        title,
        url,
        timestamp,
        UUID.randomUUID().toString(),
        true,
        false
    )

    @Ignore
    constructor(title: String?, url: String, timestamp: Long, id: String) : this(
        title,
        url,
        timestamp,
        id,
        true,
        false
    )

    @Ignore
    constructor(title: String?, url: String, timestamp: Long, completed: Boolean) : this(
        title,
        url,
        timestamp,
        UUID.randomUUID().toString(),
        completed,
        false
    )

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val site = o as Site?

        return id == site?.id && title == site.title && url == site.url
    }

    override fun hashCode(): Int {
        return id.hashCode() + (title?.hashCode() ?: 0) + (url.hashCode())
    }

    override fun toString(): String {
        return "Site with title " + title!!
    }
}
