package com.bernaferrari.changedetection.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Primary color (matching the original)
private val Primary = Color(0xFF13ABF1)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0EBFF),
    onPrimaryContainer = Color(0xFF001E2E),
    secondary = Color(0xFF4E616D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1E5F4),
    onSecondaryContainer = Color(0xFF0A1E28),
    tertiary = Color(0xFF5F5A7D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE5DEFF),
    onTertiaryContainer = Color(0xFF1B1736),
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41484D),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF72787E),
    outlineVariant = Color(0xFFC1C7CE),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF96CCFF),
    onPrimary = Color(0xFF00344D),
    primaryContainer = Color(0xFF004C6D),
    onPrimaryContainer = Color(0xFFCDE5FF),
    secondary = Color(0xFFB5C9D7),
    onSecondary = Color(0xFF20333D),
    secondaryContainer = Color(0xFF374955),
    onSecondaryContainer = Color(0xFFD1E5F4),
    tertiary = Color(0xFFC9C2EA),
    onTertiary = Color(0xFF312C4C),
    tertiaryContainer = Color(0xFF474264),
    onTertiaryContainer = Color(0xFFE5DEFF),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E5),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E5),
    surfaceVariant = Color(0xFF41484D),
    onSurfaceVariant = Color(0xFFC1C7CE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8B9198),
    outlineVariant = Color(0xFF41484D),
)

@Composable
fun ChangeDetectionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
