package com.bernaferrari.changedetection.ui.settings

import com.bernaferrari.changedetection.background.MonitoringExecution
import com.bernaferrari.changedetection.background.monitoringCapabilities

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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.background.MonitoringSchedule
import com.bernaferrari.changedetection.shared.generated.resources.settings_sync_unit_minutes
import com.bernaferrari.changedetection.shared.generated.resources.settings_sync_unit_hours
import com.bernaferrari.changedetection.shared.generated.resources.settings_sync_unit_days
import com.bernaferrari.changedetection.shared.generated.resources.settings_sync_interval_value
import com.bernaferrari.changedetection.shared.generated.resources.settings_automatic_checks_title
import com.bernaferrari.changedetection.shared.generated.resources.settings_checks_while_open
import com.bernaferrari.changedetection.shared.generated.resources.settings_checks_unavailable
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.settings_24_hours
import com.bernaferrari.changedetection.shared.generated.resources.settings_background_sync_disabled
import com.bernaferrari.changedetection.shared.generated.resources.settings_background_sync_title
import com.bernaferrari.changedetection.shared.generated.resources.settings_interval_minimum
import com.bernaferrari.changedetection.shared.generated.resources.settings_sync_constraint_battery
import com.bernaferrari.changedetection.shared.generated.resources.settings_sync_constraint_charging
import com.bernaferrari.changedetection.shared.generated.resources.settings_sync_constraint_wifi
import com.bernaferrari.changedetection.shared.generated.resources.settings_sync_monitoring_in_background
import com.bernaferrari.changedetection.ui.kit.ConnectedChoice
import com.bernaferrari.changedetection.ui.theme.spacing
import com.bernaferrari.changedetection.localization.stringResource

@Composable
internal fun BackgroundSyncHeader(
    syncEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val iconBackground = if (syncEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    val iconColor = if (syncEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .heightIn(min = spacing.touchTarget)
            .toggleable(
                value = syncEnabled,
                enabled = monitoringCapabilities.execution != MonitoringExecution.Unavailable,
                role = Role.Switch,
                onValueChange = onToggle,
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
                text = stringResource(if (monitoringCapabilities.execution == MonitoringExecution.Background) Res.string.settings_background_sync_title else Res.string.settings_automatic_checks_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    when {
                        monitoringCapabilities.execution == MonitoringExecution.Unavailable -> Res.string.settings_checks_unavailable
                        !syncEnabled -> Res.string.settings_background_sync_disabled
                        monitoringCapabilities.execution == MonitoringExecution.WhileOpen -> Res.string.settings_checks_while_open
                        else -> Res.string.settings_sync_monitoring_in_background
                    },
                ),
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

/** Draft editor: no scheduler or persistence work happens until the sheet's Save action. */
@Composable
internal fun SyncIntervalInput(
    currentInterval: Long,
    onIntervalChanged: (Long) -> Unit,
    onValidityChanged: (Boolean) -> Unit = {},
    enabled: Boolean = true,
) {
    fun unitFor(minutes: Long) = when {
        minutes % 1440L == 0L -> 1440L
        minutes % 60L == 0L -> 60L
        else -> 1L
    }
    var unit by remember { mutableStateOf(unitFor(currentInterval)) }
    var amount by remember { mutableStateOf(com.bernaferrari.changedetection.util.localizedInteger(currentInterval / unit)) }
    var isError by remember { mutableStateOf(false) }
    fun update(value: String, newUnit: Long) {
        amount = value
        unit = newUnit
        val count = com.bernaferrari.changedetection.util.parseLocalizedWholeNumber(value)
        val valid = count != null && count > 0 && count <= MonitoringSchedule.MAX_INTERVAL_MINUTES / newUnit &&
            count * newUnit >= MonitoringSchedule.MIN_INTERVAL_MINUTES
        isError = !valid
        onValidityChanged(valid)
        if (valid) onIntervalChanged(requireNotNull(count) * newUnit)
    }
    val units = listOf(
        1L to stringResource(Res.string.settings_sync_unit_minutes),
        60L to stringResource(Res.string.settings_sync_unit_hours),
        1440L to stringResource(Res.string.settings_sync_unit_days),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextField(
            value = amount,
            onValueChange = { update(it, unit) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.settings_sync_interval_value)) },
            textStyle = MaterialTheme.typography.headlineLarge,
            singleLine = true,
            isError = isError,
            enabled = enabled,
            supportingText = { Text(stringResource(Res.string.settings_interval_minimum)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.extraLarge,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
            ),
        )
        ConnectedChoice(
            options = units.map { it.first },
            selected = unit,
            onSelect = { update(amount, it) },
            modifier = Modifier,
            enabled = enabled,
        ) { value -> units.first { it.first == value }.second }
    }
}

@Composable
internal fun SyncConstraints(
    requireWifi: Boolean,
    requireCharging: Boolean,
    requireBatteryNotLow: Boolean,
    onWifiChange: (Boolean) -> Unit,
    onChargingChange: (Boolean) -> Unit,
    onBatteryChange: (Boolean) -> Unit,
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
    val iconBackground = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val iconColor = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.touchTarget)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
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
