/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bernaferrari.changedetection.util.formatTime
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import com.bernaferrari.changedetection.ui.kit.friendlyDay
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.ui.components.checkFailureTitle
import com.bernaferrari.changedetection.ui.kit.Callout
import com.bernaferrari.changedetection.ui.kit.GroupRow
import com.bernaferrari.changedetection.ui.kit.friendlyDateTime
import com.bernaferrari.changedetection.ui.kit.name
import com.bernaferrari.changedetection.ui.kit.previewLabel
import com.bernaferrari.changedetection.ui.kit.rememberPairPreview
import com.bernaferrari.changedetection.ui.theme.marks
import com.bernaferrari.changedetection.ui.kit.ConnectedChoice
import com.bernaferrari.changedetection.util.displayPageAddress
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** One reader for every kind of page: what is compared, how to look at it, and how to move through it. */
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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPruneDialog by remember { mutableStateOf(false) }
    var showVersions by rememberSaveable { mutableStateOf(showHistoryInitially) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    LaunchedEffect(siteId, initialSnapId) { viewModel.openComparison(siteId, initialSnapId) }
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

                "read_changed" -> if (snackbarHostState.showSnackbar(readChangedMessage, latestLabel, withDismissAction = true, duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) {
                    viewModel.loadSiteAndSnaps(siteId)
                }

                "pruned" -> snackbarHostState.showSnackbar(prunedMessage, duration = SnackbarDuration.Short)

                "prune_failed" -> snackbarHostState.showSnackbar(pruneFailedMessage, duration = SnackbarDuration.Short)
            }
        }
    }

    val pairIndex = uiState.selectedPairIndex ?: 0
    val newer = uiState.snaps.getOrNull(pairIndex)
    val older = uiState.snaps.getOrNull(pairIndex + 1)
    val reader = remember(siteId, newer?.snapId, older?.snapId, uiState.singleSnap?.snapId) { ReaderState() }
    val isImagePair = newer?.isImageSnapshot() == true && older?.isImageSnapshot() == true
    val isHtmlPair = newer?.isHtmlSnapshot() == true || older?.isHtmlSnapshot() == true
    val modes = when {
        isImagePair -> listOf(ComparisonMode.ImageChanges, ComparisonMode.ImageSwipe, ComparisonMode.ImageDiff)
        isHtmlPair -> listOf(ComparisonMode.Text, ComparisonMode.Html, ComparisonMode.Source)
        else -> listOf(ComparisonMode.Text)
    }
    var mode by remember(isImagePair, isHtmlPair) { mutableStateOf(modes.first()) }
    val markRead: () -> Unit = {
        scope.launch {
            val receipt = viewModel.markVisibleComparisonRead() ?: return@launch
            snackbarHostState.currentSnackbarData?.dismiss()
            if (snackbarHostState.showSnackbar(markedReadMessage, actionLabel = undoLabel, withDismissAction = true, duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) {
                viewModel.undoRead(receipt)
            }
        }
    }
    val canMarkRead = uiState.showsLatestComparison && uiState.site?.isRead == false && uiState.canMarkRead

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.site?.name() ?: stringResource(Res.string.app_name), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        uiState.site?.let {
                            Text(it.url.displayPageAddress(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                navigationIcon = {
                    if (showNavigateUp) IconButton(onClick = onNavigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.a11y_navigate_back)) }
                },
                actions = {
                    val canShare = (isImagePair && uiState.imageDiff != null) || (uiState.snapContent1 != null && uiState.snapContent2 != null)
                    if (canShare && uiState.snaps.size > 1) {
                        IconButton(onClick = { share(viewModel, uiState, mode) }) { Icon(Icons.Outlined.Share, stringResource(Res.string.comparison_share)) }
                    }
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(Res.string.more_options)) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(Res.string.home_check_now)) }, leadingIcon = { Icon(Icons.Outlined.Refresh, null) }, enabled = uiState.site != null, onClick = {
                                viewModel.reloadSite()
                                menu = false
                            })
                            DropdownMenuItem(text = { Text(stringResource(Res.string.open_in_browser)) }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null) }, onClick = {
                                uiState.site?.url?.let { runCatching { uriHandler.openUri(it) } }
                                menu = false
                            })
                            DropdownMenuItem(text = { Text(stringResource(Res.string.edit)) }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = {
                                onSiteEdit(siteId)
                                menu = false
                            })
                            DropdownMenuItem(text = { Text(stringResource(Res.string.prune_history)) }, leadingIcon = { Icon(Icons.Outlined.CleaningServices, null) }, onClick = {
                                showPruneDialog = true
                                menu = false
                            })
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.detail_delete)) },
                                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
                                colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error),
                                onClick = {
                                    showDeleteDialog = true
                                    menu = false
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            val showNav = mode == ComparisonMode.Text && reader.changeCount > 1 && uiState.snaps.size > 1 && uiState.singleSnap == null && !uiState.isLoadingDiff
            AnimatedVisibility(showNav || canMarkRead, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                ReaderBar(reader, showNav, canMarkRead, onMarkRead = markRead)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> LoadingIndicator(Modifier.align(Alignment.Center))

                uiState.site == null -> SharedEmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = stringResource(if (uiState.error != null) Res.string.comparison_load_failed else Res.string.site_not_found),
                    description = if (uiState.error != null) stringResource(Res.string.comparison_load_failed_detail) else "",
                    modifier = Modifier.fillMaxSize(),
                    primaryActionLabel = if (uiState.error != null) stringResource(Res.string.retry) else null,
                    onPrimaryAction = if (uiState.error != null) ({ viewModel.retryComparison() }) else null,
                )

                uiState.requestedSnapshotUnavailable -> SharedEmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = stringResource(Res.string.requested_comparison_unavailable),
                    description = stringResource(Res.string.requested_comparison_unavailable_description),
                    modifier = Modifier.fillMaxSize(),
                    primaryActionLabel = stringResource(Res.string.view_latest_saved_version),
                    onPrimaryAction = { viewModel.loadSiteAndSnaps(siteId) },
                )

                uiState.snaps.isEmpty() -> EmptySnapshotState(Modifier.fillMaxSize())

                uiState.snaps.size == 1 || uiState.singleSnap != null -> Column(Modifier.fillMaxSize()) {
                    val snap = uiState.singleSnap ?: uiState.snaps.first()
                    if (uiState.snaps.size > 1) {
                        GroupRow(
                            title = friendlyDateTime(snap.timestamp),
                            supporting = stringResource(Res.string.label_first_version),
                            onClick = { showVersions = true },
                            modifier = Modifier.widthIn(max = 720.dp).align(Alignment.CenterHorizontally),
                        ) { Icon(Icons.Rounded.UnfoldMore, stringResource(Res.string.comparison_section_snapshots)) }
                    }
                    SingleSnapshotState(
                        snap = snap,
                        content = uiState.singleSnapContent,
                        bytes = uiState.singleSnapBytes,
                        isLoadingContent = uiState.isLoadingDiff,
                        hasError = uiState.error != null,
                        onRetry = viewModel::retryComparison,
                        onCheckNow = { viewModel.reloadSite() },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }

                else -> Column(Modifier.fillMaxSize()) {
                    val site = requireNotNull(uiState.site)
                    if (!site.isSuccessful && site.isSyncEnabled) {
                        Text(
                            checkFailureTitle(site.lastFailureCode),
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Column(Modifier.widthIn(max = 720.dp).align(Alignment.CenterHorizontally)) {
                        VersionStrip(uiState.snaps, pairIndex, onSelect = { viewModel.selectPairForComparison(it) }, onShowAll = { showVersions = true })
                        if (modes.size > 1) {
                            ConnectedChoice(modes, mode, { mode = it }, Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp)) { modeLabel(it) }
                        }
                    }
                    Box(Modifier.fillMaxWidth().weight(1f).testTag("comparison-reader"), contentAlignment = Alignment.TopCenter) {
                        val content1 = uiState.snapContent1
                        val content2 = uiState.snapContent2
                        when {
                            uiState.isLoadingDiff -> LoadingIndicator(Modifier.align(Alignment.Center))

                            isImagePair -> ImageComparisonContent(uiState.snapBytes2, uiState.snapBytes1, uiState.imageDiff, mode, Modifier.fillMaxSize(), onRetry = viewModel::retryComparison)

                            content1 != null && content2 != null -> when (mode) {
                                ComparisonMode.Html -> RenderedHtmlComparison(oldContent = content2, newContent = content1, modifier = Modifier.fillMaxSize())

                                ComparisonMode.Source -> SimpleDiffView(oldContent = content2, newContent = content1, modifier = Modifier.fillMaxSize())

                                else -> ChangeReader(
                                    oldContent = content2,
                                    newContent = content1,
                                    oldIsHtml = older?.isHtmlSnapshot() == true,
                                    newIsHtml = newer?.isHtmlSnapshot() == true,
                                    state = reader,
                                    onShowSource = { mode = ComparisonMode.Source },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }

                            uiState.mixedTypePair -> Callout(
                                title = stringResource(Res.string.comparison_capture_changed_title),
                                body = stringResource(Res.string.comparison_capture_changed_supporting),
                                icon = Icons.Outlined.SwapHoriz,
                                modifier = Modifier.padding(top = 24.dp),
                            )

                            else -> Callout(
                                title = stringResource(Res.string.failed_to_load_content),
                                body = stringResource(Res.string.comparison_load_failed_detail),
                                icon = Icons.Outlined.ErrorOutline,
                                modifier = Modifier.padding(top = 24.dp),
                                actions = { Button(onClick = viewModel::retryComparison) { Text(stringResource(Res.string.retry)) } },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showVersions && uiState.snaps.size > 1) {
        VersionSheet(
            snaps = uiState.snaps,
            selectedIndex = if (uiState.singleSnap != null) -2 else pairIndex,
            selectedSingleSnapId = uiState.singleSnap?.snapId,
            onDismiss = { showVersions = false },
            onSelect = {
                viewModel.selectPairForComparison(it)
                showVersions = false
            },
        )
    }

    if (showDeleteDialog) {
        com.bernaferrari.changedetection.ui.components.ConfirmDialog(
            visible = true,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.deleteSite()
                scope.launch {
                    val outcome = withTimeoutOrNull(5_000) { viewModel.deleteResult.filterNotNull().first() }
                    if (outcome == true) onSiteDelete() else snackbarHostState.showSnackbar(deleteFailedMessage, duration = SnackbarDuration.Short)
                }
            },
            title = stringResource(Res.string.detail_delete_title, uiState.site?.name() ?: ""),
            message = stringResource(Res.string.delete_site_message, uiState.site?.name() ?: stringResource(Res.string.site_unnamed)),
            icon = Icons.Outlined.DeleteOutline,
            confirmText = stringResource(Res.string.delete),
            isDestructive = true,
        )
    }
    if (showPruneDialog) {
        com.bernaferrari.changedetection.ui.components.ConfirmDialog(
            visible = true,
            onDismiss = { showPruneDialog = false },
            onConfirm = { viewModel.pruneSite() },
            title = stringResource(Res.string.prune_confirm_title),
            message = stringResource(Res.string.prune_confirm_message),
            icon = Icons.Outlined.CleaningServices,
            confirmText = stringResource(Res.string.prune_history),
            isDestructive = true,
        )
    }
}

private fun share(viewModel: ComparisonViewModel, state: ComparisonUiState, mode: ComparisonMode) {
    val idx = state.selectedPairIndex ?: 0
    val newer = state.snaps.getOrNull(idx) ?: return
    val older = state.snaps.getOrNull(idx + 1) ?: return
    val imageDiff = state.imageDiff
    if (newer.isImageSnapshot() && older.isImageSnapshot() && imageDiff != null) {
        viewModel.shareImageDiff(newerSnap = newer, olderSnap = older, imageDiff = imageDiff)
    } else if (state.snapContent1 != null && state.snapContent2 != null) {
        viewModel.shareDiff(snap1 = older, snap2 = newer, content1 = state.snapContent2, content2 = state.snapContent1, readableText = mode == ComparisonMode.Text)
    }
}

@Composable
private fun modeLabel(mode: ComparisonMode): String = stringResource(
    when (mode) {
        ComparisonMode.Text, ComparisonMode.ImageChanges -> Res.string.reader_mode_changes
        ComparisonMode.Html -> Res.string.reader_mode_page
        ComparisonMode.Source -> Res.string.reader_mode_source
        ComparisonMode.ImageSwipe -> Res.string.reader_mode_slider
        ComparisonMode.ImageDiff -> Res.string.reader_mode_highlight
        ComparisonMode.ImageBefore -> Res.string.preview_before
        ComparisonMode.ImageAfter -> Res.string.preview_after
    },
)

/**
 * Version history as a filmstrip: each saved version is a thumbnail (the top of the page for screenshots,
 * what changed for text), newest first. Tapping one compares it with the version saved before it.
 */
@Composable
private fun VersionStrip(snaps: List<Snap>, selectedIndex: Int, onSelect: (Int) -> Unit, onShowAll: () -> Unit) {
    val list = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(selectedIndex) { if (selectedIndex in snaps.indices) list.animateScrollToItem((selectedIndex - 1).coerceAtLeast(0)) }
    androidx.compose.foundation.lazy.LazyRow(
        state = list,
        modifier = Modifier.fillMaxWidth().testTag("version-strip"),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // The oldest version has nothing before it, so it is never the "after" of a comparison.
        itemsIndexed(snaps.dropLast(1), key = { _, snap -> snap.snapId }) { index, snap ->
            VersionThumb(snap, snaps[index + 1], selected = index == selectedIndex, latest = index == 0, onClick = { onSelect(index) })
        }
        item("all") {
            Surface(onClick = onShowAll, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceBright, modifier = Modifier.size(width = 72.dp, height = 112.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Outlined.History, null)
                    Text(stringResource(Res.string.detail_see_all), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun VersionThumb(snap: Snap, previous: Snap, selected: Boolean, latest: Boolean, onClick: () -> Unit) {
    val preview = rememberPairPreview(previous.snapId, snap.snapId)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceBright,
        border = androidx.compose.foundation.BorderStroke(2.dp, if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent),
        modifier = Modifier.width(104.dp).semantics { this.selected = selected },
    ) {
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(Modifier.fillMaxWidth().height(60.dp).clip(MaterialTheme.shapes.medium).background(androidx.compose.ui.graphics.Color.White)) {
                if (snap.isImageSnapshot()) {
                    val bytes by produceState<ByteArray?>(null, snap.snapId) {
                        value = runCatching { com.bernaferrari.changedetection.ui.kit.optional<com.bernaferrari.changedetection.repo.source.SnapsRepository>()?.getSnapContent(snap.snapId) }.getOrNull()
                    }
                    // Each version shows what changed in it, not the same page header every time.
                    val lead = (preview as? com.bernaferrari.changedetection.preview.ChangePreview.Crop)?.lead
                    bytes?.let { data ->
                        if (lead != null) {
                            com.bernaferrari.changedetection.ui.kit.CroppedImage(data, com.bernaferrari.changedetection.image.ChangedBounds(lead.left, lead.afterTop, lead.right, lead.afterBottom), Modifier.fillMaxSize())
                        } else {
                            SnapshotImage(data, null, Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true), androidx.compose.ui.layout.ContentScale.FillWidth)
                        }
                    }
                } else {
                    Text(previewLabel(preview), Modifier.padding(6.dp), style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF1B1B21), maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(Modifier.padding(start = 2.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(formatTime(snap.timestamp), style = MaterialTheme.typography.labelLarge, maxLines = 1, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                if (latest) Box(Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary))
            }
            Text(
                friendlyDay(snap.timestamp),
                Modifier.padding(start = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReaderBar(reader: ReaderState, showNav: Boolean, canMarkRead: Boolean, onMarkRead: () -> Unit) {
    val scope = rememberCoroutineScope()
    if (!showNav) {
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            Button(
                onClick = onMarkRead,
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
            ) {
                Icon(Icons.Rounded.Check, null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.mark_read_short), style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 3.dp,
        ) {
            BoxWithConstraints(Modifier.padding(6.dp)) {
                val stacked = canMarkRead && (maxWidth < 360.dp || LocalDensity.current.fontScale > 1.3f)
                val navigation: @Composable (Modifier) -> Unit = { modifier ->
                    Row(modifier.heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                        val colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = { scope.launch { reader.go(-1) } }, enabled = reader.current > 0, colors = colors) {
                            Icon(Icons.Rounded.KeyboardArrowUp, stringResource(Res.string.reader_previous))
                        }
                        Text(
                            stringResource(Res.string.reader_position, reader.current + 1, reader.changeCount),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                        )
                        IconButton(onClick = { scope.launch { reader.go(1) } }, enabled = reader.current < reader.changeCount - 1, colors = colors) {
                            Icon(Icons.Rounded.KeyboardArrowDown, stringResource(Res.string.reader_next))
                        }
                    }
                }
                val readButton: @Composable (Modifier) -> Unit = { modifier ->
                    Button(
                        onClick = onMarkRead,
                        modifier = modifier.heightIn(min = 48.dp),
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Icon(Icons.Rounded.Check, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(Res.string.mark_read_short))
                    }
                }
                if (stacked) {
                    Column(Modifier.fillMaxWidth()) {
                        navigation(Modifier.fillMaxWidth())
                        readButton(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        navigation(Modifier.weight(1f))
                        if (canMarkRead) readButton(Modifier)
                    }
                }
            }
        }
    }
}

/** Each version labelled by what changed, so "the one where the dates moved" is easy to find. */
@Composable
private fun VersionSheet(snaps: List<Snap>, selectedIndex: Int, selectedSingleSnapId: String?, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        LazyColumn(Modifier.fillMaxWidth().testTag("version-list"), contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)) {
            item(key = "title") {
                Text(stringResource(Res.string.reader_versions_title), Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(Res.string.reader_versions_body), Modifier.padding(horizontal = 24.dp, vertical = 6.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            itemsIndexed(snaps, key = { _, snap -> snap.snapId }) { index, snap ->
                val previous = snaps.getOrNull(index + 1)
                val preview = rememberPairPreview(previous?.snapId, snap.snapId)
                GroupRow(
                    title = friendlyDateTime(snap.timestamp),
                    supporting = previewLabel(preview),
                    onClick = { onSelect(index) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    when {
                        snap.snapId == selectedSingleSnapId -> SelectionTag(stringResource(Res.string.comparison_selected))
                        index == selectedIndex -> SelectionTag(stringResource(Res.string.preview_after))
                        index == selectedIndex + 1 -> SelectionTag(stringResource(Res.string.preview_before))
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionTag(text: String) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
    }
}
