/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.bernaferrari.changedetection.navigation.LogsDestination
import com.bernaferrari.changedetection.navigation.MainDestination
import com.bernaferrari.changedetection.navigation.SettingsDestination
import com.bernaferrari.changedetection.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private enum class NavigationLayout { BottomBar, Rail }

data class BottomNavItem(
    val destination: NavKey,
    val labelRes: StringResource,
    val icon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(MainDestination, Res.string.nav_home, Icons.Default.Home),
    BottomNavItem(LogsDestination, Res.string.nav_logs, Icons.AutoMirrored.Filled.List),
    BottomNavItem(SettingsDestination, Res.string.nav_settings, Icons.Default.Settings),
)

@Composable
fun ChangeDetectionBottomNavigation(
    backStack: NavBackStack<NavKey>,
    useNavigationRail: Boolean,
    content: @Composable () -> Unit,
) {
    val currentDestination = backStack.lastOrNull()
    val isTabDestination = currentDestination is MainDestination ||
        currentDestination is LogsDestination ||
        currentDestination is SettingsDestination

    if (!isTabDestination) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
        return
    }

    val layout = if (useNavigationRail) NavigationLayout.Rail else NavigationLayout.BottomBar

    when (layout) {
        NavigationLayout.BottomBar -> BottomNavigationLayout(backStack, currentDestination, content)
        NavigationLayout.Rail -> NavigationRailLayout(backStack, currentDestination, content)
    }
}

@Composable
private fun BottomNavigationLayout(
    backStack: NavBackStack<NavKey>,
    currentDestination: NavKey?,
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val navigationColors = NavigationBarItemDefaults.colors(
        selectedIconColor = colorScheme.onSecondaryContainer,
        selectedTextColor = colorScheme.onSecondaryContainer,
        indicatorColor = colorScheme.secondaryContainer,
        unselectedIconColor = colorScheme.onSurfaceVariant,
        unselectedTextColor = colorScheme.onSurfaceVariant,
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            content()
        }

        NavigationBar(
            modifier = Modifier
                .fillMaxWidth(),
            containerColor = colorScheme.surfaceContainer,
            contentColor = colorScheme.onSurfaceVariant,
            tonalElevation = 0.dp,
            windowInsets = NavigationBarDefaults.windowInsets,
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = when (item.destination) {
                    is MainDestination -> currentDestination is MainDestination
                    is LogsDestination -> currentDestination is LogsDestination
                    is SettingsDestination -> currentDestination is SettingsDestination
                    else -> false
                }
                val label = stringResource(item.labelRes)

                NavigationBarItem(
                    icon = { Icon(imageVector = item.icon, contentDescription = null) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        )
                    },
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            navigateToTab(backStack, item.destination)
                        }
                    },
                    colors = navigationColors,
                    alwaysShowLabel = true,
                )
            }
        }
    }
}

@Composable
private fun NavigationRailLayout(
    backStack: NavBackStack<NavKey>,
    currentDestination: NavKey?,
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val railColors = NavigationRailItemDefaults.colors(
        selectedIconColor = colorScheme.onPrimaryContainer,
        selectedTextColor = colorScheme.onPrimaryContainer,
        indicatorColor = colorScheme.primaryContainer,
        unselectedIconColor = colorScheme.onSurfaceVariant,
        unselectedTextColor = colorScheme.onSurfaceVariant,
    )

    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding(),
            containerColor = colorScheme.surfaceContainer,
            contentColor = colorScheme.onSurfaceVariant,
            header = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = when (item.destination) {
                    is MainDestination -> currentDestination is MainDestination
                    is LogsDestination -> currentDestination is LogsDestination
                    is SettingsDestination -> currentDestination is SettingsDestination
                    else -> false
                }
                val label = stringResource(item.labelRes)

                NavigationRailItem(
                    icon = { Icon(imageVector = item.icon, contentDescription = label) },
                    label = { Text(text = label) },
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            navigateToTab(backStack, item.destination)
                        }
                    },
                    alwaysShowLabel = true,
                    colors = railColors,
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            content()
        }
    }
}

/**
 * Navigate to a tab destination, replacing the current tab in the back stack.
 */
private fun navigateToTab(backStack: NavBackStack<NavKey>, destination: NavKey) {
    // Find the index of the first tab destination
    val tabIndex = backStack.indexOfFirst {
        it is MainDestination || it is LogsDestination || it is SettingsDestination
    }

    if (tabIndex >= 0) {
        // Replace the existing tab with the new one
        backStack[tabIndex] = destination
        // Remove any items after the tab (detail screens)
        while (backStack.size > tabIndex + 1) {
            backStack.removeLast()
        }
    } else {
        // No tab exists, just add it
        backStack.add(destination)
    }
}
