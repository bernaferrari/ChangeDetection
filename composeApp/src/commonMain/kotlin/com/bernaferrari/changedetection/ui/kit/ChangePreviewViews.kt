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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloat
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
import com.bernaferrari.changedetection.shared.generated.resources.preview_nothing_visible
import com.bernaferrari.changedetection.shared.generated.resources.preview_nothing_visible_body
import com.bernaferrari.changedetection.shared.generated.resources.preview_large
import com.bernaferrari.changedetection.shared.generated.resources.preview_numbers_only
import com.bernaferrari.changedetection.shared.generated.resources.visual_added
import com.bernaferrari.changedetection.shared.generated.resources.visual_removed
import com.bernaferrari.changedetection.shared.generated.resources.crop_changed
import com.bernaferrari.changedetection.shared.generated.resources.crop_added
import com.bernaferrari.changedetection.shared.generated.resources.crop_removed
import com.bernaferrari.changedetection.shared.generated.resources.preview_added
import com.bernaferrari.changedetection.shared.generated.resources.preview_reordered
import com.bernaferrari.changedetection.shared.generated.resources.preview_reordered_body
import com.bernaferrari.changedetection.shared.generated.resources.preview_removed
import com.bernaferrari.changedetection.shared.generated.resources.preview_updated
import com.bernaferrari.changedetection.shared.generated.resources.preview_large_body
import com.bernaferrari.changedetection.shared.generated.resources.preview_unavailable
import com.bernaferrari.changedetection.ui.comparison.SnapshotImage
import com.bernaferrari.changedetection.ui.theme.marks
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.remember
import org.koin.mp.KoinPlatformTools
import kotlinx.coroutines.CancellationException

/** Loads the bounded preview for a page's newest change; re-runs only when a new version lands. */
@Composable
fun rememberLatestPreview(siteId: String, latestSnapId: String?): ChangePreview? {
    val repository = remember { optional<ChangePreviewRepository>() }
    val preview by produceState<ChangePreview?>(null, siteId, latestSnapId) {
        value = null
        value = loadPreview { repository?.latest(siteId) }
    }
    return preview
}

@Composable
fun rememberVersionPreview(snap: com.bernaferrari.changedetection.repo.Snap): ChangePreview? {
    val repository = remember { optional<ChangePreviewRepository>() }
    val preview by produceState<ChangePreview?>(null, snap.snapId) {
        value = null
        value = loadPreview { repository?.forVersion(snap) }
    }
    return preview
}

@Composable
fun rememberPairPreview(beforeSnapId: String?, afterSnapId: String): ChangePreview? {
    val repository = remember { optional<ChangePreviewRepository>() }
    val snaps = remember { optional<SnapsRepository>() }
    val preview by produceState<ChangePreview?>(null, beforeSnapId, afterSnapId) {
        value = null
        value = loadPreview {
            if (repository == null || snaps == null) {
                ChangePreview.Unavailable
            } else if (beforeSnapId == null) {
                ChangePreview.FirstVersion
            } else {
                val before = snaps.getSnapById(beforeSnapId)
                val after = snaps.getSnapById(afterSnapId)
                if (before == null || after == null) ChangePreview.Unavailable else repository.between(before, after)
            }
        }
    }
    return preview
}

private suspend fun loadPreview(load: suspend () -> ChangePreview?): ChangePreview = try {
    load() ?: ChangePreview.Unavailable
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    ChangePreview.Unavailable
}

