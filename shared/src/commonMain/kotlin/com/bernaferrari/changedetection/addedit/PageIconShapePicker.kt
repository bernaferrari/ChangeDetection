package com.bernaferrari.changedetection.addedit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.localization.stringResource
import com.bernaferrari.changedetection.model.AppPalette
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.PageIconColor
import com.bernaferrari.changedetection.repo.PageIconShape
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.components.PaletteSwatch
import com.bernaferrari.changedetection.ui.components.rememberPageIconMotion
import com.bernaferrari.changedetection.ui.kit.PageAvatar
import com.bernaferrari.changedetection.ui.kit.pageStartingColor
import com.bernaferrari.changedetection.ui.kit.vivid
import com.bernaferrari.changedetection.ui.kit.vividStops

/**
 * A sheet that is the icon: a large live preview on a soft glow of its color, the shapes as real previews, and one row of
 * colors. Original keeps the site's own icon as it is, so it takes no color; any other shape puts the icon on a colored
 * shape, and only then do colors appear, starting on the page's own stable color. Shapes and colors share one way of showing
 * the choice: a ring that hugs the chosen outline. Changes apply as they are made, so the sheet needs no button; it is
 * dismissed like any sheet. The caller decides whether each choice updates an editor draft or a saved page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageIconShapePicker(name: String, key: String, icon: String?, selected: PageIconShape, onSelect: (PageIconShape) -> Unit, colors: ColorGroup, onColorSelect: (ColorGroup) -> Unit, onDismiss: () -> Unit) {
    val current = PageIconColor.from(colors)
    // What every shape option previews: the chosen color, or the one the page would start on.
    val shapeColors = if (current == PageIconColor.AUTOMATIC) pageStartingColor(key).colors else colors
    val choose: (PageIconShape) -> Unit = { shape ->
        onSelect(shape)
        when {
            shape == PageIconShape.AUTOMATIC && current != PageIconColor.AUTOMATIC -> onColorSelect(PageIconColor.AUTOMATIC.colors)
            shape != PageIconShape.AUTOMATIC && current == PageIconColor.AUTOMATIC -> onColorSelect(shapeColors)
        }
    }
    val sheet = MaterialTheme.colorScheme.surfaceContainerLow
    val title = stringResource(Res.string.editor_icon_shape_title)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheet,
        modifier = Modifier.semantics { paneTitle = title },
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Stage(name, key, icon, selected, colors)
            OriginalChip(name, key, icon, selected == PageIconShape.AUTOMATIC) { choose(PageIconShape.AUTOMATIC) }
            // One column for everything below: the shapes and colors share its edges.
            BoxWithConstraints(Modifier.widthIn(max = 400.dp).padding(top = 12.dp)) {
            // The colors start and end where the first and last shapes do, not where their cells do.
            val inset = ((maxWidth / 4 - SHAPE_SIZE) / 2).coerceAtLeast(0.dp)
            Column {
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PageIconShape.entries.filter { it != PageIconShape.AUTOMATIC }.chunked(4).forEach { row ->
                        Row {
                            row.forEach { shape ->
                                ShapeChoice(shape, selected == shape, { choose(shape) }, Modifier.weight(1f)) { motion ->
                                    PageAvatar(
                                        name,
                                        key,
                                        modifier = motion.modifier,
                                        size = SHAPE_SIZE,
                                        icon = icon,
                                        iconShape = shape,
                                        colors = shapeColors,
                                        selectionProgress = motion.outlineProgress,
                                        selectionColor = MaterialTheme.colorScheme.primary,
                                        selectionGapColor = sheet,
                                    )
                                }
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = selected != PageIconShape.AUTOMATIC,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                ) {
                    ColorGrid(current, Modifier.padding(top = 20.dp, start = inset, end = inset)) { onColorSelect(it.colors) }
                }
            }
            }
        }
    }
}

/** The icon large, with a faint halo of its color once one is chosen. */
@Composable
private fun Stage(name: String, key: String, icon: String?, shape: PageIconShape, colors: ColorGroup) {
    val color = PageIconColor.from(colors)
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val glow by animateColorAsState(
        if (color == PageIconColor.AUTOMATIC) Color.Transparent else color.vivid(dark).copy(alpha = if (dark) 0.14f else 0.18f),
        tween(320),
        label = "stage-glow",
    )
    Box(
        Modifier.padding(top = 4.dp, bottom = 8.dp).size(128.dp).drawBehind {
            val radius = size.minDimension / 2
            drawCircle(Brush.radialGradient(listOf(glow, glow.copy(alpha = 0f)), center = center, radius = radius), radius = radius)
        },
        contentAlignment = Alignment.Center,
    ) {
        PageAvatar(name, key, size = 96.dp, icon = icon, iconShape = shape, colors = colors)
    }
}

