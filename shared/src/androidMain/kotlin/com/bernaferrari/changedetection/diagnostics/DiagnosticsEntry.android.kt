package com.bernaferrari.changedetection.diagnostics

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.BugReport
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
import com.bernaferrari.changedetection.localization.stringResource
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.components.CaptureFailureDetails
import kotlinx.coroutines.*
import java.io.File

@Composable
actual fun DiagnosticsEntry(siteId: String?, captureDetails: Boolean, onTryText: (suspend () -> Boolean)?) {
    var show by remember(siteId) { mutableStateOf(false) }
    val context = LocalContext.current
    TextButton(onClick = { show = true }, modifier = Modifier.testTag("export-diagnostics")) {
        Text(if (captureDetails) stringResource(Res.string.capture_details) else context.getString(R.string.diagnostics_export))
    }
    if (show) DiagnosticsDialog(siteId, onClose = { show = false }, captureDetails = captureDetails, onTryText = onTryText)
}

@Composable
actual fun DiagnosticsRow() {
    var show by remember { mutableStateOf(false) }
    val context = LocalContext.current
    com.bernaferrari.changedetection.ui.kit.GroupRow(
        context.getString(R.string.diagnostics_export),
        modifier = Modifier.testTag("export-diagnostics"),
        icon = androidx.compose.material.icons.Icons.Outlined.BugReport,
        onClick = { show = true },
    )
    if (show) DiagnosticsDialog(null, onClose = { show = false })
}

/** Preparing, file IO and JSON work are owned by the dialog and leave Compose on cancellation. */
@Composable
internal fun DiagnosticsDialog(
    siteId: String?,
    onClose: () -> Unit,
    store: AndroidDiagnosticStore = AndroidDiagnostics.install(LocalContext.current),
    captureDetails: Boolean = false,
    onTryText: (suspend () -> Boolean)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var report by remember(siteId) { mutableStateOf<DiagnosticReport?>(null) }
    var failed by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }
    var tryingText by remember(siteId) { mutableStateOf(false) }
    var switchedToText by remember(siteId) { mutableStateOf<Boolean?>(null) }
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
        onDismissRequest = { if (!sharing && !tryingText) onClose() },
        title = { Text(if (captureDetails) stringResource(Res.string.capture_details) else context.getString(R.string.diagnostics_title), Modifier.semantics { heading() }) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp).testTag("diagnostic-report-content"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (captureDetails) {
                    report?.let { current ->
                        val details = current.captureDetails
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                CaptureFailureDetails(details?.resources.orEmpty(), details?.extent, details?.attached)
                                switchedToText?.let { switched ->
                                    Text(stringResource(if (switched) Res.string.capture_text_switched else Res.string.capture_text_switch_failed))
                                }
                                if (onTryText != null && switchedToText != true && details != null &&
                                    details.resources.isNotEmpty() && details.resources.all { it.canCaptureText }
                                ) {
                                    Text(stringResource(Res.string.capture_try_text_hint), style = MaterialTheme.typography.bodyMedium)
                                    OutlinedButton(
                                        enabled = !tryingText && !sharing,
                                        onClick = {
                                            tryingText = true
                                            switchedToText = null
                                            scope.launch {
                                                try {
                                                    switchedToText = onTryText()
                                                } catch (cancelled: CancellationException) {
                                                    throw cancelled
                                                } catch (_: Exception) {
                                                    switchedToText = false
                                                } finally {
                                                    tryingText = false
                                                }
                                            }
                                        },
                                    ) {
                                        Text(stringResource(if (tryingText) Res.string.capture_trying_text else Res.string.capture_try_text))
                                    }
                                }
                            }
                        }
                    }
                }
                item { Text(context.getString(R.string.diagnostics_explanation), style = MaterialTheme.typography.bodyMedium) }
                item { Text(context.getString(if (siteId == null) R.string.diagnostics_scope else R.string.diagnostics_page_scope), style = MaterialTheme.typography.bodySmall) }
                when {
                    failed -> item {
                        Text(context.getString(R.string.diagnostics_unavailable))
                        TextButton(onClick = { generation++ }) { Text(context.getString(R.string.diagnostics_retry)) }
                    }

                    report == null -> item {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            com.bernaferrari.changedetection.ui.components.AppLoadingIndicator(size = 32.dp)
                            Text(context.getString(R.string.diagnostics_loading))
                        }
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
            TextButton(enabled = report?.isEmpty == false && !sharing && !tryingText, onClick = {
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
        dismissButton = { TextButton(onClick = onClose, enabled = !sharing && !tryingText) { Text(context.getString(R.string.diagnostics_close)) } },
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
