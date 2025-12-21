/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */

package com.bernaferrari.changedetection.repo.source

import com.bernaferrari.changedetection.domain.model.Site
import com.bernaferrari.changedetection.domain.repository.SitesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapter that bridges the existing app-level SitesDataSource
 * to the new KMP SitesRepository interface.
 *
 * This allows the KMP shared layer to use the existing Room database
 * through the established app-level data source pattern.
 */
@Singleton
class KmpSitesRepositoryAdapter @Inject constructor(
    private val sitesDataSource: SitesDataSource
) : SitesRepository {

    override fun getAllSites(): Flow<List<Site>> {
        return sitesDataSource.getDataWithChanges()
            .map { appSites ->
                appSites.map { it.toKmpSite() }
            }
    }

    override suspend fun getSiteById(id: String): Site? {
        val appSite = sitesDataSource.getSiteById(id)
        return appSite?.toKmpSite()
    }

    override suspend fun insertOrUpdateSite(site: Site) {
        val appSite = site.toAppSite()
        sitesDataSource.updateSite(appSite)
    }

    override suspend fun deleteSite(id: String) {
        sitesDataSource.deleteSite(id)
    }

    override suspend fun deleteAllSites() {
        sitesDataSource.deleteAllSites()
    }

    override fun searchSites(query: String): Flow<List<Site>> {
        return getAllSites()
            .map { sites ->
                sites.filter { site ->
                    site.title?.contains(query, ignoreCase = true) == true ||
                    site.url.contains(query, ignoreCase = true)
                }
            }
    }

    override fun getSitesByTag(tag: String): Flow<List<Site>> {
        return getAllSites()
            .map { sites ->
                sites.filter { site ->
                    site.tags.contains(tag, ignoreCase = true)
                }
            }
    }

    /**
     * Adapter: App Site entity → KMP Site model
     * Maps app-level Site to KMP domain Site
     */
    private fun com.bernaferrari.changedetection.repo.Site.toKmpSite(): Site {
        return Site(
            id = this.id,
            title = this.title,
            url = this.url,
            timestamp = this.timestamp,
            tags = this.notes,
            colors = this.colors,
            isSyncEnabled = this.isSyncEnabled,
            isNotificationEnabled = this.isNotificationEnabled,
            threshold = this.threshold,
            isBrowser = this.isBrowser,
            cssSelector = this.cssSelector,
            elementXPath = this.elementXPath,
            elementMonitoring = this.elementMonitoring,
            userAgent = this.userAgent,
            followRedirects = this.followRedirects,
            requestTimeoutMs = this.requestTimeoutMs,
            notes = this.notes
        )
    }

    /**
     * Adapter: KMP Site model → App Site entity
     * Maps KMP domain Site to app-level Site
     */
    private fun Site.toAppSite(): com.bernaferrari.changedetection.repo.Site {
        return com.bernaferrari.changedetection.repo.Site(
            id = this.id,
            title = this.title,
            url = this.url,
            timestamp = this.timestamp,
            isSuccessful = true,
            isBrowser = this.isBrowser,
            threshold = this.threshold,
            isRead = false,
            isSyncEnabled = this.isSyncEnabled,
            isNotificationEnabled = this.isNotificationEnabled,
            notes = this.tags, // Map tags back to notes field
            colors = this.colors,
            constraints = com.bernaferrari.changedetection.repo.ConstraintsRequired(false, false, false, false),
            browserMode = this.isBrowser,
            userAgent = this.userAgent,
            followRedirects = this.followRedirects,
            requestTimeoutMs = this.requestTimeoutMs,
            customHeadersJson = "",
            requiresAuthentication = false,
            authenticationMethod = com.bernaferrari.changedetection.repo.AuthenticationMethod.NONE,
            encryptedCredentials = "",
            cookieData = "",
            oauthTokenData = "",
            sessionPersistence = true,
            cssSelector = this.cssSelector,
            elementXPath = this.elementXPath,
            elementMonitoring = this.elementMonitoring,
            multipleElementMonitoring = false,
            elementHighlighting = false,
            smartMonitoringEnabled = false,
            zapRulesJson = "",
            customFiltersJson = "",
            contentFocusMode = com.bernaferrari.changedetection.repo.ContentFocusMode.AUTO,
            noiseFiltering = true,
            contentNormalization = true,
            syncPriority = com.bernaferrari.changedetection.repo.SyncPriority.NORMAL,
            backgroundSyncEnabled = true,
            syncFrequencyMinutes = 60,
            useProxy = false,
            proxyConfigJson = "",
            retryAttempts = 3,
            retryDelayMs = 5000L
        )
    }
}
