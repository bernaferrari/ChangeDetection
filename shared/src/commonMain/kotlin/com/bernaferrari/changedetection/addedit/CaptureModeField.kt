package com.bernaferrari.changedetection.addedit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.localization.stringResource
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.kit.ConnectedChoice

/** Show the content choices first and surface an advanced choice when it is active. */
@Composable
internal fun CompareChoice(
    mode: CaptureMode,
    onChange: (CaptureMode) -> Unit,
    enabled: Boolean,
    screenshotAvailable: Boolean,
    isEditing: Boolean,
    modifier: Modifier = Modifier,
) {
    val code = mode == CaptureMode.HTML || mode == CaptureMode.SOURCE
    val options = buildList {
        if (mode == CaptureMode.DEFAULT) add(CaptureMode.DEFAULT)
        add(CaptureMode.DIRECT_TEXT)
        if (screenshotAvailable || mode == CaptureMode.SCREENSHOT) add(CaptureMode.SCREENSHOT)
        if (code) add(CaptureMode.HTML)
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val selected = when {
            code -> CaptureMode.HTML
            mode.isText -> CaptureMode.DIRECT_TEXT
            else -> mode
        }
        ConnectedChoice(options, selected, { if (it != selected) onChange(it) }, Modifier, enabled = enabled) {
            stringResource(
                when (it) {
                    CaptureMode.SCREENSHOT -> Res.string.mode_screenshot
                    CaptureMode.TEXT, CaptureMode.DIRECT_TEXT -> Res.string.mode_text
                    CaptureMode.DEFAULT -> Res.string.mode_default
                    CaptureMode.HTML, CaptureMode.SOURCE -> Res.string.mode_code
                },
            )
        }
        // The explanation crossfades; the card around it animates its height, so a longer one grows smoothly.
        AnimatedContent(mode, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "compare-explanation") { shown ->
            Text(stringResource(explanation(shown)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isEditing) {
            Text(stringResource(Res.string.capture_mode_baseline), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun CodeCompareChoices(mode: CaptureMode, onChange: (CaptureMode) -> Unit, enabled: Boolean) {
    Column {
        Text(stringResource(Res.string.mode_code), style = MaterialTheme.typography.labelLarge)
        FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (choice in listOf(CaptureMode.HTML, CaptureMode.SOURCE)) {
                FilterChip(
                    selected = mode == choice,
                    onClick = { if (mode != choice) onChange(choice) },
                    enabled = enabled,
                    label = { Text(stringResource(if (choice == CaptureMode.HTML) Res.string.capture_mode_html else Res.string.capture_mode_source)) },
                    modifier = Modifier.semantics { role = Role.RadioButton },
                )
            }
        }
    }
}

private fun explanation(mode: CaptureMode) = when (mode) {
    CaptureMode.TEXT -> Res.string.editor_text_body
    CaptureMode.DIRECT_TEXT -> Res.string.editor_direct_text_body
    CaptureMode.SCREENSHOT -> Res.string.editor_screenshot_body
    CaptureMode.SOURCE -> Res.string.editor_source_body
    CaptureMode.HTML -> Res.string.editor_html_body
    CaptureMode.DEFAULT -> Res.string.editor_default_body
}
