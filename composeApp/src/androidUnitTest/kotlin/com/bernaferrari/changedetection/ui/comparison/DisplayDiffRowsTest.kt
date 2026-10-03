/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import org.junit.Assert.*
import org.junit.Test

class DisplayDiffRowsTest {
    @Test fun longLinesRemainCompleteWithBoundedDisplayFragments() {
        val text = "0123456789".repeat(100_000)
        val source = DiffRowModel(23, text, DiffLineType.ADDED)
        val rows = displayDiffRows(listOf(source))
        assertEquals(text, rows.joinToString("") { it.content })
        assertTrue(rows.all { it.content.length <= 2_048 && it.lineNumber == 23 && it.type == DiffLineType.ADDED })
        assertFalse(rows.first().isContinuation)
        assertTrue(rows.drop(1).all { it.isContinuation })
    }

    @Test fun fragmentsDoNotSplitUtf16SurrogatePairs() {
        val text = "abc😀def🌍ghi"
        val rows = displayDiffRows(listOf(DiffRowModel(1, text, DiffLineType.REMOVED)), 4)
        assertEquals(text, rows.joinToString("") { it.content })
        assertTrue(rows.all { !it.content.first().isLowSurrogate() && !it.content.last().isHighSurrogate() })
    }

    @Test fun manyBlankLinesCannotCreateAnUnboundedHeightTextLayout() {
        val text = "\n".repeat(2_000)
        val rows = displayDiffRows(listOf(DiffRowModel(1, text, DiffLineType.UNCHANGED)))
        assertEquals(text, rows.joinToString("") { it.content })
        assertTrue(rows.all { it.content.count { char -> char == '\n' } <= 64 })
    }

    @Test fun ordinaryAndEmptyLinesKeepTheirIdentity() {
        val rows = listOf(DiffRowModel(1, "", DiffLineType.UNCHANGED), DiffRowModel(2, "hello", DiffLineType.ADDED))
        assertEquals(rows, displayDiffRows(rows))
    }
}
