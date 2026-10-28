package com.bernaferrari.changedetection.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import com.bernaferrari.changedetection.model.ColorStyle
import com.materialkolor.material3.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec

/** The brand hue: a clear indigo that sits opposite the yellow highlighter, so changes always pop. */
internal val BrandSeed = Color(0xFF4F5BD5)

/**
 * A tonal Material 3 scheme. [SeedColor] is the stored "app default" marker and maps to [BrandSeed];
 * every other seed comes from the Appearance picker.
 */
internal fun brandColorScheme(seed: Color, dark: Boolean, style: ColorStyle = ColorStyle.EXPRESSIVE): ColorScheme = dynamicColorScheme(
    seedColor = if (seed == SeedColor) BrandSeed else seed,
    isDark = dark,
    style = style.palette,
    specVersion = ColorSpec.SpecVersion.SPEC_2025,
).let { if (style == ColorStyle.TONAL) it.withCalmSurfaces() else it }

internal fun referenceColorScheme(dark: Boolean) = brandColorScheme(SeedColor, dark)

/** The one place every platform builds the theme from, so type, shape and marks never drift. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AppMaterialTheme(colorScheme: ColorScheme, dark: Boolean, content: @Composable () -> Unit) {
    // Screens sit on the container tone and content cards on the bright tone (the Android 16 look),
    // so grouped content reads as crisp tiles in light and dark.
    val screen = remember(colorScheme) { colorScheme.copy(surface = colorScheme.surfaceContainer, background = colorScheme.surfaceContainer) }
    MaterialExpressiveTheme(
        colorScheme = screen,
        motionScheme = MotionScheme.expressive(),
        typography = appTypography(),
        shapes = AppShapes,
    ) {
        CompositionLocalProvider(LocalAppSpacing provides AppSpacing(), LocalMarks provides marksFor(dark)) {
            content()
        }
    }
}

/**
 * The Tonal style's quiet surfaces: they keep only a hint of the hue while the accent roles keep all of it, so
 * buttons, selection and the highlighter are the colour on screen. The other styles keep their tinted surfaces.
 */
internal fun ColorScheme.withCalmSurfaces(): ColorScheme {
    fun Color.calm(maxChroma: Double): Color {
        val hct = Hct.fromInt(toArgb())
        return if (hct.chroma <= maxChroma) this else Color(hct.withChroma(maxChroma).toInt()).copy(alpha = alpha)
    }
    return copy(
        background = background.calm(1.2),
        surface = surface.calm(1.2),
        surfaceBright = surfaceBright.calm(1.2),
        surfaceDim = surfaceDim.calm(1.2),
        surfaceContainerLowest = surfaceContainerLowest.calm(1.2),
        surfaceContainerLow = surfaceContainerLow.calm(1.2),
        surfaceContainer = surfaceContainer.calm(1.2),
        surfaceContainerHigh = surfaceContainerHigh.calm(1.6),
        surfaceContainerHighest = surfaceContainerHighest.calm(2.0),
        surfaceVariant = surfaceVariant.calm(4.0),
        onSurfaceVariant = onSurfaceVariant.calm(6.0),
        outline = outline.calm(6.0),
        outlineVariant = outlineVariant.calm(4.0),
    )
}
