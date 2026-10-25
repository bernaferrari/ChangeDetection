package com.bernaferrari.changedetection.repo.source.local

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap

/** Delivery intent commits with history. No monitored URL, body or credentials are duplicated here. */
@Entity(
    tableName = "pendingChangeAlerts",
    foreignKeys = [
        ForeignKey(entity = Site::class, parentColumns = ["siteId"], childColumns = ["siteId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Snap::class, parentColumns = ["snapId"], childColumns = ["snapId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["siteId"])],
)
data class PendingChangeAlert(
    @PrimaryKey val snapId: String,
    val siteId: String,
    val createdAt: Long,
    val policyIdentity: String,
    @androidx.room3.ColumnInfo(defaultValue = "0") val lastAttemptAt: Long = 0,
)

@Dao
interface PendingChangeAlertDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(alert: PendingChangeAlert)

    @Query("SELECT * FROM pendingChangeAlerts WHERE (:siteId IS NULL OR siteId = :siteId) ORDER BY lastAttemptAt, createdAt, snapId LIMIT :limit")
    suspend fun firstBatch(limit: Int, siteId: String? = null): List<PendingChangeAlert>

    @Query("UPDATE pendingChangeAlerts SET lastAttemptAt = :timestamp WHERE snapId = :id")
    suspend fun attempted(id: String, timestamp: Long)

    @Query("DELETE FROM pendingChangeAlerts WHERE snapId = :id")
    suspend fun complete(id: String)
}
