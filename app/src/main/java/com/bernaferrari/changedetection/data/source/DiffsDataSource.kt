package com.bernaferrari.changedetection.data.source

import android.arch.paging.DataSource
import com.bernaferrari.changedetection.data.Diff
import com.bernaferrari.changedetection.data.MinimalDiff

/**
 * Main entry point for accessing sites data.
 * Inspired from Architecture Components MVVM sample app
 */
interface DiffsDataSource {

    interface GetDiffCallback {

        fun onDiffLoaded(diff: Diff)

        fun onDataNotAvailable()
    }

    interface GetPairCallback {

        fun onDiffLoaded(pair: Pair<Diff, Diff>)

        fun onDataNotAvailable()
    }

    interface GetMinimalDiffCallback {
        fun onMinimalDiffLoaded(minimalDiffList: List<Int>?)
    }

    fun getMostRecentMinimalDiffs(siteId: String, callback: GetMinimalDiffCallback)

    fun getDiffForPaging(id: String): DataSource.Factory<Int, MinimalDiff>

    fun getDiff(diffId: String, callback: GetDiffCallback)

    fun getDiffPair(originalId: String, newId: String, callback: GetPairCallback)

    fun saveDiff(diff: Diff, callback: GetDiffCallback)

    fun deleteAllDiffsForSite(siteId: String)

    fun deleteDiff(diffId: String)
}
