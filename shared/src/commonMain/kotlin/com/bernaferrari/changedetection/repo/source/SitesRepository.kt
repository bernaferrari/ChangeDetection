/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source

import com.bernaferrari.changedetection.repo.PageIconShape

import com.bernaferrari.changedetection.extensions.canonicalUrlKey
import com.bernaferrari.changedetection.repo.Site
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

        // Older databases may contain addresses saved before canonical duplicate
        // checks existed. Compare stable URL identities as a migration-free guard
        // so host case, fragments, default ports, and trailing slashes cannot create
        // another copy of the same page.
        val requestedKey = siteUrl.canonicalUrlKey() ?: return null
        return mSitesLocalDataSource.getSites().firstOrNull { it.url.canonicalUrlKey() == requestedKey }
    }

    override suspend fun saveSite(site: Site, resetHistory: Boolean) {
        mSitesLocalDataSource.saveSite(site, resetHistory)
    }

    override suspend fun updateIconShape(siteId: String, shape: PageIconShape) = mSitesLocalDataSource.updateIconShape(siteId, shape)

    override suspend fun updateIconColor(siteId: String, colors: com.bernaferrari.changedetection.repo.ColorGroup) = mSitesLocalDataSource.updateIconColor(siteId, colors)

    override suspend fun updateSite(site: Site) {
        mSitesLocalDataSource.updateSite(site)
    }

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
