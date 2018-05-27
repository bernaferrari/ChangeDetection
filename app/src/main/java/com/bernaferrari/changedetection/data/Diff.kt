package com.bernaferrari.changedetection.data

import android.arch.persistence.room.*
import java.util.*

@Entity(
    tableName = "diffs",
    indices = [(Index(value = ["diffId"], unique = true))],
    foreignKeys = [(
            ForeignKey(
                entity = Site::class,
                parentColumns = arrayOf("siteId"),
                childColumns = arrayOf("siteId")
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
    /**
     * Use this constructor to create a new active Site.
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
// Value is HEAVY, so cursor adapter sometimes gets OOM error.
data class DiffWithoutValue(
    val diffId: String,
    val siteId: String,
    val timestamp: Long,
    val size: Int
)