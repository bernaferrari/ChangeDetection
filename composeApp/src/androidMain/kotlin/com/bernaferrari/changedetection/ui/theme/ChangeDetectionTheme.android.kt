package com.bernaferrari.changedetection.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

/**
 * 🎨 ChangeDetection Android Theme with true Material You dynamic colors!
 * - Android 12+: Uses native wallpaper-extracted colors when dynamicColor=true
 * - All other cases: Uses MaterialKolor to generate schemes from SeedColor
 */
@Composable
actual fun ChangeDetectionTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Use native Android dynamic colors on Android 12+ (Material You)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // All other cases: generate from SeedColor using MaterialKolor
        else -> {
            rememberDynamicColorScheme(
                seedColor = SeedColor,
                isDark = darkTheme,
                style = PaletteStyle.Expressive
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = Shapes,
        typography = Typography,
        content = content
    )
}

/**
 * Convenience wrapper with default values for simpler call sites.
 */
@Composable
fun ChangeDetectionTheme(
    content: @Composable () -> Unit
) {
    ChangeDetectionTheme(
        darkTheme = isSystemInDarkTheme(),
        dynamicColor = true,
        content = content
    )
}
