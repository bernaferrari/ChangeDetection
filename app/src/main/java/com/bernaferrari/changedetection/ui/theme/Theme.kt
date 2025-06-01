package com.bernaferrari.changedetection.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004881),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBEC6DC),
    onSecondary = Color(0xFF283141),
    secondaryContainer = Color(0xFF3E4759),
    onSecondaryContainer = Color(0xFFDAE2F9),
    tertiary = Color(0xFFD0BCC2),
    onTertiary = Color(0xFF352B2E),
    tertiaryContainer = Color(0xFF4C4144),
    onTertiaryContainer = Color(0xFFEDD8DE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF10131B),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF10131B),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC2C7CF),
    outline = Color(0xFF8C9199),
    outlineVariant = Color(0xFF42474E),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE2E2E9),
    inverseOnSurface = Color(0xFF2E3036),
    inversePrimary = Color(0xFF0061A4),
    surfaceDim = Color(0xFF10131B),
    surfaceBright = Color(0xFF363A40),
    surfaceContainerLowest = Color(0xFF0B0E16),
    surfaceContainerLow = Color(0xFF191C23),
    surfaceContainer = Color(0xFF1D2027),
    surfaceContainerHigh = Color(0xFF272A31),
    surfaceContainerHighest = Color(0xFF32353C)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F7),
    onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF6B5B5F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF4DEE3),
    onTertiaryContainer = Color(0xFF25191C),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF1A1C20),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1A1C20),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C7CF),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2F3036),
    inverseOnSurface = Color(0xFFF1F0F4),
    inversePrimary = Color(0xFF9ECAFF),
    surfaceDim = Color(0xFFDCDCE0),
    surfaceBright = Color(0xFFFCFCFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F6FA),
    surfaceContainer = Color(0xFFF0F0F4),
    surfaceContainerHigh = Color(0xFFEAEAEE),
    surfaceContainerHighest = Color(0xFFE4E5E9)
)

@Composable
fun ChangeDetectionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
