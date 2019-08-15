package com.bernaferrari.changedetection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.bernaferrari.changedetection.repo.ContentTypeInfo
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * Exposes the data to be used in the site list screen.
 * Inspired from Architecture Components MVVM sample app
 */
class MainViewModel(
    context: Application,
    private val mSnapsRepository: SnapsRepository,
    private val mSitesRepository: SitesRepository
) : AndroidViewModel(context), CoroutineScope {

    internal var shouldSyncWhenAppOpen = true
    internal var sortAlphabetically = false

    internal val getOutputStatus: LiveData<List<WorkInfo>>
        get() = WorkManager.getInstance().getWorkInfosForUniqueWorkLiveData(WorkerHelper.UNIQUEWORK)

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCleared() {
        job.cancel()
        super.onCleared()
    }

    internal fun removeSite(site: Site) = GlobalScope.launch {
        mSnapsRepository.deleteAllSnaps(site.id)
        mSitesRepository.deleteSite(site.id)
    }

    internal fun pruneSite(siteId: String) = GlobalScope.launch {
        mSnapsRepository.pruneSnaps(siteId)
    }

    suspend fun removeSnapsByType(siteId: String, contentType: String) {
        mSnapsRepository.deleteSnapsForSiteIdAndContentType(siteId, contentType)
    }

    // Called when clicking on fab.
    internal fun saveSite(
        site: Site
    ) = GlobalScope.launch {
        mSitesRepository.saveSite(site)
    }

    // Called when clicking on fab.
//    internal fun saveWebsite(
//        snap: Snap,
//        content: ByteArray
//    ): MutableLiveData<Boolean> {
//
//        val didItWork = MutableLiveData<Boolean>()
//
//        launch(Dispatchers.Main) {
//            val saveSnap = mSnapsRepository.saveSnap(snap, content)
//            didItWork.value = saveSnap is Result.Success
//        }
//
//        return didItWork
//    }

    suspend fun getRecentContentTypes(siteId: String): List<ContentTypeInfo> {
        return mSnapsRepository.getContentTypeInfo(siteId)
    }

    internal fun updateSite(site: Site) = GlobalScope.launch {
        mSitesRepository.updateSite(site)
    }

    private var items = MutableLiveData<MutableList<SiteAndLastSnap>>()

    internal fun loadSites(): MutableLiveData<MutableList<SiteAndLastSnap>> {
        items = MutableLiveData()
        updateItems()
        return items
    }

    internal fun updateItems() = launch(Dispatchers.Main) {
        val list = mutableListOf<SiteAndLastSnap>()
        withContext(Dispatchers.IO) {
            mSitesRepository.getSites()
                .mapTo(list) { SiteAndLastSnap(it, getLastSnap(it.id), false) }
        }
        items.value = list
    }

    private suspend fun getLastSnap(siteId: String): Snap? {
        return mSnapsRepository.getMostRecentSnap(siteId)
    }

    suspend fun getAllSites(): List<Site> = mSitesRepository.getSites()

    suspend fun isAlreadyMonitoringSite(id: String, url: String): Boolean =
        mSitesRepository.getSiteByUrl(url) != null || mSitesRepository.getSiteById(id) != null

//    internal fun getPruningList(context: Context): MutableList<DialogItemSimple> {
//
//        val color = context.getColorFromAttr(R.attr.strongColor)
//        val items = mutableListOf<DialogItemSimple>()
//
//        items += DialogItemSimple(
//            context.getString(R.string.pruning),
//            IconicsDrawable(context, CommunityMaterial.Icon.cmd_content_cut).color(color),
//            LongPress.PRUNING
//        )
//
//        items += DialogItemSimple(
//            context.getString(R.string.remove_all),
//            IconicsDrawable(context, CommunityMaterial.Icon.cmd_delete).color(color),
//            LongPress.ALL
//        )
//
//        return items
//    }
}
