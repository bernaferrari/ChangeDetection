package com.bernaferrari.changedetection

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import androidx.work.WorkManager
import androidx.work.WorkStatus
import com.bernaferrari.changedetection.data.Site
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.data.source.SitesRepository
import com.bernaferrari.changedetection.data.source.SnapsDataSource
import com.bernaferrari.changedetection.data.source.SnapsRepository
import com.bernaferrari.changedetection.data.source.local.SiteAndLastSnap
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Exposes the data to be used in the site list screen.
 * Inspired from Architecture Components MVVM sample app
 */
class MainViewModel(
    context: Application,
    private val mSnapsRepository: SnapsRepository,
    private val mSitesRepository: SitesRepository
) : AndroidViewModel(context) {

    var shouldSyncWhenAppOpen = true

    fun getOutputStatus(): LiveData<List<WorkStatus>> {
        return WorkManager.getInstance().getStatusesForUniqueWork(WorkerHelper.UNIQUEWORK)
    }

    fun currentTime(): Long = System.currentTimeMillis()

    fun removeSite(site: Site) {
        // CASCADE on Snap will take care of the rest
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
        snap: Snap
    ): MutableLiveData<Boolean> {

        val didItWork = MutableLiveData<Boolean>()
        mSnapsRepository.saveSnap(snap, object : SnapsDataSource.GetSnapsCallback {
            override fun onSnapsLoaded(snap: Snap) {
                didItWork.value = true
            }

            override fun onDataNotAvailable() {
                didItWork.value = false
            }
        })
        return didItWork
    }

    suspend fun getRecentMinimalSnaps(siteId: String): List<Int>? = suspendCoroutine { cont ->
        mSnapsRepository.getMostRecentMinimalSnaps(
            siteId,
            object : SnapsDataSource.GetMinimalSnapCallback {
                override fun onLoaded(intList: List<Int>?) {
                    cont.resume(intList)
                }
            })
    }

    internal fun updateSite(site: Site) {
        mSitesRepository.updateSite(site)
    }

    val items = MutableLiveData<MutableList<SiteAndLastSnap>>()

    fun loadSites(): MutableLiveData<MutableList<SiteAndLastSnap>> {
        items.value = null
        updateItems()
        return items
    }

    fun updateItems() {
        mSitesRepository.getSiteAndLastSnap {
            items.value = it
        }
    }
}