/** One short line describing a change, for lists of versions and activity. */
@Composable
fun previewLabel(preview: ChangePreview?): String = when (preview) {
    null -> " "
    is ChangePreview.Words -> if (preview.numbersOnly) stringResource(Res.string.preview_numbers_only) else preview.section ?: stringResource(Res.string.label_text_changed)
    is ChangePreview.Crop -> cropSummary(preview)
    is ChangePreview.Large -> largeSummary(preview)
    ChangePreview.Invisible -> stringResource(Res.string.preview_invisible)
    ChangePreview.NothingVisible -> stringResource(Res.string.preview_nothing_visible)
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
            // Comparing two full-page captures takes a moment; a gentle pulse says it is on its way.
            null -> {
                val pulse = androidx.compose.animation.core.rememberInfiniteTransition(label = "preview-loading")
                val alpha by pulse.animateFloat(
                    initialValue = 0.45f,
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(900), androidx.compose.animation.core.RepeatMode.Reverse),
                    label = "preview-loading-alpha",
                )
                Box(
                    Modifier.fillMaxWidth().height(cropHeight).graphicsLayer { this.alpha = alpha }.clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerLow),
                )
            }

            is ChangePreview.Words -> Column(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val label = if (current.numbersOnly) stringResource(Res.string.preview_numbers_only) else current.section
                if (label != null) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DiffText(editText(current.segments, MaterialTheme.marks, current.leadingEllipsis, current.trailingEllipsis), maxLines = 4)
            }

            is ChangePreview.Crop -> {
                val lead = current.lead
                if (lead != null) RegionPair(current.beforeSnapId, current.afterSnapId, lead, cropHeight) else CropPair(current.beforeSnapId, current.afterSnapId, current.bounds, cropHeight)
            }

            is ChangePreview.Large -> if (current.reordered) {
                QuietFact(Icons.Outlined.Layers, stringResource(Res.string.preview_reordered), stringResource(Res.string.preview_reordered_body))
            } else if (current.samples.isEmpty()) {
                QuietFact(Icons.Outlined.Layers, stringResource(Res.string.preview_large), stringResource(Res.string.preview_large_body, current.changedLines, current.totalLines))
            } else {
                LargeChange(current)
            }

            ChangePreview.Invisible -> QuietFact(Icons.Outlined.VisibilityOff, stringResource(Res.string.preview_invisible), stringResource(Res.string.preview_invisible_body))
            ChangePreview.NothingVisible -> QuietFact(Icons.Outlined.VisibilityOff, stringResource(Res.string.preview_nothing_visible), stringResource(Res.string.preview_nothing_visible_body))

            ChangePreview.FirstVersion -> Text(stringResource(Res.string.preview_first_version), Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(14.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            ChangePreview.Unavailable -> Text(stringResource(Res.string.preview_unavailable), Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(14.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** "2 new · 1 removed · 20 updated", or what kind of churn it was when nothing is genuinely new. */
@Composable
fun largeSummary(preview: ChangePreview.Large): String {
    if (preview.reordered) return stringResource(Res.string.preview_reordered)
    if (preview.added == 0 && preview.removed == 0) {
        return if (preview.numbersOnly) stringResource(Res.string.preview_numbers_only) else stringResource(Res.string.preview_large_body, preview.changedLines, preview.totalLines)
    }
    return listOfNotNull(
        preview.added.takeIf { it > 0 }?.let { stringResource(Res.string.preview_added, it) },
        preview.removed.takeIf { it > 0 }?.let { stringResource(Res.string.preview_removed, it) },
        preview.updated.takeIf { it > 0 }?.let { stringResource(Res.string.preview_updated, it) },
    ).joinToString(" · ")
}

/** A big text change as its few most meaningful lines, one per row, with the size of the whole change. */
@Composable
private fun LargeChange(preview: ChangePreview.Large) {
    val marks = MaterialTheme.marks
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(largeSummary(preview), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        preview.samples.forEach { sample ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 9.dp).size(5.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.outline))
                DiffText(editText(sample, marks), maxLines = 2, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuietFact(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** "3 areas changed · 1 new" for screenshot changes; falls back to the share of pixels. */
@Composable
fun cropSummary(preview: ChangePreview.Crop): String {
    if (preview.regions.isEmpty()) {
        return if (preview.sizeChanged) stringResource(Res.string.preview_size_changed) else stringResource(Res.string.label_percent, com.bernaferrari.changedetection.ui.home.formatPercent(preview.percent))
    }
    val added = preview.regions.count { it.kind == com.bernaferrari.changedetection.image.VisualRegion.Kind.Added }
    val removed = preview.regions.count { it.kind == com.bernaferrari.changedetection.image.VisualRegion.Kind.Removed }
    val changed = preview.regions.size - added - removed
    return listOfNotNull(
        changed.takeIf { it > 0 }?.let { org.jetbrains.compose.resources.pluralStringResource(Res.plurals.crop_changed, it, it) },
        added.takeIf { it > 0 }?.let { org.jetbrains.compose.resources.pluralStringResource(Res.plurals.crop_added, it, it) },
        removed.takeIf { it > 0 }?.let { org.jetbrains.compose.resources.pluralStringResource(Res.plurals.crop_removed, it, it) },
    ).joinToString(" · ")
}

/**
 * The most important area of a screenshot change. Each side is cropped from its own capture, so content that
 * moved down the page still lines up; a new area shows only the new capture.
 */
@Composable
fun RegionPair(beforeSnapId: String, afterSnapId: String, region: com.bernaferrari.changedetection.image.VisualRegion, height: Dp, modifier: Modifier = Modifier) {
    val before = ChangedBounds(region.left, region.beforeTop, region.right, region.beforeBottom)
    val after = ChangedBounds(region.left, region.afterTop, region.right, region.afterBottom)
    when (region.kind) {
        com.bernaferrari.changedetection.image.VisualRegion.Kind.Changed -> Row(modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CropTile(beforeSnapId, before, stringResource(Res.string.preview_before), highlighted = false, Modifier.weight(1f))
            CropTile(afterSnapId, after, stringResource(Res.string.preview_after), highlighted = true, Modifier.weight(1f))
        }
        com.bernaferrari.changedetection.image.VisualRegion.Kind.Added -> CropTile(afterSnapId, after, stringResource(Res.string.visual_added), highlighted = true, modifier.fillMaxWidth().height(height))
        com.bernaferrari.changedetection.image.VisualRegion.Kind.Removed -> CropTile(beforeSnapId, before, stringResource(Res.string.visual_removed), highlighted = false, modifier.fillMaxWidth().height(height))
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
    val bytes by produceState<ByteArray?>(null, snapId) {
        value = null
        value = try {
            snaps?.getSnapContent(snapId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }
    val shape = RoundedCornerShape(16.dp)
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

/**
 * The top of a saved capture, cropped to fill [modifier]. Decoded small and cached by snapshot, so long
 * full-page screenshots stay cheap in lists.
 */
@Composable
fun CaptureTop(snapId: String, modifier: Modifier, alpha: Float = 1f, decodeWidth: Int = 720) {
    val bytes by produceState<ByteArray?>(null, snapId) {
        value = runCatching { optional<com.bernaferrari.changedetection.repo.source.SnapsRepository>()?.getSnapContent(snapId) }.getOrNull()
    }
    val context = coil3.compose.LocalPlatformContext.current
    Box(modifier.background(androidx.compose.ui.graphics.Color.White)) {
        bytes?.let { data ->
            coil3.compose.AsyncImage(
                model = remember(snapId, decodeWidth) {
                    coil3.request.ImageRequest.Builder(context).data(data).memoryCacheKey("top-$snapId-$decodeWidth").size(coil3.size.Size(coil3.size.Dimension(decodeWidth), coil3.size.Dimension.Undefined)).build()
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                alpha = alpha,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
