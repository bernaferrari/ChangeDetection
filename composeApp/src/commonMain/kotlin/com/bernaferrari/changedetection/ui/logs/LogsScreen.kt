/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.logs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.bernaferrari.changedetection.shared.generated.resources.retry
import androidx.paging.LoadState
import androidx.compose.material.icons.Icons
import com.bernaferrari.changedetection.repo.Snap
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import com.bernaferrari.changedetection.ui.components.ConfirmDialog
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.util.BackHandler
import org.koin.compose.viewmodel.koinViewModel
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LogsScreen(
    onSnapClick: (siteId: String, snapId: String) -> Unit,
    onSnapOpenInBrowser: (snapId: String, url: String) -> Unit,
    viewModel: LogsViewModel = koinViewModel(),
) {
    val pagedItems = viewModel.pagedSnaps.collectAsLazyPagingItems()
    val siteMap by viewModel.siteMap.collectAsStateWithLifecycle()
    val versionsCount by viewModel.versionsCount.collectAsStateWithLifecycle()
    val changedSiteCount by viewModel.changedSiteCount.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var searchVisible by rememberSaveable { mutableStateOf(searchQuery.isNotBlank()) }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var snapPendingDeletion by remember { mutableStateOf<Snap?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snapshotDeletedMessage = stringResource(Res.string.snapshot_deleted)
    val snapshotDeleteFailedMessage = stringResource(Res.string.snapshot_delete_failed)
    val undoString = stringResource(Res.string.undo)

    LaunchedEffect(searchVisible) {
        if (searchVisible) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }
    BackHandler(enabled = searchVisible) {
        viewModel.setSearchQuery("")
        searchVisible = false
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                expandedHeight = 96.dp,
                title = {
                    if (searchVisible) {
                        TextField(
                            modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
                            value = searchQuery,
                            onValueChange = viewModel::setSearchQuery,
                            placeholder = { Text(stringResource(Res.string.logs_search_pages)) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(
                                            Icons.Rounded.Clear,
                                            contentDescription = stringResource(Res.string.home_search_clear),
                                        )
                                    }
                                }
                            } else {
                                null
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                        )
                    } else {
                        Text(text = stringResource(Res.string.nav_logs), style = MaterialTheme.typography.headlineLarge)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (searchVisible) viewModel.setSearchQuery("")
                            searchVisible = !searchVisible
                        },
                    ) {
                        Icon(
                            imageVector = if (searchVisible) Icons.Rounded.Clear else Icons.Rounded.Search,
                            contentDescription = stringResource(
                                if (searchVisible) Res.string.home_search_close else Res.string.logs_search_pages,
                            ),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        if (pagedItems.loadState.refresh is LoadState.Loading && pagedItems.itemCount == 0) {
            LogsLoadingState(modifier = Modifier.fillMaxSize().padding(padding))
        } else if (pagedItems.loadState.refresh is LoadState.Error && pagedItems.itemCount == 0) {
            SharedEmptyState(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(Res.string.logs_load_failed),
                description = stringResource(Res.string.logs_load_failed_detail),
                modifier = Modifier.fillMaxSize().padding(padding),
                primaryActionLabel = stringResource(Res.string.retry),
                onPrimaryAction = pagedItems::retry,
            )
        } else if (pagedItems.itemCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                if (searchQuery.isBlank()) {
                    LogsEmptyState()
                } else {
                    SharedEmptyState(
                        icon = Icons.Rounded.SearchOff,
                        title = stringResource(Res.string.logs_search_empty_title),
                        description = stringResource(Res.string.logs_search_empty_subtitle),
                        primaryActionLabel = stringResource(Res.string.home_search_clear),
                        onPrimaryAction = { viewModel.setSearchQuery("") },
                    )
                }
            }
        } else {
            HistoryList(
                pagedItems = pagedItems,
                siteMap = siteMap,
                versionsCount = versionsCount,
                changedSiteCount = changedSiteCount,
                loadPreview = viewModel::loadPreview,
                searchQuery = searchQuery,
                onSnapClick = onSnapClick,
                onSnapOpenInBrowser = onSnapOpenInBrowser,
                onDelete = { snapPendingDeletion = it },
                modifier = Modifier.padding(padding),
            )
        }
    }

    snapPendingDeletion?.let { snap ->
        ConfirmDialog(
            visible = true,
            onDismiss = { snapPendingDeletion = null },
            onConfirm = {
                snapPendingDeletion = null
                scope.launch {
                    if (!viewModel.removeSnap(snap).await()) {
                        snackbarHostState.showSnackbar(snapshotDeleteFailedMessage)
                        return@launch
                    }
                    val result = snackbarHostState.showSnackbar(
                        message = snapshotDeletedMessage,
                        actionLabel = undoString,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoRemoveSnap(snap)
                    } else {
                        viewModel.discardPendingDeletion(snap)
                    }
                }
            },
            title = stringResource(Res.string.delete_snapshot_title),
            message = stringResource(Res.string.delete_snapshot_message),
            icon = Icons.Outlined.Delete,
            confirmText = stringResource(Res.string.delete),
            isDestructive = true,
        )
    }
}
