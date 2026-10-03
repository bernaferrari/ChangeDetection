/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.comparison

import com.bernaferrari.changedetection.image.SnapshotImageDiff

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import com.bernaferrari.changedetection.shared.generated.resources.comparison_mode_before
import com.bernaferrari.changedetection.shared.generated.resources.comparison_mode_after
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.shared.generated.resources.comparison_section_snapshots
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.card_site_monitoring_active
import com.bernaferrari.changedetection.shared.generated.resources.comparison_change_supporting
import com.bernaferrari.changedetection.shared.generated.resources.comparison_monitoring_capture
import com.bernaferrari.changedetection.shared.generated.resources.comparison_monitoring_status
import com.bernaferrari.changedetection.shared.generated.resources.comparison_monitoring_versions
import com.bernaferrari.changedetection.shared.generated.resources.comparison_capture_changed_supporting
import com.bernaferrari.changedetection.shared.generated.resources.comparison_capture_changed_title
import com.bernaferrari.changedetection.shared.generated.resources.failed_to_load_content
import com.bernaferrari.changedetection.shared.generated.resources.retry
import com.bernaferrari.changedetection.shared.generated.resources.settings_comparison_page_text
import com.bernaferrari.changedetection.shared.generated.resources.settings_comparison_screenshots
import com.bernaferrari.changedetection.shared.generated.resources.site_status_paused
import com.bernaferrari.changedetection.shared.generated.resources.version_count
import com.bernaferrari.changedetection.shared.generated.resources.version_pair_previous
import com.bernaferrari.changedetection.shared.generated.resources.version_pair_latest
import com.bernaferrari.changedetection.ui.components.AppLoadingIndicator
import com.bernaferrari.changedetection.ui.theme.spacing
import com.bernaferrari.changedetection.util.formatTimestamp
import org.jetbrains.compose.resources.stringResource

