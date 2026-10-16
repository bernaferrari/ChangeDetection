package com.bernaferrari.changedetection.addedit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.localization.stringResource
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.kit.ConnectedChoice

/**
 * What to compare, as one choice that never changes shape: Screenshot, Text or Code. Code is the page's markup; choosing it
 * reveals which flavour, cleaned HTML or any source change. Leaving Code is just picking another option, and the row keeps
 * the same segments throughout, so nothing reflows as the choice changes.
 */
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
    // HTML stands in for the whole Code option in the row; the chips below pick the flavour.
    val options = buildList {
        if (mode == CaptureMode.DEFAULT) add(CaptureMode.DEFAULT)
        if (screenshotAvailable || mode == CaptureMode.SCREENSHOT) add(CaptureMode.SCREENSHOT)
        add(CaptureMode.TEXT)
        add(CaptureMode.HTML)
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ConnectedChoice(options, if (code) CaptureMode.HTML else mode, { if (!(code && it == CaptureMode.HTML)) onChange(it) }, Modifier, enabled = enabled) {
            stringResource(
                when (it) {
                    CaptureMode.SCREENSHOT -> Res.string.mode_screenshot
                    CaptureMode.TEXT -> Res.string.mode_text
                    CaptureMode.DEFAULT -> Res.string.mode_default
                    CaptureMode.HTML, CaptureMode.SOURCE -> Res.string.mode_code
                },
            )
        }
        AnimatedVisibility(code, enter = fadeIn() + expandVertically(expandFrom = Alignment.Top), exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)) {
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (flavour in listOf(CaptureMode.HTML, CaptureMode.SOURCE)) {
                    FilterChip(
                        selected = mode == flavour,
                        onClick = { if (mode != flavour) onChange(flavour) },
                        enabled = enabled,
                        label = { Text(stringResource(if (flavour == CaptureMode.HTML) Res.string.capture_mode_html else Res.string.capture_mode_source)) },
                        modifier = Modifier.semantics { role = Role.RadioButton },
                    )
                }
            }
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

private fun explanation(mode: CaptureMode) = when (mode) {
    CaptureMode.TEXT -> Res.string.editor_text_body
    CaptureMode.SCREENSHOT -> Res.string.editor_screenshot_body
    CaptureMode.SOURCE -> Res.string.editor_source_body
    CaptureMode.HTML -> Res.string.editor_html_body
    CaptureMode.DEFAULT -> Res.string.editor_default_body
}
