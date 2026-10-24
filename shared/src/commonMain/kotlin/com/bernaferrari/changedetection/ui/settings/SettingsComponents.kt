@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bernaferrari.changedetection.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.model.ThemeMode
import com.bernaferrari.changedetection.model.AppPalette
import com.bernaferrari.changedetection.repo.ComparisonMethod
import com.bernaferrari.changedetection.ui.components.SegmentPosition
import com.bernaferrari.changedetection.ui.theme.spacing
import com.bernaferrari.changedetection.ui.theme.supportsDynamicColors
import com.bernaferrari.changedetection.localization.stringResource
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.settings_comparison_page_text
import com.bernaferrari.changedetection.shared.generated.resources.settings_comparison_screenshots
import com.bernaferrari.changedetection.shared.generated.resources.settings_palette_label
import com.bernaferrari.changedetection.shared.generated.resources.settings_theme_label

@Composable
internal fun SettingsSectionHeader(
    title: String,
) {
    val spacing = MaterialTheme.spacing
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Normal,
        modifier = Modifier.padding(
            start = spacing.xl,
            end = spacing.lg,
            top = spacing.lg,
            bottom = spacing.sm,
        ),
    )
}

@Composable
internal fun SettingsGroup(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
}

@Composable
internal fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    showChevron: Boolean = true,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    position: SegmentPosition = SegmentPosition.Only,
    onClick: (() -> Unit)? = null,
) {
    val spacing = MaterialTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }
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
    Surface(shape = com.bernaferrari.changedetection.ui.components.segmentShape(position), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        BoxWithConstraints(Modifier.fillMaxWidth().heightIn(min = 64.dp).then(clickModifier).padding(16.dp)) {
            val constrained = maxWidth / LocalDensity.current.fontScale < 160.dp
            val showDestination = showChevron && onClick != null
            if (constrained) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SettingsIcon(icon, iconTint)
                        Spacer(Modifier.weight(1f))
                        if (showDestination) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    SettingsRowText(title, value, subtitle)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SettingsIcon(icon, iconTint)
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.weight(1f)) { SettingsRowText(title, value, subtitle) }
                    if (showDestination) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.padding(start = 8.dp).size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SettingsRowText(title: String, value: String?, subtitle: String?) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val titleStyle = MaterialTheme.typography.titleMedium
    val valueStyle = MaterialTheme.typography.bodyMedium
    val inlineWidth = with(density) {
        (
            measurer.measure(title, titleStyle, softWrap = false).size.width +
                (value?.let { measurer.measure(it, valueStyle, softWrap = false).size.width } ?: 0)
            ).toDp()
    } + 12.dp
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (value != null && inlineWidth > maxWidth) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, Modifier.fillMaxWidth(), style = titleStyle)
                    Text(value, Modifier.fillMaxWidth(), style = valueStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), style = titleStyle)
                    value?.let {
                        Spacer(Modifier.width(12.dp))
                        Text(it, style = valueStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
    val visiblePalettes = AppPalette.pickerOptions.filter { !it.usesSystemColors || supportsDynamicColors() }
    val dynamicPalette = visiblePalettes.firstOrNull { it.usesSystemColors }
    val fixedPalettes = visiblePalettes.filterNot { it.usesSystemColors }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp, 24.dp, 8.dp, 8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier.padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Text(
                    text = stringResource(Res.string.settings_theme_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                val labels = visibleModes.map { stringResource(it.titleRes) }
                val measurer = rememberTextMeasurer()
                val density = LocalDensity.current
                val labelStyle = MaterialTheme.typography.labelLarge
                val widestLabel = labels.maxOf { measurer.measure(it, labelStyle, softWrap = false).size.width }
                val requiredWidth = (with(density) { widestLabel.toDp() } + 48.dp) * visibleModes.size +
                    ButtonGroupDefaults.ConnectedSpaceBetween * (visibleModes.size - 1)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val connected = maxWidth >= requiredWidth

                    @Composable
                    fun Choice(index: Int, modifier: Modifier) {
                        val mode = visibleModes[index]
                        ToggleButton(
                            checked = mode == selectedTheme,
                            onCheckedChange = { if (it) onThemeSelected(mode) },
                            shapes = when {
                                !connected -> ToggleButtonDefaults.shapesFor(ToggleButtonDefaults.size)
                                index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                index == visibleModes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                            colors = ToggleButtonDefaults.colors(
                                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            modifier = modifier.heightIn(min = 48.dp),
                        ) { Text(labels[index], style = labelStyle) }
                    }
                    if (connected) {
                        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                            visibleModes.indices.forEach { Choice(it, Modifier.weight(1f)) }
                        }
                    } else {
                        Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            visibleModes.indices.forEach { Choice(it, Modifier.fillMaxWidth()) }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp, 8.dp, 24.dp, 24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .padding(vertical = spacing.sm, horizontal = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(
                    text = stringResource(Res.string.settings_palette_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                dynamicPalette?.let { palette ->
                    DynamicPaletteRow(
                        palette = palette,
                        selected = palette == selectedPalette,
                        onClick = { onPaletteSelected(palette) },
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    fixedPalettes.forEach { palette ->
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
}

@Composable
private fun DynamicPaletteRow(
    palette: AppPalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val name = stringResource(palette.titleRes)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun PaletteSwatch(
    palette: AppPalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    com.bernaferrari.changedetection.ui.components.PaletteColorSwatch(
        color = if (palette.usesSystemColors) MaterialTheme.colorScheme.primary else palette.seedColor,
        label = stringResource(palette.titleRes),
        selected = selected,
        onClick = onClick,
        icon = if (palette.usesSystemColors) Icons.Rounded.Palette else null,
    )
}

@Composable
internal fun ComparisonMethodSelector(
    selectedMethod: ComparisonMethod,
    onMethodSelected: (ComparisonMethod) -> Unit,
) {
    com.bernaferrari.changedetection.ui.kit.ConnectedChoice(ComparisonMethod.entries, selectedMethod, onMethodSelected, Modifier) { method ->
        stringResource(if (method == ComparisonMethod.HTML) Res.string.settings_comparison_page_text else Res.string.settings_comparison_screenshots)
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector, tint: Color) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainer) {
        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = tint)
        }
    }
}
