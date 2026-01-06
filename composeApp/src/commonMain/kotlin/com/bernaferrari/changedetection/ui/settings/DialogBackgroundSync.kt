package com.bernaferrari.changedetection.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.theme.ExpressiveSpring
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundSyncDialog(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var syncEnabled by remember { mutableStateOf(uiState.backgroundSync) }
    var intervalValue by remember { mutableLongStateOf(uiState.syncInterval) }

    LaunchedEffect(uiState) {
        syncEnabled = uiState.backgroundSync
        intervalValue = uiState.syncInterval
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Hero Section
            SyncHeroSection(
                syncEnabled = syncEnabled,
                intervalMinutes = intervalValue
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Enable/Disable Toggle
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(Res.string.settings_background_sync_enable),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (syncEnabled) stringResource(Res.string.settings_sync_monitoring_in_background) else stringResource(Res.string.settings_background_sync_disabled),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = syncEnabled,
                        onCheckedChange = {
                            syncEnabled = it
                            viewModel.updateBackgroundSync(it)
                        }
                    )
                }
            }

            if (syncEnabled) {
                Spacer(modifier = Modifier.height(24.dp))

                // Interval Picker
                IntervalChipPicker(
                    currentInterval = intervalValue,
                    onIntervalChanged = { newInterval ->
                        intervalValue = newInterval
                        viewModel.updateSyncInterval(newInterval)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Constraints Section
                ConstraintsSection(
                    requireWifi = uiState.requireWifi,
                    requireCharging = uiState.requireCharging,
                    requireBatteryNotLow = uiState.requireBatteryNotLow,
                    requireDeviceIdle = uiState.requireDeviceIdle,
                    onWifiChange = { viewModel.updateWifiConstraint(it) },
                    onChargingChange = { viewModel.updateChargingConstraint(it) },
                    onBatteryChange = { viewModel.updateBatteryNotLowConstraint(it) },
                    onIdleChange = { viewModel.updateDeviceIdleConstraint(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Done Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(stringResource(Res.string.done))
            }
        }
    }
}

@Composable
private fun SyncHeroSection(
    syncEnabled: Boolean,
    intervalMinutes: Long
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    
    val gradient = if (syncEnabled) {
        Brush.linearGradient(
            colors = listOf(primaryColor, tertiaryColor),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                MaterialTheme.colorScheme.surfaceContainerHigh
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(gradient)
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (syncEnabled) 0.2f else 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Sync,
                    contentDescription = null,
                    tint = if (syncEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Column {
                Text(
                    text = stringResource(Res.string.settings_background_sync_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (syncEnabled) Color.White else MaterialTheme.colorScheme.onSurface
                )
                
                val description = if (syncEnabled) {
                    when {
                        intervalMinutes >= 1440 -> {
                            val days = intervalMinutes / 1440
                            pluralStringResource(
                                resource = Res.plurals.settings_sync_every_days,
                                quantity = days.toInt(),
                                days.toInt()
                            )
                        }
                        intervalMinutes >= 60 -> {
                            val hours = intervalMinutes / 60
                            pluralStringResource(
                                resource = Res.plurals.settings_sync_every_hours,
                                quantity = hours.toInt(),
                                hours.toInt()
                            )
                        }
                        else -> pluralStringResource(
                            resource = Res.plurals.settings_sync_every_minutes,
                            quantity = intervalMinutes.toInt(),
                            intervalMinutes.toInt()
                        )
                    }
                } else {
                    stringResource(Res.string.settings_sync_not_syncing)
                }
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (syncEnabled) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntervalChipPicker(
    currentInterval: Long,
    onIntervalChanged: (Long) -> Unit
) {
    // Common intervals in minutes
    val intervals = listOf(
        15L to "15m",
        30L to "30m",
        60L to "1h",
        120L to "2h",
        360L to "6h",
        720L to "12h",
        1440L to "1d"
    )

    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(Res.string.settings_sync_interval_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            intervals.forEach { (minutes, label) ->
                val isSelected = currentInterval == minutes
                FilterChip(
                    selected = isSelected,
                    onClick = { onIntervalChanged(minutes) },
                    label = { Text(label) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun ConstraintsSection(
    requireWifi: Boolean,
    requireCharging: Boolean,
    requireBatteryNotLow: Boolean,
    requireDeviceIdle: Boolean,
    onWifiChange: (Boolean) -> Unit,
    onChargingChange: (Boolean) -> Unit,
    onBatteryChange: (Boolean) -> Unit,
    onIdleChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.settings_sync_constraints_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = stringResource(Res.string.settings_sync_constraints_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column {
                ConstraintItem(
                    icon = Icons.Outlined.Wifi,
                    title = stringResource(Res.string.settings_sync_constraint_wifi),
                    checked = requireWifi,
                    onCheckedChange = onWifiChange
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                
                ConstraintItem(
                    icon = Icons.Outlined.BatteryChargingFull,
                    title = stringResource(Res.string.settings_sync_constraint_charging),
                    checked = requireCharging,
                    onCheckedChange = onChargingChange
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                
                ConstraintItem(
                    icon = Icons.Outlined.BatterySaver,
                    title = stringResource(Res.string.settings_sync_constraint_battery),
                    checked = requireBatteryNotLow,
                    onCheckedChange = onBatteryChange
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                
                ConstraintItem(
                    icon = Icons.Outlined.PhoneAndroid,
                    title = stringResource(Res.string.settings_sync_constraint_idle),
                    checked = requireDeviceIdle,
                    onCheckedChange = onIdleChange
                )
            }
        }
    }
}

@Composable
private fun ConstraintItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
