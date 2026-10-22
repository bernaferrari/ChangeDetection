package com.bernaferrari.changedetection.ui.kit

import com.bernaferrari.changedetection.repo.PageIconShape
import io.ktor.http.hostWithPortIfSpecified
import androidx.graphics.shapes.Morph
import androidx.compose.ui.graphics.SolidColor
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle
import androidx.compose.material3.toPath
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.border
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.toShape
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
import com.bernaferrari.changedetection.localization.stringResource

/** Host only, for the quiet second line under a page name. */
fun Site.domain(): String = url.displayPageAddress().substringBefore('/').substringBefore('?')

fun Site.name(): String = displayPageTitle()

/** The first letter of the page name inside an expressive shape and tint, both stable for the page. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PageAvatar(
    name: String,
    key: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    dimmed: Boolean = false,
    /** The page's icon; the letter shows while it loads, or if the page has none. */
    icon: String? = null,
    iconShape: PageIconShape = PageIconShape.AUTOMATIC,
    colors: com.bernaferrari.changedetection.repo.ColorGroup = 0 to 0,
    selectionProgress: Float = 0f,
    selectionColor: Color? = null,
    /** The surface behind the icon, which the gap between the icon and its selection ring shows. */
    selectionGapColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    val tints = MaterialTheme.marks.identity
    val hash = remember(key) { mixHash(key.hashCode()) }
    val chosenColor = com.bernaferrari.changedetection.repo.PageIconColor.from(colors)
    val letter = remember(name) { name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "•" }
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val automaticColor = chosenColor == com.bernaferrari.changedetection.repo.PageIconColor.AUTOMATIC
    // Automatic pages keep a quiet tint; a chosen color is drawn vivid, like the swatch that picked it.
    val tintTarget = if (automaticColor) tints[(hash ushr 16) % tints.size] else chosenColor.vivid(dark)
    val tint by animateColorAsState(tintTarget, tween(220), label = "page-icon-color")
    val context = coil3.compose.LocalPlatformContext.current
    val painter = icon?.takeIf { it.startsWith("http") && LocalPageIcons.current }?.let { url ->
        coil3.compose.rememberAsyncImagePainter(remember(url) { coil3.request.ImageRequest.Builder(context).data(url).size(192).build() })
    }
    val loaded = painter?.state?.collectAsState()?.value is coil3.compose.AsyncImagePainter.State.Success
    // A real icon is the page's identity: left fully automatic it fills a rounded tile edge to edge, like an app icon. Once a
    // shape or color is chosen, it sits inset on that backdrop instead, so expressive lobes never crop it and the color shows.
    // The tinted shape and letter stand in only while it loads or when the page has none.
    val fullBleed = iconShape == PageIconShape.AUTOMATIC && automaticColor
    // Icons are drawn for light backgrounds: left automatic they sit on a light tile in both themes.
    val iconBackdrop by animateColorAsState(if (automaticColor) IconTile else chosenColor.vivid(dark), tween(220), label = "page-icon-backdrop")
    val polygon = if (loaded && iconShape == PageIconShape.AUTOMATIC) TilePolygon else iconShape.polygon() ?: AvatarShapes[(hash ushr 4) % AvatarShapes.size]
    val outline = rememberMorphingShape(polygon)
    val ringColor = selectionColor ?: MaterialTheme.colorScheme.primary
    val ringGapColor = selectionGapColor
    Box(
        modifier
            .size(size)
            .then(
                if (selectionProgress > 0f) {
                    Modifier.drawWithCache {
                        // Use one contour for the fill and ring. Scaling a second shape changes the
                        // curvature of expressive lobes and makes the gap uneven at their corners.
                        val contour = outline.createOutline(this.size, layoutDirection, this)
                        val growth = selectionProgress.coerceIn(0f, 1.15f)
                        val ring = Stroke(12.dp.toPx() * growth, join = StrokeJoin.Round)
                        val gap = Stroke(8.dp.toPx() * growth, join = StrokeJoin.Round)
                        onDrawWithContent {
                            drawOutline(contour, ringColor, style = ring)
                            drawOutline(contour, ringGapColor, style = gap)
                            drawContent()
                        }
                    }
                } else {
                    Modifier
                },
            )
            .clip(outline)
            .background(
                when {
                    loaded && automaticColor -> SolidColor(iconBackdrop)
                    loaded -> vividBrush(iconBackdrop)
                    dimmed -> SolidColor(MaterialTheme.colorScheme.surfaceContainerHighest)
                    automaticColor -> SolidColor(tint)
                    else -> vividBrush(tint)
                },
            )
            .then(if (loaded) Modifier.border(1.dp, Color.Black.copy(alpha = 0.06f), outline) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (painter != null) {
            androidx.compose.foundation.Image(
                painter,
                contentDescription = null,
                modifier = if (fullBleed) Modifier.fillMaxSize() else Modifier.fillMaxSize(0.58f).clip(RoundedCornerShape(size * 0.12f)),
                contentScale = if (fullBleed) androidx.compose.ui.layout.ContentScale.Crop else androidx.compose.ui.layout.ContentScale.Fit,
                alpha = if (!loaded) {
                    0f
                } else if (dimmed) {
                    0.45f
                } else {
                    1f
                },
            )
        }
        if (!loaded) {
            Text(
                letter,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = (size.value * 0.4f).sp, fontWeight = FontWeight.Bold),
                color = (
                    if (automaticColor) {
                        (if (dark) Color.White else Color(0xFF1B1B21))
                    } else if (tint.luminance() > 0.5f) {
                        Color(0xFF1B1B21)
                    } else {
                        Color.White
                    }
                    ).copy(alpha = if (dimmed) 0.45f else 0.95f),
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
internal fun PageIconShape.polygon(): RoundedPolygon? = when (this) {
    PageIconShape.AUTOMATIC -> null
    PageIconShape.CIRCLE -> androidx.compose.material3.MaterialShapes.Circle
    PageIconShape.ROUNDED -> TilePolygon
    PageIconShape.FLOWER -> androidx.compose.material3.MaterialShapes.Cookie9Sided
    PageIconShape.CLOVER -> androidx.compose.material3.MaterialShapes.Clover4Leaf
    PageIconShape.SUN -> androidx.compose.material3.MaterialShapes.Sunny
    PageIconShape.COOKIE -> androidx.compose.material3.MaterialShapes.Cookie6Sided
    PageIconShape.GEM -> androidx.compose.material3.MaterialShapes.Gem
    PageIconShape.PUFFY -> androidx.compose.material3.MaterialShapes.Puffy
}

/** A square with 28% corners, the app-icon tile a page's own icon fills. */
private val TilePolygon: RoundedPolygon by lazy {
    RoundedPolygon.rectangle(width = 2f, height = 2f, rounding = androidx.graphics.shapes.CornerRounding(0.56f, smoothing = 0.6f)).normalized()
}

/**
 * The outline morphs into a newly chosen shape instead of swapping, twisting a little as it settles. Only the outline
 * turns; the letter or icon inside stays upright. Changes landing mid-morph start from the shape being left.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun rememberMorphingShape(target: RoundedPolygon): androidx.compose.ui.graphics.Shape {
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    val progress = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(target) {
        if (target == to) return@LaunchedEffect
        from = if (progress.value < 0.5f) from else to
        to = target
        progress.snapTo(0f)
        progress.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 260f))
    }
    val morph = remember(from, to) { Morph(from, to) }
    val p = progress.value
    return remember(morph, p) { MorphShape(morph, p) }
}

private class MorphShape(private val morph: Morph, private val progress: Float) : androidx.compose.ui.graphics.Shape {
    @OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        // The spring overshoots; the outline holds its final form while the twist carries the bounce.
        val path = morph.toPath(progress.coerceIn(0f, 1f))
        val twist = (1f - progress) * 36f
        path.transform(
            androidx.compose.ui.graphics.Matrix().apply {
                scale(size.width, size.height)
            },
        )
        val bounds = path.getBounds()
        path.translate(androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2) - bounds.center)
        if (twist != 0f) {
            path.transform(
                androidx.compose.ui.graphics.Matrix().apply {
                    translate(size.width / 2, size.height / 2)
                    rotateZ(twist)
                    translate(-size.width / 2, -size.height / 2)
                },
            )
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

private val IconTile = Color(0xFFF6F6FA)

/** iOS-like system colors: bright enough to read as a choice, each with a dark-theme variant. */
internal fun com.bernaferrari.changedetection.repo.PageIconColor.vivid(dark: Boolean): Color = when (this) {
    com.bernaferrari.changedetection.repo.PageIconColor.AUTOMATIC -> Color.Gray
    com.bernaferrari.changedetection.repo.PageIconColor.ORANGE -> if (dark) Color(0xFFFF9F0A) else Color(0xFFFF9500)
    com.bernaferrari.changedetection.repo.PageIconColor.AMBER -> if (dark) Color(0xFFFFD60A) else Color(0xFFFFCC00)
    com.bernaferrari.changedetection.repo.PageIconColor.LIME -> if (dark) Color(0xFFA6E04F) else Color(0xFF8FCB3C)
    com.bernaferrari.changedetection.repo.PageIconColor.GREEN -> if (dark) Color(0xFF30D158) else Color(0xFF34C759)
    com.bernaferrari.changedetection.repo.PageIconColor.TEAL -> if (dark) Color(0xFF63E6E2) else Color(0xFF00C7BE)
    com.bernaferrari.changedetection.repo.PageIconColor.CYAN -> if (dark) Color(0xFF64D2FF) else Color(0xFF32ADE6)
    com.bernaferrari.changedetection.repo.PageIconColor.BLUE -> if (dark) Color(0xFF0A84FF) else Color(0xFF007AFF)
    com.bernaferrari.changedetection.repo.PageIconColor.INDIGO -> if (dark) Color(0xFF5E5CE6) else Color(0xFF5856D6)
    com.bernaferrari.changedetection.repo.PageIconColor.PURPLE -> if (dark) Color(0xFFBF5AF2) else Color(0xFFAF52DE)
    com.bernaferrari.changedetection.repo.PageIconColor.PINK -> if (dark) Color(0xFFFF375F) else Color(0xFFFF2D55)
}

/** A diagonal from a lighter neighbouring hue into the color, so each reads as a little gradient rather than a flat dot. */
internal fun vividBrush(color: Color): androidx.compose.ui.graphics.Brush = androidx.compose.ui.graphics.Brush.linearGradient(vividStops(color))

/** The two ends of [vividBrush], for swatches that draw their own gradient. */
internal fun vividStops(color: Color): List<Color> = listOf(androidx.compose.ui.graphics.lerp(shiftHue(color, -20f), Color.White, 0.2f), color)

private fun shiftHue(color: Color, degrees: Float): Color {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    if (delta == 0f) return color
    val hue = when (max) {
        r -> 60f * (((g - b) / delta) % 6f)
        g -> 60f * ((b - r) / delta + 2f)
        else -> 60f * ((r - g) / delta + 4f)
    }
    return Color.hsv(((hue + degrees) % 360f + 360f) % 360f, delta / max, max, color.alpha)
}

/** The color a page gets when it first leaves its original icon for a shape: stable per page, like its automatic tint. */
internal fun pageStartingColor(key: String): com.bernaferrari.changedetection.repo.PageIconColor = com.bernaferrari.changedetection.repo.PageIconColor.entries.drop(1).let { it[(mixHash(key.hashCode()) ushr 8) % it.size] }

/** Page icons come from the network; offline UI tests turn them off so nothing waits on a request. */
val LocalPageIcons = androidx.compose.runtime.staticCompositionLocalOf { true }

/** The page's saved icon, or its site's /favicon.ico until a capture finds a better one. */
fun Site.avatarIcon(): String? = iconUrl.takeIf { it.startsWith("http") } ?: runCatching {
    val parsed = io.ktor.http.Url(url)
    if (parsed.host.isBlank()) null else "${parsed.protocol.name}://${parsed.hostWithPortIfSpecified}/favicon.ico"
}.getOrNull()

/** Spreads similar keys apart so neighbouring pages rarely share a tint or shape. */
private fun mixHash(value: Int): Int {
    var h = value
    h = h xor (h ushr 16)
    h *= 0x45d9f3b
    h = h xor (h ushr 16)
    return h and Int.MAX_VALUE
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
private val AvatarShapes = listOf(
    androidx.compose.material3.MaterialShapes.Cookie9Sided,
    androidx.compose.material3.MaterialShapes.Clover4Leaf,
    androidx.compose.material3.MaterialShapes.Sunny,
    androidx.compose.material3.MaterialShapes.Cookie6Sided,
    androidx.compose.material3.MaterialShapes.Gem,
    androidx.compose.material3.MaterialShapes.Cookie4Sided,
    androidx.compose.material3.MaterialShapes.Puffy,
)

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
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onPrimary)
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
        modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 24.dp, end = 12.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val titleModifier = if (onAction != null) {
            Modifier.heightIn(min = 48.dp).clip(MaterialTheme.shapes.medium).clickable(role = Role.Button, onClick = onAction)
        } else {
            Modifier
        }
        Row(Modifier.weight(1f).then(titleModifier), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f, fill = false).semantics { heading() })
            if (count != null) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(horizontal = 8.dp, vertical = 1.dp)) {
                    Text(com.bernaferrari.changedetection.util.localizedInteger(count), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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

/** Shape of item [index] of [count] in a lazy segmented list: big outer corners, small inner ones. */
fun segmentShape(index: Int, count: Int, outer: Dp = 24.dp, inner: Dp = 4.dp): RoundedCornerShape = RoundedCornerShape(
    topStart = if (index == 0) outer else inner,
    topEnd = if (index == 0) outer else inner,
    bottomStart = if (index == count - 1) outer else inner,
    bottomEnd = if (index == count - 1) outer else inner,
)

/** The color of content tiles: bright on the container-toned screen. */
val tileColor: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceBright

private val LocalInGroup = androidx.compose.runtime.staticCompositionLocalOf { false }

/**
 * A segmented group (Android 16 style): rows are separate bright tiles with tight gaps,
 * large outer corners and small inner ones.
 */
@Composable
fun ListGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalInGroup provides true) {
        Column(
            modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(24.dp)),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content,
        )
    }
}

