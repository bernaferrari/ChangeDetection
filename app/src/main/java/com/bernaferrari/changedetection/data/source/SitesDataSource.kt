package com.bernaferrari.changedetection.data.source

import com.bernaferrari.changedetection.data.Site

/**
 * Main entry point for accessing sites data.
 * Inspired from Architecture Components MVVM sample app
 */
interface SitesDataSource {

    fun getSites(callback: ((List<Site>) -> (Unit)))

    fun getSite(siteId: String, callback: ((Site?) -> (Unit)))

    fun saveSite(site: Site)

    fun updateSite(site: Site)

    fun deleteAllSites()

    fun deleteSite(siteId: String)
}
