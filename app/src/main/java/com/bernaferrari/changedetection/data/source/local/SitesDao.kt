package com.bernaferrari.changedetection.data.source.local

import android.arch.persistence.room.*
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
    @Query("UPDATE sites SET isSuccessful = :completed WHERE siteId = :siteId")
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
