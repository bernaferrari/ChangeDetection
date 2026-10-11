/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source

import com.bernaferrari.changedetection.repo.PageIconShape

import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import kotlinx.coroutines.flow.Flow

/**
 * Main entry point for accessing sites data.
 * Inspired from Architecture Components MVVM sample app
 */
interface SitesDataSource {
    /** Observe complete page/history summaries in a single consistent read. */
    fun observeWatchlist(): Flow<List<SiteAndLastSnap>>

    suspend fun getSites(): List<Site>

    /** Remember the page's icon, found during a capture. */
    suspend fun updateIcon(siteId: String, iconUrl: String) {}

    fun getDataWithChanges(): Flow<List<Site>>

    suspend fun getSiteById(siteId: String): Site?

    suspend fun getSiteByUrl(siteUrl: String): Site?

    suspend fun saveSite(site: Site, resetHistory: Boolean = false)

    suspend fun updateIconShape(siteId: String, shape: PageIconShape)

    suspend fun updateIconColor(siteId: String, colors: com.bernaferrari.changedetection.repo.ColorGroup)

    suspend fun updateSite(site: Site)

    /** Acknowledge only the latest saved snapshot; returns false if a newer change arrived. */
    suspend fun markSiteRead(siteId: String, snapId: String): Boolean

    /** Reopen attention only if the acknowledged capture is still latest. */
    suspend fun undoSiteRead(siteId: String, snapId: String): Boolean

    suspend fun deleteAllSites()

    suspend fun deleteSite(siteId: String)
}
