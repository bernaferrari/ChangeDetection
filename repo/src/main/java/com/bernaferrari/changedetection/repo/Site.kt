package com.bernaferrari.changedetection.repo

import android.os.Parcelable
import androidx.room.*
import kotlinx.android.parcel.Parcelize
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
@Parcelize
data class Site(
    val title: String?,
    val url: String,
    val timestamp: Long,
    @field:PrimaryKey
    @field:ColumnInfo(name = "siteId")
    val id: String,
    val isSuccessful: Boolean,
    val isBrowser: Boolean,
    val isRead: Boolean, // reserved for future use
    val isSyncEnabled: Boolean, // enable/disable sync
    val isNotificationEnabled: Boolean,
    val notes: String,
    val colors: ColorGroup,
    val constraints: ConstraintsRequired // reserved for future use
) : Parcelable {

    @Ignore
    constructor(
        title: String?,
        url: String,
        timestamp: Long,
        tags: String,
        id: String,
        colors: Pair<Int, Int>
    ) : this(
        title,
        url,
        timestamp,
        id,
        true,
        false,
        false,
        true,
        true,
        tags,
        colors,
        ConstraintsRequired(false, false, false, false)
    )

    @Ignore
    constructor(
        title: String?,
        url: String,
        timestamp: Long,
        id: String,
        colors: ColorGroup,
        isSyncEnabled: Boolean,
        isNotificationEnabled: Boolean
    ) : this(
        title,
        url,
        timestamp,
        id,
        true,
        false,
        false,
        isSyncEnabled,
        isNotificationEnabled,
        "",
        colors,
        ConstraintsRequired(false, false, false, false)
    )

    @Ignore
    constructor(
        title: String?,
        url: String,
        timestamp: Long,
        tags: String,
        colors: ColorGroup,
        isBrowser: Boolean
    ) : this(
        title,
        url,
        timestamp,
        UUID.randomUUID().toString(),
        true,
        isBrowser,
        false,
        true,
        true,
        tags,
        colors,
        ConstraintsRequired(false, false, false, false)
    )

    override fun toString(): String {
        return "Site: $title | url: $url | tags: $notes | timestamp: $timestamp"
    }

    internal class ConstraintsConverter {
        @TypeConverter
        fun storedStringToConstraintsRequired(value: String): ConstraintsRequired {
            val langs = value.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toList()

            return ConstraintsRequired(langs)
        }

        @TypeConverter
        fun constraintsRequiredToStoredString(cl: ConstraintsRequired): String {
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
