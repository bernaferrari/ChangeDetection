@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.kit.groupTile
import com.bernaferrari.changedetection.util.localizedNumber
import com.bernaferrari.changedetection.localization.stringResource

/** Three common waits, with a custom editor that leaves the settings row's layout stable. */
@Composable
internal fun ScreenshotWaitChoice(delayMs: Long, onChange: (Long) -> Unit) {
    val presets = listOf(2000L, 5000L, 15000L)
    var editing by remember { mutableStateOf(false) }
    val customLabel = stringResource(Res.string.checks_custom)
    val presetLabels = presets.map { stringResource(Res.string.turbo_every_seconds_short, it / 1000) }
    val labelStyle = MaterialTheme.typography.labelLarge
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val waitLabel = stringResource(Res.string.turbo_every_seconds_short, localizedNumber(delayMs / 1000.0, maxFractionDigits = 2))
    Column(Modifier.groupTile().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.settings_screenshot_wait), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Text(waitLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Text(stringResource(Res.string.settings_wait_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // Fills the tile as one connected row; when large text would break a label, the same four buttons
        // become an even 2 × 2 grid instead of wrapping words or spilling onto a ragged second line.
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            val gap = ButtonGroupDefaults.ConnectedSpaceBetween
            val labelWidth = with(density) { presetLabels.maxOf { measurer.measure(it, labelStyle, softWrap = false).size.width }.toDp() } + 16.dp
            val columns = if ((maxWidth - EditWidth - gap * 3) / 3 >= labelWidth) 4 else 2
            val rows = (presets.indices + EDIT).chunked(columns)
            val gridShapes = com.bernaferrari.changedetection.ui.kit.rememberConnectedGridShapes()
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(gap)) {
                rows.forEachIndexed { rowIndex, row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEachIndexed { index, item ->
                            val custom = item == EDIT
                            val checked = if (custom) delayMs !in presets else delayMs == presets[item]
                            ToggleButton(
                                checked = checked,
                                onCheckedChange = { if (custom) editing = true else if (it) onChange(presets[item]) },
                                colors = com.bernaferrari.changedetection.ui.kit.connectedToggleColors(),
                                // In a single row the editor keeps its icon size and the presets share the rest.
                                modifier = (if (custom && columns == 4) Modifier.width(EditWidth) else Modifier.weight(1f)).heightIn(min = 48.dp).semantics {
                                    role = Role.RadioButton
                                    selected = checked
                                    if (custom) contentDescription = customLabel
                                },
                                shapes = gridShapes(
                                    com.bernaferrari.changedetection.ui.kit.GridCorners(
                                        topStart = rowIndex == 0 && index == 0,
                                        topEnd = rowIndex == 0 && index == row.lastIndex,
                                        bottomStart = rowIndex == rows.lastIndex && index == 0,
                                        bottomEnd = rowIndex == rows.lastIndex && index == row.lastIndex,
                                    ),
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            ) {
                                if (custom) {
                                    Icon(Icons.Rounded.Edit, null, Modifier.size(20.dp))
                                } else {
                                    Text(presetLabels[item], Modifier.fillMaxWidth(), style = labelStyle, textAlign = TextAlign.Center, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (editing) {
        var text by remember { mutableStateOf(com.bernaferrari.changedetection.util.localizedInteger(delayMs / 1000)) }
        val seconds = com.bernaferrari.changedetection.util.parseLocalizedWholeNumber(text)?.takeIf { it in 0..60 }
        val save: () -> Unit = {
            seconds?.let {
                onChange(it * 1000L)
                editing = false
            }
        }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(stringResource(Res.string.settings_screenshot_wait)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(3) },
                    label = { Text(customLabel) },
                    suffix = { Text(stringResource(Res.string.settings_wait_seconds)) },
                    supportingText = { Text("${com.bernaferrari.changedetection.util.localizedInteger(0)}–${com.bernaferrari.changedetection.util.localizedInteger(60)}") },
                    singleLine = true,
                    isError = seconds == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = { TextButton(onClick = save, enabled = seconds != null) { Text(stringResource(Res.string.done)) } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

/** Marks the custom-wait editor among the preset indices. */
private const val EDIT = -1
private val EditWidth = 56.dp
