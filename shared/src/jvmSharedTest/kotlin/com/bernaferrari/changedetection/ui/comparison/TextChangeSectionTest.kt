/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import org.junit.Assert.*
import org.junit.Test

class TextChangeSectionTest {
    @Test fun `separate source sections retain their own before and after`() {
        val old = listOf("Morning", "Starts at 09:00", "Afternoon", "20 places")
        val new = listOf("Morning", "Starts at 10:00", "Afternoon", "12 places")
        val sections = textChangeSections(displayDiffRows(calculateDiffRows(old, new)))
        assertEquals(2, sections.size)
        assertEquals("Morning", sections[0].context)
        assertEquals(listOf("Starts at 09:00"), sections[0].before.map { it.content })
        assertEquals(listOf("Starts at 10:00"), sections[0].after.map { it.content })
        assertEquals("Afternoon", sections[1].context)
        assertEquals(listOf("20 places"), sections[1].before.map { it.content })
        assertEquals(listOf("12 places"), sections[1].after.map { it.content })
    }

    @Test fun `HTML section headings take precedence over adjacent unchanged body text`() {
        val rows = calculateDiffRows(listOf("Morning sessions", "09:00 Doors open", "10:00 Opening"), listOf("Morning sessions", "09:00 Doors open", "10:30 Opening"))
        assertEquals("Morning sessions", textChangeSections(rows, setOf("Morning sessions")).single().context)
    }

    @Test fun `long text stays in one passage while layout fragments remain bounded`() {
        val old = "a".repeat(12_000)
        val new = "b".repeat(14_000)
        val section = textChangeSections(displayDiffRows(calculateDiffRows(listOf(old), listOf(new)))).single()
        assertEquals(old, section.before.joinToString("") { it.content })
        assertEquals(new, section.after.joinToString("") { it.content })
        assertTrue((section.before + section.after).all { it.content.length <= 2048 })
    }

    @Test fun `navigation anchors account for expanded text and deletion defaults`() {
        val row = DiffRowModel(1, "text", DiffLineType.ADDED)
        val sections = listOf(
            TextChangeSection("A", listOf(row, row), listOf(row)),
            TextChangeSection("B", listOf(row), emptyList()),
            TextChangeSection("C", emptyList(), listOf(row, row, row)),
        )
        assertEquals(listOf(1, 6, 12), passageItemIndices(sections, emptyMap()))
        assertEquals(listOf(1, 8, 13), passageItemIndices(sections, mapOf(0 to true, 1 to false)))
    }

    @Test fun `pure deletion does not borrow text from a later addition`() {
        val sections = textChangeSections(calculateDiffRows(listOf("Removed", "Kept"), listOf("Kept", "Added")))
        assertEquals(2, sections.size)
        assertEquals("Removed", sections[0].before.single().content)
        assertTrue(sections[0].after.isEmpty())
        assertTrue(sections[1].before.isEmpty())
        assertEquals("Added", sections[1].after.single().content)
    }
}
