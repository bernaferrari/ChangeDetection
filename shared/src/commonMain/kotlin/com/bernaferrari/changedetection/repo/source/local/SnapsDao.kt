package com.bernaferrari.changedetection.repo.source.local

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import com.bernaferrari.changedetection.repo.Snap
import kotlinx.coroutines.flow.Flow

data class ContentTypeCount(
    val contentType: String,
    val count: Int,
)

data class ContentDuplicationStats(
    val unique_content: Int,
    val total_snaps: Int,
) {
    val duplicationRate: Float get() = if (total_snaps > 0) 1f - (unique_content.toFloat() / total_snaps) else 0f
}

data class SiteSnapshotCount(
    val siteId: String,
    val count: Int,
)

/**
 * Data Access Object for the snaps table.
 * Includes performance optimizations with proper indexing
 * Inspired from Architecture Components MVVM sample app
 */
@Dao
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
interface SnapsDao {
    /** Selects only IDs beyond the newest pair; no full snapshot/archive materialization. */
    @Query("SELECT snapId FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC, snapId DESC LIMIT :batchSize OFFSET 2")
    suspend fun olderVersionIds(siteId: String, batchSize: Int): List<String>

    /** One bounded transaction preserves the newest chronological pair, including repeated content. */
    @Transaction
    suspend fun removeOlderVersionsBatch(siteId: String, batchSize: Int = 500): List<String> {
        require(batchSize in 1..500)
        return olderVersionIds(siteId, batchSize).also { obsolete ->
            if (obsolete.isNotEmpty()) deleteSnapsByIds(obsolete)
        }
    }

    /** Plans a bounded deletion batch in SQLite without materializing the archive in app memory. */
    @Query(
        """
        WITH ranked AS (
            SELECT snapId, timestamp, MAX(contentSize, 0) AS bytes,
                ROW_NUMBER() OVER (PARTITION BY siteId ORDER BY timestamp DESC, snapId DESC) AS position
            FROM snaps
        ), retained AS (
            SELECT * FROM ranked WHERE position <= :versionsPerPage
        ), budget AS (
            SELECT MAX(COALESCE(SUM(bytes), 0) - :byteBudget, 0) AS excess FROM retained
        ), candidates AS (
            SELECT snapId, COALESCE(SUM(bytes) OVER (
                ORDER BY timestamp, snapId ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
            ), 0) AS bytesBefore FROM retained WHERE position > 2
        )
        SELECT snapId FROM ranked WHERE position > :versionsPerPage
        UNION ALL
        SELECT snapId FROM candidates WHERE bytesBefore < (SELECT excess FROM budget)
        LIMIT :batchSize
    """,
    )
    suspend fun retentionBatch(versionsPerPage: Int, byteBudget: Long, batchSize: Int): List<String>

    @Query("SELECT COALESCE(SUM(contentSize), 0) FROM snaps")
    fun observeStorageBytes(): Flow<Long>

    /** Same ordering as [olderVersionIds]: everything past each page's newest pair. */
    @Query(
        """
        SELECT COALESCE(SUM(MAX(contentSize, 0)), 0) FROM (
            SELECT contentSize, ROW_NUMBER() OVER (PARTITION BY siteId ORDER BY timestamp DESC, snapId DESC) AS position
            FROM snaps
        ) WHERE position > 2
    """,
    )
    fun observeOlderVersionBytes(): Flow<Long>

    @Query("SELECT siteId FROM snaps GROUP BY siteId HAVING COUNT(*) > 2")
    suspend fun siteIdsWithOlderVersions(): List<String>

    /**
     * Select all snap metadata (which is everything except for its value) by siteId.
     *
     * @param siteId the site url to be filtered.
     * @return all minimal snaps for the siteId.
     */
    @Query("SELECT * FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC, snapId DESC")
    suspend fun getAllSnapsForSiteId(siteId: String): List<Snap>

    @Query("SELECT * FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC, snapId DESC")
    fun getAllSnapsForSiteIdWithFlow(siteId: String): Flow<List<Snap>>

    /**
     * Get last few contentTypes.
     *
     * @param siteId the site url to be filtered.
     * @return list with last 3 contentTypes.
     */
    @Query("SELECT contentType, COUNT(*) as count FROM snaps WHERE siteId = :siteId GROUP BY contentType")
    suspend fun getContentTypesWithCount(siteId: String): List<ContentTypeCount>

    @Query("SELECT count(*) FROM snaps WHERE siteId = :siteId GROUP BY contentType")
    suspend fun getContentTypesCount(siteId: String): List<Int>

