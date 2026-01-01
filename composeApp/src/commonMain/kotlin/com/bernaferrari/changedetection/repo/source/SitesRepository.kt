package com.bernaferrari.changedetection.repo.source

import com.bernaferrari.changedetection.repo.Site
import kotlinx.coroutines.flow.Flow

/**
 * Concrete implementation to load sites from the data sources into a cache.
 * KMP Version.
 */
class SitesRepository(
    private val mSitesLocalDataSource: SitesDataSource
) : SitesDataSource {

    override fun getDataWithChanges(): Flow<List<Site>> {
        return mSitesLocalDataSource.getDataWithChanges()
    }

    override suspend fun getSites(): List<Site> {
        return mSitesLocalDataSource.getSites()
    }

    override suspend fun getSiteById(siteId: String): Site? {
        return mSitesLocalDataSource.getSiteById(siteId)
    }

    override suspend fun getSiteByUrl(siteUrl: String): Site? {
        return mSitesLocalDataSource.getSiteByUrl(siteUrl)
    }

    override suspend fun saveSite(site: Site) {
        mSitesLocalDataSource.saveSite(site)
    }

    override suspend fun updateSite(site: Site) {
        mSitesLocalDataSource.updateSite(site)
    }

    override suspend fun deleteAllSites() {
        mSitesLocalDataSource.deleteAllSites()
    }

    override suspend fun deleteSite(siteId: String) {
        mSitesLocalDataSource.deleteSite(siteId)
    }
}
