/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.detection

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RegionSelectionDraftTest {
    @Test fun selectingARestoredAreaLetsScopeReplaceItInsteadOfRetainingTheBroaderRule() = runBlocking {
        val document = selectableTextDocument("<main id='main'><p id='stock'>Available</p></main><p id='other'>Price</p>")
        val draft = RegionSelectionDraft.fromRule(TextRegion("#main, #other"))
        val narrowed = draft.choose("#main", document).smaller(document)
        assertEquals(TextRegion("#other, #stock"), narrowed.rule)
        assertEquals(draft.rule, narrowed.undo().undo().rule)
    }

    @Test fun reopeningPreservesComplexSelectorGroupsAndExactUnchangedRule() = runBlocking {
        val rule = TextRegion("p[data-code='a,b'],section:not(:has(.clock,.ad))", ".clock,.ad")
        val draft = RegionSelectionDraft.fromRule(rule)
        assertEquals(listOf("p[data-code='a,b']", "section:not(:has(.clock,.ad))"), draft.areas)
        assertEquals(listOf(".clock", ".ad"), draft.ignored)
        assertEquals(rule, draft.rule)
        assertFalse(draft.canUndo)
        val document = selectableTextDocument("<p id='stock'>Available</p>")
        assertEquals(rule, draft.choose("#stock", document).undo().rule)
        assertEquals(listOf("#a\\,b", "[title=\"a,b\"]"), selectorGroups("#a\\,b, [title=\"a,b\"]"))
    }

    @Test fun boundedUndoDoesNotResurrectTheOriginalRuleAfterHistoryWasTrimmed() = runBlocking {
        val document = selectableTextDocument("<p id='a'>A</p><p id='b'>B</p>")
        val initial = RegionSelectionDraft.fromRule(TextRegion("#a,#b"))
        var draft = initial
        repeat(110) { draft = draft.choose(if (it % 2 == 0) "#a" else "#b", document) }
        repeat(100) { draft = draft.undo() }
        assertFalse(draft.canUndo)
        assertNotEquals(initial.rule, draft.rule)
        assertEquals(TextRegion("#b"), draft.rule)
    }

    @Test fun ignoredScopeCannotConsumeAnIndependentlyRetainedExclusion() = runBlocking {
        val document = selectableTextDocument("<main id='main'><section id='noise'><p id='clock'>Countdown</p></section><p>Available</p></main>")
        val draft = RegionSelectionDraft().choose("#main", document).toggleIgnored("#noise", document).toggleIgnored("#clock", document)
        assertNull(draft.largerTarget(document, true))
        assertEquals(draft, draft.larger(document, true))
        assertEquals(TextRegion("#main", "#noise, #clock"), draft.rule)
    }

    @Test fun ignoredScopePreservesOtherExclusionsAndUndoRestoresItsFocus() = runBlocking {
        val html = "<main id='main'><p id='stock'>Available</p><section id='noise'><p id='ad'>Ad</p><p id='clock'>Countdown</p></section><p id='other'>Other noise</p></main>"
        val document = selectableTextDocument(html)
        val draft = RegionSelectionDraft().choose("#main", document).toggleIgnored("#other", document).toggleIgnored("#clock", document)
        assertNull(draft.smallerTarget(document, true))
        val enlarged = draft.larger(document, true)
        assertEquals(TextRegion("#main", "#other, #noise"), enlarged.rule)
        assertEquals("#noise", enlarged.activeIgnored)
        assertEquals(draft.rule, enlarged.smaller(document, true).rule)
        assertEquals(draft.rule, enlarged.undo().rule)
        assertEquals("#clock", enlarged.undo().activeIgnored)
        val restored = enlarged.restoreIgnored("#noise")
        assertNull(restored.largerTarget(document, true))
        assertEquals(enlarged.activeIgnored, restored.undo().activeIgnored)
        assertEquals(TextRegion("#main", "#other"), restored.rule)
    }

    @Test fun ignoreAndRestoreArePartOfTheUndoableCaptureRule() = runBlocking {
        val html = "<main id='all'><p id='stock'>Available</p><p id='clock'>10 seconds</p></main>"
        val document = selectableTextDocument(html)
        val watch = RegionSelectionDraft().choose("#all", document)
        val ignored = watch.toggleIgnored("#clock", document)
        assertEquals(TextRegion("#all", "#clock"), ignored.rule)
        assertFalse(requireNotNull(ignored.rule).extractHtml(html, true).contains("seconds"))
        assertEquals(watch.rule, ignored.undo().rule)
        assertEquals(watch.rule, ignored.restoreIgnored("#clock").rule)
        assertEquals(ignored.rule, ignored.restoreIgnored("#clock").undo().rule)
        assertEquals(ignored, ignored.toggleIgnored("#missing", document))
    }

    @Test fun combinedSelectorLimitPreventsConfirmationAndUndoRecovers() {
        val first = "#" + "a".repeat(260)
        val second = "#" + "b".repeat(260)
        val document = SelectableTextDocument(listOf(SelectableTextRegion(first, "First", "p", 0), SelectableTextRegion(second, "Second", "p", 0)), false)
        val draft = RegionSelectionDraft().choose(first, document).addArea().choose(second, document)
        assertTrue(draft.exceedsRuleLimit)
        assertNull(draft.rule)
        assertEquals(TextRegion(first), draft.undo().rule)
        assertEquals(TextRegion(second), draft.removeArea(first).rule)
    }

    @Test fun multipleAreasCaptureInSourceOrderAndUndoRestoresTheWholeSet() = runBlocking {
        val html = "<main><p id='price'>Price 100</p><p id='noise'>Unrelated</p><p id='stock'>Available</p></main>"
        val document = selectableTextDocument(html)
        val stock = RegionSelectionDraft().choose("#stock", document)
        val pinned = stock.addArea()
        assertNull(pinned.selected)
        assertEquals(TextRegion("#stock"), pinned.rule)
        val both = pinned.choose("#price", document)
        val captured = requireNotNull(both.rule).extractHtml(html, true)
        assertFalse(captured.contains("Unrelated"))
        assertTrue(captured.indexOf("Price 100") < captured.indexOf("Available"))
        assertEquals(pinned.rule, both.undo().rule)
        assertEquals(stock.selected, both.undo().undo().selected)
        val removed = both.removeArea("#stock")
        assertEquals(TextRegion("#price"), removed.rule)
        assertEquals(both.rule, removed.undo().rule)
        assertEquals(1, pinned.choose("#stock", document).watched.size)
    }

    @Test fun scopeUsesActualAncestorsAndReturnsToChosenChild() = runBlocking {
        val document = selectableTextDocument("<main id='main'><p id='first'>First</p><aside><p id='chosen'>Chosen</p></aside></main><p id='outside'>Outside</p>")
        val draft = RegionSelectionDraft().choose("#chosen", document)
        assertNull(draft.smallerTarget(document))
        assertEquals("#main", draft.largerTarget(document))
        val larger = draft.larger(document)
        assertNull(larger.largerTarget(document))
        assertEquals("#chosen", larger.smaller(document).selected)
        assertEquals("#chosen", larger.undo().selected)
        assertNull(larger.undo().undo().selected)
        assertFalse(larger.undo().undo().canUndo)
        assertEquals(draft, draft.choose("#missing", document))
        assertEquals(draft, draft.choose("#chosen", document))
    }
}
