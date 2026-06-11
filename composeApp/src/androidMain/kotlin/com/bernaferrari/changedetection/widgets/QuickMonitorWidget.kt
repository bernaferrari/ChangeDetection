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
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import kotlinx.coroutines.runBlocking

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class QuickMonitorWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent(context)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context) {
        val ACTIVE = booleanPreferencesKey("quick_monitor_active")
        val SITE_IDS = stringPreferencesKey("quick_monitor_site_ids")
        val START_TIME = longPreferencesKey("quick_monitor_start_time")
        val DEADLINE = longPreferencesKey("quick_monitor_deadline_time")

        val prefs = runBlocking { context.widgetDataStore.data.first() }
        val isActive = prefs[ACTIVE] ?: false
        val siteCount = (prefs[SITE_IDS] ?: "").split(',').filter { it.isNotBlank() }.size
        val subtitle = if (isActive) {
            context.getString(R.string.quick_monitor_notification_subtitle, siteCount, 15)
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
                        text = context.getString(R.string.quick_monitor_extend_1h),
                        modifier = GlanceModifier
                            .padding(end = 12.dp)
                            .clickable(actionRunCallback<WidgetExtendAction>()),
                        style = TextStyle(color = accentColor, fontWeight = FontWeight.Medium),
                    )
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

class WidgetExtendAction : androidx.glance.appwidget.action.ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        com.bernaferrari.changedetection.QuickMonitorService.extend(context)
        updateAppWidgetState(context, glanceId) { }
        QuickMonitorWidget().update(context, glanceId)
    }
}

class WidgetStopAction : androidx.glance.appwidget.action.ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        com.bernaferrari.changedetection.QuickMonitorService.stop(context)
        updateAppWidgetState(context, glanceId) { }
        QuickMonitorWidget().update(context, glanceId)
    }
}
