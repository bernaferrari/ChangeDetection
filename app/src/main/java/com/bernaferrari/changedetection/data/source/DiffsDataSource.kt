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

import android.arch.paging.DataSource
import com.bernaferrari.changedetection.data.Diff
import com.bernaferrari.changedetection.data.DiffWithoutValue

/**
 * Main entry point for accessing sites data.
 */
interface DiffsDataSource {

    interface LoadDiffsCallback {

        fun onDiffsLoaded(diffs: List<Diff>)

        fun onDataNotAvailable()
    }

    interface GetDiffCallback {

        fun onDiffLoaded(diff: Diff)

        fun onDataNotAvailable()
    }

    interface GetPairCallback {

        fun onDiffLoaded(pair: Pair<Diff, Diff>)

        fun onDataNotAvailable()
    }

    fun getCheese(id: String): DataSource.Factory<Int, DiffWithoutValue>

    fun getDiff(diffId: String, callback: GetDiffCallback)

    fun getDiffStorage(originalId: String, newId: String, callback: GetPairCallback)

    fun saveDiff(diff: Diff, callback: GetDiffCallback)

    fun deleteAllDiffsForSite(siteId: String)

    fun deleteDiff(diffId: String)
}
