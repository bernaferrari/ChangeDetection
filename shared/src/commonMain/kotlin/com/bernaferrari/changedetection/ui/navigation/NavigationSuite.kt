/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.bernaferrari.changedetection.navigation.LogsDestination
import com.bernaferrari.changedetection.navigation.MainDestination
import com.bernaferrari.changedetection.navigation.SettingsDestination
import com.bernaferrari.changedetection.navigation.TurboDestination
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.nav_home
import com.bernaferrari.changedetection.shared.generated.resources.nav_activity
import com.bernaferrari.changedetection.shared.generated.resources.nav_settings
import com.bernaferrari.changedetection.shared.generated.resources.nav_turbo
import com.bernaferrari.changedetection.localization.stringResource

private data class AppNavItem(
    val destination: NavKey,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val appNavItems = listOf(
    AppNavItem(MainDestination, Icons.Filled.Home, Icons.Outlined.Home),
    AppNavItem(LogsDestination, Icons.Filled.Timeline, Icons.Outlined.Timeline),
    AppNavItem(SettingsDestination, Icons.Filled.Settings, Icons.Outlined.Settings),
)

/**
 * The same stock adaptive navigation model used by QuietGuard. Material chooses the bottom bar or
 * compact rail from the current window posture; the product does not add a second branded sidebar
 * or invent a separate wide-navigation treatment.
 */
@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
@Composable
fun ChangeDetectionNavigationSuite(
    backStack: NavBackStack<NavKey>,
    retainNavigationForDetail: Boolean = false,
    homeBadge: Int = 0,
    content: @Composable () -> Unit,
) {
    val currentTab = backStack.lastOrNull { it.isRootTab() } ?: MainDestination
    val labels = listOf(
        stringResource(Res.string.nav_home),
        stringResource(Res.string.nav_activity),
        stringResource(Res.string.nav_settings),
    )

    // Subsequent pages get the full canvas. M3 toolbars and the primary navigation bar should not
    // compete at the bottom of the same compact screen. The content keeps one place in the tree whether
    // or not the bar shows: rebuilding it would drop every screen's state and skip the push animation.
    val showBar = backStack.lastOrNull()?.isRootTab() == true || retainNavigationForDetail
    val currentContent by androidx.compose.runtime.rememberUpdatedState(content)
    val movableContent = androidx.compose.runtime.remember { androidx.compose.runtime.movableContentOf { currentContent() } }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth < 600.dp) {
            Scaffold(
                contentWindowInsets = WindowInsets(0),
                bottomBar = {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showBar,
                        enter = androidx.compose.animation.expandVertically(expandFrom = androidx.compose.ui.Alignment.Top),
                        exit = androidx.compose.animation.shrinkVertically(shrinkTowards = androidx.compose.ui.Alignment.Top),
                    ) {
                        ShortNavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            appNavItems.forEachIndexed { index, item ->
                                val selected = item.destination.matches(currentTab)
                                ShortNavigationBarItem(
                                    selected = selected,
                                    onClick = { if (!selected) navigateToTab(backStack, item.destination) },
                                    icon = { NavIcon(if (selected) item.selectedIcon else item.unselectedIcon, if (item.destination is MainDestination && !selected) homeBadge else 0) },
                                    label = { Text(labels[index]) },
                                )
                            }
                        }
                    }
                },
            ) { padding -> Box(Modifier.fillMaxSize().padding(padding)) { movableContent() } }
        } else if (!showBar) {
            Box(Modifier.fillMaxSize()) { movableContent() }
        } else {
            BoundedNavigationSuiteScaffold(
                navigationSuiteItems = {
                    appNavItems.forEachIndexed { index, item ->
                        val selected = item.destination.matches(currentTab)
                        val label = labels[index]
                        item(
                            selected = selected,
                            onClick = {
                                if (!selected) navigateToTab(backStack, item.destination)
                            },
                            icon = { NavIcon(if (selected) item.selectedIcon else item.unselectedIcon, if (item.destination is MainDestination && !selected) homeBadge else 0) },
                            label = { Text(label) },
                        )
                    }
                },
                content = movableContent,
            )
        }
    }
}

@Composable
private fun NavIcon(icon: ImageVector, badge: Int) {
    if (badge > 0) {
        BadgedBox(badge = {
            Badge(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) { Text(if (badge > 99) "${com.bernaferrari.changedetection.util.localizedInteger(99)}+" else com.bernaferrari.changedetection.util.localizedInteger(badge)) }
        }) { Icon(icon, null, Modifier.size(24.dp)) }
    } else {
        Icon(icon, null, Modifier.size(24.dp))
    }
}

/** Compose Web can briefly issue an unbounded probe before the real viewport measurement. */
@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
@Composable
private fun BoundedNavigationSuiteScaffold(
    navigationSuiteItems: NavigationSuiteScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (
            constraints.hasBoundedWidth &&
            constraints.hasBoundedHeight &&
            constraints.maxWidth > 0 &&
            constraints.maxHeight > 0
        ) {
            NavigationSuiteScaffold(
                navigationSuiteItems = navigationSuiteItems,
                modifier = Modifier.fillMaxSize(),
                content = content,
            )
        }
    }
}

private fun NavKey.matches(other: NavKey?): Boolean = when (this) {
    is MainDestination -> other is MainDestination
    is LogsDestination -> other is LogsDestination
    is TurboDestination -> other is TurboDestination
    is SettingsDestination -> other is SettingsDestination
    else -> false
}

private fun navigateToTab(backStack: NavBackStack<NavKey>, destination: NavKey) {
    val tabIndex = backStack.indexOfFirst { it.isRootTab() }
    if (tabIndex >= 0) {
        backStack[tabIndex] = destination
        while (backStack.size > tabIndex + 1) backStack.removeLast()
    } else {
        backStack.add(destination)
    }
}

private fun NavKey.isRootTab(): Boolean = this is MainDestination || this is LogsDestination || this is SettingsDestination
