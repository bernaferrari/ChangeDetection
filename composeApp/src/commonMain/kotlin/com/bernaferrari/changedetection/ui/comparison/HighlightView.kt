/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.image.VisualRegion
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.reader_next
import com.bernaferrari.changedetection.shared.generated.resources.reader_position
import com.bernaferrari.changedetection.shared.generated.resources.reader_previous
import com.bernaferrari.changedetection.ui.theme.marks
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The new screenshot with every changed area marked. It opens on the current change (the first, at first),
 * and the shared bottom bar steps through the rest; scrolling by hand keeps that bar in sync.
 */
@Composable
internal fun HighlightView(afterBytes: ByteArray, regions: List<VisualRegion>, cursor: RegionCursor, modifier: Modifier = Modifier) {
    val marks = MaterialTheme.marks
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val aspect by produceState(0f, afterBytes) { value = imageAspect(afterBytes) }
    val ordered = regions
    val current = cursor.current

    BoxWithConstraints(modifier.background(MaterialTheme.colorScheme.surfaceBright).testTag("highlight-changes")) {
        val density = LocalDensity.current
        val pad = 12.dp
        val imageWidthPx = with(density) { (maxWidth - pad * 2).toPx() }
        val imageHeightPx = if (aspect > 0f) imageWidthPx / aspect else 0f
        val padPx = with(density) { pad.toPx() }
        val viewportPx = with(density) { maxHeight.toPx() }
        // A change sits a quarter of the way down the screen, so what comes before it is still visible.
        fun targetFor(index: Int): Int = ordered.getOrNull(index)?.let { region ->
            (padPx + region.afterTop * imageHeightPx - viewportPx * 0.25f).roundToInt().coerceAtLeast(0)
        } ?: 0

        fun go(index: Int) {
            if (index !in ordered.indices) return
            scope.launch { cursor.go(index - cursor.current) }
        }

        DisposableEffect(cursor, ordered, imageHeightPx) {
            cursor.changeCount = ordered.size
            cursor.scroller = { index -> scroll.animateScrollTo(targetFor(index)) }
            onDispose { cursor.scroller = null }
        }

        // Open on the current change as soon as the screenshot's size is known.
        LaunchedEffect(ordered, imageHeightPx) {
            if (imageHeightPx > 0f && ordered.isNotEmpty()) scroll.scrollTo(targetFor(cursor.current.coerceIn(0, ordered.lastIndex)))
        }
        // After a manual scroll, the bar follows the change nearest to that quarter line.
        LaunchedEffect(ordered, imageHeightPx) {
            if (imageHeightPx <= 0f || ordered.isEmpty()) return@LaunchedEffect
            snapshotFlow { scroll.isScrollInProgress to scroll.value }.collectLatest { (moving, value) ->
                if (moving) return@collectLatest
                val line = (value + viewportPx * 0.25f - padPx) / imageHeightPx
                cursor.current = ordered.indices.minBy { abs(ordered[it].afterTop - line) }
            }
        }

        Box(Modifier.fillMaxSize().verticalScroll(scroll).padding(start = pad, end = pad, top = pad, bottom = 24.dp)) {
            if (aspect > 0f) {
                Box(Modifier.fillMaxWidth().aspectRatio(aspect)) {
                    SnapshotImage(afterBytes, null, Modifier.fillMaxSize(), ContentScale.FillBounds)
                    Canvas(Modifier.matchParentSize()) {
                        val edge = 2.dp.toPx()
                        val corner = CornerRadius(6.dp.toPx())
                        ordered.forEachIndexed { index, region ->
                            val selected = index == current
                            if (region.kind == VisualRegion.Kind.Removed) {
                                // Removed content has no place in the new screenshot: mark where it used to be.
                                val y = region.afterTop * size.height
                                drawRect(marks.removed, Offset(0f, y - edge), Size(size.width, edge * if (selected) 2.5f else 1.5f))
                            } else {
                                val topLeft = Offset(region.left * size.width - edge, region.afterTop * size.height - edge)
                                val box = Size((region.right - region.left) * size.width + edge * 2, (region.afterBottom - region.afterTop) * size.height + edge * 2)
                                drawRoundRect(marks.highlight.copy(alpha = if (selected) 0.28f else 0.16f), topLeft, box, corner)
                                drawRoundRect(marks.highlightEdge, topLeft, box, corner, style = Stroke(if (selected) edge * 1.75f else edge))
                            }
                        }
                    }
                    // Tapping a marked area makes it the current one.
                    ordered.forEachIndexed { index, region ->
                        val top = with(density) { (region.afterTop * imageHeightPx).toDp() }
                        val height = with(density) { ((region.afterBottom - region.afterTop) * imageHeightPx).toDp() }.coerceAtLeast(24.dp)
                        Box(
                            Modifier.offset(y = top).fillMaxWidth().height(height)
                                .clickable(onClickLabel = stringResource(Res.string.reader_position, index + 1, ordered.size)) { go(index) },
                        )
                    }
                }
            }
        }

    }
}

