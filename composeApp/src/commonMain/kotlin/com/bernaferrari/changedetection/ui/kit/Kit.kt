/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bernaferrari.changedetection.preview.EditSegment
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.ui.theme.Marks
import com.bernaferrari.changedetection.ui.theme.marks
import com.bernaferrari.changedetection.util.displayPageAddress
import com.bernaferrari.changedetection.util.displayPageTitle
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.preview_before
import com.bernaferrari.changedetection.shared.generated.resources.preview_after
import org.jetbrains.compose.resources.stringResource

/** Host only, for the quiet second line under a page name. */
fun Site.domain(): String = url.displayPageAddress().substringBefore('/').substringBefore('?')

fun Site.name(): String = displayPageTitle()

/** The first letter of the page name on a soft tint that is stable for the page. */
@Composable
fun PageAvatar(
    name: String,
    key: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    dimmed: Boolean = false,
) {
    val tints = MaterialTheme.marks.identity
    val tint = remember(key, tints) { tints[(key.hashCode() and Int.MAX_VALUE) % tints.size] }
    val letter = remember(name) { name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "•" }
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(if (dimmed) MaterialTheme.colorScheme.surfaceContainer else tint),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            letter,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (dimmed) 0.5f else 1f),
        )
    }
}

/** Removed text struck through in red, added text on marker yellow. */
fun editText(
    segments: List<EditSegment>,
    marks: Marks,
    leadingEllipsis: Boolean = false,
    trailingEllipsis: Boolean = false,
): AnnotatedString = buildAnnotatedString {
    if (leadingEllipsis) append("…")
    segments.forEach { segment ->
        when (segment.kind) {
            EditSegment.Kind.Same -> append(segment.text)

            EditSegment.Kind.Removed -> {
                // Struck through by DiffText with a readable 2dp line; serif fonts draw hairlines.
                pushStringAnnotation(REMOVED_TAG, "")
                withStyle(SpanStyle(color = marks.removed)) { append(segment.text) }
                pop()
            }

            EditSegment.Kind.Added -> {
                pushStringAnnotation(ADDED_TAG, "")
                withStyle(SpanStyle(background = marks.highlight, color = marks.onHighlight)) { append(segment.text) }
                pop()
            }
        }
    }
    if (trailingEllipsis) append("…")
}

const val REMOVED_TAG = "removed"
const val ADDED_TAG = "added"

/** Gives spoken previews the same before/after meaning as the visual marks. */
internal fun spokenEditText(text: AnnotatedString, before: String, after: String): String {
    val edits = text.getStringAnnotations(0, text.length).filter { it.tag == REMOVED_TAG || it.tag == ADDED_TAG }.sortedBy { it.start }
    return buildString {
        var position = 0
        edits.forEach { edit ->
            append(text.text.substring(position, edit.start))
            append(" ")
            append(if (edit.tag == REMOVED_TAG) before else after)
            append(": ")
            append(text.text.substring(edit.start, edit.end))
            append(". ")
            position = edit.end
        }
        append(text.text.substring(position))
    }
}

/** Text that draws a confident strike through removed ranges. */
@Composable
fun DiffText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = SerifSnippet,
    maxLines: Int = Int.MAX_VALUE,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val strike = MaterialTheme.marks.removed
    val ranges = remember(text) { text.getStringAnnotations(REMOVED_TAG, 0, text.length) }
    val before = stringResource(Res.string.preview_before)
    val after = stringResource(Res.string.preview_after)
    val description = remember(text, before, after) { spokenEditText(text, before, after) }
    Text(
        text,
        modifier = modifier.semantics { contentDescription = description }.drawWithContent {
            drawContent()
            val result = layout ?: return@drawWithContent
            val thickness = 2.dp.toPx()
            ranges.forEach { range ->
                val start = range.start
                val end = range.end.coerceAtMost(result.layoutInput.text.length)
                if (end <= start) return@forEach
                val firstLine = result.getLineForOffset(start)
                val lastLine = result.getLineForOffset(end - 1)
                for (line in firstLine..lastLine) {
                    if (line >= result.lineCount) break
                    val lineStart = maxOf(start, result.getLineStart(line))
                    val lineEnd = minOf(end, result.getLineEnd(line, visibleEnd = true))
                    if (lineEnd <= lineStart) continue
                    val x1 = result.getHorizontalPosition(lineStart, usePrimaryDirection = true)
                    val x2 = result.getHorizontalPosition(lineEnd, usePrimaryDirection = true)
                    val y = result.getLineBaseline(line) - (result.getLineBaseline(line) - result.getLineTop(line)) * 0.32f
                    drawLine(strike, androidx.compose.ui.geometry.Offset(minOf(x1, x2), y), androidx.compose.ui.geometry.Offset(maxOf(x1, x2), y), thickness)
                }
            }
        },
        style = style,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { layout = it },
    )
}