    @Query("SELECT contentType FROM snaps WHERE siteId = :siteId GROUP BY contentType")
    suspend fun getContentTypesParams(siteId: String): List<String>

    /** The same chronological order used by history and comparison, bounded to one row. */
    @Query("SELECT * FROM snaps WHERE siteId = :siteId AND (timestamp < :timestamp OR (timestamp = :timestamp AND snapId < :snapId)) ORDER BY timestamp DESC, snapId DESC LIMIT 1")
    suspend fun getPrecedingSnap(siteId: String, timestamp: Long, snapId: String): Snap?

    @Query("SELECT * FROM snaps WHERE snapId = :id")
    suspend fun getSnapById(id: String): Snap?

    /**
     * Select the most recent snap metadata (which is everything except for its value) by siteId
     *
     * @param siteId the site url.
     * @return the snap metadata with snapId.
     */
    @Query("SELECT * FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC, snapId DESC LIMIT 1")
    suspend fun getLastSnapForSiteId(siteId: String): Snap?

    @Query("SELECT * FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC, snapId DESC LIMIT :limit")
    suspend fun getLastNSnapsForSiteId(
        siteId: String,
        limit: Int,
    ): List<Snap>

    /**
     * Select the most recent snap metadata (which is everything except for its value) by siteId
     *
     * @param siteId the site url.
     * @return a list with the last 100 sizes.
     */
    @Query("SELECT contentSize FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC, snapId DESC LIMIT 100")
    suspend fun getLastSnapsSize(siteId: String): List<Int>

    /**
     * Get all snaps
     *
     * @return all the snaps.
     */
    @Query("SELECT * FROM snaps")
    suspend fun getAllSnaps(): List<Snap>

    /**
     * Delete a snap by url
     *
     * @param snapId the snap url.
     */
    @Query("DELETE FROM snaps WHERE snapId = :snapId")
    suspend fun deleteSnapById(snapId: String)

    /**
     * Batch delete snaps by ids.
     *
     * @param snapIds the snap ids to delete.
     * @return number of rows deleted.
     */
    @Query("DELETE FROM snaps WHERE snapId IN (:snapIds)")
    suspend fun deleteSnapsByIds(snapIds: List<String>): Int

    /**
     * Delete all snaps by siteId.
     */
    @Query("DELETE FROM snaps WHERE siteId = :id")
    suspend fun deleteAllSnapsForSite(id: String)

    /**
     * Cheap invalidation probe: re-emits whenever any row in the snaps table changes.
     * Lets ViewModels refresh derived in-memory state after snap mutations (prune,
     * delete-by-type) without observing full snap lists.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM snaps)")
    fun observeSnapsTable(): Flow<Boolean>

    /**
     * Insert a snap in the database. If the snap already exists, replace it.
     *
     * @param snap the snap to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSnap(snap: Snap)

    /**
     * Delete all snaps.
     */
    @Query("DELETE FROM snaps")
    suspend fun deleteTasks()

    // A site's first snapshot is its baseline, not a detected change.
    @Query("SELECT COALESCE(SUM(changeCount), 0) FROM (SELECT COUNT(*) - 1 AS changeCount FROM snaps GROUP BY siteId HAVING COUNT(*) > 1)")
    fun countNumberOfChanges(): Flow<Int>

    @Query("SELECT COUNT(*) FROM (SELECT siteId FROM snaps GROUP BY siteId HAVING COUNT(*) > 1)")
    fun countSitesWithChanges(): Flow<Int>

    @Query("SELECT COUNT(*) FROM snaps WHERE siteId IN (SELECT siteId FROM snaps GROUP BY siteId HAVING COUNT(*) > 1)")
    fun countVersionsInHistory(): Flow<Int>

    // Optimized: Get snaps from sites with changes - simplified for KMP
    @Query("SELECT * FROM snaps WHERE siteId IN (SELECT siteId FROM snaps GROUP BY siteId HAVING COUNT(*) > 1) ORDER BY timestamp DESC, snapId DESC")
    suspend fun getSnapsWithChanges(): List<Snap>

    // Enhanced: Get only the most recent snap for each site with changes
    @Query(
        """
        SELECT s1.* FROM snaps s1
        INNER JOIN (
            SELECT siteId, MAX(timestamp) as max_timestamp 
            FROM snaps 
            GROUP BY siteId 
            HAVING COUNT(*) > 1
        ) s2 ON s1.siteId = s2.siteId AND s1.timestamp = s2.max_timestamp
        ORDER BY s1.timestamp DESC
    """,
    )
    suspend fun getLatestSnapsFromSitesWithChangesList(): List<Snap>