/** Wraps any custom row so it becomes one tile of the surrounding [ListGroup]. */
fun Modifier.groupTile(): Modifier = composed {
    if (LocalInGroup.current) clip(RoundedCornerShape(4.dp)).background(tileColor) else this
}

@Composable
fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 8.dp).semantics { heading() },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun GroupRow(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    @Suppress("UNUSED_PARAMETER") showDivider: Boolean = false,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    supportingColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    role: Role = Role.Button,
    checked: Boolean? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier
            .groupTile()
            .fillMaxWidth()
            .then(
                when {
                    onClick != null && checked != null -> Modifier.toggleable(value = checked, enabled = enabled, role = role, onValueChange = { onClick() })
                    onClick != null -> Modifier.clickable(enabled = enabled, role = role, onClick = onClick)
                    else -> Modifier
                },
            )
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (icon != null) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = if (enabled) 1f else 0.4f))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = if (enabled) titleColor else titleColor.copy(alpha = 0.4f))
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodyMedium, color = supportingColor)
            }
        }
        if (trailing != null) trailing()
    }
}

/** A calm, centered moment used for empty and success states. */
@Composable
fun Callout(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Surface(modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = MaterialTheme.shapes.extraLarge, color = container) {
        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (icon != null) Icon(icon, null, Modifier.size(24.dp), tint = iconTint)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (actions != null) {
                    FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { actions() }
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
        drawRoundRect(Color(0xFF3F4AB8), cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner))
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
