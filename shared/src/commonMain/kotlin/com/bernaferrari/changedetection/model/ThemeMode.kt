package com.bernaferrari.changedetection.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.ui.graphics.vector.ImageVector
import com.bernaferrari.changedetection.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource

/** Theme choices shared by Settings and the application theme. */
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

    ;

    companion object {
        fun fromStoredName(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
