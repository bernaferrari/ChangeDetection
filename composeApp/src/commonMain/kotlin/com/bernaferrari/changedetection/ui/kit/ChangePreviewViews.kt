/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.kit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.image.ChangedBounds
import com.bernaferrari.changedetection.preview.ChangePreview
import com.bernaferrari.changedetection.preview.ChangePreviewRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.label_first_version
import com.bernaferrari.changedetection.shared.generated.resources.label_percent
import com.bernaferrari.changedetection.shared.generated.resources.label_saved
import com.bernaferrari.changedetection.shared.generated.resources.label_text_changed
import com.bernaferrari.changedetection.shared.generated.resources.preview_size_changed
import com.bernaferrari.changedetection.shared.generated.resources.preview_after
import com.bernaferrari.changedetection.shared.generated.resources.preview_before
import com.bernaferrari.changedetection.shared.generated.resources.preview_first_version
import com.bernaferrari.changedetection.shared.generated.resources.preview_invisible
import com.bernaferrari.changedetection.shared.generated.resources.preview_invisible_body
import com.bernaferrari.changedetection.shared.generated.resources.preview_large
import com.bernaferrari.changedetection.shared.generated.resources.preview_large_body
import com.bernaferrari.changedetection.shared.generated.resources.preview_unavailable
import com.bernaferrari.changedetection.ui.comparison.SnapshotImage
import com.bernaferrari.changedetection.ui.theme.marks
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.remember
import org.koin.mp.KoinPlatformTools

/** Loads the bounded preview for a page's newest change; re-runs only when a new version lands. */
@Composable
fun rememberLatestPreview(siteId: String, latestSnapId: String?): ChangePreview? {
    val repository = remember { optional<ChangePreviewRepository>() }
    val preview by produceState<ChangePreview?>(null, siteId, latestSnapId) {
        value = repository?.latest(siteId) ?: ChangePreview.Unavailable
    }
    return preview
}

@Composable
fun rememberVersionPreview(snap: com.bernaferrari.changedetection.repo.Snap): ChangePreview? {
    val repository = remember { optional<ChangePreviewRepository>() }
    val preview by produceState<ChangePreview?>(null, snap.snapId) {
        value = repository?.forVersion(snap) ?: ChangePreview.Unavailable
    }
    return preview
}

@Composable
fun rememberPairPreview(beforeSnapId: String?, afterSnapId: String): ChangePreview? {
    val repository = remember { optional<ChangePreviewRepository>() }
    val snaps = remember { optional<SnapsRepository>() }
    val preview by produceState<ChangePreview?>(null, beforeSnapId, afterSnapId) {
        value = if (repository == null || snaps == null) {
            ChangePreview.Unavailable
        } else if (beforeSnapId == null) {
            ChangePreview.FirstVersion
        } else {
            val before = snaps.getSnapById(beforeSnapId)
            val after = snaps.getSnapById(afterSnapId)
            if (before == null || after == null) ChangePreview.Unavailable else repository.between(before, after)
        }
    }
    return preview
}

/** One short line describing a change, for lists of versions and activity. */
@Composable
fun previewLabel(preview: ChangePreview?): String = when (preview) {
    null -> " "
    is ChangePreview.Words -> preview.section ?: stringResource(Res.string.label_text_changed)
    is ChangePreview.Crop -> if (preview.sizeChanged) stringResource(Res.string.preview_size_changed) else stringResource(Res.string.label_percent, com.bernaferrari.changedetection.ui.home.formatPercent(preview.percent))
    is ChangePreview.Large -> stringResource(Res.string.preview_large)
    ChangePreview.Invisible -> stringResource(Res.string.preview_invisible)
    ChangePreview.FirstVersion -> stringResource(Res.string.label_first_version)
    ChangePreview.Unavailable -> stringResource(Res.string.label_saved)
}

