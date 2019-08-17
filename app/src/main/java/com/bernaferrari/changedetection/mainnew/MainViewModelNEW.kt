package com.bernaferrari.changedetection.mainnew

import androidx.lifecycle.MutableLiveData
import androidx.work.WorkInfo
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.bernaferrari.base.mvrx.MvRxViewModel
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.jakewharton.rxrelay2.BehaviorRelay
import com.pacoworks.komprehensions.rx2.doSwitchMap
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.squareup.inject.assisted.dagger2.AssistedModule
import dagger.Module
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * Exposes the data to be used in the site list screen.
 * Inspired from Architecture Components MVVM sample app
 */

data class MainState(
    val listOfItems: List<SiteAndLastSnap> = emptyList(),
    val listOfColors: List<ColorGroup> = emptyList(),
    val listOfTags: List<String> = emptyList(),
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

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    val workManagerObserver: BehaviorRelay<List<WorkInfo>> = BehaviorRelay.create()

    val selectedColors: BehaviorRelay<List<ColorGroup>> = BehaviorRelay.createDefault(emptyList())

    val selectedTags: BehaviorRelay<List<String>> = BehaviorRelay.createDefault(emptyList())

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

    private fun updateListOfFilters(items: List<Site>) {
        setState {
            copy(
                listOfTags = pullOutTags(items),
                listOfColors = items.groupBy { it.colors }.keys.toList()
            )
        }
    }

    fun fetchData() = withState {

        val sites = Observables.combineLatest(
            mSitesRepository.getDataWithChanges().doOnNext { updateListOfFilters(it) },
            selectedColors,
            selectedTags,
            workManagerObserver
        ) { items, selectedColors, selectedTags, workInfo ->

            val colorItems = items.takeIf { selectedColors.isNotEmpty() }
                ?.filter { it.colors in selectedColors } ?: items

            val tagColoredItems = colorItems.takeIf { selectedTags.isNotEmpty() }
                ?.filter { site ->
                    val itemTags = site.notes.toLowerCase().split(",")
                    itemTags.any { tag -> tag in selectedTags }
                } ?: colorItems

            runBlocking {
                tagColoredItems.map { site ->
                    SiteAndLastSnap(site, getLastSnap(site.id), workInfo.any { site.id in it.tags })
                }
            }
        }

        doSwitchMap(
            { sites },
            { Observable.interval(0, 60, TimeUnit.SECONDS) }
        ) { list, interval ->
            Observable.just(Pair(list, interval))
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

    private fun pullOutTags(sites: List<Site>): List<String> {
        return mutableListOf<String>().apply {
            for (site in sites) {
                val splitTags = site.notes.toLowerCase()
                    .split(',')
                splitTags
                    .filter { it.isNotEmpty() }
                    .forEach { tag ->
                        if (!this.contains(tag)) {
                            this.add(tag)
                        }
                    }
            }
            sort()
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
            val fragment: MainFragment =
                (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.mainViewModelFactory.create(state)
        }
    }
}

@AssistedModule
@Module(includes = [AssistedInject_AppModule::class])
abstract class AppModule