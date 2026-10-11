/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.detail

import com.bernaferrari.changedetection.ui.kit.avatarIcon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.bernaferrari.changedetection.ui.components.rememberPageIconMotion
import com.bernaferrari.changedetection.ui.kit.PageAvatar
import com.bernaferrari.changedetection.ui.kit.tileColor
import androidx.compose.ui.text.style.TextAlign
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
import com.bernaferrari.changedetection.ui.comparison.isImageSnapshot
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import org.jetbrains.compose.resources.stringResource
import com.bernaferrari.changedetection.addedit.PageIconShapePicker
import com.bernaferrari.changedetection.repo.PageIconShape
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
    var confirmDelete by remember(siteId) { mutableStateOf(false) }
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
        onIconShapeChange = { shape -> item?.site?.let { viewModel.updateIconShape(it.id, shape) } },
        onIconColorChange = { colors -> item?.site?.let { viewModel.updateIconColor(it.id, colors) } },
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
    onIconShapeChange: (PageIconShape) -> Unit = {},
    onIconColorChange: (com.bernaferrari.changedetection.repo.ColorGroup) -> Unit = {},
    notificationsAvailable: Boolean = monitoringCapabilities.notifications,
    errorContent: @Composable () -> Unit = {},
) {
    val listState = rememberLazyListState()
    var nameBottom by remember(item?.site?.id) { mutableIntStateOf(Int.MAX_VALUE) }
    val showBarTitle by remember(listState, item?.site?.id) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset >= nameBottom }
    }
    var choosingShape by remember(item?.site?.id) { mutableStateOf(false) }
    if (choosingShape && item != null) {
        PageIconShapePicker(item.site.name(), item.site.id, item.site.avatarIcon(), item.site.iconShape, onSelect = onIconShapeChange, colors = item.site.colors, onColorSelect = onIconColorChange, onDismiss = { choosingShape = false })
    }
    Scaffold(
        topBar = {
            com.bernaferrari.changedetection.ui.navigation.AppTopBar(
                title = item?.site?.name()?.takeIf { showBarTitle },
                onBack = onBack,
                actions = if (item != null) listOf(com.bernaferrari.changedetection.ui.navigation.BarAction(stringResource(Res.string.edit), Icons.Outlined.Edit, "pencil", onClick = onEdit)) else emptyList(),
                menu = if (item != null) listOf(com.bernaferrari.changedetection.ui.navigation.BarAction(stringResource(Res.string.detail_delete), Icons.Outlined.DeleteOutline, "trash", destructive = true, onClick = onDelete)) else emptyList(),
            )
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

                else -> DetailList(item, versions, schedule, turbo, onCompare, onEdit, onTurbo, onSchedule, onOpenUrl, onCheck, onMarkRead, onSetPaused, onSetNotify, onHistory, notificationsAvailable, errorContent, onChooseShape = { choosingShape = true }, listState = listState, onNameMeasured = { nameBottom = it })
            }
        }
    }
}

