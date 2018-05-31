package com.bernaferrari.changedetection.data

import android.arch.persistence.room.*
import com.bernaferrari.changedetection.WorkerHelper
import java.util.*

/**
 * Immutable model class for a Site.
 * Inspired from Architecture Components MVVM sample app
 */
@Entity(
    tableName = "sites",
    indices = [(Index(value = ["siteId"], unique = true))]
)
@TypeConverters(Site.LanguageConverter::class)
data class Site
/**
 * Use this constructor to specify a completed Site if the Site already has an id (copy of
 * another Site).
 *
 * @param title       title of the site
 * @param url         url of the site
 * @param id          id of the site
 * @param completed   true if the site is completed, false if it's isActive
 */
    (
    val title: String?,
    val url: String,
    val timestamp: Long,
    @field:PrimaryKey
    @field:ColumnInfo(name = "siteId")
    val id: String,
    val isSuccessful: Boolean,
    val isRead: Boolean, // reserved for future use
    val isActive: Boolean,
    val isNotificationOn: Boolean,
    val notes: String,
    val constraints: WorkerHelper.ConstraintsRequired // reserved for future use
) {
    /**
     * Use this constructor to create a new isActive Site.
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
        false,
        true,
        true,
        "",
        WorkerHelper.ConstraintsRequired(false, false, false, false)
    )

    @Ignore
    constructor(title: String?, url: String, timestamp: Long, id: String) : this(
        title,
        url,
        timestamp,
        id,
        true,
        false,
        true,
        true,
        "",
        WorkerHelper.ConstraintsRequired(false, false, false, false)
    )

    @Ignore
    constructor(title: String?, url: String, timestamp: Long, completed: Boolean) : this(
        title,
        url,
        timestamp,
        UUID.randomUUID().toString(),
        completed,
        false,
        true,
        true,
        "",
        WorkerHelper.ConstraintsRequired(false, false, false, false)
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

    class LanguageConverter {
        @TypeConverter
        fun storedStringToConstraintsRequired(value: String): WorkerHelper.ConstraintsRequired {
            val langs = value.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toList()

            return WorkerHelper.ConstraintsRequired(langs)
        }

        @TypeConverter
        fun constraintsRequiredToStoredString(cl: WorkerHelper.ConstraintsRequired): String {
            var value = ""
            value += "${cl.batteryNotLow}, "
            value += "${cl.wifi}, "
            value += "${cl.charging}, "
            value += "${cl.deviceIdle}"

            return value
        }
    }
}
