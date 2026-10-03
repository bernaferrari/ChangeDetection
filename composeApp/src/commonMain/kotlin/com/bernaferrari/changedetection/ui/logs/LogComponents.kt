/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.delete
import com.bernaferrari.changedetection.shared.generated.resources.logs_editorial_sites
import com.bernaferrari.changedetection.shared.generated.resources.logs_empty_subtitle
import com.bernaferrari.changedetection.shared.generated.resources.logs_empty_title
import com.bernaferrari.changedetection.shared.generated.resources.logs_saved_versions
import com.bernaferrari.changedetection.shared.generated.resources.open_in_browser
import com.bernaferrari.changedetection.shared.generated.resources.loading
import com.bernaferrari.changedetection.shared.generated.resources.more_options
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.ui.components.SegmentPosition
import com.bernaferrari.changedetection.ui.components.segmentShape
import com.bernaferrari.changedetection.ui.theme.spacing
import com.bernaferrari.changedetection.util.formatTime
import com.bernaferrari.changedetection.util.formatTimestamp
import com.bernaferrari.changedetection.util.displayPageAddress
import com.bernaferrari.changedetection.util.displayPageTitle
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LogsPulse(
    versionsCount: Int,
    siteCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = pluralStringResource(Res.plurals.logs_saved_versions, versionsCount, versionsCount),
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = pluralStringResource(Res.plurals.logs_editorial_sites, siteCount, siteCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun LogItem(
    snap: Snap,
    site: Site,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onOpenInBrowser: (() -> Unit)? = null,
    position: SegmentPosition = SegmentPosition.Only,
    loadPreview: (suspend (Snap) -> HistoryPreviewContent)? = null,
) {
    val title = remember(site.title, site.url) { site.displayPageTitle() }
    val domain = remember(site.url) { runCatching { io.ktor.http.Url(site.url).host.removePrefix("www.") }.getOrDefault(site.url.displayPageAddress()) }
    val spacing = MaterialTheme.spacing
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp),
        shape = segmentShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                com.bernaferrari.changedetection.ui.main.SiteIdentity(title, Modifier.size(32.dp))
                Text(domain, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.size(spacing.touchTarget),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(Res.string.more_options),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        onOpenInBrowser?.let { openInBrowser ->
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.open_in_browser)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.OpenInBrowser,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    openInBrowser()
                                },
                            )
                            HorizontalDivider()
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.delete)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onDeleteClick()
                            },
                        )
                    }
                }
            }
            Text(title, Modifier.fillMaxWidth().padding(end = 8.dp), style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (loadPreview != null) {
                HistoryPreview(snap, loadPreview)
            }
            Row(Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.History, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    formatTime(snap.timestamp, includeSeconds = true),
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { contentDescription = formatTimestamp(snap.timestamp, includeSeconds = true) },
                )
            }
        }
    }
}

@Composable
fun LogsLoadingState(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val loadingDescription = stringResource(Res.string.loading)
    Box(
        modifier = modifier.semantics { contentDescription = loadingDescription },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .clearAndSetSemantics { }
                .padding(top = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            repeat(4) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = spacing.lg),
                    shape = RoundedCornerShape(8.dp, 24.dp, 24.dp, 24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {}
            }
        }
    }
}

@Composable
fun LogsEmptyState() {
    SharedEmptyState(
        icon = Icons.Rounded.History,
        title = stringResource(Res.string.logs_empty_title),
        description = stringResource(Res.string.logs_empty_subtitle),
    )
}

@Composable
fun DateHeader(date: String) {
    val label = historyDateLabel(date, rememberToday())
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = 24.dp,
            end = MaterialTheme.spacing.lg,
            top = MaterialTheme.spacing.md,
            bottom = MaterialTheme.spacing.xs,
        ),
    )
}

internal fun getTitle(site: Site?): String {
    if (site == null) return ""
    return if (site.title.isNullOrBlank()) {
        site.url.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)".toRegex(), "")
    } else {
        site.title
    }
}
