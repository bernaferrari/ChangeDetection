/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.PageIconColor
import com.bernaferrari.changedetection.repo.PageIconShape
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.components.rememberPageIconMotion
import com.bernaferrari.changedetection.ui.kit.PageAvatar
import org.jetbrains.compose.resources.stringResource

/** The caller decides whether each choice updates an editor draft or a saved page. */
@Composable
internal fun PageIconShapePicker(name: String, key: String, icon: String?, selected: PageIconShape, onSelect: (PageIconShape) -> Unit, colors: ColorGroup, onColorSelect: (ColorGroup) -> Unit, onDismiss: () -> Unit) {
    val haptics = LocalHapticFeedback.current
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
                            val motion = rememberPageIconMotion(interactions, chosen)
                            val label = shape.label()
                            Box(
                                Modifier.weight(1f).height(80.dp)
                                    .semantics { contentDescription = label }
                                    .selectable(selected = chosen, role = Role.RadioButton, interactionSource = interactions, indication = null, onClick = {
                                        if (!chosen) {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSelect(shape)
                                        }
                                    }),
                                contentAlignment = Alignment.Center,
                            ) {
                                PageAvatar(
                                    name,
                                    key,
                                    modifier = motion.modifier,
                                    size = 48.dp,
                                    icon = icon,
                                    iconShape = shape,
                                    colors = colors,
                                    selectionProgress = motion.outlineProgress,
                                    selectionColor = if (chosen || motion.focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text(stringResource(Res.string.icon_color_title), style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    PageIconColor.entries.forEach { color ->
                        val palette = com.bernaferrari.changedetection.model.AppPalette.entries.firstOrNull { it.name == color.name } ?: com.bernaferrari.changedetection.model.AppPalette.DEFAULT
                        com.bernaferrari.changedetection.ui.components.PaletteSwatch(
                            colors = com.bernaferrari.changedetection.ui.components.paletteSwatchColors(palette),
                            label = if (color == PageIconColor.AUTOMATIC) stringResource(Res.string.icon_shape_automatic) else stringResource(palette.titleRes),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                            size = 48.dp,
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
