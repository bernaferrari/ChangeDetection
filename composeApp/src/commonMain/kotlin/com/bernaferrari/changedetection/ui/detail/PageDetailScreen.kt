/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.background.MonitoringSchedule
import com.bernaferrari.changedetection.background.TurboState
import com.bernaferrari.changedetection.background.monitoringCapabilities
import com.bernaferrari.changedetection.error.CheckFailure
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.components.ErrorBanner
import com.bernaferrari.changedetection.ui.components.checkFailureTitle
import com.bernaferrari.changedetection.ui.components.monitoringCadenceLabel
import com.bernaferrari.changedetection.ui.kit.Callout
import com.bernaferrari.changedetection.ui.kit.ChangePreviewBlock
import com.bernaferrari.changedetection.ui.kit.GroupRow
import com.bernaferrari.changedetection.ui.kit.ListGroup
import com.bernaferrari.changedetection.ui.kit.NewBadge
import com.bernaferrari.changedetection.ui.kit.PageAvatar
import com.bernaferrari.changedetection.ui.kit.SectionHeader
import com.bernaferrari.changedetection.ui.kit.cadence
import com.bernaferrari.changedetection.ui.kit.friendlyDateTime
import com.bernaferrari.changedetection.ui.kit.localDate
import com.bernaferrari.changedetection.ui.kit.name
import com.bernaferrari.changedetection.ui.kit.previewLabel
import com.bernaferrari.changedetection.ui.kit.rememberLatestPreview
import com.bernaferrari.changedetection.ui.kit.rememberPairPreview
import com.bernaferrari.changedetection.ui.main.MainViewModel
import com.bernaferrari.changedetection.ui.theme.marks
import com.bernaferrari.changedetection.util.displayPageAddress
import com.bernaferrari.changedetection.util.getCurrentTimeMillis
import com.bernaferrari.changedetection.util.getRelativeTimeLabel
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
internal fun PageDetailScreen(
    siteId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onCompare: (String) -> Unit,
    onEdit: () -> Unit,
    onTurbo: () -> Unit,
    onSchedule: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onHistory: (String) -> Unit,
    onEnableNotifications: () -> Unit = {},
    repository: SnapsRepository = koinInject(),
) {
    val rows by viewModel.sitesFlow.collectAsStateWithLifecycle()
    val schedule by viewModel.monitoringSchedule.collectAsStateWithLifecycle()
    val turbo by viewModel.quickMonitorState.collectAsStateWithLifecycle()
    val error by viewModel.currentError.collectAsStateWithLifecycle()
    val versions by remember(siteId) { repository.getSnaps(siteId) }.collectAsStateWithLifecycle(null)
    val item = rows?.firstOrNull { it.site.id == siteId }
    var confirmDelete by remember { mutableStateOf(false) }
    PageDetailContent(
        item = item,
        isLoading = rows == null,
        versions = versions.orEmpty(),
        schedule = schedule,
        turbo = turbo,
        onBack = onBack,
        onCompare = onCompare,
        onEdit = onEdit,
        onTurbo = onTurbo,
        onSchedule = onSchedule,
        onOpenUrl = { item?.site?.url?.let(onOpenUrl) },
        onHistory = onHistory,
        onCheck = { item?.site?.let(viewModel::reloadSite) },
        onMarkRead = { item?.let(viewModel::markRead) },
        onSetPaused = { paused -> item?.site?.let { viewModel.updateSite(it.copy(isSyncEnabled = !paused)) } },
        onSetNotify = { on ->
            if (on) onEnableNotifications()
            item?.site?.let { viewModel.updateSite(it.copy(isNotificationEnabled = on)) }
        },
        onDelete = { confirmDelete = true },
        errorContent = { error?.let { ErrorBanner(error = it, onDismiss = viewModel::dismissError, onRetry = viewModel::retryLastAction) } },
    )
    if (confirmDelete && item != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Outlined.DeleteOutline, null) },
            title = { Text(stringResource(Res.string.detail_delete_title, item.site.name())) },
            text = { Text(pluralStringResource(Res.plurals.detail_delete_body, item.snapshotCount, item.snapshotCount)) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        val ticket = viewModel.onDeleteSite(item.site)
                        onBack()
                        viewModel.commitDeletion(ticket)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                ) { Text(stringResource(Res.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(Res.string.dialog_cancel)) } },
        )
    }
}

