/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.main.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.getValue
import com.bernaferrari.changedetection.ui.main.SiteIdentity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.components.SegmentPosition
import com.bernaferrari.changedetection.ui.components.segmentShape
import com.bernaferrari.changedetection.ui.main.hasDetectedChange
import com.bernaferrari.changedetection.util.displayPageTitle
import com.bernaferrari.changedetection.util.displayPageAddress
import com.bernaferrari.changedetection.util.getRelativeTimeLabel
import org.jetbrains.compose.resources.stringResource

/** Selection is explicit; in a running session a page opens its actual result. */
@Composable
internal fun TurboSiteRow(
    item: SiteAndLastSnap,
    selected: Boolean,
    selecting: Boolean,
    sessionStarted: Long,
    position: SegmentPosition,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val site = item.site
    val failed = site.timestamp >= sessionStarted && site.timestamp > 0 && !site.isSuccessful
    val changed = if (sessionStarted > 0) {
        item.snapshotCount > 1 && (item.snap?.timestamp ?: Long.MIN_VALUE) >= sessionStarted
    } else {
        item.hasDetectedChange()
    }
    val status = when {
        !site.isSyncEnabled -> stringResource(Res.string.site_status_paused)
        item.isSyncing -> stringResource(Res.string.home_action_checking)
        site.timestamp == 0L || (sessionStarted > 0 && site.timestamp < sessionStarted) -> stringResource(Res.string.turbo_waiting_check)
        failed -> if (site.lastFailureCode != null) com.bernaferrari.changedetection.ui.components.checkFailureTitle(site.lastFailureCode) else stringResource(Res.string.turbo_check_failed)
        changed -> stringResource(Res.string.turbo_changed_badge)
        else -> stringResource(Res.string.turbo_checked_ago, getRelativeTimeLabel(site.timestamp))
    }
    val containerColor by animateColorAsState(
        if (selecting && selected && site.isSyncEnabled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "turboPageSelection",
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = segmentShape(position),
        color = containerColor,
        contentColor = if (selecting && selected && site.isSyncEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().then(
                if (selecting && site.isSyncEnabled) {
                    Modifier.toggleable(selected, role = Role.Checkbox, onValueChange = { onToggle() })
                } else {
                    Modifier.clickable(onClick = onOpen)
                },
            ).padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SiteIdentity(site.displayPageTitle(), Modifier.size(44.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(site.displayPageTitle(), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        selecting && !site.isSyncEnabled -> stringResource(Res.string.turbo_paused_open)
                        selecting -> site.url.displayPageAddress()
                        else -> status
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        selecting && selected && site.isSyncEnabled -> MaterialTheme.colorScheme.onPrimaryContainer
                        !selecting && failed -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            when {
                selecting && site.isSyncEnabled -> Checkbox(checked = selected, onCheckedChange = null)
                item.isSyncing -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                failed -> Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                else -> Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
