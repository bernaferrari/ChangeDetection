@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle

import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Difference
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.outlined.Description
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
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.liveRegion
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
import com.bernaferrari.changedetection.util.formatTimestamp
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
    // The home card already found this pair's changes; the reader opens on them while its full comparison runs.
    val knownRegions = remember(older?.snapId, newer?.snapId, isImagePair) {
        if (!isImagePair || older == null || newer == null) {
            null
        } else {
            (com.bernaferrari.changedetection.ui.kit.optional<com.bernaferrari.changedetection.preview.ChangePreviewRepository>()?.cachedBetween(older.snapId, newer.snapId) as? com.bernaferrari.changedetection.preview.ChangePreview.Crop)?.regions?.takeIf { it.isNotEmpty() }
        }
    }
    val shownImageDiff = uiState.imageDiff ?: knownRegions?.takeIf { uiState.isLoadingDiff }?.let(::provisionalImageDiff)
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
            val canShare = !uiState.requestedSnapshotUnavailable && ((isImagePair && uiState.imageDiff != null) || (uiState.snapContent1 != null && uiState.snapContent2 != null))
            com.bernaferrari.changedetection.ui.navigation.AppTopBar(
                // Blank while the page loads, so the title never flashes the app's name first.
                title = uiState.site?.name() ?: if (uiState.isLoading) "" else stringResource(Res.string.app_name),
                subtitle = uiState.site?.url?.displayPageAddress(),
                onBack = if (showNavigateUp) onNavigateUp else null,
                // Present from the first frame of a comparison and enabled once it can be shared, so the bar never shifts.
                actions = if (uiState.snaps.size > 1 && uiState.singleSnap == null && !uiState.requestedSnapshotUnavailable) {
                    listOf(com.bernaferrari.changedetection.ui.navigation.BarAction(stringResource(Res.string.comparison_share), Icons.Outlined.Share, "square.and.arrow.up", enabled = canShare) { share(viewModel, uiState, mode) })
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
            val comparing = uiState.snaps.size > 1 && uiState.singleSnap == null && (!uiState.isLoadingDiff || (isImagePair && shownImageDiff != null))
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
                    if (uiState.newVersionAvailable) NewVersionAction { viewModel.selectPairForComparison(0) }
                    val snap = uiState.singleSnap ?: uiState.snaps.first()
                    // Web pages read as text or as the page itself, switched like the comparison's modes.
                    val singleModes = if (snap.isHtmlSnapshot() && uiState.singleSnapContent != null) listOf(ComparisonMode.SavedText, ComparisonMode.Html) else emptyList()
                    var singleMode by remember(snap.snapId) { mutableStateOf(ComparisonMode.SavedText) }
                    Column(Modifier.widthIn(max = 720.dp).align(Alignment.CenterHorizontally)) {
                        SavedVersionHeader(snap, singleModes, singleMode, onVersions = if (uiState.snaps.size > 1) ({ showVersions = true }) else null) { singleMode = it }
                    }
                    SingleSnapshotState(
                        snap = snap,
                        content = uiState.singleSnapContent,
                        bytes = uiState.singleSnapBytes,
                        isLoadingContent = uiState.isLoadingDiff,
                        hasError = uiState.error != null,
                        onRetry = viewModel::retryComparison,
                        onCheckNow = { viewModel.reloadSite() },
                        asPage = singleMode == ComparisonMode.Html,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }

                else -> Column(Modifier.fillMaxSize()) {
                    if (uiState.newVersionAvailable) NewVersionAction { viewModel.selectPairForComparison(0) }
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
                        ComparisonHeader(uiState.snaps, pairIndex, modes, mode, { showVersions = true }) { mode = it }
                    }
                    // One readable column on tablets, aligned with the version navigator and modes above.
                    Box(Modifier.widthIn(max = 720.dp).fillMaxWidth().weight(1f).align(Alignment.CenterHorizontally).testTag("comparison-reader"), contentAlignment = Alignment.TopCenter) {
                        val content1 = uiState.snapContent1
                        val content2 = uiState.snapContent2
                        val screenshotsReady = uiState.snapBytes1 != null && uiState.snapBytes2 != null
                        when {
                            // One view from the first frame: the changes found for the card that was tapped, or the
                            // slider while they are found, then the full comparison in the same place.
                            isImagePair && (!uiState.isLoadingDiff || screenshotsReady) -> ImageComparisonContent(
                                uiState.snapBytes2,
                                uiState.snapBytes1,
                                shownImageDiff,
                                mode,
                                Modifier.fillMaxSize(),
                                onRetry = viewModel::retryComparison,
                                cursor = regionCursor,
                                isPreparing = uiState.isLoadingDiff,
                                opaqueWhiteBackdrop = older != null && newer != null && com.bernaferrari.changedetection.image.renderedScreenshotPair(older, newer),
                            )

                            uiState.isLoadingDiff -> com.bernaferrari.changedetection.ui.components.DelayedLoadingIndicator(Modifier.align(Alignment.Center).testTag("comparison-progress"))

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
        ComparisonMode.SavedText -> Res.string.mode_text
    },
)

@Composable
private fun NewVersionAction(onLatest: () -> Unit) {
    TextButton(onClick = onLatest, modifier = Modifier.fillMaxWidth().semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }) {
        Text(stringResource(Res.string.comparison_new_version))
    }
}

/**
 * Which two versions are on screen: the newer one and what it is compared with. One tap lists
 * every version; timestamps wrap instead of losing their meaning.
 */
@Composable
internal fun ComparisonHeader(snaps: List<Snap>, pairIndex: Int, modes: List<ComparisonMode>, mode: ComparisonMode, onVersions: () -> Unit, onMode: (ComparisonMode) -> Unit) {
    ReaderHeader(modes, mode, onMode) { modifier -> VersionNavigator(snaps, pairIndex, onShowAll = onVersions, modifier = modifier) }
}

/** The same header for one saved version: when it was saved, and Text or Page for web pages. */
@Composable
private fun SavedVersionHeader(snap: Snap, modes: List<ComparisonMode>, mode: ComparisonMode, onVersions: (() -> Unit)?, onMode: (ComparisonMode) -> Unit) {
    ReaderHeader(modes, mode, onMode) { modifier ->
        VersionPill(stringResource(Res.string.single_saved_on, friendlyDateTime(snap.timestamp)), formatTimestamp(snap.timestamp), onVersions, modifier)
    }
}

/** One row: the versions take what the modes leave and wrap their dates. Only very large text stacks them. */
@Composable
private fun ReaderHeader(modes: List<ComparisonMode>, mode: ComparisonMode, onMode: (ComparisonMode) -> Unit, versions: @Composable (Modifier) -> Unit) {
    if (LocalDensity.current.fontScale > 1.3f) {
        androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            versions(Modifier.width(IntrinsicSize.Max))
            if (modes.size > 1) ModeSwitcher(modes, mode, onMode)
        }
        return
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        versions(Modifier.weight(1f, fill = false))
        if (modes.size > 1) ModeSwitcher(modes, mode, onMode)
    }
}

