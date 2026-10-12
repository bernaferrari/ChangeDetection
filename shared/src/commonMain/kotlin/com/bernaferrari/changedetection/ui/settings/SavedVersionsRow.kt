package com.bernaferrari.changedetection.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.runtime.*
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.kit.GroupRow
import com.bernaferrari.changedetection.localization.stringResource

@Composable
internal fun SavedVersionsRow(state: SettingsUiState, onRetry: () -> Unit, showDivider: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    GroupRow(
        title = stringResource(Res.string.settings_saved_versions),
        supporting = when {
            state.storageLoadFailed -> stringResource(Res.string.retry)
            state.storageBytes == null -> stringResource(Res.string.loading)
            else -> stringResource(Res.string.settings_storage_value, formatStorage(state.storageBytes))
        },
        icon = Icons.Outlined.Storage,
        showDivider = showDivider,
        onClick = { expanded = true },
    )
    if (expanded) {
        HistoryStorageDialog(state.storageBytes, state.storageLoadFailed, onRetry, onDismiss = { expanded = false })
    }
}

private fun formatStorage(bytes: Long): String = when {
    bytes == 0L -> "${com.bernaferrari.changedetection.util.localizedInteger(0)} MB"
    bytes < 1024 * 1024 -> "<${com.bernaferrari.changedetection.util.localizedInteger(1)} MB"
    bytes < 1024L * 1024 * 1024 -> "${com.bernaferrari.changedetection.util.localizedNumber((bytes / (1024 * 1024)).toDouble(), maxFractionDigits = 0)} MB"
    else -> "${com.bernaferrari.changedetection.util.localizedNumber((bytes * 10 / (1024L * 1024 * 1024)) / 10.0)} GB"
}
