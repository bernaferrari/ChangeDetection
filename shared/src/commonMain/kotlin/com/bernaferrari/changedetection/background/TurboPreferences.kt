package com.bernaferrari.changedetection.background

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.first

/** One durable format across hosts; restoring a running record never resumes its timer. */
internal object TurboPreferences {
    private val active = booleanPreferencesKey("quick_monitor_active")
    private val ids = stringPreferencesKey("quick_monitor_site_ids")
    private val started = longPreferencesKey("quick_monitor_start_time")
    private val deadline = longPreferencesKey("quick_monitor_deadline_time")
    private val interval = intPreferencesKey("quick_monitor_interval_seconds")
    private val duration = intPreferencesKey("quick_monitor_duration_minutes")
    private val reason = stringPreferencesKey("quick_monitor_end_reason")

    fun read(preferences: Preferences) = TurboState(
        isActive = preferences[active] == true,
        siteIds = preferences[ids].orEmpty().split(',').filter(String::isNotBlank).toSet(),
        startTime = preferences[started] ?: 0,
        deadlineTime = preferences[deadline] ?: 0,
        intervalSeconds = preferences[interval] ?: 30,
        durationMinutes = preferences[duration] ?: 180,
        endReason = TurboEndReason.entries.firstOrNull { it.name == preferences[reason] },
    )

    fun write(preferences: MutablePreferences, state: TurboState) {
        preferences[active] = state.isActive
        preferences[ids] = state.siteIds.sorted().joinToString(",")
        preferences[started] = state.startTime
        preferences[deadline] = state.deadlineTime
        preferences[interval] = state.intervalSeconds
        preferences[duration] = state.durationMinutes
        preferences[reason] = state.endReason?.name.orEmpty()
    }
}

internal suspend fun persistTurboSession(
    session: TurboSession,
    preferences: DataStore<Preferences>,
    afterPublish: suspend () -> Unit = {},
    onFailure: (Throwable) -> Unit,
) = publishTurboState(
    session = session,
    read = { TurboPreferences.read(preferences.data.first()) },
    publish = { state ->
        preferences.edit { TurboPreferences.write(it, state) }
        afterPublish()
    },
    onFailure = onFailure,
)
