package com.bernaferrari.changedetection.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.localization.stringResource
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.kit.GroupLabel
import com.bernaferrari.changedetection.ui.kit.GroupRow
import com.bernaferrari.changedetection.ui.kit.ListGroup
import com.bernaferrari.changedetection.ui.kit.optional
import com.bernaferrari.changedetection.util.ApplicationScope
import com.bernaferrari.changedetection.util.localizedInteger
import java.time.LocalDate
import kotlinx.coroutines.CancellationException

@Composable
actual fun BackupRow() {
    val context = LocalContext.current
    val files = remember { BackupFiles.get(context, optional<BackupService>(), optional<MonitoringManager>(), optional<ApplicationScope>()) } ?: return
    var show by rememberSaveable { mutableStateOf(false) }
    GroupRow(
        stringResource(Res.string.backup_title),
        modifier = Modifier.testTag("backup"),
        supporting = stringResource(Res.string.backup_supporting),
        icon = Icons.Outlined.SaveAlt,
        showDivider = true,
        onClick = { show = true },
    )
    if (show) BackupDialog(files, onClose = { show = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupDialog(files: BackupFiles, onClose: () -> Unit) {
    val progress by files.state.collectAsState()
    var restoreSettings by rememberSaveable { mutableStateOf(true) }
    var includeVersions by rememberSaveable { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var preview by remember { mutableStateOf<RestorePreview?>(null) }
    var previewError by remember { mutableStateOf(false) }
    LaunchedEffect(restoreUri) {
        preview = null
        previewError = false
        restoreUri?.let { uri ->
            try {
                preview = files.preview(uri)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                previewError = true
            }
        }
    }
    val name = "change-detection-backup-${LocalDate.now()}"
    val savePages = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(files::savePages) }
    val saveEverything = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { it?.let(files::saveEverything) }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> restoreUri = uri }

    val result = when (val state = progress) {
        is BackupProgress.Saved -> stringResource(Res.string.backup_saved_confirmation)
        is BackupProgress.Restored -> stringResource(Res.string.backup_restore_complete)
        BackupProgress.SaveFailed -> stringResource(Res.string.backup_failed)
        BackupProgress.NotABackup -> stringResource(Res.string.backup_invalid)
        BackupProgress.TooNew -> stringResource(Res.string.backup_too_new)
        BackupProgress.Idle, BackupProgress.Working -> null
    }
    val working = progress == BackupProgress.Working
    val close = {
        files.acknowledge()
        onClose()
    }
    ModalBottomSheet(
        onDismissRequest = close,
        sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text(
                stringResource(Res.string.backup_title),
                Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
            )
            when {
                working -> Row(Modifier.padding(horizontal = 24.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text(stringResource(Res.string.backup_working), Modifier.padding(start = 16.dp))
                }

                result != null -> Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp).testTag("backup-result"),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                        if (progress is BackupProgress.Saved || progress is BackupProgress.Restored) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Column(
                            Modifier.weight(1f).padding(start = if (progress is BackupProgress.Saved || progress is BackupProgress.Restored) 16.dp else 0.dp),
                        ) {
                            Text(result, style = MaterialTheme.typography.titleMedium)
                            val saved = progress as? BackupProgress.Saved
                            if (saved != null) {
                                Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BackupCount(stringResource(Res.string.home_pages), saved.summary.pages)
                                    BackupCount(stringResource(Res.string.detail_versions), saved.summary.versions)
                                }
                            }
                            val restored = progress as? BackupProgress.Restored
                            if (restored != null) {
                                Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BackupCount(stringResource(Res.string.backup_pages_added), restored.summary.pagesAdded)
                                    BackupCount(stringResource(Res.string.backup_already_here), restored.summary.pagesAlreadyHere)
                                    BackupCount(stringResource(Res.string.detail_versions), restored.summary.versionsAdded)
                                    if (restored.summary.pagesRejected > 0) {
                                        BackupCount(stringResource(Res.string.backup_skipped_pages), restored.summary.pagesRejected)
                                    }
                                    if (restored.summary.duplicateVersions > 0) {
                                        BackupCount(stringResource(Res.string.backup_duplicate_versions), restored.summary.duplicateVersions)
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    GroupLabel(stringResource(Res.string.backup_create))
                    ListGroup {
                        GroupRow(
                            stringResource(Res.string.backup_include_versions),
                            supporting = stringResource(Res.string.backup_save_everything_body),
                            role = Role.Checkbox,
                            checked = includeVersions,
                            onClick = { includeVersions = !includeVersions },
                        ) {
                            Checkbox(checked = includeVersions, onCheckedChange = null)
                        }
                        GroupRow(
                            stringResource(Res.string.backup_create),
                            modifier = Modifier.testTag("backup-save-pages"),
                            supporting = stringResource(Res.string.backup_save_pages),
                            icon = Icons.Outlined.SaveAlt,
                            onClick = {
                                if (includeVersions) saveEverything.launch("$name.zip") else savePages.launch("$name.json")
                            },
                        )
                    }
                    GroupLabel(stringResource(Res.string.backup_restore))
                    ListGroup {
                        GroupRow(
                            stringResource(Res.string.backup_restore),
                            modifier = Modifier.testTag("backup-restore"),
                            icon = Icons.Outlined.FolderOpen,
                            onClick = { restore.launch(arrayOf("application/json", "application/zip", "application/octet-stream")) },
                        )
                    }
                    Text(
                        stringResource(Res.string.backup_privacy_note),
                        Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = close, modifier = Modifier.align(Alignment.End).padding(end = 16.dp)) {
                Text(stringResource(Res.string.done))
            }
        }
    }
    restoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { restoreUri = null },
            title = { Text(stringResource(Res.string.backup_restore)) },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val current = preview
                    when {
                        previewError -> Text(stringResource(Res.string.backup_invalid), color = MaterialTheme.colorScheme.error)

                        current == null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            Text(stringResource(Res.string.backup_working))
                        }

                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                BackupCount(stringResource(Res.string.backup_pages_to_add), current.pagesToAdd.size)
                                current.pagesToAdd.forEach { site ->
                                    Text(site.title?.takeIf { it.isNotBlank() } ?: site.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                BackupCount(stringResource(Res.string.backup_pages_here), current.pagesAlreadyHere.size)
                                current.pagesAlreadyHere.forEach { site ->
                                    Text(site.title?.takeIf { it.isNotBlank() } ?: site.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                BackupCount(stringResource(Res.string.backup_versions_to_add), current.versionsToAdd)
                            }
                            GroupRow(
                                stringResource(Res.string.backup_restore_settings),
                                modifier = Modifier.padding(top = 4.dp),
                                role = Role.Checkbox,
                                checked = restoreSettings,
                                onClick = { restoreSettings = !restoreSettings },
                            ) {
                                Checkbox(checked = restoreSettings, onCheckedChange = null)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = preview != null, onClick = {
                    val withSettings = restoreSettings
                    restoreUri = null
                    files.restore(uri, withSettings)
                }) {
                    Text(stringResource(Res.string.backup_restore))
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreUri = null }) { Text(stringResource(Res.string.dialog_cancel)) }
            },
        )
    }
}

@Composable
private fun BackupCount(label: String, count: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(localizedInteger(count), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
