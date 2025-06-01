package com.bernaferrari.changedetection.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val sitesDao: SitesDao,
    private val snapsDao: SnapsDao
) : ViewModel() {

    val siteMap: StateFlow<Map<String, Site>> = flow {
        emit(withContext(Dispatchers.IO) {
            sitesDao.sites.associateBy { it.id }
        })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val versionsCount: StateFlow<Int> = snapsDao.countNumberOfChanges()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val pagedSnaps: Flow<PagingData<Snap>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            prefetchDistance = 10,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { snapsDao.getSnapsPagedSource() }
    ).flow.cachedIn(viewModelScope)

    fun removeSnap(snapId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            snapsDao.deleteSnapById(snapId)
        }
    }
}
