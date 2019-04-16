package com.bernaferrari.changedetection.repo.source

import com.bernaferrari.changedetection.repo.Site
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation to load sites from the data sources into a cache.
 *
 *
 * For simplicity, this implements a dumb synchronisation between locally persisted data and data
 * obtained from the server, by using the remote data source only if the local database doesn't
 * exist or is empty.
 *
 *
 * Inspired from Architecture Components MVVM sample app
 */
@Singleton
class SitesRepository @Inject constructor(
    sitesLocalDataSource: SitesDataSource
) : SitesDataSource {

    override fun getDataWithChanges(): Observable<List<Site>> {
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

    private val mSitesLocalDataSource: SitesDataSource = checkNotNull(sitesLocalDataSource)

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
        mSitesLocalDataSource.deleteSite(checkNotNull(siteId))
    }
}
