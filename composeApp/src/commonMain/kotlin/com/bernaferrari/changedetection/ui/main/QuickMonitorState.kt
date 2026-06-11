/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

data class QuickMonitorState(
    val isActive: Boolean = false,
    val siteIds: Set<String> = emptySet(),
    val startTime: Long = 0,
    val deadlineTime: Long = 0,
    val intervalSeconds: Int = 15,
)