/** Keep action labels readable on compact windows and with large system text. */
@Composable
private fun DetailActions(
    modifier: Modifier = Modifier,
    first: @Composable (Modifier) -> Unit,
    second: (@Composable (Modifier) -> Unit)? = null,
) {
    BoxWithConstraints(modifier) {
        if (maxWidth < 300.dp || LocalDensity.current.fontScale > 1.3f) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                first(Modifier.fillMaxWidth())
                second?.invoke(Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                first(Modifier.weight(1f))
                second?.invoke(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DetailList(
    item: SiteAndLastSnap,
    versions: List<Snap>,
    schedule: MonitoringSchedule,
    turbo: TurboState,
    onCompare: (String) -> Unit,
    onEdit: () -> Unit,
    onTurbo: () -> Unit,
    onSchedule: () -> Unit,
    onOpenUrl: () -> Unit,
    onCheck: () -> Unit,
    onMarkRead: () -> Unit,
    onSetPaused: (Boolean) -> Unit,
    onSetNotify: (Boolean) -> Unit,
    onHistory: (String) -> Unit,
    notificationsAvailable: Boolean,
    errorContent: @Composable () -> Unit,
    onChooseShape: () -> Unit,
    listState: LazyListState,
    onNameMeasured: (Int) -> Unit,
) {
    val site = item.site
    val paused = !site.isSyncEnabled
    val failing = !paused && !site.isSuccessful
    LazyColumn(
        Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("page-details"),
        contentPadding = PaddingValues(bottom = 32.dp),
        state = listState,
    ) {
        item(key = "identity") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val iconInteractions = remember { MutableInteractionSource() }
                val iconMotion = rememberPageIconMotion(iconInteractions)
                Box(Modifier.clickable(interactionSource = iconInteractions, indication = null, role = androidx.compose.ui.semantics.Role.Button, onClickLabel = stringResource(Res.string.editor_icon_shape_title), onClick = onChooseShape).padding(8.dp)) {
                    PageAvatar(site.name(), site.id, modifier = iconMotion.modifier, size = 88.dp, dimmed = paused, icon = site.avatarIcon(), iconShape = site.iconShape, colors = site.colors, selectionProgress = iconMotion.outlineProgress, selectionColor = if (iconMotion.focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    site.name(),
                    modifier = Modifier.testTag("page-name").onGloballyPositioned { onNameMeasured((it.positionInParent().y + it.size.height).roundToInt()) },
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    site.url.displayPageAddress(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Row(
                    Modifier.padding(top = 12.dp).testTag("page-check-status"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val dot = when {
                        paused -> MaterialTheme.colorScheme.outline
                        failing -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.marks.healthy
                    }
                    if (item.isSyncing) LoadingIndicator(Modifier.size(16.dp)) else Box(Modifier.size(8.dp).background(dot, CircleShape))
                    Text(
                        when {
                            paused -> stringResource(Res.string.detail_status_paused)
                            item.isSyncing -> stringResource(Res.string.home_row_checking)
                            failing -> stringResource(Res.string.detail_status_failed, getRelativeTimeLabel(site.timestamp).lowercase())
                            site.timestamp <= 0 -> stringResource(Res.string.home_row_waiting)
                            schedule.enabled -> stringResource(Res.string.detail_status_checked, getRelativeTimeLabel(site.timestamp).lowercase(), cadence(schedule.intervalMinutes))
                            else -> stringResource(Res.string.detail_status_checked_manual, getRelativeTimeLabel(site.timestamp).lowercase())
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (failing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroAction(Icons.Rounded.Refresh, stringResource(if (item.isSyncing) Res.string.home_row_checking else Res.string.home_check_now), onCheck, Modifier.weight(1f), enabled = !item.isSyncing, filled = true, busy = item.isSyncing)
                    if (paused) {
                        HeroAction(Icons.Rounded.PlayArrow, stringResource(Res.string.home_resume_checks), { onSetPaused(false) }, Modifier.weight(1f), filled = true)
                    } else if (monitoringCapabilities.turbo) {
                        val running = turbo.isActive && site.id in turbo.siteIds
                        HeroAction(Icons.Rounded.Bolt, stringResource(if (running) Res.string.detail_turbo_on else Res.string.turbo_mode), onTurbo, Modifier.weight(1f), filled = running)
                    }
                    HeroAction(Icons.AutoMirrored.Outlined.OpenInNew, stringResource(Res.string.detail_open_short), onOpenUrl, Modifier.weight(1f))
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
                val today = getCurrentTimeMillis().localDate()
                val start = today.minus(DatePeriod(days = 29))
                val changes = versions.dropLast(1).count { it.timestamp.localDate() in start..today }
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
                    supporting = run {
                        // Picked parts apply to every mode, so the summary always says what is watched and what is ignored.
                        val ignored = site.textExcludeSelector.split(", ").count { it.isNotBlank() }
                        val base = stringResource(
                            when {
                                site.textIncludeSelector.isNotBlank() -> Res.string.detail_what_part
                                site.captureMode == CaptureMode.SCREENSHOT -> Res.string.detail_what_page_screenshot
                                else -> Res.string.detail_what_page
                            },
                        )
                        if (ignored > 0) base + " · " + pluralStringResource(Res.plurals.parts_ignored, ignored, ignored) else base
                    },
                    icon = if (site.captureMode == CaptureMode.SCREENSHOT) Icons.Outlined.Image else Icons.Outlined.TextSnippet,
                    onClick = onEdit,
                ) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline) }
                GroupRow(
                    title = stringResource(Res.string.detail_how_often),
                    supporting = if (schedule.enabled) cadence(schedule.intervalMinutes).replaceFirstChar { it.uppercase() } else stringResource(Res.string.settings_background_sync_disabled),
                    icon = Icons.Outlined.Schedule,
                    showDivider = true,
                    onClick = onSchedule,
                ) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline) }
                if (notificationsAvailable) {
                    GroupRow(
                        title = stringResource(Res.string.detail_notify),
                        icon = Icons.Outlined.Notifications,
                        showDivider = true,
                        onClick = { onSetNotify(!site.isNotificationEnabled) },
                        role = Role.Switch,
                        checked = site.isNotificationEnabled,
                    ) { Switch(site.isNotificationEnabled, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics { }) }
                }
                GroupRow(
                    title = stringResource(Res.string.detail_paused),
                    supporting = stringResource(Res.string.detail_paused_body),
                    icon = Icons.Outlined.PauseCircle,
                    showDivider = true,
                    onClick = { onSetPaused(!paused) },
                    role = Role.Switch,
                    checked = paused,
                ) { Switch(paused, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics { }) }
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
        container = tileColor,
        modifier = Modifier.padding(bottom = 12.dp),
        actions = {
            when (failure) {
                CheckFailure.REGION, CheckFailure.CONTENT, CheckFailure.CONTENT_TOO_LARGE, CheckFailure.CAPTURE_UNAVAILABLE ->
                    Button(onClick = onEdit) { Text(stringResource(Res.string.detail_fix_settings)) }

                CheckFailure.RATE_LIMITED -> Button(onClick = onSchedule) { Text(stringResource(Res.string.detail_check_less)) }

                CheckFailure.LOGIN, CheckFailure.ACCESS_DENIED -> Button(onClick = onEdit) { Text(stringResource(Res.string.detail_sign_in)) }

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
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = tileColor,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when {
                snap == null -> {
                    Text(stringResource(Res.string.detail_no_version), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(Res.string.detail_first_check), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onCheck, enabled = !item.isSyncing) { Text(stringResource(Res.string.home_check_now)) }
                }

                item.snapshotCount <= 1 -> {
                    if (snap.isImageSnapshot()) {
                        com.bernaferrari.changedetection.ui.kit.CaptureTop(
                            snap.snapId,
                            Modifier.fillMaxWidth().height(180.dp).clip(MaterialTheme.shapes.large)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                                .clickable(onClickLabel = stringResource(Res.string.detail_view_saved)) { onCompare(snap.snapId) },
                        )
                    }
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
                    val compareButton: @Composable (Modifier) -> Unit = { buttonModifier ->
                        Button(onClick = { onCompare(snap.snapId) }, modifier = buttonModifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.detail_compare_action)) }
                    }
                    DetailActions(
                        first = if (unread) {
                            { buttonModifier ->
                                FilledTonalButton(onClick = onMarkRead, modifier = buttonModifier.heightIn(min = 48.dp)) {
                                    Icon(Icons.Rounded.Check, null, Modifier.size(18.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text(stringResource(Res.string.mark_read_short))
                                }
                            }
                        } else {
                            compareButton
                        },
                        second = if (unread) compareButton else null,
                    )
                }
            }
        }
    }
}

/** Native expressive buttons keep primary actions distinct from idle secondary actions. */
@Composable
private fun HeroAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, filled: Boolean = false, busy: Boolean = false) {
    val content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
            if (busy) LoadingIndicator(Modifier.size(24.dp), color = LocalContentColor.current) else Icon(icon, null, Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
    if (filled) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = 76.dp),
            shapes = ButtonDefaults.shapes(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
            content = content,
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = 76.dp),
            shapes = ButtonDefaults.shapes(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
            content = content,
        )
    }
}

/** Thirty squares, one per day; filled on days a new version was saved. */
@Composable
private fun ChangeCalendar(versions: List<Snap>) {
    val today = getCurrentTimeMillis().localDate()
    val start = today.minus(DatePeriod(days = 29))
    val changes = remember(versions, today) { versions.dropLast(1).filter { it.timestamp.localDate() in start..today } }
    val changedDays = remember(changes) { changes.mapTo(mutableSetOf()) { it.timestamp.localDate() } }
    val latestDay = versions.firstOrNull()?.timestamp?.localDate()
    val summary = stringResource(Res.string.detail_calendar_summary, changes.size, changedDays.size)
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clearAndSetSemantics { contentDescription = summary }, shape = MaterialTheme.shapes.extraLarge, color = tileColor) {
        Column(Modifier.padding(20.dp)) {
            for (week in 0 until 2) {
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 0 until 15) {
                        val day = today.minus(DatePeriod(days = 29 - (week * 15 + i)))
                        val changed = day in changedDays
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(if (changed) CircleShape else RoundedCornerShape(6.dp)).background(
                                when {
                                    changed && day == latestDay -> MaterialTheme.marks.highlight
                                    changed -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
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
            Box(Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 10.dp, vertical = 3.dp)) {
                Text(stringResource(Res.string.detail_latest), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        } else {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}
