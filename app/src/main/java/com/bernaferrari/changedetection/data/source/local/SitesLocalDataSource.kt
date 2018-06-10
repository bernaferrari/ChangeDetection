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
    private val mSitesDao: SitesDao,
    private val mSnapsDao: SnapsDao
) : SitesDataSource {

    override fun getLastFewContentTypes(siteId: String, callback: (List<String>) -> Unit) {
        val runnable = Runnable {
            val list = mSnapsDao.getLastFewContentTypes(siteId)

            mAppExecutors.mainThread().execute {
                callback.invoke(list)
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    override fun getSiteAndLastSnap(callback: (MutableList<SiteAndLastSnap>) -> Unit) {
        val runnable = Runnable {
            val sites = mSitesDao.sites
            val list = mutableListOf<SiteAndLastSnap>()
            sites.mapTo(list) { site ->
                SiteAndLastSnap(site, mSnapsDao.getLastSnapForSiteId(site.id))
            }

            mAppExecutors.mainThread().execute {
                callback.invoke(list)
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    override fun getSites(callback: SitesDataSource.LoadSitesCallback) {
        val runnable = Runnable {
            val sites = mSitesDao.sites
            mAppExecutors.mainThread().execute {
                if (sites.isEmpty()) {
                    // This will be called if the table is new or just empty.
                    callback.onDataNotAvailable()
                } else {
                    callback.onSitesLoaded(sites)
                }
            }
        }

        mAppExecutors.diskIO().execute(runnable)
    }

    override fun getSite(siteId: String, callback: SitesDataSource.GetSiteCallback) {
        val runnable = Runnable {
            val site = mSitesDao.getSiteById(siteId)

            mAppExecutors.mainThread().execute {
                if (site != null) {
                    callback.onSiteLoaded(site)
                } else {
                    callback.onDataNotAvailable()
                }
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
            sitesDao: SitesDao,
            snapsDao: SnapsDao
        ): SitesLocalDataSource {
            if (INSTANCE == null) {
                synchronized(SitesLocalDataSource::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = SitesLocalDataSource(appExecutors, sitesDao, snapsDao)
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
