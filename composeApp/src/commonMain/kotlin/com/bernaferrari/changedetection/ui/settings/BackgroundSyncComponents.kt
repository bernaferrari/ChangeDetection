/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.settings_24_hours
import com.bernaferrari.changedetection.shared.generated.resources.settings_background_sync_disabled
import com.bernaferrari.changedetection.shared.generated.resources.settings_background_sync_title
import com.bernaferrari.changedetection.shared.generated.resources.settings_interval_minutes
import com.bernaferrari.changedetection.shared.generated.resources.settings_interval_minimum
import com.bernaferrari.changedetection.shared.generated.resources.settings_quick_presets
import com.bernaferrari.changedetection.shared.generated.resources.settings_sync_constraint_battery
import com.bernaferrari.changedetection.shared.generated.resources.settings_sync_constraint_charging
import com.bernaferrari.changedetection.shared.generated.resources.settings_sync_constraint_idle
import com.bernaferrari.changedetection.shared.generated.resources.settings_sync_constraint_wifi
import com.bernaferrari.changedetection.shared.generated.resources.settings_sync_monitoring_in_background
import com.bernaferrari.changedetection.ui.theme.ExpressiveSpring
import com.bernaferrari.changedetection.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BackgroundSyncHeader(
    syncEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val haptic = LocalHapticFeedback.current
    val iconBackground by animateColorAsState(
        targetValue = if (syncEnabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS,
        ),
        label = "backgroundSyncIconBackground",
    )
    val iconColor by animateColorAsState(
        targetValue = if (syncEnabled) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS,
        ),
        label = "backgroundSyncIconColor",
    )
    val toggle = { checked: Boolean ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onToggle(checked)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .heightIn(min = spacing.touchTarget)
            .toggleable(
                value = syncEnabled,
                role = Role.Switch,
                onValueChange = toggle,
            )
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Sync,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.settings_background_sync_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (syncEnabled) {
                    stringResource(Res.string.settings_sync_monitoring_in_background)
                } else {
                    stringResource(Res.string.settings_background_sync_disabled)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = syncEnabled,
            onCheckedChange = null,
        )
    }
}

@Composable
internal fun BackgroundSyncSectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            content()
        }
    }
}

@Composable
internal fun SyncIntervalInput(
    currentInterval: Long,
    onIntervalChanged: (Long) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    var textValue by remember(currentInterval) { mutableStateOf(currentInterval.toString()) }
    var isError by remember { mutableStateOf(false) }
    val presets = listOf(15L, 30L, 60L, 120L, 360L, 720L)
    val twentyFourHoursLabel = stringResource(Res.string.settings_24_hours)

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = textValue,
                onValueChange = { newValue ->
                    textValue = newValue
                    val parsed = newValue.toLongOrNull()
                    if (parsed != null && parsed >= 15) {
                        isError = false
                        onIntervalChanged(parsed)
                    } else {
                        isError = newValue.isNotEmpty() && (parsed ?: 0) < 15
                    }
                },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(Res.string.settings_interval_minutes)) },
                singleLine = true,
                isError = isError,
                supportingText = if (isError) {
                    { Text(stringResource(Res.string.settings_interval_minimum)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                ),
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = formatInterval(currentInterval),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                )
            }
        }

        Text(
            text = stringResource(Res.string.settings_quick_presets),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PresetRow(
            presets = presets.take(4),
            selectedInterval = currentInterval,
            onIntervalSelected = { preset ->
                textValue = preset.toString()
                onIntervalChanged(preset)
            },
        )
        PresetRow(
            presets = presets.drop(4) + 1440L,
            selectedInterval = currentInterval,
            onIntervalSelected = { preset ->
                textValue = preset.toString()
                onIntervalChanged(preset)
            },
            labelForPreset = { preset ->
                if (preset == 1440L) twentyFourHoursLabel else formatInterval(preset)
            },
        )
    }
}

@Composable
private fun PresetRow(
    presets: List<Long>,
    selectedInterval: Long,
    onIntervalSelected: (Long) -> Unit,
    labelForPreset: (Long) -> String = ::formatInterval,
) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        presets.forEach { preset ->
            PresetChip(
                label = labelForPreset(preset),
                isSelected = selectedInterval == preset,
                onClick = { onIntervalSelected(preset) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val haptic = LocalHapticFeedback.current
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS,
        ),
        label = "syncPresetBackground",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS,
        ),
        label = "syncPresetContent",
    )

    Box(
        modifier = modifier
            .heightIn(min = spacing.touchTarget)
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
        )
    }
}

@Composable
internal fun SyncConstraints(
    requireWifi: Boolean,
    requireCharging: Boolean,
    requireBatteryNotLow: Boolean,
    requireDeviceIdle: Boolean,
    onWifiChange: (Boolean) -> Unit,
    onChargingChange: (Boolean) -> Unit,
    onBatteryChange: (Boolean) -> Unit,
    onIdleChange: (Boolean) -> Unit,
) {
    Column {
        SyncConstraintRow(
            icon = Icons.Outlined.Wifi,
            title = stringResource(Res.string.settings_sync_constraint_wifi),
            checked = requireWifi,
            onCheckedChange = onWifiChange,
        )
        SyncConstraintRow(
            icon = Icons.Outlined.BatteryChargingFull,
            title = stringResource(Res.string.settings_sync_constraint_charging),
            checked = requireCharging,
            onCheckedChange = onChargingChange,
        )
        SyncConstraintRow(
            icon = Icons.Outlined.BatterySaver,
            title = stringResource(Res.string.settings_sync_constraint_battery),
            checked = requireBatteryNotLow,
            onCheckedChange = onBatteryChange,
        )
        SyncConstraintRow(
            icon = Icons.Outlined.PhoneAndroid,
            title = stringResource(Res.string.settings_sync_constraint_idle),
            checked = requireDeviceIdle,
            onCheckedChange = onIdleChange,
        )
    }
}

@Composable
private fun SyncConstraintRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val haptic = LocalHapticFeedback.current
    val iconBackground by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS,
        ),
        label = "syncConstraintIconBackground",
    )
    val iconColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS,
        ),
        label = "syncConstraintIconColor",
    )
    val toggle = { value: Boolean ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onCheckedChange(value)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.touchTarget)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = toggle,
            )
            .padding(vertical = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.small)
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}

private fun formatInterval(minutes: Long): String = when {
    minutes < 60 -> "${minutes}m"
    minutes < 1440 -> "${minutes / 60}h"
    else -> "${minutes / 1440}d"
}
