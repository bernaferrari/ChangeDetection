/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.comparison_no_changes_title
import com.bernaferrari.changedetection.shared.generated.resources.failed_to_load_content
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.util.formatTimestamp
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

internal enum class ComparisonMode {
    Text,
    Html,
    ImageDiff,
    ImageSwipe,
    ImageBefore,
    ImageAfter,
}

internal fun ComparisonMode.label(): String = when (this) {
    ComparisonMode.Text -> "Text"
    ComparisonMode.Html -> "Rendered"
    ComparisonMode.ImageDiff -> "Pixel diff"
    ComparisonMode.ImageSwipe -> "Swipe"
    ComparisonMode.ImageBefore -> "Before"
    ComparisonMode.ImageAfter -> "After"
}

@Composable
internal fun ImageComparisonContent(
    beforeSnap: Snap,
    afterSnap: Snap,
    beforeBytes: ByteArray?,
    afterBytes: ByteArray?,
    imageDiff: SnapshotImageDiff?,
    mode: ComparisonMode,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ImageDiffSummary(
            beforeSnap = beforeSnap,
            afterSnap = afterSnap,
            imageDiff = imageDiff,
        )

        if (beforeBytes == null || afterBytes == null) {
            SharedEmptyState(
                icon = Icons.Default.Warning,
                title = "Image unavailable",
                description = stringResource(Res.string.failed_to_load_content),
                modifier = Modifier.fillMaxSize().padding(32.dp),
            )
            return@Column
        }

        when (mode) {
            ComparisonMode.ImageDiff -> {
                ImageDiffPanel(
                    beforeBytes = beforeBytes,
                    afterBytes = afterBytes,
                    imageDiff = imageDiff,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }

            ComparisonMode.ImageSwipe -> {
                ImageSwipeComparison(
                    beforeBytes = beforeBytes,
                    afterBytes = afterBytes,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }

            ComparisonMode.ImageBefore -> {
                ImageViewport(
                    bytes = beforeBytes,
                    label = "Before",
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }

            ComparisonMode.ImageAfter -> {
                ImageViewport(
                    bytes = afterBytes,
                    label = "After",
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }

            else -> Unit
        }
    }
}

@Composable
private fun ImageDiffSummary(
    beforeSnap: Snap,
    afterSnap: Snap,
    imageDiff: SnapshotImageDiff?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val changedText =
                when {
                    imageDiff == null -> "Preparing"
                    imageDiff.error != null -> "Unavailable"
                    else -> formatImagePercent(imageDiff.mismatchPercentage)
                }

            ImageStat(
                label = "Changed",
                value = changedText,
                tone = when {
                    imageDiff?.error != null -> MaterialTheme.colorScheme.error
                    imageDiff?.match == true -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
            )
            ImageStat(
                label = "Pixels",
                value = imageDiff?.let {
                    "${formatPixelCount(it.differentPixels)} / ${formatPixelCount(it.totalPixels)}"
                } ?: "Waiting",
            )
            ImageStat(
                label = "Before",
                value = imageDiff?.let { "${it.beforeWidth} x ${it.beforeHeight}" }
                    ?: formatTimestamp(beforeSnap.timestamp),
            )
            ImageStat(
                label = "After",
                value = imageDiff?.let { "${it.afterWidth} x ${it.afterHeight}" }
                    ?: formatTimestamp(afterSnap.timestamp),
            )
        }
    }
}

@Composable
private fun ImageStat(
    label: String,
    value: String,
    tone: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = Modifier.widthIn(min = 92.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = tone,
            maxLines = 1,
        )
    }
}

@Composable
private fun ImageDiffPanel(
    beforeBytes: ByteArray,
    afterBytes: ByteArray,
    imageDiff: SnapshotImageDiff?,
    modifier: Modifier = Modifier,
) {
    when {
        imageDiff == null -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }

        imageDiff.error != null -> {
            SharedEmptyState(
                icon = Icons.Default.Warning,
                title = "Image diff unavailable",
                description = imageDiff.error,
                modifier = modifier.padding(32.dp),
            )
        }

        imageDiff.dimensionMismatch != null -> {
            DimensionMismatchPanel(
                beforeBytes = beforeBytes,
                afterBytes = afterBytes,
                mismatch = imageDiff.dimensionMismatch,
                modifier = modifier,
            )
        }

        imageDiff.match -> {
            SharedEmptyState(
                icon = Icons.Default.CheckCircle,
                title = stringResource(Res.string.comparison_no_changes_title),
                description = "The captured screenshots are pixel-identical.",
                modifier = modifier.padding(32.dp),
            )
        }

        imageDiff.diffImagePng != null -> {
            ImageViewport(
                bytes = imageDiff.diffImagePng,
                label = "Changed pixels",
                modifier = modifier,
            )
        }

        else -> {
            SharedEmptyState(
                icon = Icons.Default.Warning,
                title = "Image diff unavailable",
                description = "The screenshots were decoded, but no visual diff image was generated.",
                modifier = modifier.padding(32.dp),
            )
        }
    }
}

@Composable
private fun DimensionMismatchPanel(
    beforeBytes: ByteArray,
    afterBytes: ByteArray,
    mismatch: SnapshotDimensionMismatch,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = "Screenshot size changed: " +
                        "${mismatch.beforeWidth} x ${mismatch.beforeHeight} -> " +
                        "${mismatch.afterWidth} x ${mismatch.afterHeight}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            ImageViewport(
                bytes = beforeBytes,
                label = "Before",
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            ImageViewport(
                bytes = afterBytes,
                label = "After",
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun ImageSwipeComparison(
    beforeBytes: ByteArray,
    afterBytes: ByteArray,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        var splitFraction by remember { mutableFloatStateOf(0.5f) }
        val splitX = widthPx * splitFraction
        val draggableState = rememberDraggableState { delta ->
            splitFraction = (splitFraction + delta / widthPx).coerceIn(0f, 1f)
        }

        ImageViewport(
            bytes = beforeBytes,
            label = null,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(HorizontalSplitShape(splitX)),
        ) {
            ImageViewport(
                bytes = afterBytes,
                label = null,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ImagePillLabel("Before")
            ImagePillLabel("After")
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(splitX.roundToInt(), 0) }
                .fillMaxHeight()
                .width(3.dp)
                .background(MaterialTheme.colorScheme.primary)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                ),
        )

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        splitX.roundToInt() - 20.dp.toPx().roundToInt(),
                        0,
                    )
                }
                .fillMaxHeight()
                .width(40.dp)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                    contentDescription = "Drag to compare screenshots",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(8.dp).size(20.dp),
                )
            }
        }
    }
}

@Composable
internal fun ImageViewport(
    bytes: ByteArray,
    label: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest),
        contentAlignment = Alignment.Center,
    ) {
        SnapshotImage(
            bytes = bytes,
            contentDescription = label,
            modifier = Modifier.fillMaxSize().padding(12.dp),
            contentScale = ContentScale.Fit,
        )

        if (label != null) {
            ImagePillLabel(
                text = label,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            )
        }
    }
}

@Composable
private fun ImagePillLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
        tonalElevation = 3.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

private fun formatImagePercent(value: Double): String = "${((value * 10).roundToInt() / 10.0)}%"

private fun formatPixelCount(value: Long): String = when {
    value >= 1_000_000 -> "${((value / 100_000.0).roundToInt() / 10.0)}M"
    value >= 1_000 -> "${((value / 100.0).roundToInt() / 10.0)}K"
    else -> value.toString()
}