/** Latest change first, then the page's rhythm, its versions, and finally how it is watched. */
@Composable
internal fun PageDetailContent(
    item: SiteAndLastSnap?,
    isLoading: Boolean,
    versions: List<Snap>,
    schedule: MonitoringSchedule,
    turbo: TurboState,
    onBack: () -> Unit,
    onCompare: (String) -> Unit,
    onEdit: () -> Unit,
    onTurbo: () -> Unit,
    onSchedule: () -> Unit,
    onOpenUrl: () -> Unit,
    onCheck: () -> Unit,
    onMarkRead: () -> Unit,
    onSetPaused: (Boolean) -> Unit,
    onSetNotify: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onHistory: (String) -> Unit = onCompare,
    errorContent: @Composable () -> Unit = {},
) {
    var menu by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(Res.string.a11y_navigate_back)) } },
                actions = {
                    if (item != null) {
                        IconButton(onClick = onOpenUrl) { Icon(Icons.AutoMirrored.Outlined.OpenInNew, stringResource(Res.string.open_in_browser)) }
                        IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, stringResource(Res.string.edit)) }
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(Res.string.more_options)) }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.detail_delete), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        menu = false
                                        onDelete()
                                    },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            if (item != null) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = onCheck, enabled = !item.isSyncing, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                            if (item.isSyncing) LoadingIndicator(Modifier.size(24.dp)) else Icon(Icons.Rounded.Refresh, null)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(if (item.isSyncing) Res.string.home_row_checking else Res.string.home_check_now))
                        }
                        if (monitoringCapabilities.turbo && item.site.isSyncEnabled) {
                            val running = turbo.isActive && item.site.id in turbo.siteIds
                            Button(
                                onClick = onTurbo,
                                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                            ) {
                                Icon(Icons.Rounded.Bolt, null)
                                Spacer(Modifier.size(8.dp))
                                Text(stringResource(if (running) Res.string.detail_turbo_on else Res.string.turbo_mode))
                            }
                        }
                    }
                }
            }
        },
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
            when {
                isLoading -> LoadingIndicator(Modifier.padding(48.dp))

                item == null -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    errorContent()
                    Text(stringResource(Res.string.detail_missing), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onBack) { Text(stringResource(Res.string.nav_home)) }
                }

                else -> DetailList(item, versions, schedule, onCompare, onEdit, onSchedule, onOpenUrl, onCheck, onMarkRead, onSetPaused, onSetNotify, onHistory, errorContent)
            }
        }
    }
}

