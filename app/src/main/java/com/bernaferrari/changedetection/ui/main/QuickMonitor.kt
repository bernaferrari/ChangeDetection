package com.bernaferrari.changedetection.ui.main

data class QuickMonitorState(
    val isActive: Boolean = false,
    val siteIds: Set<String> = emptySet(),
    val startTime: Long = 0,
    val deadlineTime: Long = 0,
    val intervalSeconds: Int = 15
)