package com.bernaferrari.changedetection.background

enum class MonitoringExecution { Background, WhileOpen, Unavailable }

internal data class MonitoringCapabilities(
    val execution: MonitoringExecution,
    val constraints: Boolean = false,
    val turbo: Boolean = true,
    val notifications: Boolean = false,
    val osScheduled: Boolean = false,
    /** Text checks load the page in the app's browser, so they are a different route from a Source or Screenshot check. */
    val browserText: Boolean = false,
)

internal expect val monitoringCapabilities: MonitoringCapabilities
