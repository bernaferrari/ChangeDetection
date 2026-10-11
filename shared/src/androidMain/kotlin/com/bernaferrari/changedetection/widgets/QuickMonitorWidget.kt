/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.compose.ui.graphics.Color
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
import androidx.glance.appwidget.state.updateAppWidgetState
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
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.MainActivityProvider
import com.bernaferrari.changedetection.library.R
import kotlinx.coroutines.flow.first

// Single 'settings' DataStore is declared only in PlatformDiModule.android.kt;
// obtained here via Koin so the widget never registers a second file delegate.
private fun Context.dataStore(): DataStore<Preferences> = org.koin.mp.KoinPlatformTools.defaultContext().get().get()

class QuickMonitorWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val manager = org.koin.mp.KoinPlatformTools.defaultContext().get().get<com.bernaferrari.changedetection.WorkerHelper>()
        val state = manager.quickMonitorState.value
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
            context.getString(R.string.empty_add_site_hint)
        }

        val backgroundColor = if (isActive) ColorProvider(Color(0xFFEEF0FF)) else GlanceTheme.colors.background
        val headerTextColor = if (isActive) ColorProvider(Color(0xFF1D2147)) else GlanceTheme.colors.onBackground
        val subtitleColor = if (isActive) ColorProvider(Color(0xFF444B8F)) else GlanceTheme.colors.onBackground
        val accentColor = if (isActive) ColorProvider(Color(0xFF4C64FF)) else GlanceTheme.colors.primary

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
                    color = subtitleColor,
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
        QuickMonitorWidget().update(context, glanceId)
    }
}
