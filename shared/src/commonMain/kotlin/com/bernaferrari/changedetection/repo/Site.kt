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
@Entity(tableName = "sites")
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
    val isRead: Boolean, // false while a saved change still needs review
    val isSyncEnabled: Boolean, // enable/disable sync
    val isNotificationEnabled: Boolean,
    val notes: String,
    val colors: ColorGroup,
    val constraints: ConstraintsRequired, // reserved for future use
    @ColumnInfo(defaultValue = "'DEFAULT'")
    val captureMode: CaptureMode = CaptureMode.DEFAULT,
    /** Unknown for an unobserved page or a legacy failed check; never inferred from a failure. */
    @ColumnInfo(defaultValue = "NULL")
    val lastSuccessfulCheck: Long? = null,
    @ColumnInfo(defaultValue = "NULL")
    val lastFailureCode: String? = null,
    @ColumnInfo(defaultValue = "''")
    val textIncludeSelector: String = "",
    @ColumnInfo(defaultValue = "''")
    val textExcludeSelector: String = "",
    @ColumnInfo(defaultValue = "0")
    val visualChangeThresholdBps: Int = 0,
    /** The page's own icon (apple-touch-icon or favicon), found while capturing; blank until the first capture. */
    @ColumnInfo(defaultValue = "''")
    val iconUrl: String = "",
    @ColumnInfo(defaultValue = "'AUTOMATIC'")
    val iconShape: PageIconShape = PageIconShape.AUTOMATIC,
) {
    /** Zero disables filtering; one basis point is 0.01% of sampled pixels. */
    fun visualFilterBasisPoints(): Int = if (captureMode == CaptureMode.SCREENSHOT || captureMode == CaptureMode.DEFAULT) {
        visualChangeThresholdBps.takeIf { it in 1..10_000 } ?: 0
    } else {
        0
    }

    fun textRegion() = if (captureMode == CaptureMode.TEXT) {
        com.bernaferrari.changedetection.detection.TextRegion(textIncludeSelector, textExcludeSelector)
    } else {
        com.bernaferrari.changedetection.detection.TextRegion()
    }

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
            // Stored in the writer's order (battery, wifi, charging, idle), as every release since 2018 wrote it.
            val parts = value.split(",").map { it.trim().toBoolean() }
            return ConstraintsRequired(
                wifi = parts.getOrElse(1) { false },
                charging = parts.getOrElse(2) { false },
                batteryNotLow = parts.getOrElse(0) { false },
                deviceIdle = parts.getOrElse(3) { false },
            )
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
