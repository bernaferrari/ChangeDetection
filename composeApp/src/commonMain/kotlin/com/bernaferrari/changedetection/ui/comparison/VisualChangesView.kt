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
internal fun VisualChangesView(beforeBytes: ByteArray, afterBytes: ByteArray, diff: SnapshotImageDiff?, modifier: Modifier = Modifier) {
    val change = diff?.visualChange
    val regions = change?.regions.orEmpty()
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    if (change == null || regions.isEmpty()) {
        Box(modifier.padding(32.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.visual_no_areas), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Row(modifier.fillMaxSize()) {
        PageOverview(afterBytes, regions, Modifier.width(64.dp).fillMaxHeight().padding(start = 12.dp, top = 8.dp, bottom = 16.dp)) { index ->
            scope.launch { list.animateScrollToItem(index + 1) }
        }
        LazyColumn(
            state = list,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("summary") {
                Text(
                    com.bernaferrari.changedetection.ui.kit.cropSummary(com.bernaferrari.changedetection.preview.ChangePreview.Crop("", "", null, 0.0, false, regions)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }
            itemsIndexed(regions) { index, region -> RegionCard(index + 1, region, beforeBytes, afterBytes) }
        }
    }
}

/** The whole page, shrunk, with each area marked; tapping a mark jumps to its card. */
@Composable
private fun PageOverview(afterBytes: ByteArray, regions: List<VisualRegion>, modifier: Modifier, onJump: (Int) -> Unit) {
    val marks = MaterialTheme.marks
    BoxWithConstraints(modifier) {
        val available = maxHeight
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
        ) {
            SnapshotImage(afterBytes, null, Modifier.fillMaxWidth().height(available), ContentScale.FillBounds)
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
                    drawRect(color.copy(alpha = 0.12f), Offset(0f, y), Size(size.width, h))
                    drawRect(color, Offset(0f, y), Size(3.dp.toPx(), h))
                    drawRect(color, Offset(size.width - 3.dp.toPx(), y), Size(3.dp.toPx(), h))
                }
            }
        }
        // Tap targets over the marks; at least 24dp so they are easy to hit.
        regions.forEachIndexed { index, region ->
            Box(
                Modifier.fillMaxWidth().height(24.dp).offset(y = available * region.afterTop - 8.dp).clickable { onJump(index) },
            )
        }
    }
}

@Composable
private fun RegionCard(number: Int, region: VisualRegion, beforeBytes: ByteArray, afterBytes: ByteArray) {
    val label = stringResource(
        when (region.kind) {
            VisualRegion.Kind.Changed -> Res.string.visual_changed
            VisualRegion.Kind.Added -> Res.string.visual_added
            VisualRegion.Kind.Removed -> Res.string.visual_removed
        },
    )
    Surface(shape = MaterialTheme.shapes.extraLarge, color = tileColor, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$label $number" }) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    val shape = RoundedCornerShape(14.dp)
    // Pad the crop a little so the change has context, and keep at least a third of the width.
    val padX = 0.03f
    var l = (left - padX).coerceAtLeast(0f)
    var r = (right + padX).coerceAtMost(1f)
    if (r - l < 1f / 3f) {
        val c = (l + r) / 2f
        l = (c - 1f / 6f).coerceIn(0f, 2f / 3f)
        r = l + 1f / 3f
    }
    val t = (top - 0.004f).coerceAtLeast(0f)
    val b = (bottom + 0.004f).coerceAtMost(1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (label != null) Text(label, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BoxWithConstraints(
            Modifier.fillMaxWidth().clip(shape).background(Color.White)
                .border(if (highlighted) 2.dp else 1.dp, if (highlighted) marks.highlightEdge else if (removed) marks.removed else MaterialTheme.colorScheme.outlineVariant, shape),
        ) {
            val cardWidth = constraints.maxWidth
            // Image width so that the [l, r] slice fills the card.
            val imageWidth = (cardWidth / (r - l)).roundToInt()
            val aspect by produceState(0f, bytes) { value = imageAspect(bytes) }
            val imageHeight = if (aspect > 0f) imageWidth / aspect else 0f
            val cropHeightPx = ((b - t) * imageHeight).roundToInt().coerceIn(48, 1_400)
            Box(
                Modifier.fillMaxWidth().height(with(androidx.compose.ui.platform.LocalDensity.current) { cropHeightPx.toDp() }).clipToBounds(),
            ) {
                if (aspect > 0f) {
                    SnapshotImage(
                        bytes,
                        null,
                        Modifier.layout { measurable, _ ->
                            val placeable = measurable.measure(Constraints.fixed(imageWidth, imageHeight.roundToInt().coerceAtLeast(1)))
                            layout(cardWidth, cropHeightPx) { placeable.place(-(l * imageWidth).roundToInt(), -(t * imageHeight).roundToInt()) }
                        },
                        ContentScale.FillBounds,
                    )
                }
                if (removed) {
                    Box(Modifier.matchParentSize().background(marks.removed.copy(alpha = 0.08f)))
                    Icon(Icons.Outlined.VisibilityOff, null, Modifier.align(Alignment.TopEnd).padding(8.dp).size(18.dp), tint = marks.removed)
                }
            }
        }
    }
}

/** Width / height of an encoded image, read once per capture. */
internal suspend fun imageAspect(bytes: ByteArray): Float = com.bernaferrari.changedetection.image.decodeArgbAtWidth(bytes, 64)?.let { (_, w, h) -> w.toFloat() / h } ?: 0f
