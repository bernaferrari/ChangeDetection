package com.bernaferrari.changedetection.data

import android.arch.persistence.room.*
import java.util.*

/**
 * Immutable model class for a Site.
 * Inspired from Architecture Components MVVM sample app
 */
@Entity(
    tableName = "sites",
    indices = [(Index(value = ["siteId"], unique = true))]
)
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val site = other as Site?

        return id == site?.id && title == site.title && url == site.url
    }

    override fun hashCode(): Int {
        return id.hashCode() + (title?.hashCode() ?: 0) + (url.hashCode())
    }

    override fun toString(): String {
        return "Site with title " + title!!
    }
}