@Composable
private fun DetailList(
    item: SiteAndLastSnap,
    versions: List<Snap>,
    schedule: MonitoringSchedule,
    onCompare: (String) -> Unit,
    onEdit: () -> Unit,
    onSchedule: () -> Unit,
    onOpenUrl: () -> Unit,
    onCheck: () -> Unit,
    onMarkRead: () -> Unit,
    onSetPaused: (Boolean) -> Unit,
    onSetNotify: (Boolean) -> Unit,
    onHistory: (String) -> Unit,
    errorContent: @Composable () -> Unit,
) {
    val site = item.site
    val paused = !site.isSyncEnabled
    val failing = !paused && !site.isSuccessful
    LazyColumn(
        Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("page-details"),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "identity") {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PageAvatar(site.name(), site.id, size = 56.dp, dimmed = paused)
                    Column(Modifier.weight(1f)) {
                        Text(site.name(), style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            site.url.displayPageAddress(),
                            Modifier.clip(MaterialTheme.shapes.small).clickable(role = Role.Button, onClick = onOpenUrl),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Row(Modifier.padding(top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val dot = when {
                        paused -> MaterialTheme.colorScheme.outline
                        failing -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.marks.healthy
                    }
                    Box(Modifier.size(8.dp).background(dot, CircleShape))
                    Text(
                        when {
                            paused -> stringResource(Res.string.detail_status_paused)
                            site.timestamp <= 0 -> stringResource(Res.string.home_row_waiting)
                            schedule.enabled -> stringResource(Res.string.detail_status_checked, getRelativeTimeLabel(site.timestamp).lowercase(), cadence(schedule.intervalMinutes))
                            else -> stringResource(Res.string.detail_status_checked_manual, getRelativeTimeLabel(site.timestamp).lowercase())
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                errorContent()
            }
        }
        if (failing) {
            item(key = "failure") {
                FailureCallout(site.lastFailureCode, site.lastSuccessfulCheck, item.isSyncing, onCheck, onOpenUrl, onEdit, onSchedule)
            }
        }
        item(key = "latest") {
            LatestChange(item, onCompare, onMarkRead, onCheck)
        }
        if (versions.size > 1) {
            item(key = "calendar-h") {
                val month = 30L * 24 * 60 * 60 * 1000
                val changes = versions.dropLast(1).count { getCurrentTimeMillis() - it.timestamp < month }
                SectionHeader(stringResource(Res.string.detail_last_30), trailingText = pluralStringResource(Res.plurals.detail_changes_count, changes, changes))
            }
            item(key = "calendar") { ChangeCalendar(versions) }
            item(key = "versions-h") {
                SectionHeader(
                    stringResource(Res.string.detail_versions),
                    count = versions.size,
                    actionLabel = stringResource(Res.string.detail_see_all),
                    onAction = { versions.firstOrNull()?.snapId?.let(onHistory) },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item(key = "versions") {
                ListGroup {
                    versions.take(4).forEachIndexed { index, snap ->
                        VersionRow(snap, versions.getOrNull(index + 1), latest = index == 0, showDivider = index > 0, onClick = { onCompare(snap.snapId) })
                    }
                }
            }
        }
        item(key = "watch-h") { SectionHeader(stringResource(Res.string.detail_how_watched), actionLabel = stringResource(Res.string.edit), onAction = onEdit, modifier = Modifier.padding(top = 8.dp)) }
        item(key = "watch") {
            ListGroup {
                GroupRow(
                    title = stringResource(Res.string.detail_what),
                    supporting = stringResource(
                        when {
                            site.captureMode == CaptureMode.SCREENSHOT -> Res.string.detail_what_page_screenshot
                            site.captureMode == CaptureMode.TEXT && (site.textIncludeSelector.isNotBlank() || site.textExcludeSelector.isNotBlank()) -> Res.string.detail_what_part
                            else -> Res.string.detail_what_page
                        },
                    ),
                    icon = Icons.Outlined.CropFree,
                    onClick = onEdit,
                )
                GroupRow(
                    title = stringResource(Res.string.detail_compare),
                    supporting = stringResource(
                        when (site.captureMode) {
                            CaptureMode.SCREENSHOT -> Res.string.mode_screenshot
                            CaptureMode.SOURCE -> Res.string.mode_source
                            CaptureMode.HTML -> Res.string.mode_html
                            CaptureMode.DEFAULT -> Res.string.mode_default
                            CaptureMode.TEXT -> Res.string.mode_text
                        },
                    ),
                    icon = if (site.captureMode == CaptureMode.SCREENSHOT) Icons.Outlined.Image else Icons.Outlined.TextSnippet,
                    showDivider = true,
                    onClick = onEdit,
                )
                GroupRow(
                    title = stringResource(Res.string.detail_how_often),
                    supporting = if (schedule.enabled) cadence(schedule.intervalMinutes).replaceFirstChar { it.uppercase() } else stringResource(Res.string.settings_background_sync_disabled),
                    icon = Icons.Outlined.Schedule,
                    showDivider = true,
                    onClick = onSchedule,
                ) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline) }
                GroupRow(
                    title = stringResource(Res.string.detail_notify),
                    icon = Icons.Outlined.Notifications,
                    showDivider = true,
                    onClick = { onSetNotify(!site.isNotificationEnabled) },
                    role = Role.Switch,
                ) { Switch(site.isNotificationEnabled, onCheckedChange = null) }
                GroupRow(
                    title = stringResource(Res.string.detail_paused),
                    supporting = stringResource(Res.string.detail_paused_body),
                    icon = Icons.Outlined.PauseCircle,
                    showDivider = true,
                    onClick = { onSetPaused(!paused) },
                    role = Role.Switch,
                ) { Switch(paused, onCheckedChange = null) }
            }
        }
    }
}

@Composable
private fun FailureCallout(code: String?, lastSuccess: Long?, checking: Boolean, onCheck: () -> Unit, onOpenUrl: () -> Unit, onEdit: () -> Unit, onSchedule: () -> Unit) {
    val failure = CheckFailure.fromCode(code)
    Callout(
        title = checkFailureTitle(code),
        body = stringResource(failureAdvice(failure)) + (lastSuccess?.let { "\n" + stringResource(Res.string.detail_last_success, friendlyDateTime(it)) } ?: ""),
        icon = Icons.Outlined.ErrorOutline,
        iconTint = MaterialTheme.colorScheme.error,
        container = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.padding(bottom = 12.dp),
        actions = {
            when (failure) {
                CheckFailure.REGION, CheckFailure.CONTENT, CheckFailure.CONTENT_TOO_LARGE, CheckFailure.CAPTURE_UNAVAILABLE ->
                    Button(onClick = onEdit) { Text(stringResource(Res.string.detail_fix_settings)) }

                CheckFailure.RATE_LIMITED -> Button(onClick = onSchedule) { Text(stringResource(Res.string.detail_check_less)) }

                else -> Button(onClick = onCheck, enabled = !checking) { Text(stringResource(if (checking) Res.string.home_row_checking else Res.string.retry)) }
            }
            OutlinedButton(onClick = onOpenUrl) { Text(stringResource(Res.string.open_in_browser)) }
        },
    )
}

private fun failureAdvice(failure: CheckFailure) = when (failure) {
    CheckFailure.SERVER, CheckFailure.TIMEOUT -> Res.string.advice_temporary
    CheckFailure.CONNECTION -> Res.string.advice_connection
    CheckFailure.LOGIN, CheckFailure.ACCESS_DENIED -> Res.string.advice_login
    CheckFailure.NOT_FOUND -> Res.string.advice_not_found
    CheckFailure.RATE_LIMITED -> Res.string.advice_rate
    CheckFailure.REGION -> Res.string.advice_region
    CheckFailure.CONTENT, CheckFailure.CONTENT_TOO_LARGE -> Res.string.advice_content
    CheckFailure.CAPTURE_UNAVAILABLE -> Res.string.advice_capture
    else -> Res.string.advice_generic
}

@Composable
private fun LatestChange(item: SiteAndLastSnap, onCompare: (String) -> Unit, onMarkRead: () -> Unit, onCheck: () -> Unit) {
    val snap = item.snap
    val unread = !item.site.isRead && item.snapshotCount > 1
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                snap == null -> {
                    Text(stringResource(Res.string.detail_no_version), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(Res.string.detail_first_check), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onCheck, enabled = !item.isSyncing) { Text(stringResource(Res.string.home_check_now)) }
                }

                item.snapshotCount <= 1 -> {
                    Text(stringResource(Res.string.detail_first_saved_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(Res.string.detail_first_saved_body, friendlyDateTime(snap.timestamp)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { onCompare(snap.snapId) }) { Text(stringResource(Res.string.detail_view_saved)) }
                }

                else -> {
                    val preview = rememberLatestPreview(item.site.id, snap.snapId)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(Res.string.detail_latest_change), style = MaterialTheme.typography.titleMedium)
                            Text(friendlyDateTime(snap.timestamp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (unread) NewBadge(text = stringResource(Res.string.badge_new))
                    }
                    ChangePreviewBlock(preview, cropHeight = 132.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (unread) {
                            FilledTonalButton(onClick = onMarkRead, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                                Icon(Icons.Rounded.Check, null, Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(stringResource(Res.string.mark_read_short))
                            }
                        }
                        Button(onClick = { onCompare(snap.snapId) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(stringResource(Res.string.detail_compare_action)) }
                    }
                }
            }
        }
    }
}

/** Thirty squares, one per day; filled on days a new version was saved. */
@Composable
private fun ChangeCalendar(versions: List<Snap>) {
    val today = getCurrentTimeMillis().localDate()
    val changedDays = remember(versions) { versions.dropLast(1).mapTo(mutableSetOf()) { it.timestamp.localDate() } }
    val latestUnread = versions.firstOrNull()?.timestamp?.localDate()
    Surface(Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(Modifier.padding(16.dp)) {
            for (week in 0 until 2) {
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 0 until 15) {
                        val day = today.minus(DatePeriod(days = 29 - (week * 15 + i)))
                        val changed = day in changedDays
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(5.dp)).background(
                                when {
                                    changed && day == latestUnread -> MaterialTheme.marks.highlight
                                    changed -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.surfaceContainer
                                },
                            ),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(stringResource(Res.string.detail_30_days_ago), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(Res.string.today), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun VersionRow(snap: Snap, previous: Snap?, latest: Boolean, showDivider: Boolean, onClick: () -> Unit) {
    val preview = rememberPairPreview(previous?.snapId, snap.snapId)
    GroupRow(
        title = friendlyDateTime(snap.timestamp),
        supporting = previewLabel(preview),
        showDivider = showDivider,
        onClick = onClick,
    ) {
        if (latest) {
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.marks.highlight).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text(stringResource(Res.string.detail_latest), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.marks.onHighlight)
            }
        } else {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}
