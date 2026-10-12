package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.localization.stringResource

/** Old monitors keep their filter until an explicit saved edit removes it. */
@Composable
internal fun LegacyThresholdNotice(bytes: Int, enabled: Boolean, onRemove: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.legacy_threshold_title, bytes), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(Res.string.legacy_threshold_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onRemove, enabled = enabled) {
                Text(stringResource(Res.string.legacy_threshold_remove))
            }
        }
    }
}
