package com.bernaferrari.changedetection.ui.comparison

import com.bernaferrari.changedetection.util.readableSnapshotText

import org.junit.Assert.assertEquals
import org.junit.Test

class ComparisonDiffTest {
    @Test fun readableHtmlKeepsParagraphsAndDecodesEntities() {
        assertEquals("Tickets & prices\nOnly 2 left", readableSnapshotText("<h1>Tickets &amp; prices</h1><p>Only <b>2</b> left</p>", true))
    }

    @Test fun readableHtmlExcludesScriptsStylesAndExplicitlyHiddenText() {
        assertEquals("Open", readableSnapshotText("<style>body{color:red}</style><script>secret()</script><p hidden>Hidden</p><p aria-hidden='true'>Hidden</p><p>Open</p>", true))
    }

    @Test fun nestedBlocksAndTableCellsKeepWordBoundaries() {
        assertEquals("Start\nMiddle\nEnd\nA B", readableSnapshotText("<div>Start<p>Middle</p>End</div><table><tr><td>A</td><td>B</td></tr></table>", true))
    }

    @Test fun plainTextIsNeverParsedAsMarkup() {
        val source = "a < b\n<example>literal</example>"
        assertEquals(source, readableSnapshotText(source, false))
    }

    @Test fun reorderingAndRepetitionRemainVisibleToTheDiff() {
        val before = readableSnapshotText("<p>red blue red</p>", true)
        val after = readableSnapshotText("<p>blue red blue</p>", true)
        assertEquals(listOf(DiffLineType.REMOVED, DiffLineType.ADDED), calculateDiffRows(before.lines(), after.lines()).map { it.type })
    }

    @Test
    fun insertedLineDoesNotMarkFollowingLinesAsChanged() {
        val rows = calculateDiffRows(
            oldLines = listOf("alpha", "bravo", "charlie"),
            newLines = listOf("alpha", "inserted", "bravo", "charlie"),
        )

        assertEquals(
            listOf(
                DiffLineType.UNCHANGED,
                DiffLineType.ADDED,
                DiffLineType.UNCHANGED,
                DiffLineType.UNCHANGED,
            ),
            rows.map { it.type },
        )
        assertEquals("inserted", rows.single { it.type == DiffLineType.ADDED }.content)
    }

    @Test
    fun replacementProducesRemovalAndAddition() {
        val rows = calculateDiffRows(
            oldLines = listOf("before"),
            newLines = listOf("after"),
        )

        assertEquals(
            listOf(DiffLineType.REMOVED, DiffLineType.ADDED),
            rows.map { it.type },
        )
    }
}
