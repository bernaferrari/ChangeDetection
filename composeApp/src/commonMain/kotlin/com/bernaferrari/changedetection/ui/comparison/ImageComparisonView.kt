/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Warning
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
import com.bernaferrari.changedetection.shared.generated.resources.a11y_drag_screenshot_comparison
import com.bernaferrari.changedetection.shared.generated.resources.comparison_mode_after
import com.bernaferrari.changedetection.shared.generated.resources.comparison_mode_before
import com.bernaferrari.changedetection.shared.generated.resources.comparison_mode_pixel_diff
import com.bernaferrari.changedetection.shared.generated.resources.comparison_mode_rendered
import com.bernaferrari.changedetection.shared.generated.resources.comparison_mode_swipe
import com.bernaferrari.changedetection.shared.generated.resources.comparison_mode_text
import com.bernaferrari.changedetection.shared.generated.resources.comparison_no_changes_title
import com.bernaferrari.changedetection.shared.generated.resources.failed_to_load_content
import com.bernaferrari.changedetection.shared.generated.resources.image_comparison_unavailable
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_changed
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_changed_pixels
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_drag_supporting
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_missing_output
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_pixel_identical
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_pixels
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_pixels_different
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_preparing
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_same_dimensions
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_size_changed
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_status_unavailable
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_unavailable
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_waiting
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.ui.components.AppLoadingIndicator
import org.jetbrains.compose.resources.StringResource
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

internal fun ComparisonMode.labelResource(): StringResource = when (this) {
    ComparisonMode.Text -> Res.string.comparison_mode_text
    ComparisonMode.Html -> Res.string.comparison_mode_rendered
    ComparisonMode.ImageDiff -> Res.string.comparison_mode_pixel_diff
    ComparisonMode.ImageSwipe -> Res.string.comparison_mode_swipe
    ComparisonMode.ImageBefore -> Res.string.comparison_mode_before
    ComparisonMode.ImageAfter -> Res.string.comparison_mode_after
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
            imageDiff = imageDiff,
        )

        if (beforeBytes == null || afterBytes == null) {
            SharedEmptyState(
                icon = Icons.Default.Warning,
                title = stringResource(Res.string.image_comparison_unavailable),
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
                    label = stringResource(Res.string.comparison_mode_before),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }

            ComparisonMode.ImageAfter -> {
                ImageViewport(
                    bytes = afterBytes,
                    label = stringResource(Res.string.comparison_mode_after),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }

            else -> Unit
        }
    }
}

@Composable
private fun ImageDiffSummary(
    imageDiff: SnapshotImageDiff?,
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Default.Compare,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp).size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = when {
                        imageDiff == null -> stringResource(Res.string.image_diff_preparing)
                        imageDiff.error != null -> stringResource(Res.string.image_diff_status_unavailable)
                        imageDiff.match -> stringResource(Res.string.comparison_no_changes_title)
                        else -> "${formatImagePercent(imageDiff.mismatchPercentage)} ${stringResource(Res.string.image_diff_changed).lowercase()}"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = imageDiff?.takeIf { it.error == null }?.let {
                        stringResource(Res.string.image_diff_pixels_different, formatPixelCount(it.differentPixels))
                    } ?: stringResource(Res.string.image_diff_drag_supporting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                )
            }
            imageDiff?.takeIf { it.beforeWidth == it.afterWidth && it.beforeHeight == it.afterHeight }?.let { diff ->
                Text(
                    text = stringResource(Res.string.image_diff_same_dimensions, diff.beforeWidth, diff.beforeHeight),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                )
            }
        }
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
                AppLoadingIndicator(size = 24.dp)
            }
        }

        imageDiff.error != null -> {
            ImageSwipeComparison(
                beforeBytes = beforeBytes,
                afterBytes = afterBytes,
                modifier = modifier,
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
                description = stringResource(Res.string.image_diff_pixel_identical),
                modifier = modifier.padding(32.dp),
            )
        }

        imageDiff.diffImagePng != null -> {
            ImageViewport(
                bytes = imageDiff.diffImagePng,
                label = stringResource(Res.string.image_diff_changed_pixels),
                modifier = modifier,
            )
        }

        else -> {
            ImageSwipeComparison(
                beforeBytes = beforeBytes,
                afterBytes = afterBytes,
                modifier = modifier,
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
                    text = stringResource(
                        Res.string.image_diff_size_changed,
                        mismatch.beforeWidth,
                        mismatch.beforeHeight,
                        mismatch.afterWidth,
                        mismatch.afterHeight,
                    ),
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
                label = stringResource(Res.string.comparison_mode_before),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            ImageViewport(
                bytes = afterBytes,
                label = stringResource(Res.string.comparison_mode_after),
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
            ImagePillLabel(stringResource(Res.string.comparison_mode_before))
            ImagePillLabel(stringResource(Res.string.comparison_mode_after))
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
                    contentDescription = stringResource(Res.string.a11y_drag_screenshot_comparison),
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
