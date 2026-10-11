/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle

import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.role

import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import com.bernaferrari.changedetection.ui.kit.localDate

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
import androidx.compose.material.icons.outlined.Info
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
import com.bernaferrari.changedetection.localization.stringResource
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
    onOpenPage: (String) -> Unit,
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
    val regionCursor = remember(siteId, newer?.snapId, older?.snapId) { RegionCursor() }
    val isImagePair = newer?.isImageSnapshot() == true && older?.isImageSnapshot() == true
    val isHtmlPair = newer?.isHtmlSnapshot() == true || older?.isHtmlSnapshot() == true
    val modes = when {
        isImagePair -> listOf(ComparisonMode.ImageChanges, ComparisonMode.ImageDiff, ComparisonMode.ImageSwipe)
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
            val canShare = (isImagePair && uiState.imageDiff != null) || (uiState.snapContent1 != null && uiState.snapContent2 != null)
            com.bernaferrari.changedetection.ui.navigation.AppTopBar(
                title = uiState.site?.name() ?: stringResource(Res.string.app_name),
                subtitle = uiState.site?.url?.displayPageAddress(),
                onBack = if (showNavigateUp) onNavigateUp else null,
                actions = if (canShare && uiState.snaps.size > 1) {
                    listOf(com.bernaferrari.changedetection.ui.navigation.BarAction(stringResource(Res.string.comparison_share), Icons.Outlined.Share, "square.and.arrow.up") { share(viewModel, uiState, mode) })
                } else {
                    emptyList()
                },
                menu = listOf(
                    com.bernaferrari.changedetection.ui.navigation.BarAction(stringResource(Res.string.comparison_page_details), Icons.Outlined.Info, "info.circle", enabled = uiState.site != null) { onOpenPage(siteId) },
                    com.bernaferrari.changedetection.ui.navigation.BarAction(stringResource(Res.string.home_check_now), Icons.Outlined.Refresh, "arrow.clockwise", enabled = uiState.site != null) { viewModel.reloadSite() },
                    com.bernaferrari.changedetection.ui.navigation.BarAction(stringResource(Res.string.open_in_browser), Icons.AutoMirrored.Outlined.OpenInNew, "safari") { uiState.site?.url?.let { runCatching { uriHandler.openUri(it) } } },
                    com.bernaferrari.changedetection.ui.navigation.BarAction(stringResource(Res.string.edit), Icons.Outlined.Edit, "pencil") { onSiteEdit(siteId) },
                    com.bernaferrari.changedetection.ui.navigation.BarAction(stringResource(Res.string.prune_history), Icons.Outlined.CleaningServices, "scissors") { showPruneDialog = true },
                    com.bernaferrari.changedetection.ui.navigation.BarAction(stringResource(Res.string.detail_delete), Icons.Outlined.DeleteOutline, "trash", destructive = true) { showDeleteDialog = true },
                ),
            )
        },
        bottomBar = {
            val comparing = uiState.snaps.size > 1 && uiState.singleSnap == null && !uiState.isLoadingDiff
            // One bar steps through changes in every mode that has them, next to Mark read.
            val cursor: ChangeCursor = if (isImagePair) regionCursor else reader
            val showNav = comparing && cursor.changeCount > 1 && when (mode) {
                ComparisonMode.Text -> true
                ComparisonMode.ImageChanges, ComparisonMode.ImageDiff -> isImagePair
                else -> false
            }
            AnimatedVisibility(showNav || canMarkRead, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                ReaderBar(cursor, showNav, canMarkRead, onMarkRead = markRead)
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
                        // One row: which versions (tap to pick another) and how to look at them.
                        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VersionNavigator(uiState.snaps, pairIndex, onSelect = { viewModel.selectPairForComparison(it) }, onShowAll = { showVersions = true }, modifier = Modifier.weight(1f))
                            if (modes.size > 1) ModeSwitcher(modes, mode) { mode = it }
                        }
                    }
                    // One readable column on tablets, aligned with the version navigator and modes above.
                    Box(Modifier.widthIn(max = 720.dp).fillMaxWidth().weight(1f).align(Alignment.CenterHorizontally).testTag("comparison-reader"), contentAlignment = Alignment.TopCenter) {
                        val content1 = uiState.snapContent1
                        val content2 = uiState.snapContent2
                        when {
                            uiState.isLoadingDiff -> LoadingIndicator(Modifier.align(Alignment.Center))

                            isImagePair -> ImageComparisonContent(uiState.snapBytes2, uiState.snapBytes1, uiState.imageDiff, mode, Modifier.fillMaxSize(), onRetry = viewModel::retryComparison, cursor = regionCursor)

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
        ComparisonMode.ImageDiff -> Res.string.reader_mode_page
        ComparisonMode.ImageBefore -> Res.string.preview_before
        ComparisonMode.ImageAfter -> Res.string.preview_after
    },
)

