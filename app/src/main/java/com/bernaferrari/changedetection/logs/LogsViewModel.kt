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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val sitesDao: SitesDao,
    private val snapsDao: SnapsDao
) : ViewModel() {

    private val _siteMap = MutableStateFlow<Map<String, Site>>(emptyMap())
    val siteMap: StateFlow<Map<String, Site>> = _siteMap.asStateFlow()

    private val _versionsCount = MutableStateFlow(0)
    val versionsCount: StateFlow<Int> = _versionsCount.asStateFlow()

    val pagedSnaps: Flow<PagingData<Snap>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            prefetchDistance = 60,
            enablePlaceholders = true
        ),
        pagingSourceFactory = { snapsDao.getSnapsPaged() }
    ).flow.cachedIn(viewModelScope)

    init {
        loadSiteMap()
        loadVersionsCount()
    }

    private fun loadSiteMap() {
        viewModelScope.launch {
            val sites = withContext(Dispatchers.IO) {
                sitesDao.sites.associateBy { it.id }
            }
            _siteMap.value = sites
        }
    }

    private fun loadVersionsCount() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                snapsDao.countNumberOfChanges()
            }.observeForever { count ->
                _versionsCount.value = count ?: 0
            }
        }
    }

    fun removeSnap(snapId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            snapsDao.deleteSnapById(snapId)
        }
    }
}
