/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import com.bernaferrari.changedetection.image.SnapshotDimensionMismatch

import com.bernaferrari.changedetection.image.SnapshotImageDiff

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.focusable
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.*
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
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_dimensions
import com.bernaferrari.changedetection.shared.generated.resources.done
import com.bernaferrari.changedetection.shared.generated.resources.retry
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_highlight_unavailable_body
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_dimensions_supporting
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
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_no_detected_change
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_pixels
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_sampled_pixels_different
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_pixels_different
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_preparing
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_size_changed
import com.bernaferrari.changedetection.shared.generated.resources.image_diff_status_unavailable
import com.bernaferrari.changedetection.ui.components.AppLoadingIndicator
import com.bernaferrari.changedetection.shared.generated.resources.comparison_mode_source
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

internal enum class ComparisonMode {
    Text,
    Source,
    Html,
    ImageDiff,
    ImageSwipe,
    ImageBefore,
    ImageAfter,
}

internal fun ComparisonMode.labelResource(): StringResource = when (this) {
    ComparisonMode.Source -> Res.string.comparison_mode_source
    ComparisonMode.Text -> Res.string.comparison_mode_text
    ComparisonMode.Html -> Res.string.comparison_mode_rendered
    ComparisonMode.ImageDiff -> Res.string.comparison_mode_pixel_diff
    ComparisonMode.ImageSwipe -> Res.string.comparison_mode_swipe
    ComparisonMode.ImageBefore -> Res.string.comparison_mode_before
    ComparisonMode.ImageAfter -> Res.string.comparison_mode_after
}

