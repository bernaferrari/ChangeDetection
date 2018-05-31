package com.bernaferrari.changedetection.data

import android.arch.persistence.room.*
import java.util.*

@Entity(
    tableName = "diffs",
    indices = [(Index(value = ["siteId", "diffId"], unique = true))],
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

    // DiffWithoutValue was a temporary solution.
    // Migrating it to this constructor might be a better idea.
    @Ignore
    constructor(timestamp: Long, size: Int, siteId: String) : this(
        UUID.randomUUID().toString(),
        siteId,
        timestamp,
        size,
        ""
    )
}

// Same as Diff, but without Value.
// Temporary solution.
data class DiffWithoutValue(
    val diffId: String,
    val siteId: String,
    val timestamp: Long,
    val size: Int
)