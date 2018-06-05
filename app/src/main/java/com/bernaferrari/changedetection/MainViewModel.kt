package com.bernaferrari.changedetection

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import androidx.work.WorkManager
import androidx.work.WorkStatus
import com.bernaferrari.changedetection.data.Diff
import com.bernaferrari.changedetection.data.Site
import com.bernaferrari.changedetection.data.source.DiffsDataSource
import com.bernaferrari.changedetection.data.source.DiffsRepository
import com.bernaferrari.changedetection.data.source.SitesRepository
import com.bernaferrari.changedetection.data.source.local.SiteAndLastDiff
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Exposes the data to be used in the site list screen.
 * Inspired from Architecture Components MVVM sample app
 */
class MainViewModel(
    context: Application,
    private val mDiffsRepository: DiffsRepository,
    private val mSitesRepository: SitesRepository
) : AndroidViewModel(context) {

    var shouldSyncWhenAppOpen = true

    fun getOutputStatus(): LiveData<List<WorkStatus>> {
        return WorkManager.getInstance().getStatusesForUniqueWork(WorkerHelper.UNIQUEWORK)
    }

    fun currentTime(): Long = System.currentTimeMillis()

    fun removeSite(site: Site) {
        // CASCADE on Diff will take care of the rest
        mSitesRepository.deleteSite(site.id)
    }

    // Called when clicking on fab.
    internal fun saveSite(
        title: String,
        url: String,
        timestamp: Long,
        colors: Pair<Int, Int>
    ): Site {
        val site = Site(title, url, timestamp, colors)

        mSitesRepository.saveSite(site)
        return site
    }

    // Called when clicking on fab.
    internal fun saveWebsite(
        diff: Diff
    ): MutableLiveData<Boolean> {

        val didItWork = MutableLiveData<Boolean>()
        mDiffsRepository.saveDiff(diff, object : DiffsDataSource.GetDiffCallback {
            override fun onDiffLoaded(diff: Diff) {
                didItWork.value = true
            }

            override fun onDataNotAvailable() {
                didItWork.value = false
            }
        })
        return didItWork
    }

    suspend fun getRecentMinimalDiffs(siteId: String): List<Int>? = suspendCoroutine { cont ->
        mDiffsRepository.getMostRecentMinimalDiffs(siteId,
            object : DiffsDataSource.GetMinimalDiffCallback {
                override fun onMinimalDiffLoaded(minimalDiffList: List<Int>?) {
                    cont.resume(minimalDiffList)
                }
            })
    }

    internal fun updateSite(site: Site) {
        mSitesRepository.updateSite(site)
    }

    val items = MutableLiveData<MutableList<SiteAndLastDiff>>()

    fun loadSites(): MutableLiveData<MutableList<SiteAndLastDiff>> {
        items.value = null
        updateItems()
        return items
    }

    fun updateItems() {
        mSitesRepository.getSiteAndLastDiff {
            items.value = it
        }
    }
}
