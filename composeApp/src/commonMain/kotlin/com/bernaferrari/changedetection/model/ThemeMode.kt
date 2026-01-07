package com.bernaferrari.changedetection.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.ui.graphics.vector.ImageVector
import com.bernaferrari.changedetection.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource

/**
 * 🎨 Beautiful Theme Mode Options - The Perfect Visual Experience!
 * Supports Material You dynamic theming with gorgeous fallbacks.
 */
enum class ThemeMode(
    val titleRes: StringResource,
    val descriptionRes: StringResource,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    SYSTEM(
        titleRes = Res.string.theme_system_title,
        descriptionRes = Res.string.theme_system_desc,
        icon = Icons.Outlined.PhoneAndroid,
        selectedIcon = Icons.Filled.PhoneAndroid,
    ),
    MATERIAL_YOU(
        titleRes = Res.string.theme_material_you_title,
        descriptionRes = Res.string.theme_material_you_desc,
        icon = Icons.Outlined.Palette,
        selectedIcon = Icons.Filled.Palette,
    ),
    LIGHT(
        titleRes = Res.string.theme_light_title,
        descriptionRes = Res.string.theme_light_desc,
        icon = Icons.Outlined.LightMode,
        selectedIcon = Icons.Filled.LightMode,
    ),
    DARK(
        titleRes = Res.string.theme_dark_title,
        descriptionRes = Res.string.theme_dark_desc,
        icon = Icons.Outlined.DarkMode,
        selectedIcon = Icons.Filled.DarkMode,
    ),
}
