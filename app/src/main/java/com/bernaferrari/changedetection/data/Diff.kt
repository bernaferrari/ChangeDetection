package com.bernaferrari.changedetection.data

import android.arch.persistence.room.*
import android.arch.persistence.room.ForeignKey.CASCADE
import java.util.*

@Entity(
    tableName = "diffs",
    indices = [(Index(value = ["siteId", "diffId"], unique = true))],
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
data class Diff(
    @PrimaryKey
    val diffId: String,
    val siteId: String,
    val timestamp: Long,
    val size: Int,
    val value: String
) {
    /**maxLineHeight
     * Use this constructor to create a new isActive Site.
     *
     * @param title       title of the site
     * @param url url of the site
     */
    @Ignore
    constructor(timestamp: Long, size: Int, siteId: String, value: String) : this(
        UUID.randomUUID().toString(),
        siteId,
        timestamp,
        size,
        value
    )
}

// Same as Diff, but without Value.
data class MinimalDiff(
    val diffId: String,
    val siteId: String,
    val timestamp: Long,
    val size: Int
)