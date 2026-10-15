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

    /** Edits require a live row inside the write transaction; creation may insert a new page. */
    suspend fun saveSite(site: Site, resetHistory: Boolean = false, requireExisting: Boolean = false)

    suspend fun updateIconShape(siteId: String, shape: PageIconShape)

    suspend fun updateIconColor(siteId: String, colors: com.bernaferrari.changedetection.repo.ColorGroup)

    suspend fun setMonitoringEnabled(siteId: String, enabled: Boolean)

    suspend fun setNotificationsEnabled(siteId: String, enabled: Boolean)

    /** Acknowledge only the latest saved snapshot; returns false if a newer change arrived. */
    suspend fun markSiteRead(siteId: String, snapId: String): Boolean

    /** Reopen attention only if the acknowledged capture is still latest. */
    suspend fun undoSiteRead(siteId: String, snapId: String): Boolean

    suspend fun deleteAllSites()

    suspend fun deleteSite(siteId: String)
}

/** A stale editor may not recreate a page removed by another screen. */
class SiteNotFoundException(val siteId: String) : IllegalStateException("The monitored page no longer exists")
