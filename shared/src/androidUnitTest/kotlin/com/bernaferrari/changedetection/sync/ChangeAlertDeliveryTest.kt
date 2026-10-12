package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.repo.Site
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChangeAlertDeliveryTest {
    private val page = Site("Page", "https://example.test", 0L, "", "page", 0 to 0)

    @Test fun callerCancellationStopsPreparationAndDoesNotLeaveAnUnownedAlert() = runTest {
        val posted = mutableListOf<String>()
        val delivery = ChangeAlertDelivery(prepare = { _, id ->
            if (id == "A") delay(10_000)
            id
        }, publish = { _, id, _ -> posted.add(id) })
        val owner = async { delivery.deliver(page, "A") }
        runCurrent()
        owner.cancelAndJoin()
        assertTrue(posted.isEmpty())
        delivery.deliver(page, "B")
        assertEquals(listOf("B"), posted)
    }

    @Test fun asynchronousPublicationIsAcknowledgedBeforeTheOwnerCompletes() = runTest {
        val submitted = CompletableDeferred<Unit>()
        val acknowledged = CompletableDeferred<Unit>()
        val delivery = ChangeAlertDelivery(prepare = { _, _ -> null }, publish = { _, _, _ ->
            submitted.complete(Unit)
            acknowledged.await()
        })
        val owner = async { delivery.deliver(page, "A") }
        submitted.await()
        assertFalse(owner.isCompleted)
        acknowledged.complete(Unit)
        owner.await()
    }

    @Test fun deliveryBelongsToTheCallingOperationUntilItHasPosted() = runTest {
        val gate = CompletableDeferred<Unit>()
        val posted = mutableListOf<String>()
        val delivery = ChangeAlertDelivery(prepare = { _, id ->
            gate.await()
            id
        }, publish = { _, id, _ -> posted.add(id) })
        val owner = async { delivery.deliver(page, "A") }
        runCurrent()
        assertFalse("The owning worker must not finish before posting", owner.isCompleted)
        assertTrue(posted.isEmpty())
        gate.complete(Unit)
        owner.await()
        assertEquals(listOf("A"), posted)
    }

    @Test fun olderPreparationCannotReplaceNewerAlertEvenIfItIgnoresCancellation() = runTest {
        val posted = mutableListOf<String>()
        val delivery = ChangeAlertDelivery(prepare = { _, id ->
            if (id == "A") withContext(NonCancellable) { delay(1000) }
            id
        }, publish = { _, id, _ -> posted.add(id) })
        val first = async { delivery.deliver(page, "A") }
        runCurrent()
        val second = async { delivery.deliver(page, "B") }
        runCurrent()
        advanceTimeBy(2000)
        runCurrent()
        first.await()
        second.await()
        assertEquals(listOf("B"), posted)
    }

    @Test fun boundedPreparationPostsOnceAndNeverRevivesADismissedAlert() = runTest {
        val posted = mutableListOf<String?>()
        val delivery = ChangeAlertDelivery(prepare = { _, _ ->
            delay(10_000)
            "Late"
        }, publish = { _, _, summary -> posted.add(summary) })
        val first = async { delivery.deliver(page, "A") }
        runCurrent()
        advanceTimeBy(1501)
        runCurrent()
        assertEquals(listOf<String?>(null), posted)
        first.await()
        posted.clear() // The user dismisses the notification.
        advanceTimeBy(15_000)
        runCurrent()
        assertTrue(posted.isEmpty())
    }
}