@Composable
fun NewBadge(modifier: Modifier = Modifier, text: String = "New") {
    Box(
        modifier
            .clip(CircleShape)
            .background(MaterialTheme.marks.highlight)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.marks.onHighlight)
    }
}

/** Small outlined status pill, for "Paused", "Screenshot" and similar facts. */
@Composable
fun FactPill(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, container: Color = MaterialTheme.colorScheme.surfaceContainer, content: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(
        modifier.clip(RoundedCornerShape(8.dp)).background(container).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(14.dp), tint = content)
        Text(text, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    trailingText: String? = null,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 20.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false).semantics { heading() })
            if (count != null) {
                Spacer(Modifier.width(6.dp))
                Text(count.toString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailingText != null) {
            Text(trailingText, Modifier.padding(end = 12.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

/** A white rounded group of rows; rows decide their own dividers. */
@Composable
fun ListGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(content = content)
    }
}

@Composable
fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(start = 32.dp, end = 32.dp, top = 20.dp, bottom = 8.dp).semantics { heading() },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun GroupRow(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    showDivider: Boolean = false,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    supportingColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    role: Role = Role.Button,
    checked: Boolean? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Column(modifier) {
        if (showDivider) {
            HorizontalDivider(
                Modifier.padding(start = if (icon != null) 60.dp else 20.dp, end = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .then(
                    when {
                        onClick != null && checked != null -> Modifier.toggleable(value = checked, enabled = enabled, role = role, onValueChange = { onClick() })
                        onClick != null -> Modifier.clickable(enabled = enabled, role = role, onClick = onClick)
                        else -> Modifier
                    },
                )
                .heightIn(min = 60.dp)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = if (enabled) titleColor else titleColor.copy(alpha = 0.4f))
                if (supporting != null) {
                    Text(supporting, style = MaterialTheme.typography.bodyMedium, color = supportingColor)
                }
            }
            if (trailing != null) trailing()
        }
    }
}

/** A calm, centered moment used for empty and success states. */
@Composable
fun Callout(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = MaterialTheme.colorScheme.surfaceContainer,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Surface(modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = MaterialTheme.shapes.large, color = container) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (icon != null) Icon(icon, null, Modifier.size(24.dp), tint = iconTint)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (actions != null) {
                    FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { actions() }
                }
            }
        }
    }
}

val SerifSnippet: TextStyle
    @Composable get() = com.bernaferrari.changedetection.ui.theme.Reading.snippet.copy(color = MaterialTheme.colorScheme.onSurface)

/** The app mark: an ink tile with one marker stroke across a line of "text". */
@Composable
fun AppMark(modifier: Modifier = Modifier) {
    val marks = MaterialTheme.marks
    androidx.compose.foundation.Canvas(modifier) {
        val corner = size.minDimension * 0.3f
        drawRoundRect(Color(0xFF17160F), cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner))
        val w = size.width
        val h = size.height
        val stroke = h * 0.07f
        drawLine(Color.White.copy(alpha = 0.85f), androidx.compose.ui.geometry.Offset(w * 0.24f, h * 0.34f), androidx.compose.ui.geometry.Offset(w * 0.76f, h * 0.34f), stroke, androidx.compose.ui.graphics.StrokeCap.Round)
        rotate(-5f) {
            drawRoundRect(marks.highlight, topLeft = androidx.compose.ui.geometry.Offset(w * 0.18f, h * 0.5f), size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.2f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.06f, h * 0.06f))
        }
        drawLine(Color(0xFF17160F), androidx.compose.ui.geometry.Offset(w * 0.26f, h * 0.6f), androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.6f), stroke, androidx.compose.ui.graphics.StrokeCap.Round)
    }
}
