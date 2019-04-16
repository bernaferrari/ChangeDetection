package com.bernaferrari.changedetection.repo.source

import com.bernaferrari.changedetection.repo.Site
import io.reactivex.Observable

/**
 * Main entry point for accessing sites data.
 * Inspired from Architecture Components MVVM sample app
 */
interface SitesDataSource {

    suspend fun getSites(): List<Site>

    fun getDataWithChanges(): Observable<List<Site>>

    suspend fun getSiteById(siteId: String): Site?

    suspend fun getSiteByUrl(siteUrl: String): Site?

    suspend fun saveSite(site: Site)

    suspend fun updateSite(site: Site)

    suspend fun deleteAllSites()

    suspend fun deleteSite(siteId: String)
}
