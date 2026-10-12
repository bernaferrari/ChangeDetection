package com.bernaferrari.changedetection.ui.components

import androidx.compose.runtime.Composable
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.localization.pluralStringResource

/** Requested cadence, not a promise of exact background execution. Never rounds mixed intervals. */
@Composable
internal fun monitoringCadenceLabel(minutes: Long): String {
    val (resource, count) = when {
        minutes % 1440L == 0L -> Res.plurals.settings_sync_every_days to minutes / 1440L
        minutes % 60L == 0L -> Res.plurals.settings_sync_every_hours to minutes / 60L
        else -> Res.plurals.settings_sync_every_minutes to minutes
    }
    return pluralStringResource(resource, count.toInt(), count)
}
