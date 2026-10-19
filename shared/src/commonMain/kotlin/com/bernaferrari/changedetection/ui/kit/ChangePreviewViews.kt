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
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
import com.bernaferrari.changedetection.shared.generated.resources.loading
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
import com.bernaferrari.changedetection.localization.stringResource
import androidx.compose.runtime.remember
import org.koin.mp.KoinPlatformTools
import kotlinx.coroutines.CancellationException

/** Loads the bounded preview for a page's newest change; re-runs only when a new version lands. */
@Composable
fun rememberLatestPreview(siteId: String, latestSnapId: String?): ChangePreview? {
    val repository = remember { optional<ChangePreviewRepository>() }
    val preview by androidx.compose.runtime.key(siteId) {
        produceState<ChangePreview?>(repository?.cachedVersion(latestSnapId), latestSnapId) {
            // Keep the last readable preview while this page's next version is prepared.
            value = loadPreview { repository?.latest(siteId) { before, after ->
                value = ChangePreview.Crop(before.snapId, after.snapId, null, 0.0, false, isPreparing = true)
            } }
        }
    }
    return preview
}

@Composable
fun rememberVersionPreview(snap: com.bernaferrari.changedetection.repo.Snap): ChangePreview? {
    val repository = remember { optional<ChangePreviewRepository>() }
    val preview by androidx.compose.runtime.key(snap.snapId) {
        produceState<ChangePreview?>(repository?.cachedVersion(snap.snapId), snap.snapId) {
            value = loadPreview { repository?.forVersion(snap) }
        }
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
    null -> stringResource(com.bernaferrari.changedetection.shared.generated.resources.Res.string.loading)
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
fun ChangePreviewBlock(preview: ChangePreview?, modifier: Modifier = Modifier, cropHeight: Dp = 148.dp) {
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
                if (!label.isNullOrBlank()) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                DiffText(editText(current.segments, MaterialTheme.marks, current.leadingEllipsis, current.trailingEllipsis), maxLines = 4)
            }

            is ChangePreview.Crop -> Box {
                val lead = current.lead
                val beforeBounds = lead?.let { ChangedBounds(it.left, it.beforeTop, it.right, it.beforeBottom) } ?: current.bounds
                val afterBounds = lead?.let { ChangedBounds(it.left, it.afterTop, it.right, it.afterBottom) } ?: current.bounds
                Row(Modifier.fillMaxWidth().height(cropHeight), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CropTile(current.beforeSnapId, beforeBounds, stringResource(Res.string.preview_before), false, Modifier.weight(1f))
                    CropTile(current.afterSnapId, afterBounds, stringResource(Res.string.preview_after), true, Modifier.weight(1f))
                }
                if (current.isPreparing) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
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
    if (preview.isPreparing) return stringResource(Res.string.loading)
    if (preview.regions.isEmpty()) {
        return if (preview.sizeChanged) stringResource(Res.string.preview_size_changed) else stringResource(Res.string.label_percent, com.bernaferrari.changedetection.ui.home.formatPercent(preview.percent))
    }
    // One number: "4 changes". Whether each is new, updated or removed is shown on the change itself.
    return com.bernaferrari.changedetection.localization.pluralStringResource(Res.plurals.crop_changed, preview.regions.size, preview.regions.size)
}

/**
 * The most important area of a screenshot change, as one wide tile: zoomed to the change and outlined, so it
 * is readable at a glance. The Before/After pill flips the tile in place (a tap elsewhere still opens the change); each side is cropped from its own
 * capture, so content that moved down the page still lines up.
 */
@Composable
fun RegionPair(beforeSnapId: String, afterSnapId: String, region: com.bernaferrari.changedetection.image.VisualRegion, height: Dp, modifier: Modifier = Modifier) {
    val kind = region.kind
    val canFlip = kind == com.bernaferrari.changedetection.image.VisualRegion.Kind.Changed
    var showBefore by remember(beforeSnapId, afterSnapId) { mutableStateOf(kind == com.bernaferrari.changedetection.image.VisualRegion.Kind.Removed) }
    val marks = MaterialTheme.marks
    val shape = RoundedCornerShape(16.dp)
    val label = stringResource(
        when {
            kind == com.bernaferrari.changedetection.image.VisualRegion.Kind.Added -> Res.string.visual_added
            kind == com.bernaferrari.changedetection.image.VisualRegion.Kind.Removed -> Res.string.visual_removed
            showBefore -> Res.string.preview_before
            else -> Res.string.preview_after
        },
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(marks.page)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .semantics { contentDescription = label },
    ) {
        androidx.compose.animation.Crossfade(showBefore, label = "flip") { before ->
            val snapId = if (before) beforeSnapId else afterSnapId
            val bounds = if (before) {
                ChangedBounds(region.left, region.beforeTop, region.right, region.beforeBottom)
            } else {
                ChangedBounds(region.left, region.afterTop, region.right, region.afterBottom)
            }
            ChangeSpot(snapId, bounds, outline = marks.highlightEdge, modifier = Modifier.fillMaxSize())
        }
        if (canFlip) {
            BeforeAfterPill(showBefore, onSelect = { showBefore = it }, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp))
        } else {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (showBefore) MaterialTheme.colorScheme.surfaceContainerHigh else marks.highlight)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = if (showBefore) MaterialTheme.colorScheme.onSurfaceVariant else marks.onHighlight)
            }
        }
    }
}

