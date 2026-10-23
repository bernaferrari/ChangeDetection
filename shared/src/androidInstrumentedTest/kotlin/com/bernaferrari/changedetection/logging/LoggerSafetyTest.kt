package com.bernaferrari.changedetection.logging

import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.After
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class LoggerSafetyTest {
    @After fun restore() {
        AppLog.configureDebugLogging(false)
    }

    @Test fun loggingOnANineFrameThreadMustNotThrow() {
        AppLog.configureDebugLogging(true)
        val failure = AtomicReference<Throwable?>()
        val thread = object : Thread() {
            override fun getStackTrace(): Array<StackTraceElement> = Array(9) { StackTraceElement("Fixture", "frame", "Fixture.kt", it) }
            override fun run() {
                try {
                    co.touchlab.kermit.Logger(co.touchlab.kermit.StaticConfig(logWriterList = listOf(co.touchlab.kermit.LogcatWriter())), "LoggerSafety").d("Direct fixed-tag Kermit boundary fixture")
                    AppLog.tagged("LoggerSafety").d("Inert logger boundary fixture")
                } catch (error: Throwable) {
                    failure.set(error)
                }
            }
        }
        thread.start()
        thread.join(5_000)
        assertFalse("Logger thread must finish", thread.isAlive)
        failure.get()?.printStackTrace()
        assertNull("Logging must not fail a successful check", failure.get())
    }
}
