/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.ui.components.AppLoadingIndicator
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ComparisonScreen(
    siteId: String,
    onNavigateUp: () -> Unit,
    showNavigateUp: Boolean = true,
    onSiteEdit: (String) -> Unit,
    onSiteDelete: () -> Unit,
    viewModel: ComparisonViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val siteTitle = uiState.site?.title?.takeIf { it.isNotBlank() }
        ?: uiState.site?.url?.displayDomain()
        ?: stringResource(Res.string.app_name)
    val showUrlSubtitle = uiState.site?.title?.isNotBlank() == true

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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val siteColor = uiState.site?.colors?.first?.let { androidx.compose.ui.graphics.Color(it) }
                            ?: MaterialTheme.colorScheme.primary
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(13.dp),
                            color = siteColor.copy(alpha = 0.16f),
                            contentColor = siteColor,
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = siteTitle.firstOrNull()?.uppercase().orEmpty(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = siteTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            uiState.site?.url?.takeIf { showUrlSubtitle }?.let { url ->
                                Text(
                                    text = url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (showNavigateUp) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.a11y_navigate_back),
                            )
                        }
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }

                    IconButton(onClick = viewModel::reloadSite, enabled = uiState.site != null) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(Res.string.refresh),
                        )
                    }

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
                    AppLoadingIndicator()
                }
            }

            uiState.site == null -> SharedEmptyState(
                icon = Icons.Default.ErrorOutline,
                title = if (uiState.error != null) {
                    stringResource(Res.string.comparison_load_failed)
                } else {
                    stringResource(Res.string.site_not_found)
                },
                description = uiState.error.orEmpty(),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                primaryActionLabel = if (uiState.error != null) stringResource(Res.string.retry) else null,
                onPrimaryAction = if (uiState.error != null) ({ viewModel.loadSiteAndSnaps(siteId) }) else null,
            )

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    when {
                        uiState.snaps.isEmpty() -> EmptySnapshotState(modifier = Modifier.fillMaxSize())

                        uiState.snaps.size == 1 -> SingleSnapshotState(
                            snap = uiState.snaps.first(),
                            content = uiState.singleSnapContent,
                            bytes = uiState.singleSnapBytes,
                            isLoadingContent = uiState.isLoadingDiff,
                            hasError = uiState.error != null,
                            onRetry = { viewModel.loadSiteAndSnaps(siteId) },
                            modifier = Modifier.fillMaxSize(),
                        )

                        else -> DiffFirstContent(
                            site = requireNotNull(uiState.site),
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
                                } else if (
                                    idx + 1 < uiState.snaps.size &&
                                    uiState.snapContent1 != null &&
                                    uiState.snapContent2 != null
                                ) {
                                    viewModel.shareDiff(
                                        snap1 = uiState.snaps[idx + 1],
                                        snap2 = uiState.snaps[idx],
                                        content1 = uiState.snapContent2!!,
                                        content2 = uiState.snapContent1!!,
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
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
                uiState.site?.title?.takeIf { it.isNotBlank() }
                    ?: uiState.site?.url
                    ?: stringResource(Res.string.site_unnamed),
            ),
            icon = Icons.Default.Delete,
            confirmText = stringResource(Res.string.delete),
            isDestructive = true,
        )
    }
}

private fun String.displayDomain(): String = removePrefix("https://")
    .removePrefix("http://")
    .removePrefix("www.")
    .substringBefore("/")
    .ifBlank { this }
