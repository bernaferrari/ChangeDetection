/*
 * Copyright 2017, The Android Open Source Project
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

package com.bernaferrari.changedetection.data.source.local

import android.arch.persistence.room.*
import com.bernaferrari.changedetection.data.Site

/**
 * Data Access Object for the sites table.
 */
@Dao
interface SitesDao {

    /**
     * Select all sites from the sites table.
     *
     * @return all sites.
     */
    @get:Query("SELECT * FROM sites")
    val sites: List<Site>

    /**
     * Select a site by id.
     *
     * @param siteId the site id.
     * @return the site with siteId.
     */
    @Query("SELECT * FROM sites WHERE siteId = :siteId")
    fun getSiteById(siteId: String): Site?

    /**
     * Insert a site in the database. If the site already exists, replace it.
     *
     * @param site the site to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSite(site: Site)

    /**
     * Update a site.
     *
     * @param site site to be updated
     * @return the number of sites updated. This should always be 1.
     */
    @Update
    fun updateSite(site: Site): Int

    /**
     * Update the complete status of a site
     *
     * @param siteId    id of the site
     * @param completed status to be updated
     */
    @Query("UPDATE sites SET successful = :completed WHERE siteId = :siteId")
    fun updateCompleted(siteId: String, completed: Boolean)

    /**
     * Delete a site by id.
     *
     * @return the number of sites deleted. This should always be 1.
     */
    @Query("DELETE FROM sites WHERE siteId = :siteId")
    fun deleteSiteById(siteId: String): Int

    /**
     * Delete all sites.
     */
    @Query("DELETE FROM sites")
    fun deleteSites()
}
