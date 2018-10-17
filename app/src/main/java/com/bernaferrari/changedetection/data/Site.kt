package com.bernaferrari.changedetection.data

import androidx.room.*
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
@TypeConverters(Site.ConstraintsConverter::class, Site.PairConverter::class)
data class Site(
    val title: String?,
    val url: String,
    val timestamp: Long,
    @field:PrimaryKey
    @field:ColumnInfo(name = "siteId")
    val id: String,
    val isSuccessful: Boolean,
    val isRead: Boolean, // reserved for future use
    val isSyncEnabled: Boolean, // enable/disable sync
    val isNotificationEnabled: Boolean,
    val notes: String,
    val colors: Pair<Int, Int>,
    val constraints: WorkerHelper.ConstraintsRequired // reserved for future use
) {

    @Ignore
    constructor(
        title: String?,
        url: String,
        timestamp: Long,
        id: String,
        colors: Pair<Int, Int>
    ) : this(
        title,
        url,
        timestamp,
        id,
        true,
        false,
        true,
        true,
        "",
        colors,
        WorkerHelper.ConstraintsRequired(false, false, false, false)
    )

    @Ignore
    constructor(title: String?, url: String, timestamp: Long, colors: Pair<Int, Int>) : this(
        title,
        url,
        timestamp,
        UUID.randomUUID().toString(),
        true,
        false,
        true,
        true,
        "",
        colors,
        WorkerHelper.ConstraintsRequired(false, false, false, false)
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val site = other as? Site

        return id == site?.id && title == site.title && url == site.url
    }

    override fun hashCode(): Int {
        return id.hashCode() + (title?.hashCode() ?: 0) + (url.hashCode())
    }

    override fun toString(): String {
        return "Site with title $title"
    }

    internal class ConstraintsConverter {
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

    internal class PairConverter {
        @TypeConverter
        fun storedStringToPair(value: String): Pair<Int, Int> {
            val langs = value.split(",".toRegex())
            return Pair(langs.getOrNull(0)?.toInt() ?: 0, langs.getOrNull(1)?.toInt() ?: 0)
        }

        @TypeConverter
        fun pairToStoredString(pair: Pair<Int, Int>): String {
            return "${pair.first},${pair.second}"
        }
    }
}
