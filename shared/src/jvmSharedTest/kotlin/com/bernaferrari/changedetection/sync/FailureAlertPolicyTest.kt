package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.error.CheckFailure
import com.bernaferrari.changedetection.error.FailureUrgency
import com.bernaferrari.changedetection.repo.Site
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FailureAlertPolicyTest {
    private val hour = 60 * 60 * 1000L
    private val now = 100 * hour

    private fun failing(failure: CheckFailure, count: Int, hoursAgo: Long) = Site("Page", "https://example.test", now, "", "page", 0 to 0)
        .copy(isSuccessful = false, isSyncEnabled = true, isNotificationEnabled = true, lastFailureCode = failure.name, failureCount = count, failingSince = now - hoursAgo * hour)

    @Test fun aProblemOnlyThePersonCanFixAlertsOnceItIsConfirmed() {
        assertFalse("one odd answer is not a verdict", shouldAlertFailure(failing(CheckFailure.LOGIN, 1, 0), now))
        assertTrue(shouldAlertFailure(failing(CheckFailure.LOGIN, 2, 0), now))
        assertTrue(shouldAlertFailure(failing(CheckFailure.NOT_FOUND, 2, 0), now))
        assertTrue(shouldAlertFailure(failing(CheckFailure.REGION, 5, 30), now))
    }

    @Test fun aProblemThatMayPassStaysQuietUntilItHasLasted() {
        assertFalse("a bad hour is not worth an interruption", shouldAlertFailure(failing(CheckFailure.SERVER, 8, 5), now))
        assertTrue(shouldAlertFailure(failing(CheckFailure.SERVER, 3, 6), now))
        assertFalse("two failures need a full day between them", shouldAlertFailure(failing(CheckFailure.TIMEOUT, 2, 23), now))
        assertTrue("a slow schedule still reports a day without a successful check", shouldAlertFailure(failing(CheckFailure.TIMEOUT, 2, 24), now))
        assertFalse(shouldAlertFailure(failing(CheckFailure.CONNECTION, 1, 72), now))
    }

    @Test fun theDeviceBeingTheCauseNeverBlamesThePage() {
        for (failure in CheckFailure.entries.filter { it.urgency == FailureUrgency.DEVICE }) {
            assertFalse(failure.name, shouldAlertFailure(failing(failure, 50, 500), now))
        }
    }

    @Test fun oneAlertPerRunAndNoneWhereThePersonAskedForQuiet() {
        val due = failing(CheckFailure.LOGIN, 4, 10)
        assertTrue(shouldAlertFailure(due, now))
        assertFalse("already said", shouldAlertFailure(due.copy(failureAlerted = true), now))
        assertFalse("notifications are off for this page", shouldAlertFailure(due.copy(isNotificationEnabled = false), now))
        assertFalse("a paused page is not expected to be checked", shouldAlertFailure(due.copy(isSyncEnabled = false), now))
        assertFalse("it works again", shouldAlertFailure(due.copy(isSuccessful = true), now))
        assertFalse("no run on record", shouldAlertFailure(due.copy(failingSince = null), now))
    }

    @Test fun everyFailureHasADeliberateUrgency() {
        assertEquals(
            setOf(CheckFailure.BACKGROUND_RESTRICTED, CheckFailure.STORAGE_FULL, CheckFailure.SAVE_FAILED),
            CheckFailure.entries.filter { it.urgency == FailureUrgency.DEVICE }.toSet(),
        )
        assertEquals(
            setOf(
                CheckFailure.LOGIN,
                CheckFailure.NOT_FOUND,
                CheckFailure.CONTENT_TOO_LARGE,
                CheckFailure.CONTENT,
                CheckFailure.CAPTURE_UNAVAILABLE,
                CheckFailure.REGION,
                CheckFailure.MULTIPLE_VISUAL_REGIONS,
            ),
            CheckFailure.entries.filter { it.urgency == FailureUrgency.NEEDS_YOU }.toSet(),
        )
    }
}
