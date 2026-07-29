/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.bernaferrari.changedetection.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.util.formatTimestamp
import com.bernaferrari.changedetection.ui.theme.ExpressiveSpring
import org.koin.compose.viewmodel.koinViewModel
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.bernaferrari.changedetection.ui.components.SharedEmptyState

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

@Composable
private fun EmptySnapshotState(modifier: Modifier = Modifier) {
    SharedEmptyState(
        icon = Icons.Default.Schedule,
        title = stringResource(Res.string.comparison_empty_title),
        description = stringResource(Res.string.comparison_empty_subtitle),
        modifier = modifier,
    )
}

@Composable
@ExperimentalMaterial3Api
private fun SingleSnapshotState(
    snap: Snap,
    content: String?,
    bytes: ByteArray?,
    isLoadingContent: Boolean,
    hasError: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (snap.isImageSnapshot() && bytes != null) {
        Box(modifier = modifier) {
            ImageViewport(
                bytes = bytes,
                label = null,
                modifier = Modifier.fillMaxSize(),
            )

            SnapshotTimestampLabel(
                timestamp = snap.timestamp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
            )
        }
    } else if (content != null) {
        val isHtml = remember(snap.contentType) {
            snap.isHtmlSnapshot()
        }

        Box(modifier = modifier) {
            if (isHtml) {
                val webViewState = rememberWebViewStateWithHTMLData(
                    data = content,
                )
                WebView(
                    state = webViewState,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                ) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            SnapshotTimestampLabel(
                timestamp = snap.timestamp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
            )
        }
    } else if (isLoadingContent) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    } else if (hasError) {
        Column(
            modifier = modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(Res.string.failed_to_load_content))
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text(stringResource(Res.string.retry))
            }
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(48.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(Res.string.comparison_single_snapshot_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.comparison_single_snapshot_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = formatTimestamp(snap.timestamp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SnapshotTimestampLabel(
    timestamp: Long,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
        tonalElevation = 4.dp,
    ) {
        Text(
            text = formatTimestamp(timestamp),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun DiffFirstContent(
    snaps: List<Snap>,
    selectedPairIndex: Int,
    content1: String?,
    content2: String?,
    bytes1: ByteArray?,
    bytes2: ByteArray?,
    imageDiff: SnapshotImageDiff?,
    isLoadingDiff: Boolean,
    onSelectPair: (Int) -> Unit,
    onRetry: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val newerSnap = snaps.getOrNull(selectedPairIndex)
    val olderSnap = snaps.getOrNull(selectedPairIndex + 1)
    val isImagePair =
        newerSnap?.isImageSnapshot() == true && olderSnap?.isImageSnapshot() == true
    var mode by remember(selectedPairIndex, isImagePair) {
        mutableStateOf(if (isImagePair) ComparisonMode.ImageDiff else ComparisonMode.Html)
    }
    val modes = remember(isImagePair) {
        if (isImagePair) {
            listOf(
                ComparisonMode.ImageDiff,
                ComparisonMode.ImageSwipe,
                ComparisonMode.ImageBefore,
                ComparisonMode.ImageAfter,
            )
        } else {
            listOf(ComparisonMode.Text, ComparisonMode.Html)
        }
    }

    Column(modifier = modifier) {
        // Version selector - horizontal scrollable chips
        VersionSelector(
            snaps = snaps,
            selectedIndex = selectedPairIndex,
            onSelectPair = onSelectPair,
        )

        // Diff mode selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            modes.forEach { option ->
                FilterChip(
                    selected = mode == option,
                    onClick = { mode = option },
                    label = { Text(stringResource(option.labelResource())) },
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Share button
            if ((isImagePair && imageDiff != null) || (content1 != null && content2 != null)) {
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(Res.string.share_comparison))
                }
            }
        }

        // Diff content
        if (isLoadingDiff) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else if (isImagePair) {
            ImageComparisonContent(
                beforeSnap = olderSnap,
                afterSnap = newerSnap,
                beforeBytes = bytes2,
                afterBytes = bytes1,
                imageDiff = imageDiff,
                mode = mode,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (content1 != null && content2 != null) {
            when (mode) {
                ComparisonMode.Text -> {
                    SimpleDiffView(
                        oldContent = content2,
                        newContent = content1,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                ComparisonMode.Html -> {
                    RenderedHtmlComparison(
                        oldContent = content2,
                        newContent = content1,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                else -> Unit
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(Res.string.failed_to_load_content),
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRetry) {
                    Text(stringResource(Res.string.retry))
                }
            }
        }
    }
}

@Composable
private fun VersionSelector(
    snaps: List<Snap>,
    selectedIndex: Int,
    onSelectPair: (Int) -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = stringResource(Res.string.version_count, snaps.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (i in 0 until snaps.size - 1) {
                val isSelected = i == selectedIndex
                val newerSnap = snaps[i]
                val olderSnap = snaps[i + 1]

                VersionChip(
                    olderLabel = if (i == snaps.size - 2) {
                        stringResource(Res.string.version_older)
                    } else {
                        stringResource(Res.string.version_previous)
                    },
                    newerLabel = if (i == 0) {
                        stringResource(Res.string.version_latest)
                    } else {
                        stringResource(Res.string.version_previous)
                    },
                    olderTime = formatTimestamp(olderSnap.timestamp),
                    newerTime = formatTimestamp(newerSnap.timestamp),
                    isSelected = isSelected,
                    onClick = { onSelectPair(i) },
                )
            }
        }
    }
}

@Composable
private fun VersionChip(
    olderLabel: String,
    newerLabel: String,
    olderTime: String,
    newerTime: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS,
        ),
        label = "chipColor",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Show comparison label
            Text(
                text = "$olderLabel → $newerLabel",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Show timestamp of newer version
            Text(
                text = newerTime,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun SimpleDiffView(
    oldContent: String,
    newContent: String,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var showOnlyChanges by remember { mutableStateOf(false) }

    val oldLines = remember(oldContent) { oldContent.lines() }
    val newLines = remember(newContent) { newContent.lines() }
    val diffRows = remember(oldLines, newLines) { calculateDiffRows(oldLines, newLines) }
    val changeStats = remember(diffRows) {
        ChangeStats(
            added = diffRows.count { it.type == DiffLineType.ADDED },
            removed = diffRows.count { it.type == DiffLineType.REMOVED },
            modified = 0,
            unchanged = diffRows.count { it.type == DiffLineType.UNCHANGED },
        )
    }
    val primaryLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val addedLineColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)

    Row(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp),
        ) {
            // Stats and filter row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ComparisonStatsRow(stats = changeStats)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !showOnlyChanges,
                        onClick = { showOnlyChanges = false },
                        label = { Text(stringResource(Res.string.show_all)) },
                    )
                    FilterChip(
                        selected = showOnlyChanges,
                        onClick = { showOnlyChanges = true },
                        label = { Text(stringResource(Res.string.show_changes)) },
                    )
                }
            }

            // Diff lines
            if (changeStats.added == 0 && changeStats.removed == 0) {
                SharedEmptyState(
                    icon = Icons.Default.CheckCircle,
                    title = stringResource(Res.string.comparison_no_changes_title),
                    description = stringResource(Res.string.comparison_no_changes_text),
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                )
            } else {
                diffRows
                    .filter { !showOnlyChanges || it.type != DiffLineType.UNCHANGED }
                    .forEach { row ->
                        DiffLine(row.lineNumber, row.content, row.type)
                    }
            }
        }

        // Minimap
        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .padding(top = 16.dp, bottom = 16.dp, end = 8.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val total = diffRows.size.coerceAtLeast(1)
                val h = size.height
                val step = h / total
                var y = 0f
                diffRows.forEach { row ->
                    val color = when (row.type) {
                        DiffLineType.UNCHANGED -> Color.Transparent
                        DiffLineType.ADDED -> addedLineColor
                        DiffLineType.REMOVED, DiffLineType.MODIFIED -> primaryLineColor
                    }
                    if (color.alpha > 0f) {
                        drawLine(
                            color = color,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                        )
                    }
                    y += step
                }
            }
        }
    }
}

// Use calculateChangeStats and ComparisonStatsRow from ComparisonComponents.kt
