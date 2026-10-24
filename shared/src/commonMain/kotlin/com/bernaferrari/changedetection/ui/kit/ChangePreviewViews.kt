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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.bernaferrari.changedetection.shared.generated.resources.preview_more_changes
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
            value = loadPreview {
                repository?.latest(siteId) { before, after ->
                    value = ChangePreview.Crop(before.snapId, after.snapId, null, 0.0, false, isPreparing = true)
                }
            }
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
        contentKey = { if (it is ChangePreview.Crop && it.isPreparing) "preparing" else it?.let { p -> p::class } },
        modifier = modifier.fillMaxWidth(),
        label = "preview",
    ) { current ->
        Column {
            PreviewBody(current, cropHeight)
            PreviewFooter(current)
        }
    }
}

@Composable
private fun PreviewBody(current: ChangePreview?, cropHeight: Dp) {
    when (current) {
        // Comparing two full-page captures takes a moment; a gentle pulse says it is on its way.
        null -> Box(Modifier.fillMaxWidth().height(cropHeight).clip(MaterialTheme.shapes.large).placeholderPulse())

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

        // While a screenshot change is prepared, the two tiles it will fill pulse in place.
        is ChangePreview.Crop -> Row(Modifier.fillMaxWidth().height(cropHeight), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (current.isPreparing) {
                repeat(2) { Box(Modifier.weight(1f).fillMaxHeight().clip(TileShape).placeholderPulse()) }
            } else {
                val lead = current.lead
                val beforeBounds = lead?.let { ChangedBounds(it.left, it.beforeTop, it.right, it.beforeBottom) } ?: current.bounds
                val afterBounds = lead?.let { ChangedBounds(it.left, it.afterTop, it.right, it.afterBottom) } ?: current.bounds
                // Like the reader's change cards: something new or gone has one side worth showing, not two.
                when (lead?.kind) {
                    com.bernaferrari.changedetection.image.VisualRegion.Kind.Added -> CropTile(current.afterSnapId, afterBounds, stringResource(Res.string.visual_added), true, Modifier.weight(1f))

                    com.bernaferrari.changedetection.image.VisualRegion.Kind.Removed -> CropTile(current.beforeSnapId, beforeBounds, stringResource(Res.string.visual_removed), false, Modifier.weight(1f))

                    else -> {
                        CropTile(current.beforeSnapId, beforeBounds, stringResource(Res.string.preview_before), false, Modifier.weight(1f))
                        CropTile(current.afterSnapId, afterBounds, stringResource(Res.string.preview_after), true, Modifier.weight(1f))
                    }
                }
            }
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

/** How big the change is ("7 changes", "+2 more changes"), under every preview that has more than it shows. */
@Composable
private fun PreviewFooter(current: ChangePreview?) {
    val text = when (current) {
        is ChangePreview.Crop -> if (current.isPreparing) null else cropSummary(current)
        is ChangePreview.Words -> if (current.otherChanges > 0) com.bernaferrari.changedetection.localization.pluralStringResource(Res.plurals.preview_more_changes, current.otherChanges, current.otherChanges) else return
        else -> return
    }
    val modifier = Modifier.padding(start = 4.dp, top = 10.dp)
    if (text == null) {
        // Holds the line the count will fill, so the card does not grow when it arrives.
        val height = with(androidx.compose.ui.platform.LocalDensity.current) { MaterialTheme.typography.bodySmall.lineHeight.toDp() }
        Box(modifier.size(width = 72.dp, height = height).clip(RoundedCornerShape(50)).placeholderPulse())
    } else {
        Text(text, modifier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
 * One capture at full page width, centered on the changed area. The page around the change is washed out, so the
 * change stands out without a second frame inside the tile's own border. A change taller than the tile starts at its top.
 */
@Composable
private fun ChangeSpot(snapId: String, changed: ChangedBounds?, modifier: Modifier = Modifier) {
    // Without a known area (a size change), the top of the page is shown as it is.
    val bounds = changed ?: ChangedBounds(0f, 0f, 1f, 0.12f)
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
            // Reading a capture from disk is quick: the blank page stays until it is ready, with no progress bar.
            if (loaded.second && (bytes == null || aspect == 0f)) UnavailableTile()
            return@BoxWithConstraints
        }
        val data = requireNotNull(bytes)
        val density = androidx.compose.ui.platform.LocalDensity.current
        val boxW = constraints.maxWidth.toFloat()
        val boxH = constraints.maxHeight.toFloat()
        // Full page width, like the comparison: natural text size, nothing cut at the sides.
        val imageH = boxW / aspect
        val top = bounds.top * imageH
        val bottom = bounds.bottom * imageH
        val pad = with(density) { 14.dp.toPx() }
        val y = if (bottom - top + pad * 2 <= boxH) boxH / 2f - (top + bottom) / 2f else pad - top
        val offsetY = y.coerceIn(minOf(0f, boxH - imageH), 0f)
        // Decode just the visible window of the capture (cached), not the whole page.
        val windowTop = (-offsetY / imageH).coerceIn(0f, 1f)
        val windowBottom = ((-offsetY + boxH) / imageH).coerceIn(windowTop, 1f)
        val cropState by produceState<Pair<androidx.compose.ui.graphics.ImageBitmap?, Boolean>>(null to false, data, windowTop, windowBottom, boxW) {
            value = value.first to false
            value = com.bernaferrari.changedetection.image.CropCache.get(data, 0f, windowTop, 1f, windowBottom, boxW.roundToInt()) to true
        }
        val crop = cropState.first
        if (cropState.second && crop == null) {
            UnavailableTile()
            return@BoxWithConstraints
        }
        val shown by androidx.compose.animation.core.animateFloatAsState(if (crop != null) 1f else 0f, label = "crop-in")
        crop?.let {
            androidx.compose.foundation.Image(
                it,
                null,
                Modifier.fillMaxWidth().height(with(density) { ((windowBottom - windowTop) * imageH).toDp() }).graphicsLayer { alpha = shown },
                contentScale = ContentScale.FillBounds,
                filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium,
            )
        }
        val wash = MaterialTheme.marks.page.copy(alpha = 0.6f * shown)
        if (changed != null) {
            androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                val inset = 4.dp.toPx()
                val rectTop = (top + offsetY - inset).coerceIn(0f, size.height)
                val rectBottom = (bottom + offsetY + inset).coerceIn(rectTop, size.height)
                if (rectTop > 0f) drawRect(wash, size = androidx.compose.ui.geometry.Size(size.width, rectTop))
                if (rectBottom < size.height) drawRect(wash, topLeft = androidx.compose.ui.geometry.Offset(0f, rectBottom), size = androidx.compose.ui.geometry.Size(size.width, size.height - rectBottom))
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.UnavailableTile() {
    Text(
        stringResource(Res.string.preview_unavailable),
        Modifier.align(Alignment.Center).padding(12.dp),
        style = MaterialTheme.typography.bodySmall,
        color = androidx.compose.ui.graphics.Color(0xFF5F5E66),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

private val TileShape = RoundedCornerShape(16.dp)

/**
 * One side of a change: the same treatment as the reader's change cards, where only the newer capture wears
 * the highlight colour.
 */
@Composable
fun CropTile(snapId: String, bounds: ChangedBounds?, label: String, highlighted: Boolean, modifier: Modifier = Modifier) {
    val marks = MaterialTheme.marks
    Box(
        modifier
            .fillMaxSize()
            .clip(TileShape)
            .background(marks.page)
            .border(if (highlighted) 2.dp else 1.dp, if (highlighted) marks.highlightEdge else MaterialTheme.colorScheme.outlineVariant, TileShape),
    ) {
        ChangeSpot(snapId, bounds, Modifier.fillMaxSize())
        Box(
            Modifier.align(Alignment.BottomEnd).padding(6.dp).clip(RoundedCornerShape(7.dp))
                .background(if (highlighted) marks.highlight else MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 7.dp, vertical = 2.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (highlighted) marks.onHighlight else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A soft pulse for content that is on its way, in place of progress bars. */
@Composable
fun Modifier.placeholderPulse(): Modifier {
    val pulse = androidx.compose.animation.core.rememberInfiniteTransition(label = "placeholder")
    val alpha by pulse.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(900), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "placeholder-alpha",
    )
    val color = MaterialTheme.colorScheme.surfaceContainerHigh
    return drawBehind { drawRect(color, alpha = alpha) }
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
