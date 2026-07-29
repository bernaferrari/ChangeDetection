/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.web

import androidx.compose.ui.graphics.Color

enum class DemoDestination {
    MONITORING,
    HISTORY,
    SETTINGS,
}

data class DemoSite(
    val id: String,
    val name: String,
    val url: String,
    val summary: String,
    val lastChecked: String,
    val changeDescription: String?,
    val accent: Color,
)

val demoSites = listOf(
    DemoSite(
        id = "espresso",
        name = "Espresso machine stock",
        url = "store.example.com/espresso",
        summary = "In stock · \$649",
        lastChecked = "2 min ago",
        changeDescription = "Price dropped from \$699 to \$649",
        accent = Color(0xFFB8C4FF),
    ),
    DemoSite(
        id = "tickets",
        name = "Concert tickets",
        url = "events.example.com/tickets",
        summary = "Sale opens Friday at 10:00",
        lastChecked = "7 min ago",
        changeDescription = null,
        accent = Color(0xFFFFB3C7),
    ),
    DemoSite(
        id = "release-notes",
        name = "Release notes",
        url = "docs.example.com/releases",
        summary = "Version 4.2 is now available",
        lastChecked = "18 min ago",
        changeDescription = "A new release entry was published",
        accent = Color(0xFF8DE3C2),
    ),
)
