package com.bernaferrari.changedetection.data

import android.arch.persistence.room.*
import android.arch.persistence.room.ForeignKey.CASCADE
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
 * @param snapId the unique snap id
 * @param siteId the unique site id
 * @param timestamp recorded in milliseconds
 * @param size [value] size in bytes
 * @param value the retrieved information from website, can be heavy if webpage is heavy.
 */
data class Snap(
    @PrimaryKey
    val snapId: String,
    val siteId: String,
    val timestamp: Long,
    val size: Int,
    val value: String
) {
    @Ignore
    constructor(timestamp: Long, size: Int, siteId: String, value: String) : this(
        UUID.randomUUID().toString(),
        siteId,
        timestamp,
        size,
        value
    )
}