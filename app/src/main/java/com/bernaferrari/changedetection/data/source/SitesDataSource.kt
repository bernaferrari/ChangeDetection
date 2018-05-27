/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bernaferrari.changedetection.data.source

import com.bernaferrari.changedetection.data.Site
import com.bernaferrari.changedetection.data.source.local.SiteAndLastDiff

/**
 * Main entry point for accessing sites data.
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

    fun getSiteAndLastDiff(callback: (MutableList<SiteAndLastDiff>) -> (Unit))

    fun getSites(callback: LoadSitesCallback)

    fun getSite(siteId: String, callback: GetSiteCallback)

    fun saveSite(site: Site)

    fun deleteAllSites()

    fun deleteSite(siteId: String)
}
