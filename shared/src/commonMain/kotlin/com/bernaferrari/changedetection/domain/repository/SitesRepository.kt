/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */

package com.bernaferrari.changedetection.domain.repository

import com.bernaferrari.changedetection.domain.model.Site
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing sites (websites being monitored).
 *
 * Provides access to site data with reactive streams.
 * Implementations are platform-specific but provide the same interface.
 */
interface SitesRepository {
    /**
     * Get all sites as a reactive stream
     */
    fun getAllSites(): Flow<List<Site>>

    /**
     * Get a single site by ID
     */
    suspend fun getSiteById(id: String): Site?

    /**
     * Insert or update a site
     */
    suspend fun insertOrUpdateSite(site: Site)

    /**
     * Delete a site by ID
     */
    suspend fun deleteSite(id: String)

    /**
     * Delete all sites
     */
    suspend fun deleteAllSites()

    /**
     * Search sites by title or URL
     */
    fun searchSites(query: String): Flow<List<Site>>

    /**
     * Get sites with a specific tag
     */
    fun getSitesByTag(tag: String): Flow<List<Site>>
}
