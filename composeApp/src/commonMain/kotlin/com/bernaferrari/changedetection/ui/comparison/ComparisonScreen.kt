/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.bernaferrari.changedetection.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(
    siteId: String,
    onNavigateUp: () -> Unit,
    onSiteEdit: (String) -> Unit,
    onSiteDelete: () -> Unit,
    viewModel: ComparisonViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Delete confirmation state
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(siteId) {
        viewModel.loadSiteAndSnaps(siteId)
    }

    // Auto-select first pair when snaps are loaded
    LaunchedEffect(uiState.snaps) {
        if (uiState.snaps.size > 1 && uiState.selectedPairIndex == null) {
            viewModel.selectPairForComparison(0)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.site?.title?.takeIf { it.isNotBlank() }
                                ?: stringResource(Res.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        uiState.site?.url?.let { url ->
                            Text(
                                text = url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.a11y_navigate_back),
                        )
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }

                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(Res.string.more_options),
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.refresh)) },
                            onClick = {
                                viewModel.reloadSite()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.edit)) },
                            onClick = {
                                onSiteEdit(siteId)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.prune_history)) },
                            onClick = {
                                viewModel.pruneSite()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null) },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.delete)) },
                            onClick = {
                                showDeleteDialog = true
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error,
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
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }

            uiState.site == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = if (uiState.error != null) {
                            stringResource(Res.string.comparison_load_failed)
                        } else {
                            stringResource(Res.string.site_not_found)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    if (uiState.error != null) {
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadSiteAndSnaps(siteId) }) {
                            Text(stringResource(Res.string.retry))
                        }
                    }
                }
            }

            uiState.snaps.isEmpty() -> {
                EmptySnapshotState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }

            uiState.snaps.size == 1 -> {
                SingleSnapshotState(
                    snap = uiState.snaps.first(),
                    content = uiState.singleSnapContent,
                    bytes = uiState.singleSnapBytes,
                    isLoadingContent = uiState.isLoadingDiff,
                    hasError = uiState.error != null,
                    onRetry = { viewModel.loadSiteAndSnaps(siteId) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }

            else -> {
                DiffFirstContent(
                    snaps = uiState.snaps,
                    selectedPairIndex = uiState.selectedPairIndex ?: 0,
                    content1 = uiState.snapContent1,
                    content2 = uiState.snapContent2,
                    bytes1 = uiState.snapBytes1,
                    bytes2 = uiState.snapBytes2,
                    imageDiff = uiState.imageDiff,
                    isLoadingDiff = uiState.isLoadingDiff,
                    onSelectPair = { viewModel.selectPairForComparison(it) },
                    onRetry = {
                        viewModel.selectPairForComparison(uiState.selectedPairIndex ?: 0)
                    },
                    onShare = {
                        val idx = uiState.selectedPairIndex ?: 0
                        val newerSnap = uiState.snaps.getOrNull(idx)
                        val olderSnap = uiState.snaps.getOrNull(idx + 1)
                        val imageDiff = uiState.imageDiff
                        if (
                            newerSnap != null &&
                            olderSnap != null &&
                            newerSnap.isImageSnapshot() &&
                            olderSnap.isImageSnapshot() &&
                            imageDiff != null
                        ) {
                            viewModel.shareImageDiff(
                                newerSnap = newerSnap,
                                olderSnap = olderSnap,
                                imageDiff = imageDiff,
                            )
                        } else if (idx + 1 < uiState.snaps.size && uiState.snapContent1 != null && uiState.snapContent2 != null) {
                            viewModel.shareDiff(
                                snap1 = uiState.snaps[idx + 1],
                                snap2 = uiState.snaps[idx],
                                content1 = uiState.snapContent2!!,
                                content2 = uiState.snapContent1!!,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        com.bernaferrari.changedetection.ui.components.ConfirmDialog(
            visible = true,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.deleteSite()
                onSiteDelete()
            },
            title = stringResource(Res.string.delete_site_title),
            message = stringResource(
                Res.string.delete_site_message,
                uiState.site?.title?.takeIf { it.isNotBlank() } ?: uiState.site?.url ?: "this site",
            ),
            icon = Icons.Default.Delete,
            confirmText = stringResource(Res.string.delete),
            isDestructive = true,
        )
    }
}
