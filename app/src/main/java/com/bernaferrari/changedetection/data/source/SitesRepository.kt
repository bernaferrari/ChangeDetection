package com.bernaferrari.changedetection.data.source

import com.bernaferrari.changedetection.data.Site

/**
 * Concrete implementation to load sites from the data sources into a cache.
 *
 *
 * For simplicity, this implements a dumb synchronisation between locally persisted data and data
 * obtained from the server, by using the remote data source only if the local database doesn't
 * exist or is empty.
 *
 * TODO: Implement this class using LiveData.
 *
 * Inspired from Architecture Components MVVM sample app
 */
class SitesRepository // Prevent direct instantiation.
private constructor(
    sitesLocalDataSource: SitesDataSource
) : SitesDataSource {

    private val mSitesLocalDataSource: SitesDataSource = checkNotNull(sitesLocalDataSource)

    override fun getSites(callback: ((List<Site>) -> (Unit))) {
        checkNotNull(callback)

        // Query the local storage if available. If not, query the network.
        mSitesLocalDataSource.getSites {
            callback.invoke(it)
        }
    }

    override fun saveSite(site: Site) {
        checkNotNull(site)
        mSitesLocalDataSource.saveSite(site)
    }

    override fun updateSite(site: Site) {
        checkNotNull(site)
        mSitesLocalDataSource.updateSite(site)
    }


    /**
     * Gets sites from local data source (sqlite) unless the table is new or empty. In that case it
     * uses the network data source. This is done to simplify the sample.
     */
    override fun getSite(siteId: String, callback: (Site?) -> (Unit)) {
        // Load from server/persisted if needed.

        // Is the site in the local data source? If not, query the network.
        mSitesLocalDataSource.getSite(siteId) {
            callback.invoke(it)
        }
    }

    override fun deleteAllSites() {
        mSitesLocalDataSource.deleteAllSites()
    }

    override fun deleteSite(siteId: String) {
        mSitesLocalDataSource.deleteSite(checkNotNull(siteId))
    }

    companion object {

        @Volatile
        private var INSTANCE: SitesRepository? = null

        /**
         * Returns the single instance of this class, creating it if necessary.
         *
         * @param sitesLocalDataSource  the device storage data source
         * @return the [SitesRepository] instance
         */
        fun getInstance(
            sitesLocalDataSource: SitesDataSource
        ): SitesRepository {
            if (INSTANCE == null) {
                synchronized(SitesRepository::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = SitesRepository(sitesLocalDataSource)
                    }
                }
            }
            return INSTANCE!!
        }

        /**
         * Used to force [.getInstance] to create a new instance
         * next time it's called.
         */
        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
