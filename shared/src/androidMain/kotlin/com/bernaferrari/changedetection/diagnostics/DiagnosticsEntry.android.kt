package com.bernaferrari.changedetection.diagnostics

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.bernaferrari.changedetection.library.R
import kotlinx.coroutines.*
import java.io.File

@Composable
actual fun DiagnosticsEntry(siteId: String?) {
    var show by remember(siteId) { mutableStateOf(false) }
    val context = LocalContext.current
    TextButton(onClick = { show = true }, modifier = Modifier.testTag("export-diagnostics")) { Text(context.getString(R.string.diagnostics_export)) }
    if (show) DiagnosticsDialog(siteId, onClose = { show = false })
}

/** Preparing, file IO and JSON work are owned by the dialog and leave Compose on cancellation. */
@Composable
internal fun DiagnosticsDialog(siteId: String?, onClose: () -> Unit, store: AndroidDiagnosticStore = AndroidDiagnostics.install(LocalContext.current)) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var report by remember(siteId) { mutableStateOf<DiagnosticReport?>(null) }
    var failed by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }
    var generation by remember { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { sharing = false }
    LaunchedEffect(siteId, generation) {
        report = null
        failed = false
        try {
            report = store.report(siteId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failed = true
        }
    }
    AlertDialog(
        onDismissRequest = { if (!sharing) onClose() },
        title = { Text(context.getString(R.string.diagnostics_title), Modifier.semantics { heading() }) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp).testTag("diagnostic-report-content"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text(context.getString(R.string.diagnostics_explanation), style = MaterialTheme.typography.bodyMedium) }
                item { Text(context.getString(if (siteId == null) R.string.diagnostics_scope else R.string.diagnostics_page_scope), style = MaterialTheme.typography.bodySmall) }
                when {
                    failed -> item {
                        Text(context.getString(R.string.diagnostics_unavailable))
                        TextButton(onClick = { generation++ }) { Text(context.getString(R.string.diagnostics_retry)) }
                    }

                    report == null -> item {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(context.getString(R.string.diagnostics_loading))
                    }

                    report!!.isEmpty -> item { Text(context.getString(R.string.diagnostics_empty)) }

                    else -> {
                        item { Text(context.getString(R.string.diagnostics_content), style = MaterialTheme.typography.titleSmall) }
                        itemsIndexed(report!!.lines, key = { index, _ -> index }) { _, line -> Text(line, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = report?.isEmpty == false && !sharing, onClick = {
                if (!sharing) {
                    sharing = true
                    scope.launch {
                        try {
                            val file = store.prepareShare(requireNotNull(report))
                            launcher.launch(diagnosticShareIntent(context, file))
                        } catch (cancelled: CancellationException) {
                            sharing = false
                            throw cancelled
                        } catch (_: Exception) {
                            sharing = false
                            failed = true
                        }
                    }
                }
            }, modifier = Modifier.testTag("share-diagnostics")) { Text(context.getString(R.string.diagnostics_share)) }
        },
        dismissButton = { TextButton(onClick = onClose, enabled = !sharing) { Text(context.getString(R.string.diagnostics_close)) } },
    )
}

internal fun diagnosticShareIntent(context: Context, file: File): Intent {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        // Addressed to the developer, so "Send report" works in one tap from any mail app.
        putExtra(Intent.EXTRA_EMAIL, arrayOf("bernaferrari2@gmail.com"))
        putExtra(Intent.EXTRA_SUBJECT, "Change Detection diagnostics")
        clipData = ClipData.newRawUri("diagnostics.json", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(send, context.getString(R.string.diagnostics_share))
}
