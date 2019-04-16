package com.bernaferrari.changedetection.mainnew

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.WorkerHelper
import com.bernaferrari.changedetection.core.MvRxViewModel
import com.bernaferrari.changedetection.extensions.doSwitchMap
import com.bernaferrari.changedetection.extensions.getColorFromAttr
import com.bernaferrari.changedetection.groupie.DialogItemSimple
import com.bernaferrari.changedetection.groupie.MainCardItem
import com.bernaferrari.changedetection.repo.ContentTypeInfo
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.Result
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.util.LongPress
import com.jakewharton.rxrelay2.PublishRelay
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.squareup.inject.assisted.dagger2.AssistedModule
import dagger.Module
import io.reactivex.Observable
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * Exposes the data to be used in the site list screen.
 * Inspired from Architecture Components MVVM sample app
 */

data class MainState(
    val listOfItems: List<SiteAndLastSnap> = emptyList(),
    val isLoading: Boolean = true,
    val interval: Long = 0
) : MvRxState

class MainViewModelNEW @AssistedInject constructor(
    @Assisted initialState: MainState,
    private val mSnapsRepository: SnapsRepository,
    private val mSitesRepository: SitesRepository
) : MvRxViewModel<MainState>(initialState), CoroutineScope {

    internal var shouldSyncWhenAppOpen = true
    internal var sortAlphabetically = false

    internal val getOutputStatus: LiveData<List<WorkInfo>>
        get() = WorkManager.getInstance().getWorkInfosForUniqueWorkLiveData(WorkerHelper.UNIQUEWORK)

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    init {
        fetchData()
    }

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
                item.site.takeIf { it.isNotificationEnabled }
                    ?.let { context.getString(R.string.notification_disable) }
                        ?: context.getString(R.string.notification_enable),
                IconicsDrawable(
                    context,
                    item.site.takeIf { it.isNotificationEnabled }
                        ?.let { CommunityMaterial.Icon.cmd_bell_off }
                            ?: CommunityMaterial.Icon.cmd_bell)
                    .color(color),
                LongPress.IS_NOTIFICATION_ENABLED
            )
        }

        dialogItems += DialogItemSimple(
            item.site.takeIf { it.isSyncEnabled }
                ?.let { context.getString(R.string.sync_disable) }
                    ?: context.getString(R.string.sync_enable),
            IconicsDrawable(
                context,
                item.site.takeIf { it.isSyncEnabled }
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

    val outputStatus: LiveData<List<WorkInfo>>
        get() = WorkManager.getInstance().getWorkInfosByTagLiveData("output")

    val live: PublishRelay<List<WorkInfo>> = PublishRelay.create()

    fun fetchData() = withState {
        doSwitchMap(
            { mSitesRepository.getExperimental().toObservable() },
            { live },
            { _, _ -> Observable.interval(0, 60, TimeUnit.SECONDS) }
        ) { list, workinfo, interval ->

            val ls = runBlocking {
                list.map { site ->
                    SiteAndLastSnap(site, getLastSnap(site.id), workinfo.any { site.id in it.tags })
                }
            }
            Observable.just(Pair(ls, interval))
        }
            .doOnSubscribe { setState { copy(isLoading = true) } }
            .execute {
                copy(
                    listOfItems = it()?.first ?: emptyList(),
                    interval = it()?.second ?: 0,
                    isLoading = false
                )
            }
    }


    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: MainState): MainViewModelNEW
    }

    companion object : MvRxViewModelFactory<MainViewModelNEW, MainState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: MainState
        ): MainViewModelNEW? {
            val fragment: MainFragmentNEW =
                (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.mainViewModelFactory.create(state)
        }
    }
}

@AssistedModule
@Module(includes = [AssistedInject_AppModule::class])
abstract class AppModule