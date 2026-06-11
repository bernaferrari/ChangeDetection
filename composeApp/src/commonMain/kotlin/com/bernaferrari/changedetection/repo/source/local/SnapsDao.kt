/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

/**
 * Data Access Object for the snaps table.
 * Includes performance optimizations with proper indexing
 * Inspired from Architecture Components MVVM sample app
 */
@Dao
interface SnapsDao {
    /**
     * Select all snap metadata (which is everything except for its value) by siteId.
     *
     * @param siteId the site url to be filtered.
     * @return all minimal snaps for the siteId.
     */
    @Query("SELECT * FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC")
    suspend fun getAllSnapsForSiteId(siteId: String): List<Snap>

    @Query("SELECT * FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC")
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

    /**
     * Select all snap by siteId.
     *
     * @param siteId the site url to be filtered.
     * @return all snaps for the siteId.
     */
    @Query("SELECT * FROM snaps WHERE siteId = :siteId AND contentType = :contentType ORDER BY timestamp DESC")
    suspend fun getAllSnapsForSiteIdAndContentType(
        siteId: String,
        contentType: String,
    ): List<Snap>

    /**
     * Select a snap by url
     *
     * @param id the snap url.
     * @return the snap with snapId.
     */
    @Query("SELECT * FROM snaps WHERE snapId = :id")
    suspend fun getSnapById(id: String): Snap?

    /**
     * Select the most recent snap metadata (which is everything except for its value) by siteId
     *
     * @param siteId the site url.
     * @return the snap metadata with snapId.
     */
    @Query("SELECT * FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastSnapForSiteId(siteId: String): Snap?

    @Query("SELECT * FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC LIMIT :limit")
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
    @Query("SELECT contentSize FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC LIMIT 100")
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
     * Delete all snaps by siteId.
     */
    @Query("DELETE FROM snaps WHERE siteId = :id")
    suspend fun deleteAllSnapsForSite(id: String)

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

    // Optimized: Count snaps for sites with changes (more than 1 snap)
    @Query("SELECT COUNT(*) FROM snaps WHERE siteId IN (SELECT siteId FROM snaps GROUP BY siteId HAVING COUNT(*) > 1)")
    fun countNumberOfChanges(): Flow<Int>

    // Optimized: Get snaps from sites with changes - simplified for KMP
    @Query("SELECT * FROM snaps WHERE siteId IN (SELECT siteId FROM snaps GROUP BY siteId HAVING COUNT(*) > 1) ORDER BY timestamp DESC")
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
    @Query("SELECT * FROM snaps WHERE siteId = :siteId AND contentType LIKE :filter ORDER BY timestamp DESC")
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
    @Query("SELECT * FROM snaps ORDER BY timestamp DESC")
    suspend fun getSnapsWithChange2(): List<Snap>

    @Query("SELECT count(*) FROM snaps WHERE siteId = :siteId AND contentType LIKE :filter ORDER BY timestamp DESC")
    fun countSnapsWithChange(
        siteId: String,
        filter: String,
    ): Flow<Int>

    // Fast content comparison using hashes
    @Query("SELECT contentHash FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastContentHash(siteId: String): String?

    @Query("SELECT processedContentHash FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastProcessedContentHash(siteId: String): String?

    // Check if content with same hash already exists
    @Query("SELECT COUNT(*) FROM snaps WHERE siteId = :siteId AND contentHash = :hash")
    suspend fun hasContentWithHash(
        siteId: String,
        hash: String,
    ): Int

    // Get snaps with different content hashes (actual changes)
    @Query(
        """
        SELECT * FROM snaps s1 
        WHERE siteId = :siteId 
        AND NOT EXISTS (
            SELECT 1 FROM snaps s2 
            WHERE s2.siteId = :siteId 
            AND s2.contentHash = s1.contentHash 
            AND s2.timestamp < s1.timestamp
        )
        ORDER BY timestamp DESC
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

    /**
     * Get paged snaps from sites with changes (more than 1 snap).
     * Used for the Logs screen.
     */
    @Query("SELECT * FROM snaps WHERE siteId IN (SELECT siteId FROM snaps GROUP BY siteId HAVING COUNT(*) > 1) ORDER BY timestamp DESC")
    fun getSnapsPagedSource(): PagingSource<Int, Snap>
}