    /**
     * Select all snap by siteId.
     *
     * @param siteId the site url to be filtered.
     * @return all snaps for the siteId.
     */
    @Query("SELECT * FROM snaps WHERE siteId = :siteId AND contentType LIKE :filter ORDER BY timestamp DESC, snapId DESC")
    fun getSnapsWithChange(
        siteId: String,
        filter: String,
    ): Flow<List<Snap>>

    /**
     * Select all snap by siteId.
     *
     * @param siteId the site url to be filtered.
     * @return all snaps for the siteId.
     */
    @Query("SELECT * FROM snaps ORDER BY timestamp DESC, snapId DESC")
    suspend fun getSnapsWithChange2(): List<Snap>

    @Query("SELECT count(*) FROM snaps WHERE siteId = :siteId AND contentType LIKE :filter")
    fun countSnapsWithChange(
        siteId: String,
        filter: String,
    ): Flow<Int>

    // Fast content comparison using hashes
    @Query("SELECT contentHash FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC, snapId DESC LIMIT 1")
    suspend fun getLastContentHash(siteId: String): String?

    @Query("SELECT processedContentHash FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC, snapId DESC LIMIT 1")
    suspend fun getLastProcessedContentHash(siteId: String): String?

    // Check if content with same hash already exists
    @Query("SELECT COUNT(*) FROM snaps WHERE siteId = :siteId AND contentHash = :hash")
    suspend fun hasContentWithHash(
        siteId: String,
        hash: String,
    ): Int

    // Get snaps with different content hashes (actual changes).
    // Keeps the NEWEST snap per content hash so pruning never rolls the site's
    // visible last version back to stale content.
    @Query(
        """
        SELECT * FROM snaps s1 
        WHERE siteId = :siteId 
        AND (s1.contentHash = '' OR NOT EXISTS (
            SELECT 1 FROM snaps s2 
            WHERE s2.siteId = :siteId 
            AND s2.contentHash = s1.contentHash 
            AND s2.timestamp > s1.timestamp
        ))
        ORDER BY timestamp DESC, snapId DESC
    """,
    )
    suspend fun getUniqueContentSnaps(siteId: String): List<Snap>

    // Performance monitoring queries
    @Query("SELECT AVG(contentSize) FROM snaps WHERE siteId = :siteId")
    suspend fun getAverageContentSize(siteId: String): Double?

    @Query("SELECT COUNT(DISTINCT contentHash) as unique_content, COUNT(*) as total_snaps FROM snaps WHERE siteId = :siteId")
    suspend fun getContentDuplicationStats(siteId: String): ContentDuplicationStats

    /**
     * OPTIMIZATION: Get most recent snap for ALL sites in a single query.
     *
     * @return Map of siteId to its most recent Snap (Map not supported in SQlite directly, returning list)
     */
    @Query(
        """
        SELECT s1.* FROM snaps s1
        WHERE s1.timestamp = (
            SELECT MAX(timestamp) FROM snaps s2
            WHERE s2.siteId = s1.siteId
        )
        ORDER BY s1.timestamp DESC
    """,
    )
    suspend fun getAllMostRecentSnaps(): List<Snap>

    @Query("SELECT siteId, COUNT(*) AS count FROM snaps GROUP BY siteId")
    suspend fun getSnapshotCounts(): List<SiteSnapshotCount>

    /**
     * Get paged snaps from sites with changes (more than 1 snap).
     * Used for the Logs screen.
     */
    @Query("SELECT * FROM snaps WHERE siteId IN (SELECT siteId FROM snaps GROUP BY siteId HAVING COUNT(*) > 1) ORDER BY timestamp DESC, snapId DESC")
    fun getSnapsPagedSource(): PagingSource<Int, Snap>

    @Query(
        """
        SELECT snaps.* FROM snaps
        INNER JOIN sites ON sites.siteId = snaps.siteId
        WHERE snaps.siteId IN (SELECT siteId FROM snaps GROUP BY siteId HAVING COUNT(*) > 1)
          AND (
            :query = '' OR
            LOWER(COALESCE(sites.title, '')) LIKE '%' || REPLACE(REPLACE(REPLACE(LOWER(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!' OR
            LOWER(sites.url) LIKE '%' || REPLACE(REPLACE(REPLACE(LOWER(:query), '!', '!!'), '%', '!%'), '_', '!_') || '%' ESCAPE '!'
          )
        ORDER BY snaps.timestamp DESC, snaps.snapId DESC
        """,
    )
    fun getSnapsPagedSource(query: String): PagingSource<Int, Snap>
}
