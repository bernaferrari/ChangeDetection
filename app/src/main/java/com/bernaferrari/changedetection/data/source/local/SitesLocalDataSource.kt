package com.bernaferrari.changedetection.data.source.local

import android.support.annotation.VisibleForTesting
import com.bernaferrari.changedetection.data.Site
import com.bernaferrari.changedetection.data.source.SitesDataSource
import com.bernaferrari.changedetection.util.AppExecutors
import kotlinx.coroutines.experimental.withContext


/**
 * Concrete implementation of a data source as a db.
 * Inspired from Architecture Components MVVM sample app
 */
class SitesLocalDataSource
private constructor(
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

    companion object {

        @Volatile
        private var INSTANCE: SitesLocalDataSource? = null

        fun getInstance(
            appExecutors: AppExecutors,
            sitesDao: SitesDao
        ): SitesLocalDataSource {
            if (INSTANCE == null) {
                synchronized(SitesLocalDataSource::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = SitesLocalDataSource(appExecutors, sitesDao)
                    }
                }
            }
            return INSTANCE!!
        }

        @VisibleForTesting
        internal fun clearInstance() {
            INSTANCE = null
        }
    }
}
