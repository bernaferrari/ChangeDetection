package com.bernaferrari.changedetection.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val snapsDao: SnapsDao,
    private val sitesDao: SitesDao
) : ViewModel() {

    val snaps: Flow<PagingData<Snap>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            prefetchDistance = 60,
            enablePlaceholders = true
        ),
        pagingSourceFactory = { snapsDao.getSnapsPagedSource() }
    ).flow.cachedIn(viewModelScope)

    val sitesMap: StateFlow<Map<String, Site>> = flow {
        val sites = sitesDao.sites
        emit(sites.associateBy { it.id })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val totalChanges: StateFlow<Int> = snapsDao.countNumberOfChangesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun removeSnap(snapId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            snapsDao.deleteSnapById(snapId)
        }
    }
}