@Composable
internal fun ImageComparisonContent(
    beforeBytes: ByteArray?,
    afterBytes: ByteArray?,
    imageDiff: SnapshotImageDiff?,
    mode: ComparisonMode,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Box(modifier = modifier) {
        val singleImage = when (mode) {
            ComparisonMode.ImageBefore -> beforeBytes
            ComparisonMode.ImageAfter -> afterBytes
            else -> null
        }
        if (singleImage != null) {
            ImageViewport(singleImage, stringResource(mode.labelResource()), modifier = Modifier.fillMaxSize())
            return@Box
        }
        if (beforeBytes == null || afterBytes == null) {
            ImageComparisonNotice(
                icon = Icons.Default.Warning,
                title = stringResource(Res.string.image_comparison_unavailable),
                description = stringResource(Res.string.failed_to_load_content),
                modifier = Modifier.fillMaxSize(),
                onRetry = onRetry,
            )
            return@Box
        }

        if (mode == ComparisonMode.ImageDiff && imageDiff?.error != null) {
            ImageComparisonNotice(
                icon = Icons.Default.Warning,
                title = stringResource(Res.string.image_diff_status_unavailable),
                description = stringResource(Res.string.image_diff_highlight_unavailable_body),
                modifier = Modifier.fillMaxSize(),
                onRetry = onRetry,
            )
            return@Box
        }

        when (mode) {
            ComparisonMode.ImageDiff -> {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val summaryMaxHeight = maxHeight * 0.35f
                    Column(Modifier.fillMaxSize()) {
                        if (imageDiff?.match != true) {
                            Column(Modifier.fillMaxWidth().heightIn(max = summaryMaxHeight).verticalScroll(rememberScrollState()).testTag("pixel-diff-summary")) {
                                ImageDiffSummary(
                                    imageDiff = imageDiff,
                                    modifier = Modifier.align(Alignment.End).padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                                imageDiff?.dimensionMismatch?.let { DimensionMismatchNotice(it) }
                            }
                        }
                        ImageDiffPanel(
                            beforeBytes = beforeBytes,
                            afterBytes = afterBytes,
                            imageDiff = imageDiff,
                            modifier = Modifier.fillMaxWidth().weight(1f).testTag("pixel-diff-images"),
                        )
                    }
                }
            }

            ComparisonMode.ImageSwipe -> {
                ImageSwipeComparison(
                    beforeBytes = beforeBytes,
                    afterBytes = afterBytes,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            ComparisonMode.ImageBefore -> {
                ImageViewport(
                    bytes = beforeBytes,
                    label = stringResource(Res.string.comparison_mode_before),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            ComparisonMode.ImageAfter -> {
                ImageViewport(
                    bytes = afterBytes,
                    label = stringResource(Res.string.comparison_mode_after),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            else -> Unit
        }
    }
}

/** Status messages share the reader's space and remain scrollable at large font sizes. */
@Composable
private fun ImageComparisonNotice(icon: ImageVector, title: String, description: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow).verticalScroll(rememberScrollState()).testTag("image-comparison-notice").padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onRetry != null) TextButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
    }
}

@Composable
private fun ImageDiffSummary(
    imageDiff: SnapshotImageDiff?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.widthIn(max = 260.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = when {
                        imageDiff == null -> stringResource(Res.string.image_diff_preparing)
                        imageDiff.error != null -> stringResource(Res.string.image_diff_status_unavailable)
                        imageDiff.dimensionMismatch != null -> stringResource(Res.string.image_diff_dimensions)
                        imageDiff.match -> stringResource(Res.string.comparison_no_changes_title)
                        else -> "${formatImagePercent(imageDiff.mismatchPercentage)} ${stringResource(Res.string.image_diff_changed).lowercase()}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = when {
                        imageDiff?.dimensionMismatch != null -> stringResource(Res.string.image_diff_dimensions_supporting)

                        imageDiff != null && imageDiff.error == null -> stringResource(
                            if (imageDiff.isSampled) Res.string.image_diff_sampled_pixels_different else Res.string.image_diff_pixels_different,
                            formatPixelCount(imageDiff.differentPixels),
                        )

                        else -> stringResource(Res.string.image_diff_drag_supporting)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                modifier = modifier,
            )
        }

        imageDiff.match -> {
            ImageComparisonNotice(
                icon = Icons.Default.CheckCircle,
                title = stringResource(Res.string.comparison_no_changes_title),
                description = stringResource(Res.string.image_diff_no_detected_change),
                modifier = modifier,
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
private fun DimensionMismatchNotice(mismatch: SnapshotDimensionMismatch) {
    var expanded by remember(mismatch) { mutableStateOf(false) }
    TextButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.image_diff_dimensions), style = MaterialTheme.typography.labelLarge)
    }
    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text(stringResource(Res.string.image_diff_dimensions)) },
            text = {
                Text(
                    stringResource(Res.string.image_diff_size_changed, mismatch.beforeWidth, mismatch.beforeHeight, mismatch.afterWidth, mismatch.afterHeight),
                    Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = { TextButton(onClick = { expanded = false }) { Text(stringResource(Res.string.done)) } },
        )
    }
}

@Composable
private fun DimensionMismatchPanel(
    beforeBytes: ByteArray,
    afterBytes: ByteArray,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        ImageViewport(beforeBytes, stringResource(Res.string.comparison_mode_before), modifier = Modifier.weight(1f).fillMaxHeight())
        ImageViewport(afterBytes, stringResource(Res.string.comparison_mode_after), modifier = Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun ImageSwipeComparison(
    beforeBytes: ByteArray,
    afterBytes: ByteArray,
    modifier: Modifier = Modifier,
) {
    val labelInset = with(LocalDensity.current) { MaterialTheme.typography.labelMedium.lineHeight.toDp() } + 24.dp
    val dragLabel = stringResource(Res.string.a11y_drag_screenshot_comparison)
    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        var splitFraction by remember { mutableFloatStateOf(0.5f) }
        var handleFocused by remember { mutableStateOf(false) }
        val splitX = widthPx * splitFraction
        val draggableState = rememberDraggableState { delta ->
            splitFraction = (splitFraction + delta / widthPx).coerceIn(0f, 1f)
        }

        // A single vertical viewport moves both full-width captures together. Long pages
        // remain readable, and captures of different heights still share one scroll range.
        Box(
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceBright)
                .verticalScroll(rememberScrollState())
                .testTag("screenshot-scroll"),
        ) {
            Box(Modifier.fillMaxWidth().padding(top = labelInset)) {
                WidthSizedImage(beforeBytes, stringResource(Res.string.comparison_mode_before))
            }
            Box(Modifier.fillMaxWidth().padding(top = labelInset).clip(HorizontalSplitShape(splitX))) {
                WidthSizedImage(afterBytes, stringResource(Res.string.comparison_mode_after))
            }
        }

        // The split is in physical image coordinates, independent of interface direction.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            ImagePillLabel(stringResource(Res.string.comparison_mode_before), Modifier.align(AbsoluteAlignment.TopLeft))
            ImagePillLabel(stringResource(Res.string.comparison_mode_after), Modifier.align(AbsoluteAlignment.TopRight))
        }

        Box(
            modifier = Modifier
                .align(AbsoluteAlignment.TopLeft)
                .absoluteOffset { IntOffset(splitX.roundToInt(), 0) }
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
                .align(AbsoluteAlignment.TopLeft)
                .absoluteOffset {
                    IntOffset(
                        (splitX.roundToInt() - 28.dp.toPx().roundToInt()).coerceIn(0, (widthPx - 56.dp.toPx()).roundToInt().coerceAtLeast(0)),
                        0,
                    )
                }
                .fillMaxHeight()
                .width(56.dp)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                )
                .semantics {
                    contentDescription = dragLabel
                    stateDescription = "${(splitFraction * 100).roundToInt()}%"
                    progressBarRangeInfo = ProgressBarRangeInfo(splitFraction, 0f..1f)
                    setProgress { target ->
                        if (target.isFinite()) {
                            splitFraction = target.coerceIn(0f, 1f)
                            true
                        } else {
                            false
                        }
                    }
                }
                .onFocusChanged { handleFocused = it.isFocused }
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val target = when (event.key) {
                        Key.DirectionLeft -> splitFraction - 0.05f
                        Key.DirectionRight -> splitFraction + 0.05f
                        Key.MoveHome -> 0f
                        Key.MoveEnd -> 1f
                        else -> return@onKeyEvent false
                    }
                    splitFraction = target.coerceIn(0f, 1f)
                    true
                }
                .focusable(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.size(width = 44.dp, height = 56.dp),
                border = if (handleFocused) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ImageViewport(
    bytes: ByteArray,
    label: String?,
    contentDescription: String? = label,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceBright),
        contentAlignment = Alignment.Center,
    ) {
        val labelInset = if (label == null) 0.dp else with(LocalDensity.current) { MaterialTheme.typography.labelMedium.lineHeight.toDp() } + 24.dp
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("screenshot-scroll")) {
            Box(Modifier.fillMaxWidth().padding(top = labelInset)) { WidthSizedImage(bytes, contentDescription) }
        }

        if (label != null) {
            ImagePillLabel(
                text = label,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            )
        }
    }
}

/** Preserve screenshot text size at the available width; tall captures scroll vertically. */
@Composable
private fun WidthSizedImage(bytes: ByteArray, contentDescription: String?) {
    SnapshotImage(
        bytes = bytes,
        contentDescription = contentDescription,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        contentScale = ContentScale.FillWidth,
    )
}

@Composable
private fun ImagePillLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
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
