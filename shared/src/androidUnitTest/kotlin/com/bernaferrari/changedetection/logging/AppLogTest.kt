package com.bernaferrari.changedetection.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class AppLogTest {
    @After fun restore() {
        AppLog.configureDebugLogging(false)
    }

    @Test fun explicitTagsAndThrowableArePassedWithoutStackInspection() {
        val events = mutableListOf<Pair<String, Throwable?>>()
        AppLog.configure(
            listOf(object : LogWriter() {
                override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
                    events += tag to throwable
                }
            }),
        )
        val error = IllegalStateException("fixture")
        AppLog.tagged("SyncEngine").d("unchanged")
        AppLog.tagged("SyncEngine").e("failed", error, tag = "ErrorHandler")
        assertEquals(listOf("SyncEngine", "ErrorHandler"), events.map { it.first })
        assertSame(error, events.last().second)
    }

    @Test fun releaseConfigurationRemovesTextWriters() {
        var calls = 0
        AppLog.configure(
            listOf(object : LogWriter() {
                override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
                    calls++
                }
            }),
        )
        AppLog.tagged("Fixture").e("before")
        assertEquals(1, calls)
        AppLog.configureDebugLogging(false)
        AppLog.tagged("Fixture").e("https://private.invalid?token=SECRET", IllegalStateException("cookie=SECRET"))
        assertEquals(1, calls)
    }

    @Test fun aThrowingWriterCannotBreakAnyLogLevelOrErrorHandling() {
        var calls = 0
        AppLog.configure(
            listOf(object : LogWriter() {
                override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
                    calls++
                    throw ArrayIndexOutOfBoundsException("fixture")
                }
            }),
        )
        val log = AppLog.tagged("Fixture")
        log.d("unchanged")
        log.i("baseline")
        log.w("changed")
        log.e("failed", IllegalStateException())
        assertEquals(4, calls)
    }
}
