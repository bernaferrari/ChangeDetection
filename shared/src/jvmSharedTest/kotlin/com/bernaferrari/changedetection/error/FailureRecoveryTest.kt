package com.bernaferrari.changedetection.error

import com.bernaferrari.changedetection.repo.CaptureMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FailureRecoveryTest {
    private val pictureProblems = listOf(CheckFailure.PAGE_UNSETTLED, CheckFailure.CAPTURE_INCOMPLETE, CheckFailure.CAPTURE_UNAVAILABLE, CheckFailure.CONTENT_TOO_LARGE)
    private val refusals = listOf(CheckFailure.LOGIN, CheckFailure.ACCESS_DENIED, CheckFailure.CONTENT)

    @Test fun aPictureThatCannotBeTakenCanStillBeRead() {
        for (failure in pictureProblems) {
            for (mode in CaptureMode.entries) assertEquals("$failure in $mode", mode != CaptureMode.TEXT, textMayWork(mode, failure, repeated = false))
        }
    }

    @Test fun oneTimeoutIsTheNetworkAndOnlyARepeatIsTheRoute() {
        for (mode in CaptureMode.entries) {
            assertFalse(mode.name, textMayWork(mode, CheckFailure.TIMEOUT, repeated = false))
            assertEquals(mode.name, mode != CaptureMode.TEXT, textMayWork(mode, CheckFailure.TIMEOUT, repeated = true))
        }
    }

    @Test fun aRefusalOutsideTheBrowserMayBeAcceptedInsideIt() {
        for (failure in refusals) {
            assertTrue(failure.name, textMayWork(CaptureMode.SOURCE, failure, repeated = false))
            assertTrue(failure.name, textMayWork(CaptureMode.HTML, failure, repeated = false))
            // These already go through the app's browser, so Text would be the same attempt again.
            assertFalse(failure.name, textMayWork(CaptureMode.SCREENSHOT, failure, repeated = true))
            assertFalse(failure.name, textMayWork(CaptureMode.DEFAULT, failure, repeated = true))
            assertFalse(failure.name, textMayWork(CaptureMode.TEXT, failure, repeated = true))
        }
    }

    @Test fun aPageThatIsDownOrGoneFailsByEveryRoute() {
        for (failure in CheckFailure.entries - pictureProblems.toSet() - refusals.toSet() - CheckFailure.TIMEOUT) {
            for (mode in CaptureMode.entries) assertFalse("$failure in $mode", textMayWork(mode, failure, repeated = true))
        }
    }
}
