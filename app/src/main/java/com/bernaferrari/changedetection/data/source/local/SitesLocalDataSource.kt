package com.bernaferrari.changedetection.data.source.local

import com.bernaferrari.changedetection.data.Site
import com.bernaferrari.changedetection.data.source.SitesDataSource
import com.bernaferrari.changedetection.util.AppExecutors
import kotlinx.coroutines.withContext


/**
 * Concrete implementation of a data source as a db.
 * Inspired from Architecture Components MVVM sample app
 */
class SitesLocalDataSource constructor(
    private val mAppExecutors: AppExecutors,
    private val mSitesDao: SitesDao
) : SitesDataSource {

    override suspend fun getSites(): List<Site> = withContext(mAppExecutors.ioContext) {
        mSitesDao.sites
    }

    override suspend fun getSite(siteId: String): Site? = withContext(mAppExecutors.ioContext) {
        mSitesDao.getSiteById(siteId)
    }

    override suspend fun saveSite(site: Site) = withContext(mAppExecutors.ioContext) {
        mSitesDao.insertSite(site)
    }

    override suspend fun updateSite(site: Site) = withContext(mAppExecutors.ioContext) {
        mSitesDao.updateSite(site)
    }

    override suspend fun deleteAllSites() = withContext(mAppExecutors.ioContext) {
        mSitesDao.deleteSites()
    }

    override suspend fun deleteSite(siteId: String) = withContext(mAppExecutors.ioContext) {
        mSitesDao.deleteSiteById(siteId)
    }
}