/**
 * Which two versions are on screen, in one line: the newer one and what it is compared with. One tap lists
 * every version; the only arrows on screen step through the changes themselves.
 */
@Composable
private fun VersionNavigator(snaps: List<Snap>, selectedIndex: Int, onSelect: (Int) -> Unit, onShowAll: () -> Unit, modifier: Modifier = Modifier) {
    val newer = snaps.getOrNull(selectedIndex) ?: return
    val older = snaps.getOrNull(selectedIndex + 1)
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.testTag("version-navigator"),
    ) {
        // One line, as tall as the mode icons: "Today, 16:48 · vs 16:12". The older side drops its day when it is
        // the same day, so the line stays short.
        val sameDay = older != null && newer.timestamp.localDate() == older.timestamp.localDate()
        val olderLabel = older?.let { if (sameDay) formatTime(it.timestamp) else friendlyDateTime(it.timestamp) }
        // Today needs no word: "16:48 · since 16:12".
        val today = newer.timestamp.localDate() == com.bernaferrari.changedetection.util.getCurrentTimeMillis().localDate()
        val newerLabel = if (today) formatTime(newer.timestamp) else friendlyDateTime(newer.timestamp)
        Row(
            Modifier.heightIn(min = 52.dp).clip(MaterialTheme.shapes.extraLarge)
                .clickable(onClickLabel = stringResource(Res.string.reader_versions_title), onClick = onShowAll)
                .padding(start = 16.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)) { append(newerLabel) }
                    if (olderLabel != null) {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(" · " + stringResource(Res.string.version_compared_with, olderLabel)) }
                    }
                },
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The ways to look at a comparison, as icons: their names are short on meaning, the icons say it at a glance. */
@Composable
private fun ModeSwitcher(modes: List<ComparisonMode>, selected: ComparisonMode, onSelect: (ComparisonMode) -> Unit) {
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.padding(4.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            modes.forEach { mode ->
                val label = modeLabel(mode)
                val isSelected = mode == selected
                FilledIconToggleButton(
                    checked = isSelected,
                    onCheckedChange = { if (it) onSelect(mode) },
                    modifier = Modifier.size(44.dp).semantics {
                        role = androidx.compose.ui.semantics.Role.RadioButton
                        this.selected = isSelected
                        contentDescription = label
                    },
                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(modeIcon(mode), null, Modifier.size(22.dp))
                }
            }
        }
    }
}

private fun modeIcon(mode: ComparisonMode): androidx.compose.ui.graphics.vector.ImageVector = when (mode) {
    ComparisonMode.Text, ComparisonMode.ImageChanges -> Icons.Rounded.ViewAgenda
    ComparisonMode.Html, ComparisonMode.ImageDiff -> Icons.Outlined.Description
    ComparisonMode.ImageSwipe -> Icons.Rounded.Compare
    ComparisonMode.Source -> Icons.Rounded.Code
    else -> Icons.Outlined.Description
}

/** What changed in [snap] since [previous]: the changed area for screenshots, the changed words for text. */
@Composable
private fun ChangeThumb(snap: Snap, previous: Snap?, modifier: Modifier = Modifier) {
    val preview = if (previous != null) rememberPairPreview(previous.snapId, snap.snapId) else null
    Box(modifier.clip(MaterialTheme.shapes.small).background(androidx.compose.ui.graphics.Color.White).border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)) {
        if (snap.isImageSnapshot()) {
            val bytes by produceState<ByteArray?>(null, snap.snapId) {
                value = runCatching { com.bernaferrari.changedetection.ui.kit.optional<com.bernaferrari.changedetection.repo.source.SnapsRepository>()?.getSnapContent(snap.snapId) }.getOrNull()
            }
            val lead = (preview as? com.bernaferrari.changedetection.preview.ChangePreview.Crop)?.lead
            bytes?.let { data ->
                if (lead != null) {
                    // Only the changed rectangle is decoded (cached), not the whole page.
                    val top = if (lead.kind == com.bernaferrari.changedetection.image.VisualRegion.Kind.Removed) lead.beforeTop else lead.afterTop
                    val bottom = if (lead.kind == com.bernaferrari.changedetection.image.VisualRegion.Kind.Removed) lead.beforeBottom else lead.afterBottom
                    val source = if (lead.kind == com.bernaferrari.changedetection.image.VisualRegion.Kind.Removed) previous?.snapId else snap.snapId
                    val sourceBytes by produceState<ByteArray?>(null, source) {
                        value = source?.let { id -> runCatching { com.bernaferrari.changedetection.ui.kit.optional<com.bernaferrari.changedetection.repo.source.SnapsRepository>()?.getSnapContent(id) }.getOrNull() }
                    }
                    val crop by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, sourceBytes, top, bottom) {
                        value = sourceBytes?.let { com.bernaferrari.changedetection.image.CropCache.get(it, 0f, top, 1f, bottom, 256) }
                    }
                    crop?.let { androidx.compose.foundation.Image(it, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop) }
                } else {
                    com.bernaferrari.changedetection.ui.kit.CaptureTop(snap.snapId, Modifier.fillMaxSize(), decodeWidth = 240)
                }
            }
        } else {
            Text(previewLabel(preview), Modifier.padding(4.dp), style = MaterialTheme.typography.labelSmall.copy(fontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp)), color = androidx.compose.ui.graphics.Color(0xFF1B1B21), maxLines = 4, overflow = TextOverflow.Clip)
        }
    }
}

