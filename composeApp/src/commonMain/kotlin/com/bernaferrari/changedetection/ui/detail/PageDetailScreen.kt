/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.background.MonitoringSchedule
import com.bernaferrari.changedetection.error.CheckFailure
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.components.monitoringCadenceLabel
import com.bernaferrari.changedetection.ui.components.CheckFailureNotice
import com.bernaferrari.changedetection.ui.components.ErrorBanner
import com.bernaferrari.changedetection.ui.components.ListSkeleton
import com.bernaferrari.changedetection.ui.main.MainViewModel
import com.bernaferrari.changedetection.ui.main.QuickMonitorState
import com.bernaferrari.changedetection.ui.main.hasDetectedChange
import com.bernaferrari.changedetection.util.displayPageAddress
import com.bernaferrari.changedetection.util.displayPageTitle
import com.bernaferrari.changedetection.util.formatTimestamp
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
    repository: SnapsRepository = koinInject(),
) {
    val rows by viewModel.sitesFlow.collectAsStateWithLifecycle()
    val schedule by viewModel.monitoringSchedule.collectAsStateWithLifecycle()
    val turbo by viewModel.quickMonitorState.collectAsStateWithLifecycle()
    val error by viewModel.currentError.collectAsStateWithLifecycle()
    val item = rows?.firstOrNull { it.site.id == siteId }
    val preview = rememberSavedPagePreview(item?.snap, repository)
    PageDetailContent(
        item = item, isLoading = rows == null, preview = preview, schedule = schedule, turbo = turbo,
        onBack = onBack, onCompare = onCompare, onEdit = onEdit, onTurbo = onTurbo, onSchedule = onSchedule,
        onOpenUrl = { item?.site?.url?.let(onOpenUrl) },
        onHistory = onHistory,
        onCheck = { item?.site?.let(viewModel::reloadSite) },
        onTogglePaused = { item?.site?.let { viewModel.updateSite(it.copy(isSyncEnabled = !it.isSyncEnabled)) } },
        errorContent = { error?.let { ErrorBanner(error = it, onDismiss = viewModel::dismissError, onRetry = viewModel::retryLastAction) } },
    )
}