@Composable
private fun BeforeAfterPill(showBefore: Boolean, onSelect: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val marks = MaterialTheme.marks
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.94f)).padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        listOf(true to stringResource(Res.string.preview_before), false to stringResource(Res.string.preview_after)).forEach { (before, text) ->
            val selected = showBefore == before
            Text(
                text,
                Modifier.clip(RoundedCornerShape(8.dp))
                    .background(
                        if (!selected) {
                            androidx.compose.ui.graphics.Color.Transparent
                        } else if (before) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            marks.highlight
                        },
                    )
                    .clickable { onSelect(before) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected && !before) marks.onHighlight else MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) 1f else 0.7f),
            )
        }
    }
}

/**
 * One capture, zoomed so the changed area fills the tile's width (never past 3×), centered on it, with the area
 * outlined. A change taller than the tile starts at its top.
 */
@Composable
private fun ChangeSpot(snapId: String, bounds: ChangedBounds, outline: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val snaps = remember { optional<SnapsRepository>() }
    val loaded by produceState<Pair<ByteArray?, Boolean>>(null to false, snapId) {
        value = try {
            snaps?.getSnapContent(snapId).let { it to true }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null to true
        }
    }
    val bytes = loaded.first
    val aspect by produceState(-1f, bytes) {
        value = -1f
        value = bytes?.let { com.bernaferrari.changedetection.ui.comparison.imageAspect(it) } ?: -1f
    }

    BoxWithConstraints(modifier.clipToBounds()) {
        if (bytes == null || aspect <= 0f) {
            if (loaded.second && (bytes == null || aspect == 0f)) Text(stringResource(Res.string.preview_unavailable), Modifier.align(Alignment.Center).padding(12.dp), style = MaterialTheme.typography.bodySmall)
            else androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            return@BoxWithConstraints
        }
        val data = requireNotNull(bytes)
        val density = androidx.compose.ui.platform.LocalDensity.current
        val boxW = constraints.maxWidth.toFloat()
        val boxH = constraints.maxHeight.toFloat()
        // Full page width, like the comparison: natural text size, nothing cut at the sides.
        val l = 0f
        val r = 1f
        val imageW = boxW / (r - l)
        val imageH = imageW / aspect
        val top = bounds.top * imageH
        val bottom = bounds.bottom * imageH
        val pad = with(density) { 14.dp.toPx() }
        val y = if (bottom - top + pad * 2 <= boxH) boxH / 2f - (top + bottom) / 2f else pad - top
        val offsetY = y.coerceIn(minOf(0f, boxH - imageH), 0f)
        val offsetX = -l * imageW
        // Decode just the visible window of the capture (cached), not the whole page.
        val windowTop = (-offsetY / imageH).coerceIn(0f, 1f)
        val windowBottom = ((-offsetY + boxH) / imageH).coerceIn(windowTop, 1f)
        val cropState by produceState<Pair<androidx.compose.ui.graphics.ImageBitmap?, Boolean>>(null to false, data, l, r, windowTop, windowBottom, boxW) {
            value = value.first to false
            value = com.bernaferrari.changedetection.image.CropCache.get(data, l, windowTop, r, windowBottom, boxW.roundToInt()) to true
        }
        val crop = cropState.first
        if (!cropState.second) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        else if (crop == null) Text(stringResource(Res.string.preview_unavailable), Modifier.align(Alignment.Center).padding(12.dp), style = MaterialTheme.typography.bodySmall)
        crop?.let {
            androidx.compose.foundation.Image(
                it,
                null,
                Modifier.fillMaxWidth().height(with(density) { ((windowBottom - windowTop) * imageH).toDp() }),
                contentScale = ContentScale.FillBounds,
                filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium,
            )
        }
        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            val edge = 2.dp.toPx()
            val inset = 3.dp.toPx()
            val left = (bounds.left * imageW + offsetX - inset).coerceAtLeast(edge)
            val right = (bounds.right * imageW + offsetX + inset).coerceAtMost(size.width - edge)
            val rectTop = (top + offsetY - inset).coerceAtLeast(edge)
            val rectBottom = (bottom + offsetY + inset).coerceAtMost(size.height - edge)
            if (right > left && rectBottom > rectTop) {
                drawRoundRect(
                    outline,
                    topLeft = androidx.compose.ui.geometry.Offset(left, rectTop),
                    size = androidx.compose.ui.geometry.Size(right - left, rectBottom - rectTop),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(edge),
                )
            }
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
    val shape = RoundedCornerShape(16.dp)
    val marks = MaterialTheme.marks
    Box(
        modifier
            .fillMaxSize()
            .clip(shape)
            .background(marks.page)
            .border(if (highlighted) 2.5.dp else 1.dp, if (highlighted) marks.highlightEdge else MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        ChangeSpot(snapId, bounds ?: ChangedBounds(0f, 0f, 1f, 0.12f), if (highlighted) marks.highlightEdge else MaterialTheme.colorScheme.outlineVariant, Modifier.fillMaxSize())
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
