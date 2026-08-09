/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.card_site_monitoring_active
import com.bernaferrari.changedetection.shared.generated.resources.comparison_change_supporting
import com.bernaferrari.changedetection.shared.generated.resources.comparison_monitoring_capture
import com.bernaferrari.changedetection.shared.generated.resources.comparison_monitoring_status
import com.bernaferrari.changedetection.shared.generated.resources.comparison_monitoring_versions
import com.bernaferrari.changedetection.shared.generated.resources.failed_to_load_content
import com.bernaferrari.changedetection.shared.generated.resources.retry
import com.bernaferrari.changedetection.shared.generated.resources.settings_comparison_page_text
import com.bernaferrari.changedetection.shared.generated.resources.settings_comparison_screenshots
import com.bernaferrari.changedetection.shared.generated.resources.site_status_paused
import com.bernaferrari.changedetection.shared.generated.resources.version_count
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
    onSelectPair: (Int) -> Unit,
    onRetry: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val newerSnap = snaps.getOrNull(selectedPairIndex)
    val olderSnap = snaps.getOrNull(selectedPairIndex + 1)
    val isImagePair = newerSnap?.isImageSnapshot() == true && olderSnap?.isImageSnapshot() == true
    var mode by remember(selectedPairIndex, isImagePair) {
        mutableStateOf(if (isImagePair) ComparisonMode.ImageSwipe else ComparisonMode.Html)
    }
    var showHistory by remember { mutableStateOf(false) }
    val modes = remember(isImagePair) {
        if (isImagePair) {
            listOf(
                ComparisonMode.ImageSwipe,
                ComparisonMode.ImageBefore,
                ComparisonMode.ImageAfter,
                ComparisonMode.ImageDiff,
            )
        } else {
            listOf(ComparisonMode.Html, ComparisonMode.Text)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 980.dp)
                .fillMaxWidth()
                .fillMaxSize()
                .padding(horizontal = spacing.md)
                .padding(top = spacing.xs),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            LatestChangeHeader(newerSnap = newerSnap, olderSnap = olderSnap)

            ComparisonModeSelector(
                modes = modes,
                selectedMode = mode,
                onModeSelected = { mode = it },
                canShare = (isImagePair && imageDiff != null) || (content1 != null && content2 != null),
                onShare = onShare,
                onHistory = { showHistory = true },
            )

            when {
                isLoadingDiff -> Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AppLoadingIndicator(size = spacing.lg)
                    }
                }

                isImagePair -> ImageComparisonContent(
                    beforeSnap = olderSnap,
                    afterSnap = newerSnap,
                    beforeBytes = bytes2,
                    afterBytes = bytes1,
                    imageDiff = imageDiff,
                    mode = mode,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )

                content1 != null && content2 != null -> Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    when (mode) {
                        ComparisonMode.Text -> SimpleDiffView(
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

                else -> Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 190.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.lg),
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
                    onSelectPair = { index ->
                        onSelectPair(index)
                        showHistory = false
                    },
                )
                PageFacts(site = site, snapshotCount = snaps.size)
                Spacer(Modifier.height(spacing.lg))
            }
        }
    }
}

@Composable
private fun LatestChangeHeader(
    newerSnap: Snap?,
    olderSnap: Snap?,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = stringResource(Res.string.version_pair_latest),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = if (olderSnap != null && newerSnap != null) {
                "${formatTimestamp(olderSnap.timestamp)} → ${formatTimestamp(newerSnap.timestamp)}"
            } else {
                stringResource(Res.string.comparison_change_supporting)
            },
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
        shape = RoundedCornerShape(22.dp),
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
