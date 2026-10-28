@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.localization.stringResource
import com.bernaferrari.changedetection.model.ColorStyle
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.color_style_title
import com.bernaferrari.changedetection.ui.theme.brandColorScheme
import com.materialkolor.hct.Hct

/**
 * The colour style as a connected button group, like Auto, Light and Dark above it, with no words: each segment
 * is its style's three accents blended into one soft mesh, so it shows the style's mood for the chosen colour.
 * The chosen segment widens into a pill with a frosted check. Names stay for screen readers.
 */
@Composable
internal fun ColorStylePicker(
    selected: ColorStyle,
    seedColor: Color,
    dark: Boolean,
    onSelect: (ColorStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(Res.string.color_style_title)
    Row(
        modifier.height(56.dp).selectableGroup().semantics { contentDescription = title },
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        ColorStyle.entries.forEachIndexed { index, style ->
            val mood = remember(seedColor, dark, style) {
                brandColorScheme(seedColor, dark, style).let { mood(listOf(it.primary, it.secondary, it.tertiary), dark) }
            }
            Segment(
                label = stringResource(style.titleRes),
                mood = mood,
                selected = style == selected,
                first = index == 0,
                last = index == ColorStyle.entries.lastIndex,
                onClick = { onSelect(style) },
            )
        }
    }
}

/** The three accents at one bright tone (a deeper one in dark mode), so every style is compared fairly. */
private fun mood(accents: List<Color>, dark: Boolean): List<Color> =
    accents.zip(if (dark) listOf(62.0, 68.0, 64.0) else listOf(72.0, 78.0, 74.0)) { color, tone ->
        Color(Hct.fromInt(color.toArgb()).withTone(tone).toInt())
    }

@Composable
private fun RowScope.Segment(
    label: String,
    mood: List<Color>,
    selected: Boolean,
    first: Boolean,
    last: Boolean,
    onClick: () -> Unit,
) {
    val width by animateFloatAsState(if (selected) 1.7f else 1f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "styleWidth")
    // Outer ends stay round; inner corners open into a pill when chosen.
    val dpSpec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
    val start by animateDpAsState(if (selected || first) 28.dp else 8.dp, dpSpec, label = "styleStart")
    val end by animateDpAsState(if (selected || last) 28.dp else 8.dp, dpSpec, label = "styleEnd")
    val shape = RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end)
    Box(
        Modifier
            .weight(width)
            .fillMaxHeight()
            .clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = { if (!selected) onClick() })
            .semantics { contentDescription = label }
            .drawBehind {
                // A soft mesh: primary to secondary across, tertiary glowing from the lower right, primary from the
                // upper left, and a little light along the top.
                val (primary, secondary, tertiary) = mood
                drawRect(Brush.horizontalGradient(listOf(primary, secondary)))
                val low = Offset(size.width * .85f, size.height)
                drawCircle(Brush.radialGradient(listOf(tertiary, tertiary.copy(alpha = 0f)), low, size.width * .7f), size.width * .7f, low)
                val high = Offset(size.width * .1f, 0f)
                drawCircle(Brush.radialGradient(listOf(primary, primary.copy(alpha = 0f)), high, size.width * .6f), size.width * .6f, high)
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = .35f), Color.Transparent), endY = size.height * .55f))
            },
        contentAlignment = Alignment.Center,
    ) {
        // The frosted check grows in with the pill.
        val shown by animateFloatAsState(if (selected) 1f else 0f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "styleCheck")
        if (shown > .01f) {
            Box(
                Modifier.size(28.dp)
                    .graphicsLayer { alpha = shown.coerceIn(0f, 1f); scaleX = .6f + .4f * shown; scaleY = .6f + .4f * shown }
                    .clip(CircleShape).background(Color.White.copy(alpha = .78f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF1B1B1F), modifier = Modifier.size(18.dp))
            }
        }
    }
}
