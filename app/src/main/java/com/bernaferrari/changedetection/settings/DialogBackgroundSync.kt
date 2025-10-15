package com.bernaferrari.changedetection.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.R

@Composable
fun BackgroundSyncDialog(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var syncEnabled by remember { mutableStateOf(uiState.backgroundSync) }
    var intervalValue by remember { mutableLongStateOf(uiState.syncInterval) }

    LaunchedEffect(uiState) {
        syncEnabled = uiState.backgroundSync
        intervalValue = uiState.syncInterval
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.settings_background_sync_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                ConstraintRow(
                    titleRes = R.string.settings_background_sync_enable,
                    checked = syncEnabled,
                    onCheckedChange = {
                        syncEnabled = it
                        viewModel.updateBackgroundSync(it)
                    }
                )

                if (syncEnabled) {
                    IntervalPicker(
                        currentInterval = intervalValue,
                        onIntervalChanged = { newInterval ->
                            intervalValue = newInterval
                            viewModel.updateSyncInterval(newInterval)
                        }
                    )

                    SyncDescription(intervalValue)

                    Text(
                        text = stringResource(id = R.string.settings_sync_constraints_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    ConstraintRow(
                        titleRes = R.string.settings_sync_constraint_wifi,
                        checked = uiState.requireWifi,
                        onCheckedChange = { viewModel.updateWifiConstraint(it) }
                    )

                    ConstraintRow(
                        titleRes = R.string.settings_sync_constraint_charging,
                        checked = uiState.requireCharging,
                        onCheckedChange = { viewModel.updateChargingConstraint(it) }
                    )

                    ConstraintRow(
                        titleRes = R.string.settings_sync_constraint_battery,
                        checked = uiState.requireBatteryNotLow,
                        onCheckedChange = { viewModel.updateBatteryNotLowConstraint(it) }
                    )

                    ConstraintRow(
                        titleRes = R.string.settings_sync_constraint_idle,
                        checked = uiState.requireDeviceIdle,
                        onCheckedChange = { viewModel.updateDeviceIdleConstraint(it) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(id = R.string.done))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntervalPicker(
    currentInterval: Long,
    onIntervalChanged: (Long) -> Unit
) {
    val availableUnits = listOf(
        IntervalUnit(minutesMultiplier = 1L, labelRes = R.string.settings_sync_unit_minutes),
        IntervalUnit(minutesMultiplier = 60L, labelRes = R.string.settings_sync_unit_hours),
        IntervalUnit(minutesMultiplier = 1440L, labelRes = R.string.settings_sync_unit_days)
    )

    val initialUnitIndex = when {
        currentInterval >= 1440 -> 2
        currentInterval >= 60 -> 1
        else -> 0
    }
    val initialValue = when (initialUnitIndex) {
        2 -> currentInterval / 1440
        1 -> currentInterval / 60
        else -> currentInterval
    }.coerceAtLeast(1)

    var selectedUnitIndex by rememberSaveable { mutableIntStateOf(initialUnitIndex) }
    var inputValue by rememberSaveable { mutableStateOf(initialValue.toString()) }

    fun emitInterval() {
        val numericValue = inputValue.toLongOrNull()
        if (numericValue != null && numericValue > 0) {
            val unit = availableUnits[selectedUnitIndex]
            onIntervalChanged(numericValue * unit.minutesMultiplier)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(id = R.string.settings_sync_interval_label))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputValue,
                onValueChange = { newValue ->
                    val sanitized = newValue.filter { it.isDigit() }.take(5)
                    inputValue = sanitized
                    emitInterval()
                },
                modifier = Modifier.width(100.dp),
                singleLine = true,
                label = { Text(stringResource(id = R.string.settings_sync_interval_value)) }
            )

            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = stringResource(id = availableUnits[selectedUnitIndex].labelRes),
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.width(140.dp)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableUnits.forEachIndexed { index, unit ->
                        DropdownMenuItem(
                            text = { Text(stringResource(id = unit.labelRes)) },
                            onClick = {
                                selectedUnitIndex = index
                                expanded = false
                                emitInterval()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConstraintRow(
    @StringRes titleRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(id = titleRes))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SyncDescription(intervalMinutes: Long) {
    val description = when {
        intervalMinutes >= 1440 -> {
            val days = intervalMinutes / 1440
            pluralStringResource(
                id = R.plurals.settings_sync_every_days,
                count = days.toInt(),
                days
            )
        }

        intervalMinutes >= 60 -> {
            val hours = intervalMinutes / 60
            pluralStringResource(
                id = R.plurals.settings_sync_every_hours,
                count = hours.toInt(),
                hours
            )
        }

        else -> pluralStringResource(
            id = R.plurals.settings_sync_every_minutes,
            count = intervalMinutes.toInt(),
            intervalMinutes
        )
    }

    Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private data class IntervalUnit(
    val minutesMultiplier: Long,
    @StringRes val labelRes: Int
)