@Composable
private fun ReaderBar(reader: ChangeCursor, showNav: Boolean, canMarkRead: Boolean, onMarkRead: () -> Unit) {
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
                // Always one row: with large text or a narrow screen the position reads "2 / 9" instead of a sentence.
                val compact = maxWidth < 360.dp || LocalDensity.current.fontScale > 1.3f
                val navigation: @Composable (Modifier) -> Unit = { modifier ->
                    Row(modifier.heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                        val colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = { scope.launch { reader.go(-1) } }, enabled = reader.current > 0, colors = colors) {
                            Icon(Icons.Rounded.KeyboardArrowUp, stringResource(Res.string.reader_previous))
                        }
                        val position = stringResource(Res.string.reader_position, reader.current + 1, reader.changeCount)
                        Text(
                            if (compact) "${reader.current + 1} / ${reader.changeCount}" else position,
                            Modifier.weight(1f).semantics { contentDescription = position },
                            maxLines = 1,
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
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    navigation(Modifier.weight(1f))
                    if (canMarkRead) readButton(Modifier)
                }
            }
        }
    }
}

/**
 * Every saved version, newest first and grouped by day. Each row shows what changed in that version, so
 * "the one where the price dropped" is easy to spot; the pair on screen is marked.
 */
@Composable
private fun VersionSheet(snaps: List<Snap>, selectedIndex: Int, selectedSingleSnapId: String?, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    val list = androidx.compose.foundation.lazy.rememberLazyListState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        LazyColumn(Modifier.fillMaxWidth().testTag("version-list"), state = list, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 4.dp)) {
            item(key = "title") {
                Column(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
                    Text(stringResource(Res.string.reader_versions_title), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(Res.string.reader_versions_body), Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            snaps.indices.groupBy { snaps[it].timestamp.localDate() }.forEach { (_, indices) ->
                item(key = "day-" + snaps[indices.first()].snapId) {
                    Text(
                        friendlyDay(snaps[indices.first()].timestamp),
                        Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                indices.forEachIndexed { position, index ->
                    val snap = snaps[index]
                    item(key = snap.snapId) {
                        val previous = snaps.getOrNull(index + 1)
                        val isAfter = index == selectedIndex || snap.snapId == selectedSingleSnapId
                        val isBefore = index == selectedIndex + 1
                        val shape = groupShape(position, indices.size)
                        Surface(
                            onClick = { onSelect(index) },
                            enabled = previous != null || snaps.size == 1,
                            shape = shape,
                            color = if (isAfter) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceBright,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ChangeThumb(snap, previous, Modifier.size(width = 64.dp, height = 48.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(formatTime(snap.timestamp), style = MaterialTheme.typography.titleMedium, color = if (isAfter) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        if (previous == null) stringResource(Res.string.label_first_version) else previewLabel(rememberPairPreview(previous.snapId, snap.snapId)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isAfter) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                when {
                                    snap.snapId == selectedSingleSnapId -> SelectionTag(stringResource(Res.string.comparison_selected))
                                    isAfter -> SelectionTag(stringResource(Res.string.preview_after))
                                    isBefore -> SelectionTag(stringResource(Res.string.preview_before), quiet = true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    // Open with the pair on screen in view.
    LaunchedEffect(Unit) { if (selectedIndex > 2) list.scrollToItem(selectedIndex) }
}

/** Rounded at the ends of a day's group, like the app's other grouped lists. */
@Composable
private fun groupShape(position: Int, count: Int): androidx.compose.ui.graphics.Shape {
    val large = 20.dp
    val small = 4.dp
    return androidx.compose.foundation.shape.RoundedCornerShape(
        topStart = if (position == 0) large else small,
        topEnd = if (position == 0) large else small,
        bottomStart = if (position == count - 1) large else small,
        bottomEnd = if (position == count - 1) large else small,
    )
}

@Composable
private fun SelectionTag(text: String, quiet: Boolean = false) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (quiet) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.primary,
        contentColor = if (quiet) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
    ) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
    }
}