/** Original is a mode, not a shape: the site's own icon, untouched. Picking any shape leaves it. */
@Composable
private fun OriginalChip(name: String, key: String, icon: String?, chosen: Boolean, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    FilterChip(
        selected = chosen,
        onClick = {
            if (!chosen) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        label = { Text(stringResource(Res.string.icon_original)) },
        leadingIcon = { PageAvatar(name, key, size = 20.dp, icon = icon, iconShape = PageIconShape.AUTOMATIC, colors = PageIconColor.AUTOMATIC.colors) },
        shape = CircleShape,
        modifier = Modifier.semantics { role = Role.RadioButton },
    )
}

/** Every color at once, two rows of five flush with the shapes above, so none hides off screen. */
@Composable
private fun ColorGrid(current: PageIconColor, modifier: Modifier = Modifier, onSelect: (PageIconColor) -> Unit) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Rows sit as far apart as the swatches do, so the colors form an even grid.
        val gap = ((maxWidth - SWATCH_SIZE * 5) / 4).coerceAtLeast(8.dp)
        Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(gap)) {
            PageIconColor.entries.filter { it != PageIconColor.AUTOMATIC }.chunked(5).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { color ->
                        PaletteSwatch(
                            colors = vividStops(color.vivid(dark)),
                            selected = current == color,
                            label = color.label(),
                            onClick = { onSelect(color) },
                            size = SWATCH_SIZE,
                        )
                    }
                }
            }
        }
    }
}

/** The shape's real result; the chosen one gets a ring that hugs its outline and a small wobble as it is picked. */
@Composable
private fun ShapeChoice(shape: PageIconShape, chosen: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, preview: @Composable (com.bernaferrari.changedetection.ui.components.PageIconMotion) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val interactions = remember { MutableInteractionSource() }
    val motion = rememberPageIconMotion(interactions, chosen)
    val label = shape.label()
    Box(
        modifier
            .height(SHAPE_SIZE + 20.dp)
            .semantics { contentDescription = label }
            .selectable(selected = chosen, role = Role.RadioButton, interactionSource = interactions, indication = null) {
                if (!chosen) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        preview(motion)
    }
}

private val SHAPE_SIZE = 52.dp
private val SWATCH_SIZE = 40.dp

@Composable
private fun PageIconColor.label(): String = if (this == PageIconColor.AUTOMATIC) {
    stringResource(Res.string.icon_shape_automatic)
} else {
    stringResource((AppPalette.entries.firstOrNull { it.name == name } ?: AppPalette.DEFAULT).titleRes)
}

@Composable
private fun PageIconShape.label(): String = stringResource(
    when (this) {
        PageIconShape.AUTOMATIC -> Res.string.icon_shape_automatic
        PageIconShape.CIRCLE -> Res.string.icon_shape_circle
        PageIconShape.ROUNDED -> Res.string.icon_shape_rounded
        PageIconShape.FLOWER -> Res.string.icon_shape_flower
        PageIconShape.CLOVER -> Res.string.icon_shape_clover
        PageIconShape.SUN -> Res.string.icon_shape_sun
        PageIconShape.COOKIE -> Res.string.icon_shape_cookie
        PageIconShape.GEM -> Res.string.icon_shape_gem
        PageIconShape.PUFFY -> Res.string.icon_shape_puffy
    },
)
