package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.detection.*
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.localization.stringResource

@Composable
internal fun RegionSelectionSummary(document: SelectableTextDocument, draft: RegionSelectionDraft, text: String?, loading: Boolean, onRemove: (String) -> Unit, onRestore: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.region_picker_area_count, draft.watched.size), style = MaterialTheme.typography.titleSmall)
        if (document.limited) Text(stringResource(Res.string.region_picker_limited), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(Res.string.region_picker_preview_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        draft.areas.forEach { selector ->
            val label = document.regions.firstOrNull { it.selector == selector }?.text?.take(48) ?: stringResource(Res.string.region_picker_saved_rule)
            InputChip(selected = true, onClick = { onRemove(selector) }, label = { Text(stringResource(Res.string.region_picker_remove_area, label)) })
        }
        draft.ignored.forEach { selector ->
            val label = document.regions.firstOrNull { it.selector == selector }?.text?.take(48) ?: stringResource(Res.string.region_picker_existing_rule)
            InputChip(selected = true, onClick = { onRestore(selector) }, label = { Text(stringResource(Res.string.region_picker_restore_area, label)) })
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(stringResource(Res.string.capture_preview_html), style = MaterialTheme.typography.labelLarge)
        Text(
            text ?: stringResource(
                when {
                    loading -> Res.string.capture_preview_loading
                    draft.watched.isNotEmpty() -> Res.string.region_picker_nothing_left
                    else -> Res.string.region_picker_preview_empty
                },
            ),
            Modifier.testTag("region-result-text"),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
