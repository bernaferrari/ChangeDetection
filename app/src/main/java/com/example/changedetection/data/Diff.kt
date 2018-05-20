package com.example.changedetection.data

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.ForeignKey.CASCADE
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "diffs",
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
    val fileId: String,
    val value: String,
    val timestamp: Long,
    val siteId: String
) {
    /**
     * Use this constructor to create a new active Site.
     *
     * @param title       title of the site
     * @param url url of the site
     */
    @Ignore
    constructor(value: String, timestamp: Long, siteId: String) : this(
        UUID.randomUUID().toString(),
        value,
        timestamp,
        siteId
    )
}