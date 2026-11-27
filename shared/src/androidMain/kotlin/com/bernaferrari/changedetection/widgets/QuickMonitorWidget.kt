package com.bernaferrari.changedetection.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import com.bernaferrari.changedetection.background.TurboPreferences
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.MainActivityProvider
import com.bernaferrari.changedetection.WorkerHelper
import com.bernaferrari.changedetection.library.R
import kotlinx.coroutines.flow.first

class QuickMonitorWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val koin = org.koin.mp.KoinPlatformTools.defaultContext().get()
        val manager = koin.get<WorkerHelper>()
        val stored = TurboPreferences.read(koin.get<DataStore<Preferences>>().data.first())
        val running = manager.quickMonitorState.value
        // An interrupted session can still be on disk while the new process is clearing it.
        val isLive = stored.isActive && running.isActive && running.startTime == stored.startTime &&
            stored.deadlineTime > kotlin.time.Clock.System.now().toEpochMilliseconds()
        val state = stored.copy(isActive = isLive)
        provideContent {
            GlanceTheme {
                WidgetContent(context, state)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context, state: com.bernaferrari.changedetection.background.TurboState) {
        val isActive = state.isActive
        val siteCount = state.siteIds.size
        val intervalSeconds = state.intervalSeconds
        val subtitle = if (isActive) {
            context.resources.getQuantityString(
                R.plurals.quick_monitor_notification_subtitle,
                siteCount,
                siteCount,
                intervalSeconds,
            )
        } else {
            context.getString(R.string.quick_monitor_widget_inactive)
        }

        val colors = GlanceTheme.colors
        val backgroundColor = if (isActive) colors.primaryContainer else colors.background
        val headerTextColor = if (isActive) colors.onPrimaryContainer else colors.onBackground
        val accentColor = if (isActive) colors.onPrimaryContainer else colors.primary

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(backgroundColor)
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.mipmap.ic_launcher),
                    contentDescription = null,
                    modifier = GlanceModifier.padding(end = 8.dp),
                )
                Text(
                    text = context.getString(R.string.quick_monitor_title),
                    style = TextStyle(fontWeight = FontWeight.Bold, color = headerTextColor),
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (isActive) {
                Text(
                    text = context.getString(R.string.quick_monitor_banner_live_badge),
                    style = TextStyle(color = accentColor, fontWeight = FontWeight.Medium),
                )
            }

            Text(
                text = subtitle,
                style = TextStyle(
                    color = headerTextColor,
                    fontStyle = if (isActive) FontStyle.Normal else FontStyle.Italic,
                ),
                modifier = GlanceModifier.padding(top = 4.dp, bottom = 12.dp),
            )

            Row {
                if (isActive) {
                    Text(
                        text = context.getString(R.string.action_stop_monitoring),
                        modifier = GlanceModifier.clickable(actionRunCallback<WidgetStopAction>()),
                        style = TextStyle(color = GlanceTheme.colors.error, fontWeight = FontWeight.Medium),
                    )
                } else {
                    Text(
                        text = context.getString(R.string.start),
                        modifier = GlanceModifier.clickable(
                            actionStartActivity(
                                Intent(
                                    context,
                                    MainActivityProvider.getMainActivityClass(),
                                ),
                            ),
                        ),
                        style = TextStyle(color = accentColor, fontWeight = FontWeight.Medium),
                    )
                }
            }
        }
    }
}

class WidgetStopAction : androidx.glance.appwidget.action.ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
            org.koin.mp.KoinPlatformTools.defaultContext().get().get<com.bernaferrari.changedetection.WorkerHelper>().stopQuickMonitor()
        }
    }
}