@Composable
internal fun VersionNavigator(snaps: List<Snap>, selectedIndex: Int, onShowAll: () -> Unit, modifier: Modifier = Modifier) {
    val newer = snaps.getOrNull(selectedIndex) ?: return
    val older = snaps.getOrNull(selectedIndex + 1)
    val range = older?.let { comparisonRange(it.timestamp, newer.timestamp) }
    val beforeLabel = stringResource(Res.string.preview_before)
    val afterLabel = stringResource(Res.string.preview_after)
    VersionPill(
        label = range?.label ?: friendlyDateTime(newer.timestamp),
        description = range?.let { "$beforeLabel: ${it.before}. $afterLabel: ${it.after}" } ?: formatTimestamp(newer.timestamp),
        onClick = onShowAll,
        modifier = modifier,
    )
}

/** The dates on screen as a pill; with [onClick] it opens every saved version. */
@Composable
private fun VersionPill(label: String, description: String, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.testTag("version-navigator"),
    ) {
        Row(
            Modifier.heightIn(min = 52.dp).clip(MaterialTheme.shapes.extraLarge)
                .then(if (onClick != null) Modifier.clickable(onClickLabel = stringResource(Res.string.reader_versions_title), onClick = onClick) else Modifier)
                .padding(start = 16.dp, end = if (onClick != null) 10.dp else 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                Modifier.weight(1f, fill = false).clearAndSetSemantics { contentDescription = description },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (onClick != null) Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Icon-only connected modes; each keeps its name for screen readers. */
@Composable
private fun ModeSwitcher(modes: List<ComparisonMode>, selected: ComparisonMode, onSelect: (ComparisonMode) -> Unit) {
    // Keep icon controls bounded when the wrapping header receives the screen height.
    Row(Modifier.height(52.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        modes.forEachIndexed { index, mode ->
            val label = modeLabel(mode)
            ToggleButton(
                checked = mode == selected,
                onCheckedChange = { if (it) onSelect(mode) },
                modifier = Modifier.width(52.dp).fillMaxHeight().heightIn(min = 48.dp).semantics {
                    role = Role.RadioButton
                    this.selected = mode == selected
                    contentDescription = label
                },
                shapes = when {
                    modes.size == 1 -> ToggleButtonDefaults.shapesFor(ToggleButtonDefaults.size)
                    index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    index == modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = com.bernaferrari.changedetection.ui.kit.connectedToggleColors(),
                contentPadding = PaddingValues(0.dp),
            ) { Icon(modeIcon(mode), null, Modifier.size(22.dp)) }
        }
    }
}

private fun modeIcon(mode: ComparisonMode): androidx.compose.ui.graphics.vector.ImageVector = when (mode) {
    ComparisonMode.Text, ComparisonMode.ImageChanges -> Icons.Rounded.Difference
    ComparisonMode.Html, ComparisonMode.ImageDiff -> Icons.AutoMirrored.Rounded.Article
    ComparisonMode.Source -> Icons.Rounded.Code
    ComparisonMode.ImageSwipe -> ModeIcons.Slider
    ComparisonMode.ImageBefore -> Icons.Rounded.History
    ComparisonMode.ImageAfter -> Icons.Rounded.Update
    ComparisonMode.SavedText -> Icons.AutoMirrored.Rounded.Notes
}

@Composable
private fun ReaderBar(reader: ChangeCursor, showNav: Boolean, canMarkRead: Boolean, onMarkRead: () -> Unit) {
    val scope = rememberCoroutineScope()
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
                // The same floating bar in every mode: arrows when there is more than one change, Mark read when unread.
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (showNav) navigation(Modifier.weight(1f))
                    if (canMarkRead) readButton(if (showNav) Modifier else Modifier.weight(1f))
                }
            }
        }
    }
}
