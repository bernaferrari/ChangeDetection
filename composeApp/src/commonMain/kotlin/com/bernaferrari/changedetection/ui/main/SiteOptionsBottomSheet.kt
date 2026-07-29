/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.bernaferrari.changedetection.shared.generated.resources.delete
import com.bernaferrari.changedetection.shared.generated.resources.edit
import com.bernaferrari.changedetection.shared.generated.resources.open_in_browser
import com.bernaferrari.changedetection.shared.generated.resources.refresh
import com.bernaferrari.changedetection.shared.generated.resources.remove
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
    onOpenInBrowser: (() -> Unit)? = null,
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
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.spacing.lg),
        ) {
            SiteSheetHeader(
                title = displayTitle,
                domain = displayDomain,
                color = Color(site.colors.first),
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
            onOpenInBrowser?.let { openInBrowser ->
                SheetActionRow(
                    icon = Icons.Outlined.OpenInBrowser,
                    label = stringResource(Res.string.open_in_browser),
                    onClick = openInBrowser,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.xs),
            )
            SheetActionRow(
                icon = Icons.Outlined.Delete,
                label = stringResource(Res.string.remove),
                onClick = onDelete,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SiteSheetHeader(
    title: String,
    domain: String,
    color: Color,
) {
    Row(
        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.16f),
            contentColor = color,
        ) {
            Text(
                text = title.firstOrNull()?.uppercase() ?: "?",
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
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
