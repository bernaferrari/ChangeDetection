package com.bernaferrari.changedetection.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.navigation.LogsDestination
import com.bernaferrari.changedetection.navigation.MainDestination
import com.bernaferrari.changedetection.navigation.SettingsDestination
import com.bernaferrari.changedetection.navigation.TurboDestination

private enum class NavigationLayout { BottomBar, Rail }

data class BottomNavItem(
    val destination: NavKey,
    @StringRes val labelRes: Int,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(MainDestination, R.string.nav_home, Icons.Default.Home),
    BottomNavItem(TurboDestination, R.string.home_stats_quick_monitor_label, Icons.Filled.Bolt),
    BottomNavItem(LogsDestination, R.string.nav_logs, Icons.AutoMirrored.Filled.List),
    BottomNavItem(SettingsDestination, R.string.nav_settings, Icons.Default.Settings)
)

@Composable
fun ChangeDetectionBottomNavigation(
    backStack: NavBackStack<NavKey>,
    windowSizeClass: WindowSizeClass,
    content: @Composable () -> Unit
) {
    val currentDestination = backStack.lastOrNull()
    val isTabDestination = currentDestination is MainDestination ||
            currentDestination is TurboDestination ||
            currentDestination is LogsDestination ||
            currentDestination is SettingsDestination

    if (!isTabDestination) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
        return
    }

    val layout = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact, WindowWidthSizeClass.Medium -> NavigationLayout.BottomBar
        WindowWidthSizeClass.Expanded -> NavigationLayout.Rail
        else -> NavigationLayout.BottomBar
    }

    when (layout) {
        NavigationLayout.BottomBar -> BottomNavigationLayout(backStack, currentDestination, content)
        NavigationLayout.Rail -> NavigationRailLayout(backStack, currentDestination, content)
    }
}

@Composable
private fun BottomNavigationLayout(
    backStack: NavBackStack<NavKey>,
    currentDestination: NavKey?,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val navigationColors = NavigationBarItemDefaults.colors(
        selectedIconColor = colorScheme.onPrimaryContainer,
        selectedTextColor = colorScheme.onPrimaryContainer,
        indicatorColor = colorScheme.primaryContainer,
        unselectedIconColor = colorScheme.onSurfaceVariant,
        unselectedTextColor = colorScheme.onSurfaceVariant,
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            content()
        }

        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            containerColor = colorScheme.surfaceContainer,
            contentColor = colorScheme.onSurfaceVariant,
            tonalElevation = 3.dp
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = when (item.destination) {
                    is MainDestination -> currentDestination is MainDestination
                    is TurboDestination -> currentDestination is TurboDestination
                    is LogsDestination -> currentDestination is LogsDestination
                    is SettingsDestination -> currentDestination is SettingsDestination
                    else -> false
                }
                val label = stringResource(id = item.labelRes)

                NavigationBarItem(
                    icon = { Icon(imageVector = item.icon, contentDescription = label) },
                    label = { Text(text = label) },
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            navigateToTab(backStack, item.destination)
                        }
                    },
                    colors = navigationColors
                )
            }
        }
    }
}

@Composable
private fun NavigationRailLayout(
    backStack: NavBackStack<NavKey>,
    currentDestination: NavKey?,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val railColors = NavigationRailItemDefaults.colors(
        selectedIconColor = colorScheme.onPrimaryContainer,
        selectedTextColor = colorScheme.onPrimaryContainer,
        indicatorColor = colorScheme.primaryContainer,
        unselectedIconColor = colorScheme.onSurfaceVariant,
        unselectedTextColor = colorScheme.onSurfaceVariant
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
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = when (item.destination) {
                    is MainDestination -> currentDestination is MainDestination
                    is TurboDestination -> currentDestination is TurboDestination
                    is LogsDestination -> currentDestination is LogsDestination
                    is SettingsDestination -> currentDestination is SettingsDestination
                    else -> false
                }
                val label = stringResource(id = item.labelRes)

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
                    colors = railColors
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
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
        it is MainDestination || it is TurboDestination || it is LogsDestination || it is SettingsDestination
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
