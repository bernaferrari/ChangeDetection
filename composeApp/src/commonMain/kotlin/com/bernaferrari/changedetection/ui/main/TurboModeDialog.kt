package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.cancel
import com.bernaferrari.changedetection.shared.generated.resources.label_interval
import com.bernaferrari.changedetection.shared.generated.resources.quick_monitor_sites_selected_label
import com.bernaferrari.changedetection.shared.generated.resources.turbo_mode_button_start
import com.bernaferrari.changedetection.shared.generated.resources.turbo_mode_dialog_warning_body
import com.bernaferrari.changedetection.shared.generated.resources.turbo_mode_dialog_warning_title
import com.bernaferrari.changedetection.shared.generated.resources.turbo_mode_duration_hint
import com.bernaferrari.changedetection.shared.generated.resources.turbo_mode_header
import com.bernaferrari.changedetection.shared.generated.resources.turbo_mode_selected_sites
import com.bernaferrari.changedetection.shared.generated.resources.turbo_mode_subheader
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

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
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            tonalElevation = 3.dp,
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                TurboHeroSection(
                    intervalSeconds = intervalSeconds,
                    selectedCount = selectedSiteIds.size,
                    visuals = visuals
                )

                TurboWarningCard(intervalSeconds = intervalSeconds)

                IntervalSelectorRow(
                    selectedInterval = intervalSeconds,
                    onIntervalChange = onIntervalChange
                )

                Text(
                    text = pluralStringResource(
                        resource = Res.plurals.turbo_mode_selected_sites,
                        quantity = selectedSiteIds.size,
                        selectedSiteIds.size
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sites) { site ->
                        TurboSiteRow(
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
                    text = stringResource(Res.string.turbo_mode_duration_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.cancel))
                    }
                    Button(
                        onClick = { onConfirm(selectedSiteIds.toList()) },
                        enabled = selectedSiteIds.isNotEmpty()
                    ) {
                        Text(text = stringResource(Res.string.turbo_mode_button_start))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TurboSiteRow(
    site: Site,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        },
        tonalElevation = if (isSelected) 3.dp else 1.dp,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        )
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
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium)
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

@Composable
private fun TurboHeroSection(
    intervalSeconds: Int,
    selectedCount: Int,
    visuals: QuickMonitorVisuals
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, visuals.onGradient.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(visuals.gradient)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = visuals.onGradient.copy(alpha = 0.18f)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = visuals.onGradient,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(Res.string.turbo_mode_header),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = visuals.onGradient
                    )
                    Text(
                        text = stringResource(Res.string.turbo_mode_subheader),
                        style = MaterialTheme.typography.`bodyMedium`,
                        color = visuals.onGradient.copy(alpha = 0.85f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text(
                        text = "${intervalSeconds}s",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = visuals.onGradient
                    )
                    Text(
                        text = stringResource(Res.string.label_interval),
                        style = MaterialTheme.typography.bodySmall,
                        color = visuals.onGradient.copy(alpha = 0.85f)
                    )
                }
                Column {
                    Text(
                        text = selectedCount.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = visuals.onGradient
                    )
                    Text(
                        text = stringResource(Res.string.quick_monitor_sites_selected_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = visuals.onGradient.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TurboWarningCard(intervalSeconds: Int) {
    Surface(
        shape = RoundedCornerShape(20.dp),
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
                    text = stringResource(Res.string.turbo_mode_dialog_warning_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = stringResource(Res.string.turbo_mode_dialog_warning_body, intervalSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntervalSelectorRow(
    selectedInterval: Int,
    onIntervalChange: (Int) -> Unit
) {
    val options = listOf(15, 30, 60, 120, 180)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.label_interval),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selectedInterval == option,
                    onClick = { onIntervalChange(option) },
                    label = { Text(text = "${option}s") },
                    leadingIcon = if (selectedInterval == option) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null
                            )
                        }
                    } else null,
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
            }
        }
    }
}
