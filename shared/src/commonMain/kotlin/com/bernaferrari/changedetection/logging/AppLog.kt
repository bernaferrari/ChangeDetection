package com.bernaferrari.changedetection.logging

import co.touchlab.kermit.Logger
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import co.touchlab.kermit.platformLogWriter

/** Operational logs are optional. They never determine a check result or feed diagnostic exports. */
object AppLog {
    @kotlin.concurrent.Volatile private var logger = Logger(StaticConfig(logWriterList = emptyList()), "ChangeDetection")

    /** Release defaults to no text logging: existing messages may contain page addresses or content. */
    fun configureDebugLogging(enabled: Boolean) {
        configure(if (enabled) listOf(platformLogWriter()) else emptyList())
    }

    internal fun configure(writers: List<LogWriter>) {
        logger = Logger(StaticConfig(minSeverity = Severity.Debug, logWriterList = writers), "ChangeDetection")
    }

    fun tagged(tag: String): TaggedLog = TaggedLog(tag)

    internal fun emit(severity: Severity, message: String, error: Throwable?, tag: String) {
        try {
            logger.log(severity, tag, error, message)
        } catch (_: Exception) { /* A broken optional log writer must never fail monitoring or its error handler. */ }
    }
}

class TaggedLog internal constructor(private val defaultTag: String) {
    fun d(message: String, throwable: Throwable? = null, tag: String = defaultTag) = AppLog.emit(Severity.Debug, message, throwable, tag)
    fun i(message: String, throwable: Throwable? = null, tag: String = defaultTag) = AppLog.emit(Severity.Info, message, throwable, tag)
    fun w(message: String, throwable: Throwable? = null, tag: String = defaultTag) = AppLog.emit(Severity.Warn, message, throwable, tag)
    fun e(message: String, throwable: Throwable? = null, tag: String = defaultTag) = AppLog.emit(Severity.Error, message, throwable, tag)
}
