/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source.local

import com.bernaferrari.changedetection.repo.AppExecutors
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.source.SitesDataSource
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of a data source as a db.
 * KMP Compatible version.
 */
class SitesLocalDataSource(
    private val mAppExecutors: AppExecutors,
    private val mSitesDao: SitesDao,
    private val cleanup: com.bernaferrari.changedetection.storage.SnapshotFileCleanup,
) : SitesDataSource {
    override fun observeWatchlist(): Flow<List<SiteAndLastSnap>> = mSitesDao.observeWatchlist().map { rows -> rows.map { it.toSiteAndLastSnap() } }

    override fun getDataWithChanges(): Flow<List<Site>> = mSitesDao.getDataWithChanges()

    override suspend fun getSites(): List<Site> = withContext(mAppExecutors.ioContext) {
        mSitesDao.getSitesList()
    }

    override suspend fun getSiteById(siteId: String): Site? = withContext(mAppExecutors.ioContext) {
        mSitesDao.getSiteById(siteId)
    }

    override suspend fun getSiteByUrl(siteUrl: String): Site? = withContext(mAppExecutors.ioContext) {
        mSitesDao.getSiteByUrl(siteUrl)
    }

    override suspend fun saveSite(site: Site, resetHistory: Boolean): Unit = withContext(mAppExecutors.ioContext) {
        if (resetHistory) {
            mSitesDao.saveWithHistoryReset(site)
            cleanup.drain()
        } else {
            mSitesDao.savePreservingStatus(site)
        }
    }

    override suspend fun updateSite(site: Site) = withContext(mAppExecutors.ioContext) {
        mSitesDao.updateMonitoringOptions(site.id, site.isSyncEnabled, site.isNotificationEnabled)
    }

    override suspend fun updateIcon(siteId: String, iconUrl: String) = withContext(mAppExecutors.ioContext) {
        mSitesDao.updateIcon(siteId, iconUrl)
    }

    override suspend fun markSiteRead(siteId: String, snapId: String): Boolean = withContext(mAppExecutors.ioContext) {
        mSitesDao.markSiteRead(siteId, snapId) > 0
    }

    override suspend fun undoSiteRead(siteId: String, snapId: String): Boolean = withContext(mAppExecutors.ioContext) {
        mSitesDao.undoSiteRead(siteId, snapId) > 0
    }

    override suspend fun deleteAllSites() = withContext(mAppExecutors.ioContext) {
        mSitesDao.deleteSites()
        cleanup.drain()
    }

    override suspend fun deleteSite(siteId: String) = withContext(mAppExecutors.ioContext) {
        mSitesDao.deleteSiteById(siteId)
        cleanup.drain()
    }
}