/** Page identity and saved evidence remain available independently of the latest check outcome. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageDetailContent(
    item: SiteAndLastSnap?,
    isLoading: Boolean,
    preview: SavedPagePreview,
    schedule: MonitoringSchedule,
    turbo: QuickMonitorState,
    onBack: () -> Unit,
    onCompare: (String) -> Unit,
    onEdit: () -> Unit,
    onTurbo: () -> Unit,
    onSchedule: () -> Unit,
    onOpenUrl: () -> Unit,
    onCheck: () -> Unit,
    onTogglePaused: () -> Unit,
    onHistory: (String) -> Unit = onCompare,
    errorContent: @Composable () -> Unit = {},
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.detail_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(Res.string.a11y_navigate_back)) } },
                actions = {
                    if (item != null) IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, stringResource(Res.string.edit)) }
                },
            )
        },
    ) { insets ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
            val horizontalPadding = if (maxWidth < 360.dp) 16.dp else 20.dp
            when {
                isLoading -> ListSkeleton(Modifier.fillMaxSize())

                item == null -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    errorContent()
                    Text(stringResource(Res.string.detail_missing), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onBack) { Text(stringResource(Res.string.nav_home)) }
                }

                else -> {
                    val site = item.site
                    val turboForPage = turbo.isActive && site.id in turbo.siteIds
                    LazyColumn(
                        Modifier.widthIn(max = 640.dp).fillMaxSize().testTag("page-details"),
                        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        item(key = "identity") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    com.bernaferrari.changedetection.ui.main.SiteIdentity(site.displayPageTitle(), Modifier.size(44.dp))
                                    FlowRow(
                                        Modifier.weight(1f).padding(start = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        if (!site.isSyncEnabled) {
                                            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Icon(Icons.Outlined.Pause, null, Modifier.size(16.dp))
                                                    Text(stringResource(Res.string.site_status_paused), style = MaterialTheme.typography.labelLarge)
                                                }
                                            }
                                        }
                                        if (item.hasDetectedChange()) Text(stringResource(Res.string.detail_new), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 6.dp))
                                    }
                                }
                                BoxWithConstraints(Modifier.fillMaxWidth()) {
                                    // Choose a semantic role for the usable reading width; keep system font scaling.
                                    val readingWidth = maxWidth / LocalDensity.current.fontScale
                                    val titleStyle = when {
                                        readingWidth < 140.dp -> MaterialTheme.typography.titleMedium
                                        readingWidth < 200.dp -> MaterialTheme.typography.titleLarge
                                        else -> MaterialTheme.typography.headlineMedium
                                    }
                                    Text(
                                        site.displayPageTitle(),
                                        style = titleStyle,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                Surface(onClick = onOpenUrl, color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small) {
                                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(site.url.displayPageAddress(), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(18.dp))
                                    }
                                }
                                errorContent()
                            }
                        }
                        if (!site.isSuccessful && site.lastFailureCode != null) {
                            item(key = "check-failure") {
                                val failure = CheckFailure.fromCode(site.lastFailureCode)
                                CheckFailureNotice(site.lastFailureCode) {
                                    when (failure) {
                                        CheckFailure.REGION, CheckFailure.CONTENT, CheckFailure.CONTENT_TOO_LARGE, CheckFailure.CAPTURE_UNAVAILABLE ->
                                            TextButton(onClick = onEdit) { Text(stringResource(Res.string.edit)) }

                                        CheckFailure.LOGIN, CheckFailure.ACCESS_DENIED, CheckFailure.NOT_FOUND ->
                                            TextButton(onClick = onOpenUrl) { Text(stringResource(Res.string.open_in_browser)) }

                                        CheckFailure.RATE_LIMITED ->
                                            TextButton(onClick = onSchedule) { Text(stringResource(Res.string.settings_background_sync_title)) }

                                        else -> TextButton(onClick = onCheck, enabled = !item.isSyncing) {
                                            Text(stringResource(if (item.isSyncing) Res.string.home_action_checking else Res.string.retry))
                                        }
                                    }
                                }
                            }
                        }
                        item(key = "saved") {
                            val snap = item.snap
                            if (snap != null) {
                                SavedPageTeaser(snap, preview, canCompare = item.snapshotCount > 1) { onCompare(snap.snapId) }
                            } else {
                                Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(Icons.Outlined.Description, null)
                                        Text(stringResource(Res.string.detail_no_version), style = MaterialTheme.typography.titleMedium)
                                        Text(stringResource(Res.string.detail_first_check), style = MaterialTheme.typography.bodyMedium)
                                        Button(onClick = onCheck, enabled = !item.isSyncing) { Text(stringResource(if (item.isSyncing) Res.string.home_action_checking else Res.string.home_action_check_now)) }
                                    }
                                }
                            }
                        }
                        item(key = "turbo") {
                            Surface(onClick = onTurbo, shape = MaterialTheme.shapes.large, color = if (turboForPage) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow) {
                                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Icon(Icons.Outlined.Bolt, null, Modifier.size(28.dp))
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(stringResource(Res.string.turbo_mode), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        Text(stringResource(if (turboForPage) Res.string.detail_turbo_running else Res.string.detail_turbo_hint), style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
                                }
                            }
                        }
                        item(key = "preferences") {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                DetailRow(Icons.Outlined.CropFree, stringResource(Res.string.detail_watching), stringResource(if (site.captureMode == CaptureMode.TEXT && (site.textIncludeSelector.isNotBlank() || site.textExcludeSelector.isNotBlank())) Res.string.detail_selected_text else Res.string.detail_whole_page), onEdit, position = com.bernaferrari.changedetection.ui.components.SegmentPosition.First)
                                DetailRow(Icons.Outlined.Schedule, stringResource(Res.string.detail_regular_checks), if (schedule.enabled) monitoringCadenceLabel(schedule.intervalMinutes) else stringResource(Res.string.settings_background_sync_disabled), onSchedule, position = com.bernaferrari.changedetection.ui.components.SegmentPosition.Middle)
                                DetailRow(Icons.Outlined.Refresh, stringResource(Res.string.detail_last_check), site.lastSuccessfulCheck?.let { formatTimestamp(it, includeSeconds = true) } ?: stringResource(Res.string.detail_never_checked), onCheck, trailingIcon = null, enabled = !item.isSyncing, action = stringResource(if (item.isSyncing) Res.string.home_action_checking else Res.string.home_action_check_now), position = com.bernaferrari.changedetection.ui.components.SegmentPosition.Middle)
                                DetailRow(if (site.isSyncEnabled) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle, stringResource(if (site.isSyncEnabled) Res.string.detail_pause else Res.string.detail_resume), stringResource(Res.string.detail_keep_history), onTogglePaused, trailingIcon = null, position = com.bernaferrari.changedetection.ui.components.SegmentPosition.Last)
                            }
                        }
                        item(key = "history") {
                            DetailRow(
                                Icons.Outlined.History,
                                stringResource(Res.string.comparison_stats_snapshots_label),
                                pluralStringResource(Res.plurals.logs_saved_versions, item.snapshotCount, item.snapshotCount),
                                onClick = { item.snap?.snapId?.let(onHistory) },
                                enabled = item.snap != null,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit, enabled: Boolean = true, action: String? = null, trailingIcon: ImageVector? = Icons.AutoMirrored.Rounded.KeyboardArrowRight, position: com.bernaferrari.changedetection.ui.components.SegmentPosition = com.bernaferrari.changedetection.ui.components.SegmentPosition.Only) {
    Surface(onClick = onClick, enabled = enabled, shape = com.bernaferrari.changedetection.ui.components.segmentShape(position), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(vertical = 16.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainer) {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (action != null) Text(action, style = MaterialTheme.typography.labelLarge, color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailingIcon?.let { Icon(it, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
