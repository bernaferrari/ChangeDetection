package com.bernaferrari.changedetection.data

import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import java.util.*

@Entity(
    tableName = "snaps",
    indices = [(Index(value = ["siteId", "snapId"], unique = true))],
    foreignKeys = [(
            ForeignKey(
                entity = Site::class,
                parentColumns = arrayOf("siteId"),
                childColumns = arrayOf("siteId"),
                onDelete = CASCADE
            )
            )
    ]
)
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
data class Snap(
    @PrimaryKey
    val snapId: String,
    val siteId: String,
    val timestamp: Long,
    val contentType: String,
    val contentCharset: String,
    val contentSize: Int
) {
    @Ignore
    constructor(
        siteId: String,
        timestamp: Long,
        contentType: String,
        contentCharset: String,
        contentSize: Int
    ) : this(
        UUID.randomUUID().toString(),
        siteId,
        timestamp,
        contentType,
        contentCharset,
        contentSize
    )
}
