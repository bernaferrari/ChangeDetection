/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.PageIconColor
import com.bernaferrari.changedetection.repo.PageIconShape
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.kit.PageAvatar
import org.jetbrains.compose.resources.stringResource

/** The caller decides whether each choice updates an editor draft or a saved page. */
@Composable
internal fun PageIconShapePicker(name: String, key: String, icon: String?, selected: PageIconShape, onSelect: (PageIconShape) -> Unit, colors: ColorGroup, onColorSelect: (ColorGroup) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.editor_icon_shape_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PageIconShape.entries.chunked(3).forEach { shapes ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        shapes.forEach { shape ->
                            val chosen = selected == shape
                            val interactions = remember { MutableInteractionSource() }
                            val hovered by interactions.collectIsHoveredAsState()
                            val pressed by interactions.collectIsPressedAsState()
                            val focused by interactions.collectIsFocusedAsState()
                            val active = chosen || focused || hovered || pressed
                            val ring by animateFloatAsState(if (active) 1f else 0f, if (focused) snap() else tween(160, easing = LinearOutSlowInEasing), label = "shape-ring-growth")
                            val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = 1f, stiffness = 1000f), label = "shape-press")
                            val label = shape.label()
                            Box(
                                Modifier.weight(1f).height(80.dp)
                                    .semantics { contentDescription = label }
                                    .selectable(selected = chosen, role = Role.RadioButton, interactionSource = interactions, indication = null, onClick = { onSelect(shape) }),
                                contentAlignment = Alignment.Center,
                            ) {
                                PageAvatar(
                                    name,
                                    key,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    },
                                    size = 48.dp,
                                    icon = icon,
                                    iconShape = shape,
                                    colors = colors,
                                    selectionProgress = ring,
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text(stringResource(Res.string.icon_color_title), style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    PageIconColor.entries.forEach { color ->
                        val swatch = com.bernaferrari.changedetection.model.AppPalette.entries.firstOrNull { it.name == color.name }?.seedColor ?: MaterialTheme.colorScheme.primary
                        com.bernaferrari.changedetection.ui.components.PaletteColorSwatch(
                            color = swatch,
                            label = color.label(),
                            selected = PageIconColor.from(colors) == color,
                            onClick = { onColorSelect(color.colors) },
                            icon = if (color == PageIconColor.AUTOMATIC) Icons.Rounded.Palette else null,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.done)) } },
    )
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

@Composable
private fun PageIconColor.label(): String = stringResource(
    when (this) {
        PageIconColor.AUTOMATIC -> Res.string.icon_shape_automatic
        PageIconColor.BLUE -> Res.string.icon_color_blue
        PageIconColor.GREEN -> Res.string.icon_color_green
        PageIconColor.AMBER -> Res.string.icon_color_amber
        PageIconColor.PINK -> Res.string.icon_color_pink
        PageIconColor.PURPLE -> Res.string.icon_color_purple
        PageIconColor.TEAL -> Res.string.icon_color_teal
    },
)
