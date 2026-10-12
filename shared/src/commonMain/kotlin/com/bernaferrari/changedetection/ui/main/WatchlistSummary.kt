@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.localization.pluralStringResource
import com.bernaferrari.changedetection.localization.stringResource

/** The summary explains actual observations and routes each action to the relevant pages. */
@Composable
internal fun WatchlistSummary(
    scheduledEnabled: Boolean,
    counts: MonitorCounts,
    isRefreshing: Boolean,
    onReviewChanges: () -> Unit,
    onReviewErrors: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when {
        isRefreshing -> stringResource(Res.string.monitor_summary_checking)
        counts.errors > 0 -> pluralStringResource(Res.plurals.monitor_summary_error, counts.errors, counts.errors)
        counts.changed > 0 -> pluralStringResource(Res.plurals.monitor_summary_updated, counts.changed, counts.changed)
        !scheduledEnabled || counts.paused == counts.all -> stringResource(Res.string.schedule_paused_title)
        counts.waiting > 0 -> stringResource(Res.string.schedule_waiting_title)
        else -> stringResource(Res.string.monitor_summary_quiet)
    }
    val description = when {
        isRefreshing -> stringResource(Res.string.monitor_summary_checking_supporting)
        counts.errors > 0 -> stringResource(Res.string.watchlist_failed_description)
        counts.changed > 0 -> stringResource(Res.string.monitor_summary_updated_supporting)
        !scheduledEnabled || counts.paused == counts.all -> stringResource(Res.string.schedule_paused_description)
        counts.waiting > 0 -> stringResource(Res.string.schedule_waiting_description)
        else -> pluralStringResource(Res.plurals.monitor_summary_quiet_supporting, counts.all - counts.paused, counts.all - counts.paused)
    }
    Column(
        modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isRefreshing) LoadingIndicator(Modifier.size(28.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // Existing results remain actionable while new checks are running.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (counts.errors > 0) {
                Button(onClick = onReviewErrors, modifier = Modifier.heightIn(min = 48.dp), shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(Res.string.watchlist_review_errors))
                }
            }
            if (counts.changed > 0) {
                if (counts.errors > 0) {
                    TextButton(onClick = onReviewChanges, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(Res.string.monitor_review_changes))
                    }
                } else {
                    Button(onClick = onReviewChanges, modifier = Modifier.heightIn(min = 48.dp), shapes = ButtonDefaults.shapes()) {
                        Text(stringResource(Res.string.monitor_review_changes))
                    }
                }
            }
        }
    }
}
