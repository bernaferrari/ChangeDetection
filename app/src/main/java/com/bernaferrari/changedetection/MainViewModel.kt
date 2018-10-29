package com.bernaferrari.changedetection

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkManager
import androidx.work.WorkStatus
import com.bernaferrari.changedetection.data.ContentTypeInfo
import com.bernaferrari.changedetection.data.Site
import com.bernaferrari.changedetection.data.SiteAndLastSnap
import com.bernaferrari.changedetection.data.Snap
import com.bernaferrari.changedetection.data.source.Result
import com.bernaferrari.changedetection.data.source.SitesRepository
import com.bernaferrari.changedetection.data.source.SnapsRepository
import com.bernaferrari.changedetection.extensions.getColorFromAttr
import com.bernaferrari.changedetection.groupie.DialogItemSimple
import com.bernaferrari.changedetection.groupie.MainCardItem
import com.bernaferrari.changedetection.util.LongPress
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
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

    internal val getOutputStatus: LiveData<List<WorkStatus>>
        get() = WorkManager.getInstance().getStatusesForUniqueWorkLiveData(WorkerHelper.UNIQUEWORK)

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
    internal fun saveWebsite(
        snap: Snap,
        content: ByteArray
    ): MutableLiveData<Boolean> {

        val didItWork = MutableLiveData<Boolean>()

        launch(Dispatchers.Main) {
            val saveSnap = mSnapsRepository.saveSnap(snap, content)
            didItWork.value = saveSnap is Result.Success
        }

        return didItWork
    }

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
            mSitesRepository.getSites().mapTo(list) { SiteAndLastSnap(it, getLastSnap(it.id)) }
        }
        items.value = list
    }

    private suspend fun getLastSnap(siteId: String): Snap? {
        return mSnapsRepository.getMostRecentSnap(siteId)
    }

    internal fun getPruningList(context: Context): MutableList<DialogItemSimple> {

        val color = context.getColorFromAttr(R.attr.strongColor)
        val items = mutableListOf<DialogItemSimple>()

        items += DialogItemSimple(
            context.getString(R.string.pruning),
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_content_cut).color(color),
            LongPress.PRUNING
        )

        items += DialogItemSimple(
            context.getString(R.string.remove_all),
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_delete).color(color),
            LongPress.ALL
        )

        return items
    }

    fun generateLongPressList(context: Context, item: MainCardItem): MutableList<DialogItemSimple> {

        val color = item.site.colors.second
        val dialogItems = mutableListOf<DialogItemSimple>()

        dialogItems += DialogItemSimple(
            context.getString(R.string.edit),
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_pencil).color(color),
            LongPress.EDIT
        )

        dialogItems += DialogItemSimple(
            context.getString(R.string.open_in_browser),
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_google_chrome).color(color),
            LongPress.OPEN_BROWSER
        )

        // if item is disabled, makes no sense to enable/disable the notifications
        if (item.site.isSyncEnabled) {
            dialogItems += DialogItemSimple(
                item.site.takeIf { it.isNotificationEnabled == true }
                    ?.let { context.getString(R.string.notification_disable) }
                        ?: context.getString(R.string.notification_enable),
                IconicsDrawable(
                    context,
                    item.site.takeIf { it.isNotificationEnabled == true }
                        ?.let { CommunityMaterial.Icon.cmd_bell_off }
                            ?: CommunityMaterial.Icon.cmd_bell)
                    .color(color),
                LongPress.IS_NOTIFICATION_ENABLED
            )
        }

        dialogItems += DialogItemSimple(
            item.site.takeIf { it.isSyncEnabled == true }
                ?.let { context.getString(R.string.sync_disable) }
                    ?: context.getString(R.string.sync_enable),
            IconicsDrawable(
                context,
                item.site.takeIf { it.isSyncEnabled == true }
                    ?.let { CommunityMaterial.Icon.cmd_sync_off }
                        ?: CommunityMaterial.Icon.cmd_sync)
                .color(color),
            LongPress.IS_SYNC_ENABLED
        )

        dialogItems += DialogItemSimple(
            context.getString(R.string.remove_more),
            IconicsDrawable(context, CommunityMaterial.Icon.cmd_delete).color(color),
            LongPress.REMOVE
        )

        return dialogItems
    }
}
