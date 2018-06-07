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
 * @param contentType the mime type as retrieved from the header on the request
 * @param contentSize [content] size in bytes
 * @param content the retrieved information from website, can be heavy if webpage is heavy
 */
data class Snap(
    @PrimaryKey
    val snapId: String,
    val siteId: String,
    val timestamp: Long,
    val contentType: String,
    val contentSize: Int,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val content: ByteArray
) {
    @Ignore
    constructor(
        siteId: String,
        timestamp: Long,
        contentType: String,
        contentSize: Int,
        content: ByteArray
    ) : this(
        UUID.randomUUID().toString(),
        siteId,
        timestamp,
        contentType,
        contentSize,
        content
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Snap

        if (snapId != other.snapId) return false
        if (siteId != other.siteId) return false
        if (timestamp != other.timestamp) return false
        if (contentType != other.contentType) return false
        if (contentSize != other.contentSize) return false
        if (!Arrays.equals(content, other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = snapId.hashCode()
        result = 31 * result + siteId.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + contentSize
        result = 31 * result + Arrays.hashCode(content)
        return result
    }
}