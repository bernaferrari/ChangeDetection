package com.bernaferrari.changedetection.data.source.local

import android.support.annotation.VisibleForTesting
import com.bernaferrari.changedetection.data.Site
import com.bernaferrari.changedetection.data.source.SitesDataSource
import com.bernaferrari.changedetection.util.AppExecutors


/**
 * Concrete implementation of a data source as a db.
 * Inspired from Architecture Components MVVM sample app
 */
class SitesLocalDataSource
private constructor(
    private val mAppExecutors: AppExecutors,
    private val mSitesDao: SitesDao
) : SitesDataSource {

    override fun getSites(callback: ((List<Site>) -> (Unit))) {
        val runnable = Runnable {
            val sites = mSitesDao.sites
            mAppExecutors.mainThread().execute {
                callback.invoke(sites)
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    override fun getSite(siteId: String, callback: ((Site?) -> (Unit))) {
        val runnable = Runnable {
            val site = mSitesDao.getSiteById(siteId)

            mAppExecutors.mainThread().execute {
                callback.invoke(site)
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    override fun saveSite(site: Site) {
        checkNotNull(site)
        val saveRunnable = Runnable { mSitesDao.insertSite(site) }

        mAppExecutors.diskIO().execute(saveRunnable)
    }

    override fun updateSite(site: Site) {
        checkNotNull(site)
        val saveRunnable = Runnable { mSitesDao.updateSite(site) }

        mAppExecutors.diskIO().execute(saveRunnable)
    }

    override fun deleteAllSites() {
        val deleteRunnable = Runnable { mSitesDao.deleteSites() }

        mAppExecutors.diskIO().execute(deleteRunnable)
    }

    override fun deleteSite(siteId: String) {
        val deleteRunnable = Runnable { mSitesDao.deleteSiteById(siteId) }

        mAppExecutors.diskIO().execute(deleteRunnable)
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
