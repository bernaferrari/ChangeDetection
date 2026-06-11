/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ThemeViewModel(
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {
    companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
    }

    val themeMode: StateFlow<ThemeMode> =
        dataStore.data
            .map { preferences ->
                val modeString = preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name
                ThemeMode.entries.find { it.name == modeString } ?: ThemeMode.SYSTEM
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ThemeMode.SYSTEM,
            )

    val dynamicColor: StateFlow<Boolean> =
        dataStore.data
            .map { preferences ->
                preferences[DYNAMIC_COLOR_KEY] ?: true
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true,
            )

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[THEME_MODE_KEY] = mode.name
            }
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[DYNAMIC_COLOR_KEY] = enabled
            }
        }
    }

    /**
     * Returns whether dark theme should be used.
     * Uses collectAsState() to ensure the UI recomposes when theme changes.
     */
    @Composable
    fun shouldUseDarkTheme(): Boolean {
        val mode by themeMode.collectAsState()
        return when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM, ThemeMode.MATERIAL_YOU -> isSystemInDarkTheme()
        }
    }

    /**
     * Returns whether dynamic colors (Material You) should be used.
     * MATERIAL_YOU theme mode always enables dynamic colors.
     * Uses collectAsState() to ensure the UI recomposes when preference changes.
     */
    @Composable
    fun shouldUseDynamicColor(): Boolean {
        val mode by themeMode.collectAsState()
        // MATERIAL_YOU mode always uses dynamic colors
        return mode == ThemeMode.MATERIAL_YOU
    }
}
