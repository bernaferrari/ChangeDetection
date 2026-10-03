/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.model.ThemeMode
import com.bernaferrari.changedetection.model.AppPalette
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ThemeViewModel(
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {
    companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val APP_PALETTE_KEY = stringPreferencesKey("app_palette")
    }

    val themeMode: StateFlow<ThemeMode> =
        dataStore.data
            .map { preferences ->
                ThemeMode.fromStoredName(preferences[THEME_MODE_KEY])
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ThemeMode.SYSTEM,
            )

    val appPalette: StateFlow<AppPalette> =
        dataStore.data
            .map { preferences -> AppPalette.fromStoredName(preferences[APP_PALETTE_KEY]) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AppPalette.DEFAULT,
            )

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[THEME_MODE_KEY] = mode.name
            }
        }
    }

    @Composable
    fun selectedSeedColor() = appPalette.collectAsStateWithLifecycle().value.seedColor

    /**
     * Returns whether dark theme should be used.
     * Uses lifecycle-aware collection so inactive screens do not keep observing preferences.
     */
    @Composable
    fun shouldUseDarkTheme(): Boolean {
        val mode by themeMode.collectAsStateWithLifecycle()
        return when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
    }
}
