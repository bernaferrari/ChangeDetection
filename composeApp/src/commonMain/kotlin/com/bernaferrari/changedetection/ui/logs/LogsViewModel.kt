/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import com.bernaferrari.changedetection.util.formatDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

sealed class LogUiModel {
    data class Item(
        val snap: Snap,
    ) : LogUiModel()

    data class Header(
        val date: String,
    ) : LogUiModel()
}

@KoinViewModel
class LogsViewModel(
    private val sitesDao: SitesDao,
    private val snapsDao: SnapsDao,
    private val snapsRepository: SnapsRepository,
) : ViewModel() {
    val siteMap: StateFlow<Map<String, Site>> =
        sitesDao
            .getDataWithChanges()
            .map { sites -> sites.associateBy { it.id } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyMap(),
            )

    val versionsCount: StateFlow<Int> =
        snapsDao
            .countNumberOfChanges()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0,
            )

    val changedSiteCount: StateFlow<Int> =
        snapsDao
            .countSitesWithChanges()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0,
            )

    val pagedSnaps: Flow<PagingData<LogUiModel>> =
        Pager(
            config =
            PagingConfig(
                pageSize = 20,
                prefetchDistance = 10,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { snapsDao.getSnapsPagedSource() },
        ).flow
            .map { pagingData ->
                pagingData
                    .map { LogUiModel.Item(it) }
                    .insertSeparators { before: LogUiModel.Item?, after: LogUiModel.Item? ->
                        val beforeDate = before?.snap?.timestamp?.let(::formatDate)
                        val afterDate = after?.snap?.timestamp?.let(::formatDate)

                        if (afterDate != null && beforeDate != afterDate) {
                            LogUiModel.Header(afterDate)
                        } else {
                            null
                        }
                    }
            }.cachedIn(viewModelScope)

    fun removeSnap(snapId: String) {
        viewModelScope.launch(Dispatchers.Default) {
            snapsRepository.deleteSnap(snapId)
        }
    }
}
