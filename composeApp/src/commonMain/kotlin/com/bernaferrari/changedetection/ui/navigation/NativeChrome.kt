/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation3.runtime.NavKey
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.a11y_navigate_back
import com.bernaferrari.changedetection.shared.generated.resources.more_options
import org.jetbrains.compose.resources.stringResource

/** A top bar action: an icon button on Android and desktop, a bar button or menu item with an SF Symbol on iOS. */
data class BarAction(
    val label: String,
    val icon: ImageVector,
    /** SF Symbol name used by the native iOS bar. */
    val iosSymbol: String,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * Set by the iOS shell, where UIKit owns navigation: each screen is its own view controller in a
 * UINavigationController, under a native tab bar. Null everywhere else, where Compose navigates.
 */
interface NativeNavigation {
    /** Push a screen onto the current tab's stack. */
    fun push(key: NavKey)

    /** Show a flow (adding or editing a page) as a sheet. */
    fun present(key: NavKey)

    /** Close the screen hosting this composition (pop, or dismiss a sheet). */
    fun close()

    /** Unread changes, shown as the Home tab's badge. */
    fun setHomeBadge(count: Int)

    /** The app's background, so native containers around Compose screens match it. */
    fun setBackground(argb: Int)

    /** The native navigation bar of the screen hosting this composition. */
    fun setBar(title: String?, subtitle: String?, actions: List<BarAction>, menu: List<BarAction>, hidden: Boolean)
}

val LocalNativeNavigation = staticCompositionLocalOf<NativeNavigation?> { null }

/** True when a native navigation bar draws the title and back button for this screen. */
@Composable
fun usesNativeNavigation(): Boolean = LocalNativeNavigation.current != null

/**
 * The screen's top bar. With native navigation (iOS) it configures the system navigation bar instead of
 * drawing anything, so titles, back buttons and menus look and behave like the rest of iOS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String?,
    onBack: (() -> Unit)?,
    subtitle: String? = null,
    actions: List<BarAction> = emptyList(),
    menu: List<BarAction> = emptyList(),
) {
    val native = LocalNativeNavigation.current
    if (native != null) {
        SideEffect { native.setBar(title, subtitle, actions, menu, hidden = false) }
        return
    }
    TopAppBar(
        title = {
            if (title != null) {
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.a11y_navigate_back)) }
        },
        actions = {
            actions.forEach { action ->
                IconButton(onClick = action.onClick, enabled = action.enabled) { Icon(action.icon, action.label) }
            }
            if (menu.isNotEmpty()) {
                var open by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { open = true }) { Icon(Icons.Rounded.MoreVert, stringResource(Res.string.more_options)) }
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        menu.forEach { item ->
                            val tint = if (item.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            DropdownMenuItem(
                                text = { Text(item.label) },
                                leadingIcon = { Icon(item.icon, null, tint = tint) },
                                enabled = item.enabled,
                                colors = if (item.destructive) MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error) else MenuDefaults.itemColors(),
                                onClick = {
                                    open = false
                                    item.onClick()
                                },
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

/** Screens that draw their own header (Home) hide the native bar but keep native push, pop and swipe-back. */
@Composable
fun HideNativeBar() {
    val native = LocalNativeNavigation.current ?: return
    SideEffect { native.setBar(null, null, emptyList(), emptyList(), hidden = true) }
}