/** The card body: a bounded preview, whatever the size of the real diff. */
@Composable
fun ChangePreviewBlock(preview: ChangePreview?, modifier: Modifier = Modifier, cropHeight: Dp = 112.dp) {
    AnimatedContent(
        targetState = preview,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentKey = { it?.let { p -> p::class } },
        modifier = modifier.fillMaxWidth(),
        label = "preview",
    ) { current ->
        when (current) {
            null -> Box(
                Modifier.fillMaxWidth().height(56.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer),
            )

            is ChangePreview.Words -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (current.section != null) {
                    Text(current.section, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DiffText(editText(current.segments, MaterialTheme.marks, current.leadingEllipsis, current.trailingEllipsis), maxLines = 4)
            }

            is ChangePreview.Crop -> CropPair(current.beforeSnapId, current.afterSnapId, current.bounds, cropHeight)

            is ChangePreview.Large -> QuietFact(
                Icons.Outlined.Layers,
                stringResource(Res.string.preview_large),
                stringResource(Res.string.preview_large_body, current.changedLines, current.totalLines),
            )

            ChangePreview.Invisible -> QuietFact(Icons.Outlined.VisibilityOff, stringResource(Res.string.preview_invisible), stringResource(Res.string.preview_invisible_body))

            ChangePreview.FirstVersion -> Text(stringResource(Res.string.preview_first_version), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            ChangePreview.Unavailable -> Text(stringResource(Res.string.preview_unavailable), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuietFact(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Before and after, cropped to the same changed area, side by side. */
@Composable
fun CropPair(beforeSnapId: String, afterSnapId: String, bounds: ChangedBounds?, height: Dp, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CropTile(beforeSnapId, bounds, stringResource(Res.string.preview_before), highlighted = false, Modifier.weight(1f))
        CropTile(afterSnapId, bounds, stringResource(Res.string.preview_after), highlighted = true, Modifier.weight(1f))
    }
}

@Composable
fun CropTile(snapId: String, bounds: ChangedBounds?, label: String, highlighted: Boolean, modifier: Modifier = Modifier) {
    val snaps = remember { optional<SnapsRepository>() }
    val bytes by produceState<ByteArray?>(null, snapId) { value = runCatching { snaps?.getSnapContent(snapId) }.getOrNull() }
    val shape = RoundedCornerShape(14.dp)
    val marks = MaterialTheme.marks
    Box(
        modifier
            .fillMaxSize()
            .clip(shape)
            .background(marks.page)
            .border(if (highlighted) 2.5.dp else 1.dp, if (highlighted) marks.highlightEdge else MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        bytes?.let { data -> CroppedImage(data, bounds, Modifier.fillMaxSize()) }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(6.dp).clip(RoundedCornerShape(7.dp))
                .background(if (highlighted) marks.highlight else MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 7.dp, vertical = 2.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (highlighted) marks.onHighlight else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Scales the capture so the changed area fills the tile, centered, never zooming past 3×.
 * Without bounds (identical pixels, size change) the top of the page is shown.
 */
@Composable
fun CroppedImage(bytes: ByteArray, bounds: ChangedBounds?, modifier: Modifier = Modifier) {
    val padded = bounds?.let {
        val padX = 0.04f
        val padY = 0.02f
        val minWidth = 1f / 3f
        var left = (it.left - padX).coerceAtLeast(0f)
        var right = (it.right + padX).coerceAtMost(1f)
        if (right - left < minWidth) {
            val center = (left + right) / 2f
            left = (center - minWidth / 2f).coerceIn(0f, 1f - minWidth)
            right = left + minWidth
        }
        ChangedBounds(left, (it.top - padY).coerceAtLeast(0f), right, (it.bottom + padY).coerceAtMost(1f))
    }
    Box(modifier.clipToBounds()) {
        SnapshotImage(
            bytes = bytes,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.layout { measurable, constraints ->
                val boxWidth = constraints.maxWidth
                val boxHeight = constraints.maxHeight
                val region = padded ?: ChangedBounds(0f, 0f, 1f, 1f)
                val imageWidth = (boxWidth / region.width).roundToInt().coerceAtLeast(boxWidth)
                val placeable = measurable.measure(Constraints.fixedWidth(imageWidth))
                layout(boxWidth, boxHeight) {
                    val x = -(region.left * imageWidth).roundToInt()
                    val centerY = if (padded == null) 0f else (region.top + region.height / 2f) * placeable.height
                    val y = if (padded == null) 0 else (boxHeight / 2f - centerY).roundToInt().coerceIn(minOf(0, boxHeight - placeable.height), 0)
                    placeable.place(x, y)
                }
            },
        )
    }
}

/** Resolves a dependency when the app's container is running; previews and UI tests may not have one. */
internal inline fun <reified T : Any> optional(): T? = KoinPlatformTools.defaultContext().getOrNull()?.getOrNull<T>()
