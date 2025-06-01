package com.bernaferrari.changedetection.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.settings.SettingsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    dataStore: DataStore<Preferences>
) : ViewModel() {

    private val themeMode: StateFlow<String> = dataStore.data.map { preferences ->
        preferences[SettingsViewModel.THEME_MODE_KEY] ?: "MATERIAL_YOU"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "MATERIAL_YOU"
    )

    @Composable
    fun shouldUseDarkTheme(): Boolean {
        val currentTheme by themeMode.collectAsState()
        val systemInDarkTheme = isSystemInDarkTheme()

        return when (currentTheme) {
            "LIGHT" -> false
            "DARK" -> true
            "SYSTEM" -> systemInDarkTheme
            "MATERIAL_YOU" -> systemInDarkTheme
            else -> systemInDarkTheme
        }
    }

    @Composable
    fun shouldUseDynamicColor(): Boolean {
        val currentTheme by themeMode.collectAsState()
        return currentTheme == "MATERIAL_YOU" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
}
