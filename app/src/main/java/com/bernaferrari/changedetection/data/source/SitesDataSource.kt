package com.bernaferrari.changedetection.data.source

import com.bernaferrari.changedetection.data.Site
import com.bernaferrari.changedetection.data.source.local.SiteAndLastSnap

/**
 * Main entry point for accessing sites data.
 * Inspired from Architecture Components MVVM sample app
 */
interface SitesDataSource {

    interface LoadSitesCallback {

        fun onSitesLoaded(sites: List<Site>)

        fun onDataNotAvailable()
    }

    interface GetSiteCallback {

        fun onSiteLoaded(site: Site)

        fun onDataNotAvailable()
    }

    fun getSiteAndLastSnap(callback: (MutableList<SiteAndLastSnap>) -> (Unit))

    fun getSites(callback: LoadSitesCallback)

    fun getSite(siteId: String, callback: GetSiteCallback)

    fun saveSite(site: Site)

    fun updateSite(site: Site)

    fun deleteAllSites()

    fun deleteSite(siteId: String)
}
