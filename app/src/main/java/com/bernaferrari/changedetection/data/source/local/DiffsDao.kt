package com.bernaferrari.changedetection.data.source.local

import android.arch.paging.DataSource
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.bernaferrari.changedetection.data.Diff
import com.bernaferrari.changedetection.data.MinimalDiff

/**
 * Data Access Object for the sites table.
 * Inspired from Architecture Components MVVM sample app
 */
@Dao
interface DiffsDao {

    /**
     * Select all minimalDiff metadata (which is everything except for its value) by siteId.
     * This is going to be used by the Paging Library.
     *
     * @param siteId the site id to be filtered.
     * @return all diffs for the siteId.
     */
    @Query("SELECT diffId, siteId, timestamp, size FROM diffs WHERE siteId = :siteId ORDER BY timestamp DESC")
    fun allDiffsBySiteId(siteId: String): DataSource.Factory<Int, MinimalDiff>


    /**
     * Select all minimalDiff by siteId.
     * This is going to be used by the Paging Library.
     *
     * @param siteId the site id to be filtered.
     * @return all diffs for the siteId.
     */
    @Query("SELECT * FROM diffs WHERE siteId = :siteId ORDER BY timestamp DESC")
    fun getAllDiffsBySiteId(siteId: String): List<Diff>?


    /**
     * Select a minimalDiff by id
     *
     * @param diffId the minimalDiff id.
     * @return the minimalDiff with diffId.
     */
    @Query("SELECT * FROM diffs WHERE diffId = :id")
    fun getDiffById(id: String): Diff?

    /**
     * Select the most recent minimalDiff metadata (which is everything except for its value) by siteId
     *
     * @param diffId the minimalDiff id.
     * @return the minimalDiff metadata with diffId.
     */
    @Query("SELECT diffId, siteId, timestamp, size FROM diffs WHERE siteId = :siteId ORDER BY timestamp DESC LIMIT 1")
    fun getLastDiffWithoutValueBySiteId(siteId: String): MinimalDiff?

    /**
     * Select the most recent minimalDiff metadata (which is everything except for its value) by siteId
     *
     * @param siteId the site id.
     * @return a list with the last 100 sizes.
     */
    @Query("SELECT size FROM diffs WHERE siteId = :siteId ORDER BY timestamp DESC LIMIT 100")
    fun getLastDiffsSize(siteId: String): List<Int>?

    /**
     * Select the most recent minimalDiff by siteId
     *
     * @param diffId the minimalDiff id.
     * @return the minimalDiff with diffId.
     */
    @Query("SELECT * FROM diffs WHERE siteId = :siteId ORDER BY timestamp DESC LIMIT 1")
    fun getDiffBySiteId(siteId: String): Diff?

    /**
     * Get all diffs
     *
     * @return all the diffs.
     */
    @Query("SELECT * FROM diffs")
    fun getAllDiffs(): List<Diff>


    /**
     * Delete a minimalDiff by id
     *
     * @param diffId the minimalDiff id.
     */
    @Query("DELETE FROM diffs WHERE diffId = :diffId")
    fun deleteDiffById(diffId: String): Int

    /**
     * Delete all diffs by siteId.
     */
    @Query("DELETE FROM diffs WHERE siteId = :id")
    fun deleteAllDiffsForSite(id: String)

    /**
     * Insert a minimalDiff in the database. If the minimalDiff already exists, replace it.
     *
     * @param diff the minimalDiff to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun insertDiff(diff: Diff)

    /**
     * Delete all diffs.
     */
    @Query("DELETE FROM diffs")
    fun deleteTasks()
}
