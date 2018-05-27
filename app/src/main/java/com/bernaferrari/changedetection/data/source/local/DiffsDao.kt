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

import android.arch.paging.DataSource
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.bernaferrari.changedetection.data.Diff
import com.bernaferrari.changedetection.data.DiffWithoutValue

/**
 * Data Access Object for the sites table.
 */
@Dao
interface DiffsDao {

    @Query("SELECT * FROM diffs WHERE siteId = :id ORDER BY timestamp DESC")
    fun allCheesesByName(id: String): DataSource.Factory<Int, DiffWithoutValue>

    // Arbitrary limit. If each page is 500kb, 25*500kb = 12.5mb. More than that might cause OutOfMemoryError.
    // If you need more, this might be a great use for Paging library. Issues and PRs are welcome.
    @Query("SELECT * FROM diffs WHERE siteId = :id ORDER BY timestamp DESC LIMIT 25")
    fun getDiffsForSiteWithLimit(id: String): List<Diff>?

    @Query("SELECT count(1) FROM diffs WHERE siteId = :id")
    fun getDiffsCountForUser(id: String): Int

    /**
     * Select a site by id.
     *
     * @param taskId the site id.
     * @return the site with taskId.
     */
    @Query("SELECT * FROM diffs WHERE diffId = :id")
    fun getDiffById(id: String): Diff?

    @Query("SELECT * FROM diffs WHERE siteId = :diffId ORDER BY timestamp DESC LIMIT 1")
    fun getLastDiffWithoutValueBySiteId(diffId: String): DiffWithoutValue?

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

    @Query("SELECT * FROM diffs LIMIT 50")
    fun getAllDiffsWithLimit(): List<Diff>

    @Query("DELETE FROM diffs WHERE siteId = :id")
    fun deleteAllDiffsForSite(id: String)

    @Query("DELETE FROM diffs WHERE diffId = :id")
    fun deleteDiffById(id: String): Int

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
}
