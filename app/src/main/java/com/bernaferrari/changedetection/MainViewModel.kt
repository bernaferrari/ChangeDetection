package com.bernaferrari.changedetection

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import androidx.work.WorkManager
import androidx.work.WorkStatus
import com.bernaferrari.changedetection.data.ContentTypeInfo
import com.bernaferrari.changedetection.data.Site
import com.bernaferrari.changedetection.data.SiteAndLastSnap
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.data.source.Result
import com.bernaferrari.changedetection.data.source.SitesRepository
import com.bernaferrari.changedetection.data.source.SnapsRepository
import com.bernaferrari.changedetection.util.launchSilent
import kotlinx.coroutines.experimental.android.UI

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

    fun removeSite(site: Site) = launchSilent {
        mSnapsRepository.deleteAllSnaps(site.id)
        mSitesRepository.deleteSite(site.id)
    }

    fun pruneSite(siteId: String) = launchSilent {
        mSnapsRepository.pruneSnaps(siteId)
    }

    suspend fun removeSnapsByType(siteId: String, contentType: String) {
        mSnapsRepository.deleteSnapsForSiteIdAndContentType(siteId, contentType)
    }

    // Called when clicking on fab.
    internal fun saveSite(
        site: Site
    ) = launchSilent {
        mSitesRepository.saveSite(site)
    }

    // Called when clicking on fab.
    internal fun saveWebsite(
        snap: Snap,
        content: ByteArray
    ): MutableLiveData<Boolean> {

        val didItWork = MutableLiveData<Boolean>()

        launchSilent {
            val saveSnap = mSnapsRepository.saveSnap(snap, content)

            launchSilent(UI) {
                didItWork.value = saveSnap is Result.Success
            }
        }

        return didItWork
    }

    suspend fun getRecentContentTypes(siteId: String): List<ContentTypeInfo> {
        return mSnapsRepository.getContentTypeInfo(siteId)
    }

    internal fun updateSite(site: Site) = launchSilent {
        mSitesRepository.updateSite(site)
    }

    val items = MutableLiveData<MutableList<SiteAndLastSnap>>()

    fun loadSites(): MutableLiveData<MutableList<SiteAndLastSnap>> {
        items.value = null
        updateItems()
        return items
    }

    fun updateItems() = launchSilent {

        mutableListOf<SiteAndLastSnap>().also { list ->
            mSitesRepository.getSites().mapTo(list) { SiteAndLastSnap(it, getLastSnap(it.id)) }

            launchSilent(UI) {
                items.value = list
            }
        }

    }

    private suspend fun getLastSnap(siteId: String): Snap? {
        return mSnapsRepository.getMostRecentSnap(siteId)
    }
}
