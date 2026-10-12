@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.bernaferrari.changedetection.background

import kotlinx.cinterop.*
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.Foundation.NSError
import kotlin.test.*

class BackgroundSubmissionTest {
    @Test fun appleRejectionWithAnErrorIsReported() {
        val failure = assertFailsWith<IllegalStateException> {
            submitBackgroundRequest(BGAppRefreshTaskRequest("test.periodic")) { _, error ->
                error.pointed.value = NSError.errorWithDomain("BGTaskSchedulerErrorDomain", 1, null)
                false
            }
        }
        assertTrue(failure.message!!.contains("BGTaskSchedulerErrorDomain (1)"))
        assertTrue(failure.message!!.contains("test.periodic"))
    }

    @Test fun rejectionWithoutAnErrorIsStillAFailure() {
        assertFailsWith<IllegalStateException> {
            submitBackgroundRequest(BGAppRefreshTaskRequest("test.quick")) { _, _ -> false }
        }
    }

    @Test fun acceptedRequestCompletesNormally() {
        var submitted = false
        submitBackgroundRequest(BGAppRefreshTaskRequest("test.periodic")) { request, _ ->
            submitted = request.identifier == "test.periodic"
            true
        }
        assertTrue(submitted)
    }
}
