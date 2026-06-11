/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source.local

import androidx.room.*
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
    @Query("SELECT * FROM sites WHERE url = :siteUrl")
    suspend fun getSiteByUrl(siteUrl: String): Site?

    /**
     * Insert a site in the database. If the site already exists, replace it.
     *
     * @param site the site to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: Site)

    /**
     * Update a site.
     *
     * @param site site to be updated
     */
    @Update
    suspend fun updateSite(site: Site)

    /**
     * Update the complete status of a site
     *
     * @param siteId    url of the site
     * @param completed status to be updated
     */
    @Query("UPDATE sites SET isSuccessful = :completed WHERE siteId = :siteId")
    suspend fun updateCompleted(siteId: String, completed: Boolean)

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
