package com.bernaferrari.changedetection.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.gsf_bold
import com.bernaferrari.changedetection.shared.generated.resources.gsf_display_bold
import com.bernaferrari.changedetection.shared.generated.resources.gsf_display_semibold
import com.bernaferrari.changedetection.shared.generated.resources.gsf_medium
import com.bernaferrari.changedetection.shared.generated.resources.gsf_regular
import com.bernaferrari.changedetection.shared.generated.resources.gsf_semibold
import org.jetbrains.compose.resources.Font
import androidx.compose.ui.text.intl.Locale
import com.bernaferrari.changedetection.shared.generated.resources.*

/** Google Sans Flex for text, and its rounded display cut for headlines (the M3 Expressive voice). */
@Composable
fun appTypography(): Typography {
    // The bundled Latin font lacks Indic/CJK glyphs. Use compact script fonts rather than
    // relying on device fallback, which is incomplete on some desktop and web systems.
    val scriptFonts = when (Locale.current.language) {
        "hi" -> listOf(Res.font.locale_hi_regular, Res.font.locale_hi_semibold, Res.font.locale_hi_bold)
        "bn" -> listOf(Res.font.locale_bn_regular, Res.font.locale_bn_semibold, Res.font.locale_bn_bold)
        "zh" -> listOf(Res.font.locale_zh_regular, Res.font.locale_zh_semibold, Res.font.locale_zh_bold)
        else -> null
    }
    if (scriptFonts != null) {
        val family = FontFamily(Font(scriptFonts[0], FontWeight.Normal), Font(scriptFonts[1], FontWeight.Medium), Font(scriptFonts[1], FontWeight.SemiBold), Font(scriptFonts[2], FontWeight.Bold))
        return remember(family) { typographyOf(family, family, naturalSpacing = true) }
    }
    val text = FontFamily(
        Font(Res.font.gsf_regular, FontWeight.Normal),
        Font(Res.font.gsf_medium, FontWeight.Medium),
        Font(Res.font.gsf_semibold, FontWeight.SemiBold),
        Font(Res.font.gsf_bold, FontWeight.Bold),
    )
    val display = FontFamily(
        Font(Res.font.gsf_display_semibold, FontWeight.SemiBold),
        Font(Res.font.gsf_display_bold, FontWeight.Bold),
    )
    return remember(text, display) { typographyOf(text, display) }
}

internal fun typographyOf(text: FontFamily, display: FontFamily, naturalSpacing: Boolean = false) = Typography(
    displayLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = if (naturalSpacing) 0.sp else (-0.5).sp),
    displayMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = if (naturalSpacing) 0.sp else (-0.3).sp),
    displaySmall = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = if (naturalSpacing) 0.sp else (-0.2).sp),
    headlineLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = if (naturalSpacing) 0.sp else (-0.2).sp),
    headlineMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = if (naturalSpacing) 0.sp else (-0.1).sp),
    headlineSmall = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = text, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = if (naturalSpacing) 0.sp else 0.1.sp),
    titleSmall = TextStyle(fontFamily = text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = if (naturalSpacing) 0.sp else 0.1.sp),
    bodyLarge = TextStyle(fontFamily = text, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = if (naturalSpacing) 0.sp else 0.1.sp),
    bodyMedium = TextStyle(fontFamily = text, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = if (naturalSpacing) 0.sp else 0.1.sp),
    bodySmall = TextStyle(fontFamily = text, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = if (naturalSpacing) 0.sp else 0.2.sp),
    labelLarge = TextStyle(fontFamily = text, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = if (naturalSpacing) 0.sp else 0.1.sp),
    labelMedium = TextStyle(fontFamily = text, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = if (naturalSpacing) 0.sp else 0.3.sp),
    labelSmall = TextStyle(fontFamily = text, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = if (naturalSpacing) 0.sp else 0.4.sp),
)
