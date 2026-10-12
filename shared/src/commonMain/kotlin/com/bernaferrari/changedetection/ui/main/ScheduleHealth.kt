package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.background.SyncJobInfo
import com.bernaferrari.changedetection.background.SyncState
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.util.formatTimestamp
import com.bernaferrari.changedetection.util.getCurrentTimeMillis
import kotlinx.coroutines.delay
import com.bernaferrari.changedetection.ui.components.monitoringCadenceLabel
import com.bernaferrari.changedetection.localization.stringResource

/** Requested cadence and actual scheduler eligibility are deliberately separate. */
@Composable
internal fun ScheduleHealth(intervalMinutes: Long, job: SyncJobInfo?, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(getCurrentTimeMillis()) }
    LaunchedEffect(job?.nextEligibleTime) {
        while (true) {
            now = getCurrentTimeMillis()
            delay(30_000)
        }
    }
    val eligible = job?.nextEligibleTime
    val detail = when {
        job?.state == SyncState.RUNNING -> stringResource(Res.string.schedule_running)
        eligible == null -> stringResource(Res.string.schedule_unconfirmed)
        eligible <= now -> stringResource(Res.string.schedule_eligible_waiting)
        else -> stringResource(Res.string.schedule_next_eligible, formatTimestamp(eligible))
    }
    var expanded by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
            Text(monitoringCadenceLabel(intervalMinutes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (expanded) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
