package com.bernaferrari.changedetection.logs

import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.bernaferrari.base.mvrx.MvRxViewModel
import com.bernaferrari.changedetection.mainnew.MainState
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * initialState *must* be implemented as a constructor parameter.
 */
class LogsRxViewModel @AssistedInject constructor(
    @Assisted initialState: MainState,
    private val mAppsDao: SitesDao,
    private val mSnapsDao: SnapsDao
) : MvRxViewModel<MainState>(initialState) {

    suspend fun getSiteList(): Map<String, Site> = withContext(Dispatchers.IO) {
        mutableMapOf<String, Site>().apply {
            mAppsDao.sites.forEach { this[it.id] = it }
        }
    }

    fun removeSnap(snapId: String) = GlobalScope.launch(Dispatchers.IO) {
        mSnapsDao.deleteSnapById(snapId)
    }

    fun getVersionCount(): LiveData<Int> = mSnapsDao.countNumberOfChanges()

    fun pagedVersion(): LiveData<PagedList<Snap>> {

        val myPagingConfig = PagedList.Config.Builder()
            .setPageSize(20)
            .setPrefetchDistance(60)
            .setEnablePlaceholders(true)
            .build()

        return LivePagedListBuilder<Int, Snap>(
            mSnapsDao.getSnapsPaged(),
            myPagingConfig
        ).build()
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: MainState): LogsRxViewModel
    }

    companion object : MvRxViewModelFactory<LogsRxViewModel, MainState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: MainState
        ): LogsRxViewModel? {
            val fragment: LogsFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.logsViewModelFactory.create(state)
        }
    }
}
