/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bernaferrari.changedetection.ui.settings

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.model.ThemeMode
import com.bernaferrari.changedetection.model.AppPalette
import com.bernaferrari.changedetection.repo.ComparisonMethod
import com.bernaferrari.changedetection.ui.components.SegmentPosition
import com.bernaferrari.changedetection.ui.components.segmentShape
import com.bernaferrari.changedetection.ui.theme.spacing
import com.bernaferrari.changedetection.ui.theme.supportsDynamicColors
import org.jetbrains.compose.resources.stringResource
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.settings_comparison_page_text
import com.bernaferrari.changedetection.shared.generated.resources.settings_comparison_screenshots

@Composable
internal fun SettingsSectionHeader(
    title: String,
) {
    val spacing = MaterialTheme.spacing
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(
            start = spacing.xl,
            end = spacing.lg,
            top = spacing.lg,
            bottom = spacing.xs,
        ),
    )
}

@Composable
internal fun SettingsGroup(content: @Composable () -> Unit) {
    val spacing = MaterialTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = { content() },
    )
}

@Composable
internal fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    showChevron: Boolean = true,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    position: SegmentPosition = SegmentPosition.Only,
    onClick: (() -> Unit)? = null,
) {
    val spacing = MaterialTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.99f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "settingRowPress",
    )
    val clickModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            role = Role.Button,
            onClick = onClick,
        )
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
        shape = segmentShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.touchTarget)
                .then(clickModifier)
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsLeadingIcon(icon = icon, tint = iconTint)
            Spacer(modifier = Modifier.width(spacing.sm))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    value?.let {
                        Spacer(modifier = Modifier.width(spacing.xs))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(start = spacing.xs)
                        .size(20.dp),
                )
            }
        }
    }
}

@Composable
internal fun ThemeSelector(
    selectedTheme: ThemeMode,
    selectedPalette: AppPalette,
    onThemeSelected: (ThemeMode) -> Unit,
    onPaletteSelected: (AppPalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val visibleModes = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)
    val visiblePalettes = AppPalette.entries.filter { !it.usesSystemColors || supportsDynamicColors() }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp, 24.dp, 7.dp, 7.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier.padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    visibleModes.forEachIndexed { index, mode ->
                        val selected = mode == selectedTheme
                        ToggleButton(
                            checked = selected,
                            onCheckedChange = { if (it) onThemeSelected(mode) },
                            shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                visibleModes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(mode.titleRes), maxLines = 1)
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(7.dp, 7.dp, 24.dp, 24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = spacing.sm, horizontal = spacing.md),
                horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                visiblePalettes.forEach { palette ->
                    PaletteSwatch(
                        palette = palette,
                        selected = palette == selectedPalette,
                        onClick = { onPaletteSelected(palette) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteSwatch(
    palette: AppPalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val name = stringResource(palette.titleRes)
    val swatchColor = if (palette.usesSystemColors) MaterialTheme.colorScheme.primary else palette.seedColor
    val shapeProgress by animateFloatAsState(
        targetValue = if (selected) 0.5f else 0.26f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "paletteShape_${palette.name}",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.86f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "paletteScale_${palette.name}",
    )
    Box(
        modifier = Modifier
            .size(50.dp)
            .semantics { contentDescription = name }
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(percent = (shapeProgress * 100).toInt()))
                .background(swatchColor),
            contentAlignment = Alignment.Center,
        ) {
            if (selected || palette.usesSystemColors) {
                Icon(
                    imageVector = if (selected) Icons.Rounded.Check else Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = if (swatchColor.luminance() > 0.55f) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
internal fun ComparisonMethodSelector(
    selectedMethod: ComparisonMethod,
    onMethodSelected: (ComparisonMethod) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        ComparisonMethod.entries.forEachIndexed { index, method ->
            val isSelected = method == selectedMethod
            ToggleButton(
                checked = isSelected,
                onCheckedChange = { checked -> if (checked) onMethodSelected(method) },
                modifier = Modifier.weight(1f).heightIn(min = spacing.touchTarget),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    ComparisonMethod.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.toggleButtonColors(
                    checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(
                    text = stringResource(
                        if (method == ComparisonMethod.HTML) {
                            Res.string.settings_comparison_page_text
                        } else {
                            Res.string.settings_comparison_screenshots
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun SettingsLeadingIcon(
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(MaterialTheme.shapes.small)
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}
