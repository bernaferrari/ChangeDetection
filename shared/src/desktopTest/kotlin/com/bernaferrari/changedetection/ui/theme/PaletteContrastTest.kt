package com.bernaferrari.changedetection.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.bernaferrari.changedetection.model.AppPalette
import com.materialkolor.PaletteStyle
import com.materialkolor.material3.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteContrastTest {
    @Test fun textRolesRemainReadableAcrossAllSelectableSeedsAndBothModes() {
        val failures = mutableListOf<String>()
        for (palette in AppPalette.entries) {
            for (dark in listOf(false, true)) {
                // DYNAMIC checks the generated fallback only; real Android wallpaper colors are separate.
                val scheme = if (palette == AppPalette.DEFAULT) {
                    referenceColorScheme(dark)
                } else {
                    dynamicColorScheme(
                        seedColor = palette.seedColor,
                        isDark = dark,
                        style = PaletteStyle.TonalSpot,
                        specVersion = ColorSpec.SpecVersion.SPEC_2025,
                    )
                }
                val surfaces = mapOf(
                    "surface" to scheme.surface,
                    "low" to scheme.surfaceContainerLow,
                    "container" to scheme.surfaceContainer,
                    "high" to scheme.surfaceContainerHigh,
                    "highest" to scheme.surfaceContainerHighest,
                )
                val pairs = mutableListOf(
                    Triple("primary button", scheme.onPrimary, scheme.primary),
                    Triple("secondary button", scheme.onSecondary, scheme.secondary),
                    Triple("tertiary button", scheme.onTertiary, scheme.tertiary),
                    Triple("selected primary", scheme.onPrimaryContainer, scheme.primaryContainer),
                    Triple("selected secondary", scheme.onSecondaryContainer, scheme.secondaryContainer),
                    Triple("tertiary notice", scheme.onTertiaryContainer, scheme.tertiaryContainer),
                    Triple("error notice", scheme.onErrorContainer, scheme.errorContainer),
                    Triple("inverse", scheme.inverseOnSurface, scheme.inverseSurface),
                )
                surfaces.forEach { (name, background) ->
                    pairs += Triple("text/$name", scheme.onSurface, background)
                    pairs += Triple("supporting/$name", scheme.onSurfaceVariant, background)
                    pairs += Triple("action/$name", scheme.primary, background)
                }
                pairs.forEach { (role, foreground, background) ->
                    val contrast = contrast(foreground, background)
                    if (contrast < 4.5f) failures += "$palette dark=$dark $role: $contrast"
                }
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    private fun contrast(foreground: Color, background: Color): Float {
        val a = foreground.luminance()
        val b = background.luminance()
        return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
    }
}
