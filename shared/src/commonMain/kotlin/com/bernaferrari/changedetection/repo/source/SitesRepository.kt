package com.bernaferrari.changedetection.repo.source

import com.bernaferrari.changedetection.repo.PageIconShape

import com.bernaferrari.changedetection.extensions.canonicalUrlKey
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.usesBrowserIdentity
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import kotlinx.coroutines.flow.Flow

/**
 * Concrete implementation to load sites from the data sources into a cache.
 * KMP Version.
 */
class SitesRepository(
    private val mSitesLocalDataSource: SitesDataSource,
) : SitesDataSource {
    override fun observeWatchlist(): Flow<List<SiteAndLastSnap>> = mSitesLocalDataSource.observeWatchlist()

    override fun getDataWithChanges(): Flow<List<Site>> = mSitesLocalDataSource.getDataWithChanges()

    override suspend fun getSites(): List<Site> = mSitesLocalDataSource.getSites()

    override suspend fun getSiteById(siteId: String): Site? = mSitesLocalDataSource.getSiteById(siteId)

    override suspend fun getSiteByUrl(siteUrl: String): Site? {
        mSitesLocalDataSource.getSiteByUrl(siteUrl)?.let { return it }

        // Callers without a capture rule (such as a shared URL) must preserve browser routes.
        return findDuplicate(siteUrl, CaptureMode.SCREENSHOT)
    }

    suspend fun findDuplicate(siteUrl: String, mode: CaptureMode): Site? = mSitesLocalDataSource.getSites().firstOrNull { saved ->
        val browserIdentity = mode.usesBrowserIdentity() || saved.captureMode.usesBrowserIdentity()
        val key = siteUrl.canonicalUrlKey(browserIdentity)
        key != null && saved.url.canonicalUrlKey(browserIdentity) == key
    }

    override suspend fun saveSite(site: Site, resetHistory: Boolean, requireExisting: Boolean) {
        mSitesLocalDataSource.saveSite(site, resetHistory, requireExisting)
    }

    override suspend fun updateIconShape(siteId: String, shape: PageIconShape) = mSitesLocalDataSource.updateIconShape(siteId, shape)

    override suspend fun updateIconColor(siteId: String, colors: com.bernaferrari.changedetection.repo.ColorGroup) = mSitesLocalDataSource.updateIconColor(siteId, colors)

    override suspend fun setMonitoringEnabled(siteId: String, enabled: Boolean) = mSitesLocalDataSource.setMonitoringEnabled(siteId, enabled)

    override suspend fun setNotificationsEnabled(siteId: String, enabled: Boolean) = mSitesLocalDataSource.setNotificationsEnabled(siteId, enabled)

    override suspend fun updateIcon(siteId: String, iconUrl: String) {
        if (iconUrl.startsWith("http")) mSitesLocalDataSource.updateIcon(siteId, iconUrl)
    }

    override suspend fun markSiteRead(siteId: String, snapId: String): Boolean = mSitesLocalDataSource.markSiteRead(siteId, snapId)

    override suspend fun undoSiteRead(siteId: String, snapId: String): Boolean = mSitesLocalDataSource.undoSiteRead(siteId, snapId)

    override suspend fun deleteAllSites() {
        mSitesLocalDataSource.deleteAllSites()
    }

    override suspend fun deleteSite(siteId: String) {
        mSitesLocalDataSource.deleteSite(siteId)
    }
}
