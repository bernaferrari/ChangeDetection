package com.bernaferrari.changedetection.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.settings.SettingsViewModel

@Composable
fun BackgroundSyncDialog(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
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
                    text = "Background Sync",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable background sync")
                    Switch(
                        checked = syncEnabled,
                        onCheckedChange = { 
                            syncEnabled = it
                            viewModel.updateBackgroundSync(it, context)
                        }
                    )
                }
                
                if (syncEnabled) {
                    IntervalPicker(
                        currentInterval = intervalValue,
                        onIntervalChanged = { newInterval ->
                            intervalValue = newInterval
                            viewModel.updateSyncInterval(newInterval, context)
                        }
                    )
                    
                    SyncDescription(intervalValue)
                    
                    // Sync constraints
                    Text(
                        text = "Sync Constraints",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    ConstraintRow(
                        title = "Require Wi-Fi",
                        checked = uiState.requireWifi,
                        onCheckedChange = { viewModel.updateWifiConstraint(it, context) }
                    )
                    
                    ConstraintRow(
                        title = "Require charging",
                        checked = uiState.requireCharging,
                        onCheckedChange = { viewModel.updateChargingConstraint(it, context) }
                    )
                    
                    ConstraintRow(
                        title = "Require battery not low",
                        checked = uiState.requireBatteryNotLow,
                        onCheckedChange = { viewModel.updateBatteryNotLowConstraint(it, context) }
                    )
                    
                    ConstraintRow(
                        title = "Require device idle",
                        checked = uiState.requireDeviceIdle,
                        onCheckedChange = { viewModel.updateDeviceIdleConstraint(it, context) }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
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
    // Convert minutes to appropriate display units
    val (displayValue, unit, multiplier) = when {
        currentInterval >= 1440 -> Triple(currentInterval / 1440, "days", 1440L)
        currentInterval >= 60 -> Triple(currentInterval / 60, "hours", 60L)
        else -> Triple(currentInterval, "minutes", 1L)
    }
    
    val timeUnits = arrayOf("minutes", "hours", "days")
    val multipliers = arrayOf(1L, 60L, 1440L)
    
    var selectedUnitIndex by remember { 
        mutableIntStateOf(multipliers.indexOf(multiplier))
    }
    var inputValue by remember { 
        mutableStateOf(displayValue.toString())
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Sync every:")
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputValue,
                onValueChange = { newValue ->
                    inputValue = newValue
                    newValue.toLongOrNull()?.let { value ->
                        if (value > 0) {
                            val newInterval = value * multipliers[selectedUnitIndex]
                            onIntervalChanged(newInterval)
                        }
                    }
                },
                modifier = Modifier.width(100.dp),
                singleLine = true,
                label = { Text("Value") }
            )
            
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = timeUnits[selectedUnitIndex],
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    timeUnits.forEachIndexed { index, unit ->
                        DropdownMenuItem(
                            text = { Text(unit) },
                            onClick = {
                                selectedUnitIndex = index
                                expanded = false
                                
                                inputValue.toLongOrNull()?.let { value ->
                                    if (value > 0) {
                                        val newInterval = value * multipliers[index]
                                        onIntervalChanged(newInterval)
                                    }
                                }
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
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
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
            "Sync every $days day${if (days != 1L) "s" else ""}"
        }
        intervalMinutes >= 60 -> {
            val hours = intervalMinutes / 60
            "Sync every $hours hour${if (hours != 1L) "s" else ""}"
        }
        else -> "Sync every $intervalMinutes minute${if (intervalMinutes != 1L) "s" else ""}"
    }
    
    Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

object DialogBackgroundSync {
    fun show(activity: FragmentActivity) {
        // In Compose world, this would be handled by state management
        // For now, keeping the same interface for compatibility
    }
}
