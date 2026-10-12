package com.bernaferrari.changedetection.background

enum class MonitoringExecution { Background, WhileOpen, Unavailable }

internal data class MonitoringCapabilities(val execution: MonitoringExecution, val constraints: Boolean = false, val turbo: Boolean = true, val notifications: Boolean = false, val osScheduled: Boolean = false)

internal expect val monitoringCapabilities: MonitoringCapabilities
