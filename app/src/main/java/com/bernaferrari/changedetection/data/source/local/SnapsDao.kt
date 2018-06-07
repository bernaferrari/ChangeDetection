package com.bernaferrari.changedetection.data.source.local

import android.arch.paging.DataSource
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.bernaferrari.changedetection.data.MinimalSnap
import com.bernaferrari.changedetection.data.Snap

/**
 * Data Access Object for the sites table.
 * Inspired from Architecture Components MVVM sample app
 */
@Dao
interface SnapsDao {

    /**
     * Select all snap metadata (which is everything except for its value) by siteId.
     * This is going to be used by the Paging Library.
     *
     * @param siteId the site id to be filtered.
     * @return all snaps for the siteId.
     */
    @Query("SELECT snapId, siteId, timestamp, contentSize, contentType FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC")
    fun getAllSnapsForSiteIdForPaging(siteId: String): DataSource.Factory<Int, MinimalSnap>


    /**
     * Select all snap by siteId.
     * This is going to be used by the Paging Library.
     *
     * @param siteId the site id to be filtered.
     * @return all snaps for the siteId.
     */
    @Query("SELECT * FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC")
    fun getAllSnapsForSiteId(siteId: String): List<Snap>?


    /**
     * Select a snap by id
     *
     * @param snapId the minimalSnap id.
     * @return the minimalSnap with snapId.
     */
    @Query("SELECT * FROM snaps WHERE snapId = :id")
    fun getSnapById(id: String): Snap?

    /**
     * Select the most recent minimalSnap metadata (which is everything except for its value) by siteId
     *
     * @param snapId the minimalSnap id.
     * @return the minimalSnap metadata with snapId.
     */
    @Query("SELECT snapId, siteId, timestamp, contentSize, contentType FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC LIMIT 1")
    fun getLastMinimalSnapForSiteId(siteId: String): MinimalSnap?

    /**
     * Select the most recent snap metadata (which is everything except for its value) by siteId
     *
     * @param siteId the site id.
     * @return a list with the last 100 sizes.
     */
    @Query("SELECT contentSize FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC LIMIT 100")
    fun getLastSnapsSize(siteId: String): List<Int>?

    /**
     * Select the most recent snap value by siteId
     *
     * @param snapId the snap id.
     * @return the snap with snapId.
     */
    @Query("SELECT content FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC LIMIT 1")
    fun getLastSnapValueForSiteId(siteId: String): ByteArray?

    /**
     * Get all snaps
     *
     * @return all the snaps.
     */
    @Query("SELECT * FROM snaps")
    fun getAllSnaps(): List<Snap>


    /**
     * Delete a snap by id
     *
     * @param snapId the snap id.
     */
    @Query("DELETE FROM snaps WHERE snapId = :snapId")
    fun deleteSnapById(snapId: String): Int

    /**
     * Delete all snaps by siteId.
     */
    @Query("DELETE FROM snaps WHERE siteId = :id")
    fun deleteAllSnapsForSite(id: String)

    /**
     * Insert a snap in the database. If the snap already exists, replace it.
     *
     * @param snap the snap to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun insertSnap(snap: Snap)

    /**
     * Delete all snaps.
     */
    @Query("DELETE FROM snaps")
    fun deleteTasks()
}
