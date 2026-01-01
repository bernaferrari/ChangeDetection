package com.bernaferrari.changedetection.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.ui.graphics.vector.ImageVector

enum class ThemeMode(
    val displayName: String,
    val description: String,
    val icon: ImageVector,
) {
    SYSTEM(
        displayName = "System",
        description = "Match your device settings",
        icon = Icons.Default.PhoneAndroid,
    ),
    MATERIAL_YOU(
        displayName = "For You",
        description = "Dynamic colors from your wallpaper",
        icon = Icons.Default.Palette,
    ),
    LIGHT(
        displayName = "Light",
        description = "Always use light theme",
        icon = Icons.Default.LightMode,
    ),
    DARK(
        displayName = "Dark",
        description = "Always use dark theme",
        icon = Icons.Default.DarkMode,
    ),
}
