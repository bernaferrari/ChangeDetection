/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.image.SnapshotImageDiff
import com.bernaferrari.changedetection.image.VisualRegion
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.preview_after
import com.bernaferrari.changedetection.shared.generated.resources.preview_before
import com.bernaferrari.changedetection.shared.generated.resources.visual_added
import com.bernaferrari.changedetection.shared.generated.resources.visual_area_n
import com.bernaferrari.changedetection.shared.generated.resources.visual_changed
import com.bernaferrari.changedetection.shared.generated.resources.visual_no_areas
import com.bernaferrari.changedetection.shared.generated.resources.visual_removed
import com.bernaferrari.changedetection.shared.generated.resources.visual_summary
import com.bernaferrari.changedetection.ui.kit.tileColor
import com.bernaferrari.changedetection.ui.theme.marks
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * A screenshot change as a short list of areas. A page overview on the side shows where each area is;
 * each card shows that area before and after, cropped and enlarged, so a changed headline is readable
 * without hunting through a 10,000-pixel page.
 */
@Composable
internal fun VisualChangesView(beforeBytes: ByteArray, afterBytes: ByteArray, diff: SnapshotImageDiff?, cursor: RegionCursor, modifier: Modifier = Modifier) {
    val change = diff?.visualChange
    val regions = change?.regions.orEmpty()
    // Item 0 is the summary; area n is item n + 1. Opens on the shared cursor's area.
    val list = rememberLazyListState(initialFirstVisibleItemIndex = if (cursor.current in regions.indices && cursor.current > 0) cursor.current + 1 else 0)
    val scope = rememberCoroutineScope()
    DisposableEffect(cursor, regions) {
        cursor.changeCount = regions.size
        cursor.scroller = { index -> list.animateScrollToItem(index + 1) }
        onDispose { cursor.scroller = null }
    }
    LaunchedEffect(cursor, regions) {
        if (regions.isEmpty()) return@LaunchedEffect
        // Live while scrolling: the change whose card crosses the top third of the screen is the one being read.
        snapshotFlow {
            val info = list.layoutInfo
            val line = info.viewportStartOffset + (info.viewportEndOffset - info.viewportStartOffset) / 3
            info.visibleItemsInfo.lastOrNull { it.index > 0 && it.offset <= line }?.index ?: list.firstVisibleItemIndex
        }.collect { index -> cursor.current = (index - 1).coerceIn(0, regions.lastIndex) }
    }
    if (change == null || regions.isEmpty()) {
        Box(modifier.padding(32.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.visual_no_areas), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        // The page overview only earns its width on wide screens; on phones every point goes to readable crops.
        val wide = maxWidth >= 600.dp
        Row(Modifier.fillMaxSize()) {
            // A numbered map of the page: where each change is, and a tap away.
            run {
                PageOverview(afterBytes, regions, cursor.current, Modifier.width(if (wide) 72.dp else 48.dp).fillMaxHeight().padding(start = 8.dp, top = 8.dp, bottom = 16.dp)) { index ->
                    scope.launch { cursor.go(index - cursor.current) }
                }
            }
            LazyColumn(
                state = list,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentPadding = PaddingValues(start = if (wide) 12.dp else 8.dp, end = 8.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item("summary") {
                    Text(
                        com.bernaferrari.changedetection.ui.kit.cropSummary(com.bernaferrari.changedetection.preview.ChangePreview.Crop("", "", null, 0.0, false, regions)),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                    )
                }
                itemsIndexed(regions) { index, region -> RegionCard(index + 1, region, beforeBytes, afterBytes) }
            }
        }
    }
}

/** The whole page, shrunk, with each area marked; tapping a mark jumps to its card. */
@Composable
private fun PageOverview(afterBytes: ByteArray, regions: List<VisualRegion>, current: Int, modifier: Modifier, onJump: (Int) -> Unit) {
    val marks = MaterialTheme.marks
    val aspect by produceState(0f, afterBytes) { value = imageAspect(afterBytes) }
    BoxWithConstraints(modifier) {
        // Keep the page's proportions: a short page is a short overview, never a stretched one.
        val available = if (aspect > 0f) minOf(maxHeight, maxWidth / aspect) else maxHeight
        val stripWidth = maxWidth
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
        ) {
            // A small decode of the whole page, cached: the strip is only a map.
            val widthPx = with(androidx.compose.ui.platform.LocalDensity.current) { stripWidth.roundToPx() * 2 }
            val thumb by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, afterBytes, widthPx) {
                value = com.bernaferrari.changedetection.image.CropCache.get(afterBytes, 0f, 0f, 1f, 1f, widthPx)
            }
            Box(Modifier.fillMaxWidth().height(available)) {
                thumb?.let { androidx.compose.foundation.Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds) }
            }
            Canvas(Modifier.matchParentSize()) {
                regions.forEachIndexed { index, region ->
                    val top = if (region.kind == VisualRegion.Kind.Removed) region.afterTop else region.afterTop
                    val bottom = if (region.kind == VisualRegion.Kind.Removed) region.afterTop else region.afterBottom
                    val y = top * size.height
                    val h = ((bottom - top) * size.height).coerceAtLeast(3.dp.toPx())
                    val color = when (region.kind) {
                        VisualRegion.Kind.Removed -> marks.removed
                        else -> marks.highlightEdge
                    }
                    // The change being read stands out; the rest stay quiet.
                    drawRect(color.copy(alpha = if (index == current) 0.35f else 0.10f), Offset(0f, y), Size(size.width, h))
                    drawRect(color, Offset(0f, y), Size(3.dp.toPx(), h))
                    drawRect(color, Offset(size.width - 3.dp.toPx(), y), Size(3.dp.toPx(), h))
                }
            }
        }
        // Numbered badges match the cards: nudged apart so they never overlap, and kept inside the strip
        // (squeezed together near the bottom when a page has many changes there).
        val badge = 20.dp
        val positions = regions.map { available * it.afterTop - badge / 2 }.toMutableList()
        for (i in positions.indices) positions[i] = maxOf(positions[i], if (i == 0) 0.dp else positions[i - 1] + badge + 2.dp)
        val step = if (regions.size > 1) minOf(badge + 2.dp, (available - badge) / (regions.size - 1)) else badge
        for (i in positions.indices.reversed()) {
            val limit = if (i == positions.lastIndex) available - badge else positions[i + 1] - step
            positions[i] = minOf(positions[i], limit).coerceAtLeast(0.dp)
        }
        regions.forEachIndexed { index, _ ->
            val y = positions[index]
            val selected = index == current
            Box(
                Modifier.align(Alignment.TopCenter).offset(y = y).size(if (selected) 22.dp else 18.dp).clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable(onClickLabel = (index + 1).toString()) { onJump(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text((index + 1).toString(), style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private val CardCorner = 20.dp
private val CardPadding = 6.dp

@Composable
private fun RegionCard(number: Int, region: VisualRegion, beforeBytes: ByteArray, afterBytes: ByteArray) {
    val label = stringResource(
        when (region.kind) {
            VisualRegion.Kind.Changed -> Res.string.visual_changed
            VisualRegion.Kind.Added -> Res.string.visual_added
            VisualRegion.Kind.Removed -> Res.string.visual_removed
        },
    )
    Surface(shape = RoundedCornerShape(CardCorner), color = tileColor, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$label $number" }) {
        Column(Modifier.padding(start = CardPadding, end = CardPadding, top = 10.dp, bottom = CardPadding), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.padding(start = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Text(number.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text(label, style = MaterialTheme.typography.titleSmall)
            }
            when (region.kind) {
                VisualRegion.Kind.Changed -> {
                    CropLabelled(stringResource(Res.string.preview_before), beforeBytes, region.left, region.right, region.beforeTop, region.beforeBottom, highlighted = false)
                    CropLabelled(stringResource(Res.string.preview_after), afterBytes, region.left, region.right, region.afterTop, region.afterBottom, highlighted = true)
                }
                VisualRegion.Kind.Added -> CropLabelled(null, afterBytes, region.left, region.right, region.afterTop, region.afterBottom, highlighted = true)
                VisualRegion.Kind.Removed -> CropLabelled(null, beforeBytes, region.left, region.right, region.beforeTop, region.beforeBottom, highlighted = false, removed = true)
            }
        }
    }
}

/**
 * One crop of a capture at full card width. Wide areas show the whole width; narrow ones are zoomed so text
 * stays readable, never past 3x. Very tall areas are capped and fade out.
 */
@Composable
private fun CropLabelled(label: String?, bytes: ByteArray, left: Float, right: Float, top: Float, bottom: Float, highlighted: Boolean, removed: Boolean = false) {
    val marks = MaterialTheme.marks
    // Concentric with the card around it: the card's corner minus its padding.
    val shape = RoundedCornerShape(CardCorner - CardPadding)
    // Every crop shows the full page width at the same scale: captures are phone-width already, so text reads at
    // its natural size, nothing is cut at the sides, and every change looks alike.
    val l = 0f
    val r = 1f

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (label != null) {
            Text(label, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelMedium, color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BoxWithConstraints(
            Modifier.fillMaxWidth().clip(shape).background(Color.White)
                .border(if (highlighted) 2.dp else 1.dp, if (highlighted) marks.highlightEdge else if (removed) marks.removed else MaterialTheme.colorScheme.outlineVariant, shape),
        ) {
            val cardWidth = constraints.maxWidth
            // Image width so that the [l, r] slice fills the card.
            val imageWidth = (cardWidth / (r - l)).roundToInt()
            val aspect by produceState(0f, bytes) { value = imageAspect(bytes) }
            val imageHeight = if (aspect > 0f) imageWidth / aspect else 0f
            // A little room above and below (about a line's breathing space), whatever the page's length.
            val pad = if (imageHeight > 0f) with(androidx.compose.ui.platform.LocalDensity.current) { 10.dp.toPx() } / imageHeight else 0.004f
            // Never stretch: a thin change gets more room around it, a very tall one is cut (not squeezed).
            var t = (top - pad).coerceAtLeast(0f)
            var b = (bottom + pad).coerceAtMost(1f)
            if (imageHeight > 0f) {
                val minFraction = 48f / imageHeight
                if (b - t < minFraction) {
                    val c = (t + b) / 2f
                    t = (c - minFraction / 2f).coerceIn(0f, (1f - minFraction).coerceAtLeast(0f))
                    b = (t + minFraction).coerceAtMost(1f)
                }
                val maxFraction = 1_400f / imageHeight
                if (b - t > maxFraction) b = t + maxFraction
            }
            val cropHeightPx = ((b - t) * imageHeight).roundToInt().coerceAtLeast(1)
            // Only this rectangle is decoded (and cached); the box has its final size before the pixels arrive.
            val crop by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, bytes, l, r, t, b, cardWidth) {
                value = com.bernaferrari.changedetection.image.CropCache.get(bytes, l, t, r, b, cardWidth)
            }
            Box(
                Modifier.fillMaxWidth().height(with(androidx.compose.ui.platform.LocalDensity.current) { cropHeightPx.toDp() })
                    .background(if (crop == null) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White),
            ) {
                crop?.let {
                    androidx.compose.foundation.Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds, filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium)
                }
                if (removed) {
                    Box(Modifier.matchParentSize().background(marks.removed.copy(alpha = 0.08f)))
                    Icon(Icons.Outlined.VisibilityOff, null, Modifier.align(Alignment.TopEnd).padding(8.dp).size(18.dp), tint = marks.removed)
                }
            }
        }
    }
}

/** Width / height of an encoded image. PNG sizes come straight from the header; other formats are decoded small. */
internal suspend fun imageAspect(bytes: ByteArray): Float {
    com.bernaferrari.changedetection.image.pngSize(bytes)?.let { (w, h) -> return w.toFloat() / h }
    return com.bernaferrari.changedetection.image.decodeArgbAtWidth(bytes, 64)?.let { (_, w, h) -> w.toFloat() / h } ?: 0f
}
