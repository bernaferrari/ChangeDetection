package com.bernaferrari.changedetection.data.source.local

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bernaferrari.changedetection.data.Snap

/**
 * Data Access Object for the sites table.
 * Inspired from Architecture Components MVVM sample app
 */
@Dao
interface SnapsDao {

    /**
     * Select all snap metadata (which is everything except for its value) by siteId.
     *
     * @param siteId the site url to be filtered.
     * @return all minimal snaps for the siteId.
     */
    @Query("SELECT * FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC")
    fun getAllSnapsForSiteIdWithLiveData(siteId: String): LiveData<List<Snap>>


    /**
     * Select all snap by siteId.
     * This is going to be used by the Paging Library.
     *
     * @param siteId the site url to be filtered.
     * @return all snaps for the siteId.
     */
    @Query("SELECT * FROM snaps WHERE siteId = :siteId AND contentType LIKE :filter ORDER BY timestamp DESC")
    fun getSnapsForSiteIdForPaging(siteId: String, filter: String): DataSource.Factory<Int, Snap>


    /**
     * Get last few contentTypes.
     * This is going to be to know if the contentType is changing or constant and will allow to
     * go to imageFragment or httpFragment
     *
     * @param siteId the site url to be filtered.
     * @return list with last 3 contentTypes.
     */
    @Query("SELECT count(*) FROM snaps WHERE siteId = :siteId GROUP BY contentType")
    fun getContentTypesCount(siteId: String): List<Int>

    @Query("SELECT contentType FROM snaps WHERE siteId = :siteId GROUP BY contentType")
    fun getContentTypesParams(siteId: String): List<String>

    /**
     * Select all snap by siteId.
     * This is going to be used by the Paging Library.
     *
     * @param siteId the site url to be filtered.
     * @return all snaps for the siteId.
     */
    @Query("SELECT * FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC")
    fun getAllSnapsForSiteId(siteId: String): List<Snap>

    /**
     * Select all snap by siteId.
     * This is going to be used by the Paging Library.
     *
     * @param siteId the site url to be filtered.
     * @return all snaps for the siteId.
     */
    @Query("SELECT * FROM snaps WHERE siteId = :siteId AND contentType = :contentType ORDER BY timestamp DESC")
    fun getAllSnapsForSiteIdAndContentType(siteId: String, contentType: String): List<Snap>

    /**
     * Select all snap by siteId, filtered by the contentType.
     *
     * @param siteId the site url to be filtered.
     * @param filter the filter
     * @return all snaps filtered
     */
    @Query("SELECT * FROM snaps WHERE siteId = :siteId AND contentType LIKE :filter ORDER BY timestamp DESC")
    fun getAllSnapsForSiteIdFilteredWithLiveData(
        siteId: String,
        filter: String
    ): LiveData<List<Snap>>


    /**
     * Select a snap by url
     *
     * @param snapId the snap url.
     * @return the snap with snapId.
     */
    @Query("SELECT * FROM snaps WHERE snapId = :id")
    fun getSnapById(id: String): Snap?


    /**
     * Select the most recent snap metadata (which is everything except for its value) by siteId
     *
     * @param snapId the snap url.
     * @return the snap metadata with snapId.
     */
    @Query("SELECT * FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC LIMIT 1")
    fun getLastSnapForSiteId(siteId: String): Snap?


    /**
     * Select the most recent snap metadata (which is everything except for its value) by siteId
     *
     * @param siteId the site url.
     * @return a list with the last 100 sizes.
     */
    @Query("SELECT contentSize FROM snaps WHERE siteId = :siteId ORDER BY timestamp DESC LIMIT 100")
    fun getLastSnapsSize(siteId: String): List<Int>?


    /**
     * Get all snaps
     *
     * @return all the snaps.
     */
    @Query("SELECT * FROM snaps")
    fun getAllSnaps(): List<Snap>


    /**
     * Delete a snap by url
     *
     * @param snapId the snap url.
     */
    @Query("DELETE FROM snaps WHERE snapId = :snapId")
    fun deleteSnapById(snapId: String)


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
