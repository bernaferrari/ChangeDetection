/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.preview

import com.bernaferrari.changedetection.image.ChangedBoundsTracker
import com.bernaferrari.changedetection.ui.comparison.ReaderBlock
import com.bernaferrari.changedetection.ui.comparison.readerBlocks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangePreviewTest {
    private fun List<EditSegment>.render() = joinToString("") {
        when (it.kind) {
            EditSegment.Kind.Same -> it.text
            EditSegment.Kind.Removed -> "[-${it.text}-]"
            EditSegment.Kind.Added -> "{+${it.text}+}"
        }
    }

    @Test fun neighbouringWordEditsReadAsOneReplacement() {
        val diff = wordDiff("Applications close 31 October, 23:59 CET", "Applications close 14 November, 23:59 CET")
        assertEquals("Applications close [-31 October,-] {+14 November,+} 23:59 CET", diff.render())
    }

    @Test fun insertionsAndDeletionsPreserveWordSeparators() {
        assertEquals("Price [-10 -]USD", wordDiff("Price 10 USD", "Price USD").render())
        assertEquals("Price {+10 +}USD", wordDiff("Price USD", "Price 10 USD").render())
    }

    @Test fun blankLineOnlyChangesDoNotCreateEmptyPreviewsOrReaderCards() {
        assertEquals(ChangePreview.Invisible, textPreview(listOf("Same", ""), listOf("Same")))
        assertTrue(readerBlocks(listOf("Same", ""), listOf("Same"), emptySet()).none { it is ReaderBlock.Change })
    }

    @Test fun rewrittenSentencesShowOldThenNew() {
        val diff = wordDiff("Results will be published at 12:00.", "Results are now available.")
        assertTrue(diff.render(), diff.first().kind == EditSegment.Kind.Removed || diff.any { it.kind == EditSegment.Kind.Added })
        assertTrue(diff.render().contains("{+"))
    }

    @Test fun smallTextChangeBecomesABoundedSnippetWithItsHeading() {
        val old = listOf("Key dates", "Applications close 31 October", "Funding", "Each fellow receives €18,000")
        val new = listOf("Key dates", "Applications close 14 November", "Funding", "Each fellow receives €20,000")
        val preview = textPreview(old, new, setOf("Key dates", "Funding")) as ChangePreview.Words
        assertEquals("Key dates", preview.section)
        assertEquals(1, preview.otherChanges)
        assertTrue(preview.segments.render().contains("{+14 November+}"))
    }

    @Test fun hugeChangesAreSummarisedInsteadOfQuoted() {
        val old = (1..40).map { "Listing $it · 900 €" }
        val new = (1..40).map { "Listing ${it + 100} · 1 200 €" }
        val preview = textPreview(old, new)
        assertTrue(preview is ChangePreview.Large)
    }

    @Test fun previewSkipsAnEarlierIsolatedBlankLineEdit() {
        val old = listOf("Heading", "", "Unchanged sentence", "Price 10")
        val new = listOf("Heading", "Unchanged sentence", "Price 12")
        val preview = textPreview(old, new) as ChangePreview.Words
        assertTrue(preview.segments.any { it.kind == EditSegment.Kind.Added && "12" in it.text })
        assertEquals(0, preview.otherChanges)
    }

    @Test fun completeRewritesCountEachReplacedLineOnlyOnce() {
        val old = (1..40).map { "Original listing $it · 900 €" }
        val new = (1..40).map { "Replacement listing $it · 1 200 €" }
        val preview = textPreview(old, new) as ChangePreview.Large
        assertEquals(40, preview.changedLines)
        assertEquals(40, preview.totalLines)
    }

    @Test fun identicalReadableTextIsInvisible() {
        assertEquals(ChangePreview.Invisible, textPreview(listOf("Same"), listOf("Same")))
    }

    @Test fun longPassagesAreTrimmedAroundTheFirstEdit() {
        val prefix = "word ".repeat(80)
        val preview = textPreview(listOf(prefix + "old ending"), listOf(prefix + "new ending")) as ChangePreview.Words
        assertTrue(preview.leadingEllipsis)
        assertTrue(preview.segments.sumOf { it.text.length } < 260)
    }

    @Test fun readerFoldsUnchangedRunsBetweenChanges() {
        val old = listOf("Intro") + (1..20).map { "Line $it" } + listOf("Price 10", "Outro")
        val new = listOf("Intro") + (1..20).map { "Line $it" } + listOf("Price 12", "Outro")
        val blocks = readerBlocks(old, new, emptySet())
        val changes = blocks.filterIsInstance<ReaderBlock.Change>()
        assertEquals(1, changes.size)
        assertEquals(listOf("Price 10"), changes.single().before)
        assertEquals(listOf("Price 12"), changes.single().after)
        assertTrue(blocks.first() is ReaderBlock.Unchanged)
    }

    @Test fun changedBoundsCoverOnlyDifferingPixels() {
        val tracker = ChangedBoundsTracker(width = 10)
        tracker.add(2 * 10 + 3)
        tracker.add(5 * 10 + 7)
        val bounds = requireNotNull(tracker.result(height = 10))
        assertEquals(0.3f, bounds.left, 0.001f)
        assertEquals(0.2f, bounds.top, 0.001f)
        assertEquals(0.8f, bounds.right, 0.001f)
        assertEquals(0.6f, bounds.bottom, 0.001f)
        assertNull(ChangedBoundsTracker(4).result(4))
    }
}
