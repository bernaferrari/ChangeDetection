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

    @Test fun counterOnlyChangesAreLabelledAsNumbers() {
        val preview = textPreview(listOf("Story", "76 points by ana 1 hour ago | 36 comments"), listOf("Story", "80 points by ana 2 hour ago | 41 comments")) as ChangePreview.Words
        assertTrue(preview.numbersOnly)
    }

    @Test fun meaningfulWordsLeadOverCounterChurn() {
        val old = listOf("12 comments", "Price", "Ships in 2 weeks")
        val new = listOf("15 comments", "Price", "In stock today")
        val preview = textPreview(old, new) as ChangePreview.Words
        assertTrue(preview.segments.render(), preview.segments.render().contains("{+In stock today+}"))
        assertTrue(!preview.numbersOnly)
    }

    @Test fun largeNoisyChangesCarryReadableSamples() {
        val old = (1..30).map { "Story $it about something · ${it * 3} points" }
        val new = (1..30).map { if (it % 3 == 0) "Brand new headline number $it · 1 point" else "Story $it about something · ${it * 3 + 2} points" }
        val preview = textPreview(old, new) as ChangePreview.Large
        assertEquals(3, preview.samples.size)
        assertTrue(preview.samples.all { sample -> sample.any { it.kind == EditSegment.Kind.Added && it.text.contains("headline") } })
        assertEquals(10, preview.added)
    }

    @Test fun shiftedListsReportOnlyGenuinelyNewLines() {
        val words = listOf("apple", "bridge", "cloud", "desert", "engine", "forest", "garden", "harbor", "island", "jungle", "kettle", "lantern", "meadow", "needle", "orchard", "pepper", "quartz", "river", "saddle", "tunnel")
        val stories = words.map { "A long story about the $it and friends" }
        val old = stories.flatMap { listOf(it, "${it.length} points · 3 hours ago") }
        val newStories = listOf("A completely fresh headline today", "Another brand new story appears") + stories.dropLast(2)
        val new = newStories.flatMap { listOf(it, "${it.length + 5} points · 3 hours ago") }
        val preview = textPreview(old, new) as ChangePreview.Large
        assertEquals(2, preview.added)
        assertEquals(2, preview.removed)
        assertTrue(preview.samples.first().single().kind == EditSegment.Kind.Added)
    }

    @Test fun resortedListsSayOnlyTheOrderChanged() {
        val items = (1..20).map { "Listing ${'a' + it} · ${900 + it} €" }
        val preview = textPreview(items, items.reversed()) as ChangePreview.Large
        assertTrue(preview.reordered)
    }

    @Test fun relativeTimesTickLikeNumbers() {
        assertTrue(onlyNumbersDiffer("253 points by ana 12 minutes ago | 72 comments", "260 points by ana 3 hours ago | 80 comments"))
        assertTrue(!onlyNumbersDiffer("Ships in 2 weeks", "In stock today"))
    }

    @Test fun priceChangesLeadEvenWhenOtherTextChanged() {
        val preview = textPreview(listOf("Price", "$1,599.00", "Availability", "Ships in 2 weeks"), listOf("Price", "$1,349.00", "Availability", "In stock today")) as ChangePreview.Words
        assertTrue(preview.segments.render(), preview.segments.render().contains("{+$1,349.00+}"))
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
