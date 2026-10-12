package com.bernaferrari.changedetection.ui.main

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UndoableDeletionTest {
    private class Harness(scope: TestScope, delete: suspend (String) -> Unit = {}) {
        val hidden = mutableSetOf<String>()
        val deleted = mutableListOf<String>()
        val failures = mutableListOf<Throwable>()
        val owner = UndoableDeletion(
            scope = scope,
            delete = { value: String ->
                delete(value)
                deleted += value
            },
            onHidden = { hidden += it },
            onRestored = { hidden -= it },
            onDeleted = { hidden -= it },
            onFailure = { _, error -> failures += error },
        )
    }

    @Test fun `empty and already resolved batches finish without opening a decision`() = runTest {
        val h = Harness(this)
        h.owner.resolveAll(emptyList()) { error("No pages") }
        val ticket = h.owner.stage("page")
        h.owner.resolve(ticket) { true }
        h.owner.resolveAll(listOf(ticket)) { error("Already decided") }
        runCurrent()
        assertTrue(h.deleted.isEmpty())
    }

    @Test fun `visible accessible undo remains valid beyond fallback deadline`() = runTest {
        val h = Harness(this)
        val ticket = h.owner.stage("page")
        val decision = CompletableDeferred<Boolean>()
        val visible = async { h.owner.resolve(ticket) { decision.await() } }
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()
        assertTrue(h.deleted.isEmpty())
        decision.complete(true)
        visible.await()
        advanceTimeBy(6_001)
        runCurrent()
        assertTrue(h.deleted.isEmpty())
        assertTrue(h.hidden.isEmpty())
    }

    @Test fun `queued batch decision owns every ticket until its accessible snackbar ends`() = runTest {
        val h = Harness(this)
        val tickets = listOf("first", "second", "third").map(h.owner::stage)
        val decision = CompletableDeferred<Boolean>()
        var shown = 0
        val home = async {
            h.owner.resolveAll(tickets) {
                shown++
                decision.await()
            }
        }
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()
        assertTrue(h.deleted.isEmpty())
        assertEquals(1, shown)
        decision.complete(true)
        home.await()
        advanceTimeBy(6_001)
        runCurrent()
        assertTrue(h.deleted.isEmpty())
        assertTrue(h.hidden.isEmpty())
    }

    @Test fun `leaving screen gives pending deletion an application owned fallback`() = runTest {
        val h = Harness(this)
        val ticket = h.owner.stage("page")
        val screen = async { h.owner.resolve(ticket) { awaitCancellation() } }
        runCurrent()
        screen.cancel()
        runCurrent()
        advanceTimeBy(5_999)
        runCurrent()
        assertTrue(h.deleted.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("page"), h.deleted)
        assertTrue(h.hidden.isEmpty())
    }

    @Test fun `expired ticket cannot offer undo or commit twice`() = runTest {
        val h = Harness(this)
        val ticket = h.owner.stage("page")
        advanceTimeBy(6_000)
        runCurrent()
        h.owner.resolve(ticket) {
            fail("Expired ticket must not display Undo")
            true
        }
        runCurrent()
        assertEquals(listOf("page"), h.deleted)
    }

    @Test fun `commit survives screen cancellation and rejects late undo`() = runTest {
        val save = CompletableDeferred<Unit>()
        val h = Harness(this) { save.await() }
        val ticket = h.owner.stage("page")
        val screen = async { h.owner.resolve(ticket) { false } }
        runCurrent()
        screen.cancel()
        h.owner.resolve(ticket) {
            fail("Cannot undo a committed request")
            true
        }
        assertEquals(setOf("page"), h.hidden)
        save.complete(Unit)
        runCurrent()
        assertEquals(listOf("page"), h.deleted)
        assertTrue(h.hidden.isEmpty())
    }

    @Test fun `failed delete restores visibility and explicit retry can succeed`() = runTest {
        var failing = true
        val h = Harness(this) { if (failing) error("Database unavailable") }
        h.owner.resolve(h.owner.stage("page")) { false }
        runCurrent()
        assertEquals(1, h.failures.size)
        assertTrue(h.hidden.isEmpty())
        assertTrue(h.deleted.isEmpty())
        failing = false
        h.owner.retry("page")
        runCurrent()
        assertEquals(listOf("page"), h.deleted)
        assertTrue(h.hidden.isEmpty())
    }
}
