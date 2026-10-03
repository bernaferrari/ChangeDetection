/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
        Color(0xFFF1E2C6),
        Color(0xFFD9E7D2),
        Color(0xFFD5E4F2),
        Color(0xFFF3DAD6),
        Color(0xFFE3DCF1),
        Color(0xFFE4E1D8),
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
        Color(0xFF3B3222),
        Color(0xFF26331F),
        Color(0xFF1F2C3A),
        Color(0xFF3A2522),
        Color(0xFF2C2638),
        Color(0xFF2C2A23),
    ),
)

fun marksFor(dark: Boolean): Marks = if (dark) DarkMarks else LightMarks

val LocalMarks = staticCompositionLocalOf { LightMarks }

val MaterialTheme.marks: Marks
    @Composable
    @ReadOnlyComposable
    get() = LocalMarks.current

/** Page content is set in a serif so it reads as the page, while the app itself stays sans. */
object Reading {
    val body = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 26.sp)
    val snippet = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 16.5.sp, lineHeight = 24.sp)
    val heading = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp)
    val small = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)
}
