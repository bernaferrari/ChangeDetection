package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.detection.TextRegion
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.localization.stringResource

private enum class RegionChoice { Whole, Custom }

@Composable
internal fun TextRegionFields(region: TextRegion, error: Boolean, enabled: Boolean, onChoose: (TextRegion) -> Unit, onType: (TextRegion) -> Unit = onChoose, onPick: (() -> Unit)? = null) {
    val custom = !region.isEmpty
    var selectors by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.text_region_title), style = MaterialTheme.typography.titleSmall)
        // Choosing a part opens the real picker; never invent a selector without inspecting the page.
        val options = listOf(RegionChoice.Whole, RegionChoice.Custom)
        val whole = stringResource(Res.string.text_region_whole)
        val choose = stringResource(Res.string.picker_title)
        val picked = stringResource(Res.string.editor_part_picked)
        com.bernaferrari.changedetection.ui.kit.ConnectedChoice(
            options = options,
            selected = when {
                custom -> RegionChoice.Custom
                else -> RegionChoice.Whole
            },
            onSelect = { choice ->
                when (choice) {
                    RegionChoice.Whole -> onChoose(TextRegion())

                    RegionChoice.Custom -> if (onPick != null) {
                        onPick()
                    } else {
                        selectors = true
                    }
                }
            },
            modifier = Modifier,
            enabled = enabled,
        ) {
            when (it) {
                RegionChoice.Whole -> whole
                RegionChoice.Custom -> if (custom) picked else choose
            }
        }
        TextButton(onClick = { selectors = !selectors }, enabled = enabled, contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text(stringResource(Res.string.editor_selectors))
            Icon(if (selectors || error) androidx.compose.material.icons.Icons.Default.ExpandLess else androidx.compose.material.icons.Icons.Default.ExpandMore, null)
        }
        if (selectors || error) {
            Text(stringResource(Res.string.text_region_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = region.include,
                onValueChange = { onType(region.copy(include = it)) },
                label = { Text(stringResource(Res.string.text_region_include)) },
                placeholder = { Text("#price, .availability") },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                isError = error,
                shape = MaterialTheme.shapes.medium,
            )
            OutlinedTextField(
                value = region.exclude,
                onValueChange = { onType(region.copy(exclude = it)) },
                label = { Text(stringResource(Res.string.text_region_exclude)) },
                placeholder = { Text(".advert, .countdown") },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                isError = error,
                shape = MaterialTheme.shapes.medium,
            )
        }
        if (error) Text(stringResource(Res.string.text_region_invalid, com.bernaferrari.changedetection.detection.MAX_TEXT_SELECTOR_LENGTH), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}
