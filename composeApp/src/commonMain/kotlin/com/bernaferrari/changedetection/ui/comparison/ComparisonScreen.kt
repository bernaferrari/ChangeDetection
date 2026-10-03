/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.ui.components.AppLoadingIndicator
import org.koin.compose.viewmodel.koinViewModel
import com.bernaferrari.changedetection.util.displayPageAddress
import com.bernaferrari.changedetection.util.displayPageTitle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ComparisonScreen(
    siteId: String,
    onNavigateUp: () -> Unit,
    showNavigateUp: Boolean = true,
    initialSnapId: String? = null,
    showHistoryInitially: Boolean = false,
    onSiteEdit: (String) -> Unit,
    onSiteDelete: () -> Unit,
    viewModel: ComparisonViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val siteTitle = uiState.site?.displayPageTitle()
        ?: stringResource(Res.string.app_name)
    val showUrlSubtitle = uiState.site?.title?.isNotBlank() == true

    // Delete confirmation state
    var showDeleteDialog by remember { mutableStateOf(false) }
    // Prune confirmation state
    var showPruneDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    LaunchedEffect(siteId, initialSnapId) {
        viewModel.openComparison(siteId, initialSnapId)
    }
    val deleteFailedMessage = stringResource(Res.string.delete_site_failed)
    val prunedMessage = stringResource(Res.string.pruned_snapshots)
    val pruneFailedMessage = stringResource(Res.string.prune_failed)
    val readFailedMessage = stringResource(Res.string.comparison_read_failed)
    val readChangedMessage = stringResource(Res.string.comparison_read_changed)
    val markedReadMessage = stringResource(Res.string.comparison_marked_read)
    val undoLabel = stringResource(Res.string.undo)
    val latestLabel = stringResource(Res.string.view_latest_saved_version)
    LaunchedEffect(viewModel) {
        viewModel.actionMessages.collect { message ->
            when (message) {
                "read_failed" -> snackbarHostState.showSnackbar(readFailedMessage)

                "read_changed" -> {
                    if (snackbarHostState.showSnackbar(readChangedMessage, latestLabel, withDismissAction = true, duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) {
                        viewModel.loadSiteAndSnaps(siteId)
                    }
                }

                "pruned" -> snackbarHostState.showSnackbar(prunedMessage, duration = SnackbarDuration.Short)

                "prune_failed" -> snackbarHostState.showSnackbar(
                    pruneFailedMessage,
                    duration = SnackbarDuration.Short,
                )
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = siteTitle,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            uiState.site?.url?.takeIf { showUrlSubtitle }?.let { url ->
                                Text(
                                    text = url.displayPageAddress(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
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
                                enabled = uiState.site != null,
                                leadingIcon = { Icon(Icons.Default.Refresh, null) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.open_in_browser)) },
                                onClick = {
                                    uiState.site?.url?.let { runCatching { uriHandler.openUri(it) } }
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.OpenInBrowser, contentDescription = null) },
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
                                    showPruneDialog = true
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
                uiState.site?.takeIf { !it.isSuccessful }?.let {
                    com.bernaferrari.changedetection.ui.components.CheckFailureNotice(it.lastFailureCode)
                }
            }
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                if (uiState.showsLatestComparison) {
                    ComparisonReadAction(
                        isRead = uiState.site?.isRead == true,
                        enabled = uiState.canMarkRead,
                        onMarkRead = {
                            scope.launch {
                                val receipt = viewModel.markVisibleComparisonRead() ?: return@launch
                                snackbarHostState.currentSnackbarData?.dismiss()
                                if (snackbarHostState.showSnackbar(markedReadMessage, actionLabel = undoLabel, withDismissAction = true, duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) {
                                    viewModel.undoRead(receipt)
                                }
                            }
                        },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                description = if (uiState.error != null) stringResource(Res.string.comparison_load_failed_detail) else "",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                primaryActionLabel = if (uiState.error != null) stringResource(Res.string.retry) else null,
                onPrimaryAction = if (uiState.error != null) ({ viewModel.retryComparison() }) else null,
            )

            uiState.requestedSnapshotUnavailable -> SharedEmptyState(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(Res.string.requested_comparison_unavailable),
                description = stringResource(Res.string.requested_comparison_unavailable_description),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                primaryActionLabel = stringResource(Res.string.view_latest_saved_version),
                onPrimaryAction = { viewModel.loadSiteAndSnaps(siteId) },
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
                            onRetry = viewModel::retryComparison,
                            onCheckNow = { viewModel.reloadSite() },
                            modifier = Modifier.fillMaxSize(),
                        )

                        else -> DiffFirstContent(
                            showHistoryInitially = showHistoryInitially,
                            site = requireNotNull(uiState.site),
                            snaps = uiState.snaps,
                            selectedPairIndex = uiState.selectedPairIndex ?: 0,
                            content1 = uiState.snapContent1,
                            content2 = uiState.snapContent2,
                            bytes1 = uiState.snapBytes1,
                            bytes2 = uiState.snapBytes2,
                            imageDiff = uiState.imageDiff,
                            isLoadingDiff = uiState.isLoadingDiff,
                            mixedTypePair = uiState.mixedTypePair,
                            onSelectPair = { viewModel.selectPairForComparison(it) },
                            onRetry = viewModel::retryComparison,
                            onShare = { mode ->
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
                                        readableText = mode == ComparisonMode.Text,
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

    // Delete confirmation dialog — navigates away only after a successful delete.
    if (showDeleteDialog) {
        com.bernaferrari.changedetection.ui.components.ConfirmDialog(
            visible = true,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.deleteSite()
                scope.launch {
                    val outcome = withTimeoutOrNull(5_000) {
                        viewModel.deleteResult.filterNotNull().first()
                    }
                    if (outcome == true) {
                        onSiteDelete()
                    } else {
                        snackbarHostState.showSnackbar(deleteFailedMessage, duration = SnackbarDuration.Short)
                    }
                }
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

    // Prune confirmation dialog
    if (showPruneDialog) {
        com.bernaferrari.changedetection.ui.components.ConfirmDialog(
            visible = true,
            onDismiss = { showPruneDialog = false },
            onConfirm = { viewModel.pruneSite() },
            title = stringResource(Res.string.prune_confirm_title),
            message = stringResource(Res.string.prune_confirm_message),
            icon = Icons.Default.CleaningServices,
            confirmText = stringResource(Res.string.prune_history),
            isDestructive = true,
        )
    }
}
