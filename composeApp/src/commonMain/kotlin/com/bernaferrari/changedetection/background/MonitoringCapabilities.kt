/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.background

enum class MonitoringExecution { Background, WhileOpen, Unavailable }

internal data class MonitoringCapabilities(val execution: MonitoringExecution, val constraints: Boolean = false, val turbo: Boolean = true)

internal expect val monitoringCapabilities: MonitoringCapabilities
