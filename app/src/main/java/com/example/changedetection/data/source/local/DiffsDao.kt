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

package com.example.changedetection.data.source.local

import android.arch.persistence.room.*
import com.example.changedetection.data.Diff

/**
 * Data Access Object for the sites table.
 */
@Dao
interface DiffsDao {

    @Transaction
    @Query("SELECT * FROM sites WHERE siteId = :id LIMIT 1")
    fun getDiffsForUser(id: String): UserAndAllPets?

    @Query("SELECT count(1) FROM diffs WHERE siteId = :id")
    fun getDiffsCountForUser(id: String): Int

    /**
     * Select a site by id.
     *
     * @param taskId the site id.
     * @return the site with taskId.
     */
    @Query("SELECT * FROM diffs WHERE siteId = :id ORDER BY timestamp DESC LIMIT 1")
    fun getDiffBySiteId(id: String): Diff?

    @Query("SELECT * FROM diffs")
    fun getAllDiffs(): List<Diff>

    @Query("DELETE FROM diffs WHERE siteId = :id")
    fun deleteAllDiffsForSite(id: String)

    /**
     * Insert a diff in the database. If the diff already exists, replace it.
     *
     * @param site the site to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun insertDiff(diff: Diff)

    /**
     * Delete all diffs.
     */
    @Query("DELETE FROM diffs")
    fun deleteTasks()

//    /**
//     * Select all sites from the sites table.
//     *
//     * @return all sites.
//     */
//    @get:Query("SELECT * FROM Tasks")
//    val sites: List<Site>
//
//    /**
//     * Select a site by id.
//     *
//     * @param taskId the site id.
//     * @return the site with taskId.
//     */
//    @Query("SELECT * FROM Tasks WHERE entryid = :taskId")
//    fun getTaskById(taskId: String): Site?
//
//    /**
//     * Insert a site in the database. If the site already exists, replace it.
//     *
//     * @param site the site to be inserted.
//     */
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    fun insertTask(site: Site)
//
//    /**
//     * Update a site.
//     *
//     * @param site site to be updated
//     * @return the number of sites updated. This should always be 1.
//     */
//    @Update
//    fun updateTask(site: Site): Int
//
//    /**
//     * Update the complete status of a site
//     *
//     * @param taskId    id of the site
//     * @param completed status to be updated
//     */
//    @Query("UPDATE sites SET completed = :completed WHERE entryid = :taskId")
//    fun updateCompleted(taskId: String, completed: Boolean)
//
//    /**
//     * Delete a site by id.
//     *
//     * @return the number of sites deleted. This should always be 1.
//     */
//    @Query("DELETE FROM Tasks WHERE entryid = :taskId")
//    fun deleteTaskById(taskId: String): Int
//
//
//    /**
//     * Delete all completed sites from the table.
//     *
//     * @return the number of sites deleted.
//     */
//    @Query("DELETE FROM Tasks WHERE completed = 1")
//    fun deleteCompletedTasks(): Int
}
