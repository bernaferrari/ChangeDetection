/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo

import androidx.room3.*
import kotlinx.serialization.Serializable

/**
 * Immutable model class for a Site.
 * Inspired from Architecture Components MVVM sample app
 */
@Serializable
@Entity(
    tableName = "sites",
    indices = [(Index(value = ["siteId"], unique = true))],
)
@ColumnTypeConverters(
    Site.ConstraintsConverter::class,
    Site.PairConverter::class,
)
data class Site(
    val title: String?,
    val url: String,
    val timestamp: Long,
    @PrimaryKey
    @ColumnInfo(name = "siteId")
    val id: String,
    val isSuccessful: Boolean,
    val isBrowser: Boolean,
    val threshold: Int,
    val isRead: Boolean, // reserved for future use
    val isSyncEnabled: Boolean, // enable/disable sync
    val isNotificationEnabled: Boolean,
    val notes: String,
    val colors: ColorGroup,
    val constraints: ConstraintsRequired, // reserved for future use
) {

    @Ignore
    constructor(
        title: String?,
        url: String,
        timestamp: Long,
        tags: String,
        id: String,
        colors: Pair<Int, Int>,
        threshold: Int = 0,
    ) : this(
        title,
        url,
        timestamp,
        id,
        true,
        false,
        threshold,
        false,
        true,
        true,
        tags,
        colors,
        ConstraintsRequired(false, false, false, false),
    )

    @Ignore
    constructor(
        title: String?,
        url: String,
        timestamp: Long,
        id: String,
        colors: ColorGroup,
        isSyncEnabled: Boolean,
        isNotificationEnabled: Boolean,
        threshold: Int = 0,
    ) : this(
        title,
        url,
        timestamp,
        id,
        true,
        false,
        threshold,
        false,
        isSyncEnabled,
        isNotificationEnabled,
        "",
        colors,
        ConstraintsRequired(false, false, false, false),
    )

    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    @Ignore
    constructor(
        title: String?,
        url: String,
        timestamp: Long,
        tags: String,
        colors: ColorGroup,
        isBrowser: Boolean,
        threshold: Int = 0,
    ) : this(
        title,
        url,
        timestamp,
        kotlin.uuid.Uuid.random().toString(),
        true,
        isBrowser,
        threshold,
        false,
        true,
        true,
        tags,
        colors,
        ConstraintsRequired(false, false, false, false),
    )

    override fun toString(): String = "Site: $title | url: $url | tags: $notes | timestamp: $timestamp"

    internal class ConstraintsConverter {
        @ColumnTypeConverter
        fun storedStringToConstraintsRequired(value: String): ConstraintsRequired {
            val langs = value.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toList()

            return ConstraintsRequired(langs)
        }

        @ColumnTypeConverter
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
        @ColumnTypeConverter
        fun storedStringToPair(value: String): Pair<Int, Int> {
            val langs = value.split(",".toRegex())
            return Pair(langs.getOrNull(0)?.toInt() ?: 0, langs.getOrNull(1)?.toInt() ?: 0)
        }

        @ColumnTypeConverter
        fun pairToStoredString(pair: Pair<Int, Int>): String = "${pair.first},${pair.second}"
    }
}
