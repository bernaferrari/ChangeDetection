/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.repo.Site
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChangeAlertDeliveryTest {
    private val page = Site("Page", "https://example.test", 0L, "", "page", 0 to 0)

    @Test fun olderPreparationCannotReplaceNewerAlertEvenIfItIgnoresCancellation() = runTest {
        val posted = mutableListOf<String>()
        val delivery = ChangeAlertDelivery(backgroundScope, prepare = { _, id ->
            if (id == "A") withContext(NonCancellable) { delay(1000) }
            id
        }, publish = { _, id, _ -> posted.add(id) })
        delivery.deliver(page, "A")
        runCurrent()
        delivery.deliver(page, "B")
        runCurrent()
        advanceTimeBy(2000)
        runCurrent()
        assertEquals(listOf("B"), posted)
    }

    @Test fun boundedPreparationPostsOnceAndNeverRevivesADismissedAlert() = runTest {
        val posted = mutableListOf<String?>()
        val delivery = ChangeAlertDelivery(backgroundScope, prepare = { _, _ ->
            delay(10_000)
            "Late"
        }, publish = { _, _, summary -> posted.add(summary) })
        delivery.deliver(page, "A")
        runCurrent()
        advanceTimeBy(1501)
        runCurrent()
        assertEquals(listOf<String?>(null), posted)
        posted.clear() // The user dismisses the notification.
        advanceTimeBy(15_000)
        runCurrent()
        assertTrue(posted.isEmpty())
    }
}
