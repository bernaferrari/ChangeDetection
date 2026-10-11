/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source.local

import androidx.room3.*
import com.bernaferrari.changedetection.repo.Site
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the sites table.
 * Inspired from Architecture Components MVVM sample app
 */
@Dao
interface SitesDao {

    /**
     * Select all sites from the sites table.
     *
     * @return all sites.
     */
    @Query("SELECT * FROM sites")
    suspend fun getSitesList(): List<Site>

    @Query("SELECT * FROM sites")
    fun getDataWithChanges(): Flow<List<Site>>

    /** One SQLite read observes status, latest identity and count from the same database state. */
    @Query(
        """
        SELECT sites.*,
            latest.snapId AS latest_snapId, latest.siteId AS latest_siteId,
            latest.timestamp AS latest_timestamp, latest.contentType AS latest_contentType,
            latest.contentCharset AS latest_contentCharset, latest.contentSize AS latest_contentSize,
            latest.contentHash AS latest_contentHash, latest.processedContentHash AS latest_processedContentHash,
            (SELECT COUNT(*) FROM snaps WHERE siteId = sites.siteId) AS snapshotCount
        FROM sites
        LEFT JOIN snaps latest ON latest.snapId = (
            SELECT snapId FROM snaps WHERE siteId = sites.siteId
            ORDER BY timestamp DESC, snapId DESC LIMIT 1
        )
        ORDER BY sites.siteId
        """,
    )
    fun observeWatchlist(): Flow<List<WatchlistRow>>

    /**
     * Select a site by id.
     *
     * @param siteId the site id.
     * @return the site with siteId.
     */
    @Query("SELECT * FROM sites WHERE siteId = :siteId")
    suspend fun getSiteById(siteId: String): Site?

    /**
     * Select a site by url.
     *
     * @param siteUrl the site url.
     * @return the site with siteUrl.
     */
    @Query("SELECT * FROM sites WHERE RTRIM(url, '/') = RTRIM(:siteUrl, '/') LIMIT 1")
    suspend fun getSiteByUrl(siteUrl: String): Site?

    /**
     * Save a site without deleting its existing snapshot relationships.
     *
     * @param site the site to be inserted.
     */
    @Upsert
    suspend fun insertSite(site: Site)

    /**
     * Update a site.
     *
     * @param site site to be updated
     */
    @Update
    suspend fun updateSite(site: Site)

    @Query("UPDATE sites SET isSyncEnabled = :enabled, isNotificationEnabled = :notifications WHERE siteId = :siteId")
    suspend fun updateMonitoringOptions(siteId: String, enabled: Boolean, notifications: Boolean)

    /** Only the icon column, so it can never overwrite a check result written at the same time. */
    @Query("UPDATE sites SET iconUrl = :iconUrl WHERE siteId = :siteId AND iconUrl != :iconUrl")
    suspend fun updateIcon(siteId: String, iconUrl: String)

    /** Editing a name or options must not overwrite a check completed while the form was open. */
    @Transaction
    suspend fun savePreservingStatus(site: Site) {
        val current = getSiteById(site.id)
        insertSite(
            if (current == null) {
                site
            } else {
                site.copy(
                    timestamp = current.timestamp,
                    lastSuccessfulCheck = current.lastSuccessfulCheck,
                    lastFailureCode = current.lastFailureCode,
                    isSuccessful = current.isSuccessful,
                    isBrowser = current.isBrowser,
                    isRead = current.isRead,
                    iconUrl = site.iconUrl.ifBlank { current.iconUrl },
                )
            },
        )
    }

    @Query("DELETE FROM snaps WHERE siteId = :siteId")
    suspend fun deleteSnapshotRows(siteId: String)

    /** Metadata reset and monitor edit commit together; files are swept only after commit. */
    @Transaction
    suspend fun saveWithHistoryReset(site: Site) {
        insertSite(site.copy(timestamp = 0L, lastSuccessfulCheck = null, lastFailureCode = null, isSuccessful = true, isRead = true))
        deleteSnapshotRows(site.id)
    }

    /** Clears only the attention flag, preserving any simultaneous edit or sync metadata. */
    @Query(
        "UPDATE sites SET isRead = 1 WHERE siteId = :siteId AND isRead = 0 AND :snapId = " +
            "(SELECT snapId FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC, snapId DESC LIMIT 1)",
    )
    suspend fun markSiteRead(siteId: String, snapId: String): Int

    /** Undo cannot change the attention state of a capture saved after acknowledgement. */
    @Query(
        "UPDATE sites SET isRead = 0 WHERE siteId = :siteId AND isRead = 1 AND :snapId = " +
            "(SELECT snapId FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC, snapId DESC LIMIT 1)",
    )
    suspend fun undoSiteRead(siteId: String, snapId: String): Int

    /**
     * Update the complete status of a site
     *
     * @param siteId    url of the site
     * @param completed status to be updated
     */
    @Query("UPDATE sites SET isSuccessful = :completed WHERE siteId = :siteId")
    suspend fun updateCompleted(siteId: String, completed: Boolean)

    /**
     * Targeted status refresh after a sync attempt. Touches only these columns so a
     * concurrent user edit (rename, enable/disable) can't be clobbered by a stale
     * full-row read-modify-write from the sync pipeline.
     */
    @Query(
        "UPDATE sites SET timestamp = :timestamp, isSuccessful = :successful, " +
            "lastSuccessfulCheck = CASE WHEN :successful THEN :timestamp ELSE lastSuccessfulCheck END, " +
            "lastFailureCode = CASE WHEN :successful THEN NULL ELSE :failureCode END, " +
            "isBrowser = :isBrowser, isRead = COALESCE(:isRead, isRead) WHERE siteId = :siteId",
    )
    suspend fun updateSyncMetadata(
        siteId: String,
        timestamp: Long,
        successful: Boolean,
        isBrowser: Boolean,
        isRead: Boolean?,
        failureCode: String? = null,
    )

    /**
     * Delete a site by url.
     *
     * @param siteId the site id.
     */
    @Query("DELETE FROM sites WHERE siteId = :siteId")
    suspend fun deleteSiteById(siteId: String)

    /**
     * Delete all sites.
     */
    @Query("DELETE FROM sites")
    suspend fun deleteSites()
}
