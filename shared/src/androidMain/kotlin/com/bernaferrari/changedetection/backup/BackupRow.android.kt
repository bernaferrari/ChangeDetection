package com.bernaferrari.changedetection.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.bernaferrari.changedetection.ui.kit.GroupRow
import com.bernaferrari.changedetection.ui.kit.optional
import com.bernaferrari.changedetection.util.ApplicationScope
import java.time.LocalDate

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

@Composable
private fun BackupDialog(files: BackupFiles, onClose: () -> Unit) {
    val progress by files.state.collectAsState()
    var restoreSettings by rememberSaveable { mutableStateOf(true) }
    val name = "change-detection-backup-${LocalDate.now()}"
    val savePages = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(files::savePages) }
    val saveEverything = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { it?.let(files::saveEverything) }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { files.restore(it, restoreSettings) } }

    val result = when (val state = progress) {
        is BackupProgress.Saved -> stringResource(Res.string.backup_saved, state.summary.pages, state.summary.versions)
        is BackupProgress.Restored -> stringResource(Res.string.backup_restored, state.summary.pagesAdded, state.summary.pagesAlreadyHere, state.summary.versionsAdded)
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
    AlertDialog(
        onDismissRequest = close,
        title = { Text(stringResource(Res.string.backup_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when {
                    working -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text(stringResource(Res.string.backup_working), Modifier.padding(start = 16.dp))
                    }

                    result != null -> Text(result, Modifier.testTag("backup-result"))

                    else -> {
                        GroupRow(stringResource(Res.string.backup_save_pages), modifier = Modifier.testTag("backup-save-pages"), supporting = stringResource(Res.string.backup_save_pages_body), onClick = { savePages.launch("$name.json") })
                        GroupRow(stringResource(Res.string.backup_save_everything), modifier = Modifier.testTag("backup-save-everything"), supporting = stringResource(Res.string.backup_save_everything_body), onClick = { saveEverything.launch("$name.zip") })
                        GroupRow(stringResource(Res.string.backup_restore), modifier = Modifier.testTag("backup-restore"), supporting = stringResource(Res.string.backup_restore_body), onClick = { restore.launch(arrayOf("application/json", "application/zip", "application/octet-stream")) })
                        Row(
                            Modifier.fillMaxWidth().toggleable(restoreSettings, role = Role.Checkbox) { restoreSettings = it }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = restoreSettings, onCheckedChange = null)
                            Text(stringResource(Res.string.backup_restore_settings), Modifier.padding(start = 12.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(Res.string.backup_privacy_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = close) { Text(stringResource(Res.string.done)) } },
    )
}
