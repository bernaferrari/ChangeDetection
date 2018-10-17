package com.bernaferrari.changedetection.data.source.local

import androidx.room.*
import com.bernaferrari.changedetection.data.Site

/**
 * Data Access Object for the sites table.
 * Inspired from Architecture Components MVVM sample app
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
     * Select a site by url.
     *
     * @param siteId the site url.
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
     */
    @Update
    fun updateSite(site: Site)

    /**
     * Update the complete status of a site
     *
     * @param siteId    url of the site
     * @param completed status to be updated
     */
    @Query("UPDATE sites SET isSuccessful = :completed WHERE siteId = :siteId")
    fun updateCompleted(siteId: String, completed: Boolean)

    /**
     * Delete a site by url.
     *
     */
    @Query("DELETE FROM sites WHERE siteId = :siteId")
    fun deleteSiteById(siteId: String)

    /**
     * Delete all sites.
     */
    @Query("DELETE FROM sites")
    fun deleteSites()
}
