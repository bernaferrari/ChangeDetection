/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Notifications
import com.bernaferrari.changedetection.shared.generated.resources.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.notifications_platform_unavailable
import com.bernaferrari.changedetection.shared.generated.resources.notification_system_settings
import com.bernaferrari.changedetection.shared.generated.resources.delete
import com.bernaferrari.changedetection.shared.generated.resources.edit
import com.bernaferrari.changedetection.shared.generated.resources.open_in_browser
import com.bernaferrari.changedetection.shared.generated.resources.refresh
import com.bernaferrari.changedetection.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteOptionsBottomSheet(
    site: Site,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onToggleMonitoring: () -> Unit,
    onToggleNotifications: () -> Unit,
    notificationDeliveryAvailable: Boolean,
    onNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
    )
    val displayTitle = site.title?.takeIf { it.isNotBlank() } ?: site.domain()
    val displayDomain = site.domain()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = MaterialTheme.spacing.lg),
        ) {
            SiteSheetHeader(
                title = displayTitle,
                domain = displayDomain,
            )
            Spacer(modifier = Modifier.size(MaterialTheme.spacing.xs))
            SheetActionRow(
                icon = Icons.Outlined.Refresh,
                label = stringResource(Res.string.refresh),
                onClick = onRefresh,
            )
            SheetActionRow(
                icon = Icons.Outlined.Edit,
                label = stringResource(Res.string.edit),
                onClick = onEdit,
            )
            SheetActionRow(
                icon = if (site.isSyncEnabled) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                label = stringResource(if (site.isSyncEnabled) Res.string.pause_monitoring else Res.string.resume_monitoring),
                onClick = onToggleMonitoring,
            )
            NotificationPreference(notificationDeliveryAvailable, site.isNotificationEnabled, onToggleNotifications, onNotificationSettings)
            SheetActionRow(
                icon = Icons.Outlined.OpenInBrowser,
                label = stringResource(Res.string.open_in_browser),
                onClick = onOpenInBrowser,
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.xs),
            )
            SheetActionRow(
                icon = Icons.Outlined.Delete,
                label = stringResource(Res.string.delete),
                onClick = onDelete,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
internal fun NotificationPreference(deliverySupported: Boolean, enabled: Boolean, onToggle: () -> Unit, onOpenSettings: () -> Unit) {
    if (deliverySupported) {
        SheetActionRow(
            icon = Icons.Outlined.Notifications,
            label = stringResource(if (enabled) Res.string.notification_disable else Res.string.notification_enable),
            onClick = onToggle,
        )
        SheetActionRow(
            icon = Icons.Outlined.Notifications,
            label = stringResource(Res.string.notification_system_settings),
            onClick = onOpenSettings,
        )
    } else {
        Text(
            stringResource(Res.string.notifications_platform_unavailable),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SiteSheetHeader(
    title: String,
    domain: String,
) {
    Row(
        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SiteIdentity(title = title, modifier = Modifier.size(44.dp))
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (domain != title) {
                Text(
                    text = domain,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SheetActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.spacing.touchTarget),
        color = Color.Transparent,
        contentColor = color,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.md))
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

private fun Site.domain(): String = url.replace(Regex("^https?://(www\\.)?"), "").substringBefore("/")
