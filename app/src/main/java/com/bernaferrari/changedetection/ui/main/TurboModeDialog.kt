package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.repo.Site

@Composable
fun TurboModeDialog(
    sites: List<Site>,
    lastSelectedSiteIds: List<String> = emptyList(),
    intervalSeconds: Int,
    onDismiss: () -> Unit,
    onIntervalChange: (Int) -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var selectedSiteIds by remember {
        mutableStateOf(
            if (lastSelectedSiteIds.isNotEmpty()) lastSelectedSiteIds.toSet()
            else sites.take(3).map { it.id }.toSet()
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        val visuals = rememberQuickMonitorVisuals()
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, visuals.onGradient.copy(alpha = 0.18f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(visuals.gradient)
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            color = visuals.onGradient.copy(alpha = 0.18f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(14.dp)
                                    .size(24.dp),
                                tint = visuals.onGradient
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(id = R.string.turbo_mode_header),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = visuals.onGradient
                            )
                            Text(
                                text = stringResource(id = R.string.turbo_mode_subheader),
                                style = MaterialTheme.typography.bodyMedium,
                                color = visuals.onGradient.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Warning box
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(id = R.string.turbo_mode_dialog_warning_title),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = stringResource(id = R.string.turbo_mode_dialog_warning_body, intervalSeconds),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Interval selection
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(id = R.string.label_interval),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf(15, 30, 60).forEach { option ->
                            AssistChip(
                                onClick = { onIntervalChange(option) },
                                label = { Text("${option}s") },
                                leadingIcon = {
                                    if (intervalSeconds == option) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (intervalSeconds == option) visuals.onGradient.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainer,
                                    labelColor = if (intervalSeconds == option) visuals.onGradient else MaterialTheme.colorScheme.onSurfaceVariant,
                                    leadingIconContentColor = if (intervalSeconds == option) visuals.onGradient else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                // Site selection
                Text(
                    text = pluralStringResource(
                        id = R.plurals.turbo_mode_selected_sites,
                        count = selectedSiteIds.size,
                        selectedSiteIds.size
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )

                // Sites list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sites) { site ->
                        TurboSiteCard(
                            site = site,
                            isSelected = selectedSiteIds.contains(site.id),
                            onToggle = {
                                selectedSiteIds = if (selectedSiteIds.contains(site.id)) {
                                    selectedSiteIds - site.id
                                } else {
                                    selectedSiteIds + site.id
                                }
                            }
                        )
                    }
                }

                Text(
                    text = stringResource(id = R.string.turbo_mode_duration_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(id = android.R.string.cancel))
                    }

                    Button(
                        onClick = { onConfirm(selectedSiteIds.toList()) },
                        enabled = selectedSiteIds.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = visuals.onGradient,
                            contentColor = Color.White
                        )
                    ) {
                        Text(stringResource(id = R.string.turbo_mode_button_start))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TurboSiteCard(
    site: Site,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val visuals = rememberQuickMonitorVisuals()
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = if (isSelected) 3.dp else 1.dp,
        border = BorderStroke(
            1.dp,
            if (isSelected) visuals.onGradient.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        ),
        color = if (isSelected) {
            visuals.onGradient.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = site.title ?: site.url,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = site.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
