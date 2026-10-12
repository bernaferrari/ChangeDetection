package com.bernaferrari.changedetection.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/**
 * The diff language. These colors stay fixed under every palette, including wallpaper colors, so
 * "added" and "removed" are always legible and always mean the same thing.
 */
@Immutable
data class Marks(
    /** Added text, new badges, the one "something changed" color. */
    val highlight: Color,
    val onHighlight: Color,
    /** Hairline used to outline highlighted areas on white captures. */
    val highlightEdge: Color,
    val highlightSoft: Color,
    /** Removed text, struck through. */
    val removed: Color,
    val removedSoft: Color,
    val healthy: Color,
    val warning: Color,
    val warningSoft: Color,
    /** Fake "paper" used behind captured web content so it looks like a page, not an app surface. */
    val page: Color,
    val onPage: Color,
    /** Six quiet tints for page identities. */
    val identity: List<Color>,
)

private val LightMarks = Marks(
    highlight = Color(0xFFFFE14A),
    onHighlight = Color(0xFF17160F),
    highlightEdge = Color(0xFFE2C21E),
    highlightSoft = Color(0xFFFFF6C7),
    removed = Color(0xFFB23A24),
    removedSoft = Color(0xFFFBE5DF),
    healthy = Color(0xFF1D7448),
    warning = Color(0xFF9A5600),
    warningSoft = Color(0xFFFCEBD2),
    page = Color.White,
    onPage = Color(0xFF1D1D1D),
    identity = listOf(
        Color(0xFFDDE1FF),
        Color(0xFFC9F0D8),
        Color(0xFFFFDCC2),
        Color(0xFFFFD8E4),
        Color(0xFFE9DDFF),
        Color(0xFFC4EEF0),
    ),
)

private val DarkMarks = Marks(
    highlight = Color(0xFFFFE14A),
    onHighlight = Color(0xFF14130E),
    highlightEdge = Color(0xFFFFE14A),
    highlightSoft = Color(0x24FFE14A),
    removed = Color(0xFFFF8E78),
    removedSoft = Color(0x24FF785F),
    healthy = Color(0xFF74D9A0),
    warning = Color(0xFFFFB866),
    warningSoft = Color(0x24FFB866),
    page = Color(0xFFF4F2EC),
    onPage = Color(0xFF1D1D1D),
    identity = listOf(
        Color(0xFF353F8C),
        Color(0xFF1E5A3C),
        Color(0xFF6B3B12),
        Color(0xFF7A2949),
        Color(0xFF4F3A86),
        Color(0xFF1C5457),
    ),
)

fun marksFor(dark: Boolean): Marks = if (dark) DarkMarks else LightMarks

val LocalMarks = staticCompositionLocalOf { LightMarks }

val MaterialTheme.marks: Marks
    @Composable
    @ReadOnlyComposable
    get() = LocalMarks.current

/** Page content: the app's text face, a touch larger and airier so quoted pages read comfortably. */
object Reading {
    val body: TextStyle
        @Composable get() = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 26.sp)
    val snippet: TextStyle
        @Composable get() = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp)
    val heading: TextStyle
        @Composable get() = MaterialTheme.typography.headlineSmall
    val small: TextStyle
        @Composable get() = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp)
}
