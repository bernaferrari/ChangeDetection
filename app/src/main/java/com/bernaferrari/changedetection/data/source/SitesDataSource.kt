package com.bernaferrari.changedetection.data.source

import com.bernaferrari.changedetection.data.Site
import com.bernaferrari.changedetection.data.source.local.SiteAndLastMinimalSnap

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

    fun getSiteAndLastMinimalSnap(callback: (MutableList<SiteAndLastMinimalSnap>) -> (Unit))

    fun getSites(callback: LoadSitesCallback)

    fun getLastFewContentTypes(siteId: String, callback: (List<String>) -> (Unit))

    fun getSite(siteId: String, callback: GetSiteCallback)

    fun saveSite(site: Site)

    fun updateSite(site: Site)

    fun deleteAllSites()

    fun deleteSite(siteId: String)
}
