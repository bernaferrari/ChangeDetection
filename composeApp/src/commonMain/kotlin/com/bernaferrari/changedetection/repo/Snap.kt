/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo

import androidx.room3.*
import androidx.room3.ForeignKey.Companion.CASCADE
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * Snap is short for Snapshot. A snapshot will be a downloaded website which is different from
 * the one before it.
 *
 * @param snapId the unique snap url
 * @param siteId the unique site url
 * @param timestamp recorded in milliseconds
 * @param contentType the mime type as retrieved from the header on the request
 * @param contentSize content size in bytes
 * @param contentCharset content charset
 */
@Serializable
@Entity(
    tableName = "snaps",
    // Every history query is "this page, newest first", so the main index matches that order exactly.
    indices = [
        (Index(value = ["siteId", "timestamp", "snapId"], name = "index_snaps_siteId_timestamp_snapId")),
        (Index(value = ["siteId", "contentType", "timestamp"], name = "index_snaps_composite")),
        (Index(value = ["siteId", "contentHash"], name = "index_snaps_siteId_contentHash")),
        (Index(value = ["timestamp"], name = "index_snaps_timestamp")),
    ],
    foreignKeys = [
        (
            ForeignKey(
                entity = Site::class,
                parentColumns = arrayOf("siteId"),
                childColumns = arrayOf("siteId"),
                onDelete = CASCADE,
            )
            ),
    ],
)
data class Snap(
    @PrimaryKey
    val snapId: String,
    val siteId: String,
    val timestamp: Long,
    val contentType: String,
    val contentCharset: String,
    val contentSize: Int,
    val contentHash: String = "", // SHA-256 hash for fast comparison
    val processedContentHash: String = "", // Hash of processed/cleaned content
) {
    @OptIn(ExperimentalUuidApi::class)
    @Ignore
    constructor(
        siteId: String,
        timestamp: Long,
        contentType: String,
        contentCharset: String,
        contentSize: Int,
        contentHash: String = "",
        processedContentHash: String = "",
    ) : this(
        Uuid.random().toString(),
        siteId,
        timestamp,
        contentType,
        contentCharset,
        contentSize,
        contentHash,
        processedContentHash,
    )
}
