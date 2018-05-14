package com.example.changedetection.data

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey

import android.arch.persistence.room.ForeignKey.CASCADE
import android.arch.persistence.room.Ignore
import java.util.*

@Entity(
    tableName = "diffs",
    foreignKeys = [(
                ForeignKey(
                    entity = Task::class,
                    parentColumns = arrayOf("entryid"),
                    childColumns = arrayOf("owner"),
                    onDelete = CASCADE
                )
                )
    ]
)
data class Diff(
    @PrimaryKey
    val valueId: String,
    val value: String,
    val timestamp: Long,
    val owner: String
) {
    /**
     * Use this constructor to create a new active Task.
     *
     * @param title       title of the task
     * @param url url of the task
     */
    @Ignore
    constructor(value: String, timestamp: Long, owner: String) : this(
        UUID.randomUUID().toString(),
        value,
        timestamp,
        owner
    )
}