/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.web

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoScaffold(
    destination: DemoDestination,
    selectedSiteId: String,
    isChecking: Boolean,
    lastCheckLabel: String,
    useNavigationRail: Boolean,
    onDestinationChange: (DemoDestination) -> Unit,
    onSiteSelected: (String) -> Unit,
    onCheckAll: () -> Unit,
) {
    if (useNavigationRail) {
        Row(modifier = Modifier.fillMaxSize()) {
            DemoNavigationRail(
                destination = destination,
                onDestinationChange = onDestinationChange,
            )
            DemoContentScaffold(
                modifier = Modifier.fillMaxHeight().weight(1f),
                destination = destination,
                selectedSiteId = selectedSiteId,
                lastCheckLabel = lastCheckLabel,
                isChecking = isChecking,
                onSiteSelected = onSiteSelected,
                onCheckAll = onCheckAll,
            )
        }
    } else {
        DemoContentScaffold(
            modifier = Modifier.fillMaxSize(),
            destination = destination,
            selectedSiteId = selectedSiteId,
            lastCheckLabel = lastCheckLabel,
            isChecking = isChecking,
            onSiteSelected = onSiteSelected,
            onCheckAll = onCheckAll,
            bottomBar = {
                DemoNavigationBar(
                    destination = destination,
                    onDestinationChange = onDestinationChange,
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoContentScaffold(
    destination: DemoDestination,
    selectedSiteId: String,
    isChecking: Boolean,
    lastCheckLabel: String,
    onSiteSelected: (String) -> Unit,
    onCheckAll: () -> Unit,
    modifier: Modifier,
    bottomBar: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Change Detection",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Interactive demo · synthetic local data",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onCheckAll,
                        enabled = !isChecking,
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .semantics { contentDescription = "Checking all monitored sites" },
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Check all monitored sites",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        when (destination) {
            DemoDestination.MONITORING -> MonitoringContent(
                selectedSiteId = selectedSiteId,
                lastCheckLabel = lastCheckLabel,
                onSiteSelected = onSiteSelected,
                modifier = Modifier.padding(padding),
            )

            DemoDestination.HISTORY -> HistoryContent(modifier = Modifier.padding(padding))

            DemoDestination.SETTINGS -> SettingsContent(modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun DemoNavigationBar(
    destination: DemoDestination,
    onDestinationChange: (DemoDestination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
    ) {
        DemoDestination.entries.forEach { item ->
            NavigationBarItem(
                selected = destination == item,
                onClick = { onDestinationChange(item) },
                icon = {
                    Icon(
                        imageVector = item.icon(),
                        contentDescription = null,
                    )
                },
                label = { Text(item.label()) },
            )
        }
    }
}

@Composable
private fun DemoNavigationRail(
    destination: DemoDestination,
    onDestinationChange: (DemoDestination) -> Unit,
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        DemoDestination.entries.forEach { item ->
            NavigationRailItem(
                selected = destination == item,
                onClick = { onDestinationChange(item) },
                icon = {
                    Icon(
                        imageVector = item.icon(),
                        contentDescription = null,
                    )
                },
                label = { Text(item.label()) },
            )
        }
    }
}

@Composable
private fun MonitoringContent(
    selectedSiteId: String,
    lastCheckLabel: String,
    onSiteSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedSite = demoSites.first { it.id == selectedSiteId }

    DemoContentFrame(modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MonitoringSummary(lastCheckLabel = lastCheckLabel)
            }
            item {
                Text(
                    text = "Monitored sites",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(demoSites, key = { it.id }) { site ->
                SiteCard(
                    site = site,
                    selected = site.id == selectedSiteId,
                    onClick = { onSiteSelected(site.id) },
                )
            }
            item {
                LatestChangeCard(site = selectedSite)
            }
        }
    }
}

@Composable
private fun DemoContentFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            content()
        }
    }
}

@Composable
private fun MonitoringSummary(lastCheckLabel: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Default.MonitorHeart,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "3 sites monitored",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastCheckLabel,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            StatusPill(
                label = "2 changes",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun SiteCard(
    site: DemoSite,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) site.accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.selected = selected }
            .clickable(role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(site.accent),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = site.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (site.changeDescription != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusPill(label = "Changed", color = site.accent)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = site.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Checked ${site.lastChecked}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LatestChangeCard(site: DemoSite) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Article,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Latest change",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = site.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (site.changeDescription == null) {
                Text(
                    text = "No change has been recorded for this site yet.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                DiffLine(prefix = "−", text = "Previous snapshot", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                DiffLine(prefix = "+", text = site.changeDescription, color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun DiffLine(prefix: String, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = prefix,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun HistoryContent(modifier: Modifier = Modifier) {
    DemoContentFrame(modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Recent activity",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "A concise record of the changes your demo sites observed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(demoSites.filter { it.changeDescription != null }, key = { it.id }) { site ->
                Card(shape = MaterialTheme.shapes.large) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = site.accent.copy(alpha = 0.2f),
                            contentColor = site.accent,
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(site.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = site.changeDescription.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsContent(modifier: Modifier = Modifier) {
    DemoContentFrame(modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Demo settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item {
                Text(
                    text = "This web build uses synthetic data only. It does not request pages, store credentials, or send analytics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                DemoSettingRow(
                    icon = Icons.Default.VerifiedUser,
                    title = "Private by design",
                    summary = "No account or tracking in this demo",
                )
            }
            item {
                DemoSettingRow(
                    icon = Icons.Default.MonitorHeart,
                    title = "Full Android app",
                    summary = "Monitor your own public pages on-device",
                )
            }
        }
    }
}

@Composable
private fun DemoSettingRow(
    icon: ImageVector,
    title: String,
    summary: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.16f),
        contentColor = color,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

private fun DemoDestination.label(): String = when (this) {
    DemoDestination.MONITORING -> "Monitor"
    DemoDestination.HISTORY -> "History"
    DemoDestination.SETTINGS -> "Settings"
}

private fun DemoDestination.icon(): ImageVector = when (this) {
    DemoDestination.MONITORING -> Icons.Default.MonitorHeart
    DemoDestination.HISTORY -> Icons.Default.History
    DemoDestination.SETTINGS -> Icons.Default.Settings
}