/** A diff-first detail workspace with history and page facts deliberately kept secondary. */
@Composable
@ExperimentalMaterial3Api
internal fun DiffFirstContent(
    site: Site,
    snaps: List<Snap>,
    selectedPairIndex: Int,
    content1: String?,
    content2: String?,
    bytes1: ByteArray?,
    bytes2: ByteArray?,
    imageDiff: SnapshotImageDiff?,
    isLoadingDiff: Boolean,
    mixedTypePair: Boolean = false,
    onSelectPair: (Int) -> Unit,
    onRetry: () -> Unit,
    onShare: (ComparisonMode) -> Unit,
    modifier: Modifier = Modifier,
    showHistoryInitially: Boolean = false,
) {
    val spacing = MaterialTheme.spacing
    val newerSnap = snaps.getOrNull(selectedPairIndex)
    val olderSnap = snaps.getOrNull(selectedPairIndex + 1)
    val isHtmlPair = newerSnap?.isHtmlSnapshot() == true || olderSnap?.isHtmlSnapshot() == true
    val isImagePair = newerSnap?.isImageSnapshot() == true && olderSnap?.isImageSnapshot() == true
    var mode by remember(isImagePair, isHtmlPair) {
        mutableStateOf(if (isImagePair) ComparisonMode.ImageSwipe else ComparisonMode.Text)
    }
    var showHistory by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(showHistoryInitially) }
    val modes = remember(isImagePair, isHtmlPair) {
        if (isImagePair) {
            listOf(
                ComparisonMode.ImageSwipe,
                ComparisonMode.ImageBefore,
                ComparisonMode.ImageAfter,
                ComparisonMode.ImageDiff,
            )
        } else {
            if (isHtmlPair) listOf(ComparisonMode.Text, ComparisonMode.Html, ComparisonMode.Source) else listOf(ComparisonMode.Text)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val controlsMaxHeight = maxHeight * 0.45f
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().heightIn(max = controlsMaxHeight).verticalScroll(rememberScrollState()).testTag("comparison-controls")) {
                ComparisonModeSelector(
                    modes = modes,
                    selectedMode = mode,
                    onModeSelected = { mode = it },
                    canShare = (isImagePair && imageDiff != null) || (content1 != null && content2 != null),
                    onShare = { onShare(mode) },
                    modifier = Modifier.widthIn(max = 720.dp).align(Alignment.CenterHorizontally).padding(horizontal = 20.dp, vertical = 4.dp),
                )
                LatestChangeHeader(
                    newerSnap = newerSnap,
                    olderSnap = olderSnap,
                    pairIndex = selectedPairIndex,
                    modifier = Modifier.widthIn(max = 720.dp).align(Alignment.CenterHorizontally).padding(horizontal = 20.dp, vertical = 4.dp),
                    onHistory = { showHistory = true },
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).testTag("comparison-reader"),
                contentAlignment = Alignment.TopCenter,
            ) {
                when {
                    isLoadingDiff -> Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AppLoadingIndicator(size = 48.dp)
                        }
                    }

                    isImagePair -> ImageComparisonContent(
                        beforeBytes = bytes2,
                        afterBytes = bytes1,
                        imageDiff = imageDiff,
                        mode = mode,
                        modifier = Modifier.fillMaxSize(),
                    )

                    content1 != null && content2 != null -> Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        when (mode) {
                            ComparisonMode.Text -> SimpleDiffView(
                                oldContent = content2,
                                newContent = content1,
                                oldIsHtml = olderSnap?.isHtmlSnapshot() == true,
                                newIsHtml = newerSnap?.isHtmlSnapshot() == true,
                                modifier = Modifier.fillMaxSize(),
                            )

                            ComparisonMode.Source -> SimpleDiffView(
                                oldContent = content2,
                                newContent = content1,
                                modifier = Modifier.fillMaxSize(),
                            )

                            ComparisonMode.Html -> RenderedHtmlComparison(
                                oldContent = content2,
                                newContent = content1,
                                modifier = Modifier.fillMaxSize(),
                            )

                            else -> Unit
                        }
                    }

                    mixedTypePair && !(content1 != null && content2 != null) -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()).padding(spacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(Modifier.height(spacing.sm))
                            Text(
                                text = stringResource(Res.string.comparison_capture_changed_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(spacing.xxs))
                            Text(
                                text = stringResource(Res.string.comparison_capture_changed_supporting),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    else -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()).padding(spacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.failed_to_load_content),
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.height(spacing.sm))
                            Button(onClick = onRetry, shapes = ButtonDefaults.shapes()) {
                                Text(stringResource(Res.string.retry))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHistory) {
        ModalBottomSheet(onDismissRequest = { showHistory = false }) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                VersionSelector(
                    snaps = snaps,
                    selectedIndex = selectedPairIndex,
                    onClose = { showHistory = false },
                    footer = { PageFacts(site = site, snapshotCount = snaps.size) },
                    onSelectPair = { index ->
                        onSelectPair(index)
                        showHistory = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun LatestChangeHeader(
    newerSnap: Snap?,
    olderSnap: Snap?,
    pairIndex: Int,
    modifier: Modifier = Modifier,
    onHistory: () -> Unit = {},
) {
    val historyLabel = stringResource(Res.string.comparison_section_snapshots)
    Surface(onClick = onHistory, modifier = modifier.fillMaxWidth().semantics { contentDescription = historyLabel }, color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BoxWithConstraints(Modifier.weight(1f)) {
                if (maxWidth / LocalDensity.current.fontScale < 240.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CaptureTime(stringResource(Res.string.comparison_mode_before), olderSnap)
                        CaptureTime(stringResource(Res.string.comparison_mode_after), newerSnap)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CaptureTime(stringResource(Res.string.comparison_mode_before), olderSnap, Modifier.weight(1f))
                        CaptureTime(stringResource(Res.string.comparison_mode_after), newerSnap, Modifier.weight(1f))
                    }
                }
            }
            Icon(Icons.Default.History, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CaptureTime(label: String, snap: Snap?, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            snap?.let { formatTimestamp(it.timestamp, includeSeconds = true) }
                ?: stringResource(Res.string.comparison_change_supporting),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PageFacts(
    site: Site,
    snapshotCount: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            PageFact(
                icon = Icons.Default.CheckCircle,
                label = stringResource(Res.string.comparison_monitoring_status),
                value = stringResource(
                    if (site.isSyncEnabled) Res.string.card_site_monitoring_active else Res.string.site_status_paused,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                emphasized = site.isSyncEnabled,
            )
            HorizontalDivider(modifier = Modifier.padding(start = 45.dp))
            PageFact(
                icon = Icons.Default.History,
                label = stringResource(Res.string.comparison_monitoring_versions),
                value = stringResource(Res.string.version_count, snapshotCount),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            )
            HorizontalDivider(modifier = Modifier.padding(start = 45.dp))
            PageFact(
                icon = if (site.isBrowser) Icons.Default.Image else Icons.Default.Code,
                label = stringResource(Res.string.comparison_monitoring_capture),
                value = stringResource(
                    if (site.isBrowser) Res.string.settings_comparison_screenshots else Res.string.settings_comparison_page_text,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun PageFact(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
