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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bernaferrari.changedetection.R

private enum class NavigationLayout { BottomBar, Rail }

data class BottomNavItem(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem("main", R.string.nav_home, Icons.Default.Home),
    BottomNavItem("logs", R.string.nav_logs, Icons.AutoMirrored.Filled.List),
    BottomNavItem("settings", R.string.nav_settings, Icons.Default.Settings)
)

@Composable
fun ChangeDetectionBottomNavigation(
    navController: NavController,
    windowSizeClass: WindowSizeClass,
    content: @Composable () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val routesWithNav = remember { bottomNavItems.map { it.route } }
    val shouldShowNav = currentRoute != null && routesWithNav.contains(currentRoute)

    if (!shouldShowNav) {
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
        NavigationLayout.BottomBar -> BottomNavigationLayout(navController, currentRoute, content)
        NavigationLayout.Rail -> NavigationRailLayout(navController, currentRoute, content)
    }
}

@Composable
private fun BottomNavigationLayout(
    navController: NavController,
    currentRoute: String?,
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
                val isSelected = currentRoute == item.route
                val label = stringResource(id = item.labelRes)

                NavigationBarItem(
                    icon = { Icon(imageVector = item.icon, contentDescription = label) },
                    label = { Text(text = label) },
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
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
    navController: NavController,
    currentRoute: String?,
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
                val isSelected = currentRoute == item.route
                val label = stringResource(id = item.labelRes)

                NavigationRailItem(
                    icon = { Icon(imageVector = item.icon, contentDescription = label) },
                    label = { Text(text = label) },
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
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
