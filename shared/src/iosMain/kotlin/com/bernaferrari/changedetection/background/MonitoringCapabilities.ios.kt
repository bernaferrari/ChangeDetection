/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.background

internal actual val monitoringCapabilities = MonitoringCapabilities(MonitoringExecution.Background, constraints = false, notifications = true, osScheduled = true)
