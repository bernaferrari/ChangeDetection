/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */

package com.bernaferrari.changedetection.domain.repository

import com.bernaferrari.changedetection.domain.model.Snap
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing snapshots of websites.
 *
 * Key responsibility: Implement N+1 query optimization
 * - Single batch query getAllMostRecentSnaps() instead of N queries
 * - Performance: 51 queries (1+50) reduced to 2 queries (1+1) = 96% reduction
 */
interface SnapsRepository {
    /**
     * Get all snapshots for a specific site
     */
    suspend fun getAllSnapsForSite(siteId: String): List<Snap>

    /**
     * Get the most recent snapshot for a site
     */
    suspend fun getMostRecentSnap(siteId: String): Snap?

    /**
     * OPTIMIZATION: Get most recent snaps for ALL sites in a single query
     * This solves the N+1 query problem.
     *
     * Performance impact:
     * - Before: 1 query (sites) + N queries (individual snaps) = 1+N queries
     * - After: 1 query (sites) + 1 batch query (all snaps) = 2 queries
     * - For 50 sites: 51 → 2 queries (98% reduction)
     *
     * @return Map of siteId to its most recent Snap
     */
    suspend fun getAllMostRecentSnaps(): Map<String, Snap>

    /**
     * Get snapshots as a reactive stream
     */
    fun getSnapsForSite(siteId: String): Flow<List<Snap>>

    /**
     * Get last N snapshots for a site
     */
    suspend fun getLastNSnapsForSite(siteId: String, limit: Int): List<Snap>

    /**
     * Save a new snapshot
     * Returns Kotlin stdlib Result type
     */
    suspend fun saveSnap(snap: Snap): kotlin.Result<Snap>

    /**
     * Delete a snapshot
     */
    suspend fun deleteSnap(snapId: String)

    /**
     * Delete all snapshots for a site
     */
    suspend fun deleteAllSnapsForSite(siteId: String)

    /**
     * Get snapshots with content (returns content as ByteArray)
     */
    suspend fun getSnapContent(snapId: String): ByteArray?

    /**
     * Compare two snapshots for differences
     */
    suspend fun compareSnapshots(snapId1: String, snapId2: String): SnapDiff?
}

/**
 * Result of comparing two snapshots
 */
data class SnapDiff(
    val originalId: String,
    val newId: String,
    val originalContent: ByteArray,
    val newContent: ByteArray,
    val isDifferent: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SnapDiff) return false

        if (originalId != other.originalId) return false
        if (newId != other.newId) return false
        if (!originalContent.contentEquals(other.originalContent)) return false
        if (!newContent.contentEquals(other.newContent)) return false
        if (isDifferent != other.isDifferent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = originalId.hashCode()
        result = 31 * result + newId.hashCode()
        result = 31 * result + originalContent.contentHashCode()
        result = 31 * result + newContent.contentHashCode()
        result = 31 * result + isDifferent.hashCode()
        return result
    }
}
