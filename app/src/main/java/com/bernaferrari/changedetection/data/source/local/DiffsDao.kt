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

    @Query("SELECT diffId, siteId, timestamp, size FROM diffs WHERE siteId = :id ORDER BY timestamp DESC")
    fun allDiffsBySiteId(id: String): DataSource.Factory<Int, DiffWithoutValue>

    @Query("SELECT * FROM diffs WHERE siteId = :id ORDER BY timestamp DESC LIMIT 25")
    fun getDiffsForSiteWithLimit(id: String): List<Diff>?

    @Query("SELECT count(1) FROM diffs WHERE siteId = :id")
    fun getDiffsCountForUser(id: String): Int

    @Query("SELECT * FROM diffs WHERE diffId = :id")
    fun getDiffById(id: String): Diff?

    @Query("SELECT diffId, siteId, timestamp, size FROM diffs WHERE siteId = :diffId ORDER BY timestamp DESC LIMIT 1")
    fun getLastDiffWithoutValueBySiteId(diffId: String): DiffWithoutValue?

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

    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun insertDiff(diff: Diff)

    @Query("DELETE FROM diffs")
    fun deleteTasks()
}
