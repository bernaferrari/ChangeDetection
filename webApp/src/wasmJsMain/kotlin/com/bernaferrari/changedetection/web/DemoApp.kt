/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.web

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

private val DemoColorScheme = darkColorScheme(
    primary = Color(0xFFB8C4FF),
    onPrimary = Color(0xFF17205B),
    primaryContainer = Color(0xFF2A397E),
    onPrimaryContainer = Color(0xFFDCE1FF),
    secondary = Color(0xFFFFB3C7),
    onSecondary = Color(0xFF5E1130),
    secondaryContainer = Color(0xFF7E2947),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFF8DE3C2),
    onTertiary = Color(0xFF003827),
    tertiaryContainer = Color(0xFF00513A),
    onTertiaryContainer = Color(0xFFA9F8D8),
    background = Color(0xFF12131A),
    onBackground = Color(0xFFE4E1EC),
    surface = Color(0xFF12131A),
    onSurface = Color(0xFFE4E1EC),
    surfaceVariant = Color(0xFF252735),
    onSurfaceVariant = Color(0xFFC7C5D4),
    outline = Color(0xFF8F8E9C),
)

@Composable
fun ChangeDetectionDemo() {
    var destination by remember { mutableStateOf(DemoDestination.MONITORING) }
    var selectedSiteId by remember { mutableStateOf(demoSites.first().id) }
    var isChecking by remember { mutableStateOf(false) }
    var checkCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(isChecking) {
        if (isChecking) {
            delay(700)
            checkCount += 1
            isChecking = false
        }
    }

    MaterialTheme(colorScheme = DemoColorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            DemoScaffold(
                destination = destination,
                selectedSiteId = selectedSiteId,
                isChecking = isChecking,
                lastCheckLabel = if (checkCount == 0) "Updated a few seconds ago" else "Checked just now",
                onDestinationChange = { destination = it },
                onSiteSelected = { selectedSiteId = it },
                onCheckAll = { isChecking = true },
            )
        }
    }
}
