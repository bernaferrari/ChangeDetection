/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.bernaferrari.changedetection

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation3.runtime.NavKey
import com.bernaferrari.changedetection.navigation.LogsDestination
import com.bernaferrari.changedetection.navigation.MainDestination
import com.bernaferrari.changedetection.navigation.SettingsDestination
import com.bernaferrari.changedetection.sync.IosDeepLinks
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.nav_activity
import com.bernaferrari.changedetection.shared.generated.resources.nav_home
import com.bernaferrari.changedetection.shared.generated.resources.nav_settings
import com.bernaferrari.changedetection.ui.navigation.BarAction
import com.bernaferrari.changedetection.ui.navigation.LocalNativeNavigation
import com.bernaferrari.changedetection.ui.navigation.NativeNavigation
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import platform.UIKit.UIAction
import platform.UIKit.UIBarButtonItem
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UIMenu
import platform.UIKit.UIMenuElementAttributesDestructive
import platform.UIKit.UIMenuElementAttributesDisabled
import platform.UIKit.UIModalPresentationPageSheet
import platform.UIKit.UINavigationController
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UITabBarController
import platform.UIKit.UITabBarItem
import platform.UIKit.UIViewController
import platform.UIKit.navigationController
import platform.UIKit.navigationItem
import platform.UIKit.tabBarItem
import platform.darwin.NSObject

/**
 * iOS-native navigation around shared Compose screens: a system tab bar, one navigation stack per tab with
 * native bars, back buttons and swipe-back, and sheets for adding or editing a page. Every screen's content
 * is the same Compose UI as on Android, hosted in its own ComposeUIViewController.
 */
class IosNativeShell(private val platformActions: PlatformActions) {
    /** View models shared by every screen (Home's state, messages), as if it were one composition. */
    private val appOwner = object : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }
    private val barVisibility = mutableMapOf<UIViewController, Boolean>()
    private val stackDelegate = StackDelegate(barVisibility)
    private val brand = UIColor(red = 0x4F / 255.0, green = 0x5B / 255.0, blue = 0xD5 / 255.0, alpha = 1.0)

    private val tabs: List<UINavigationController> = listOf(
        Triple(MainDestination, "Home", "house"),
        Triple(LogsDestination, "Activity", "chart.line.uptrend.xyaxis"),
        Triple(SettingsDestination, "Settings", "gearshape"),
    ).mapIndexed { index, (key, title, symbol) ->
        UINavigationController(rootViewController = screen(key, presented = false)).apply {
            tabBarItem = UITabBarItem(title = title, image = UIImage.systemImageNamed(symbol), tag = index.toLong())
            delegate = stackDelegate
            navigationBar.tintColor = brand
            // Bars sit on the app's own background instead of a white strip.
            val appearance = platform.UIKit.UINavigationBarAppearance().apply { configureWithTransparentBackground() }
            navigationBar.standardAppearance = appearance
            navigationBar.scrollEdgeAppearance = appearance
        }
    }

    private var homeBadge = 0
    private val tabDelegate = TabDelegate { refreshHomeBadge() }

    /** The badge points to Home from other tabs; on Home the title already says how many changes are new. */
    private fun refreshHomeBadge() {
        val onHome = (root as UITabBarController).selectedIndex == 0uL
        tabs[0].tabBarItem.badgeValue = if (homeBadge > 0 && !onHome) homeBadge.toString() else null
    }

    val root: UIViewController = UITabBarController().apply {
        delegate = tabDelegate
        setViewControllers(this@IosNativeShell.tabs, animated = false)
        tabBar.tintColor = brand
    }

    init {
        localizeTabTitles()
        // A tapped notification opens its change from Home.
        MainScope().launch {
            IosDeepLinks.pending.collect { link ->
                if (link != null) {
                    (root as UITabBarController).selectedIndex = 0u
                    tabs[0].popToRootViewControllerAnimated(false)
                    refreshHomeBadge()
                }
            }
        }
    }

    /** Tab titles come from the shared strings; they are read asynchronously (blocking here would deadlock). */
    private fun localizeTabTitles() {
        MainScope().launch {
            listOf(Res.string.nav_home, Res.string.nav_activity, Res.string.nav_settings).forEachIndexed { index, res ->
                runCatching { org.jetbrains.compose.resources.getString(res) }.getOrNull()?.let { tabs[index].tabBarItem.title = it }
            }
        }
    }

    private fun screen(key: NavKey, presented: Boolean): UIViewController {
        val host = Host(presented)
        val controller = ComposeUIViewController(configure = { onFocusBehavior = OnFocusBehavior.DoNothing }) {
            CompositionLocalProvider(
                LocalNativeNavigation provides host,
                LocalAppViewModelStoreOwner provides appOwner,
            ) {
                // UIKit already places the screen between the bars: Compose must not add those insets again.
                val consumed = androidx.compose.foundation.layout.WindowInsets.safeDrawing.only(
                    (if (host.barHidden.value) androidx.compose.foundation.layout.WindowInsetsSides.Horizontal else androidx.compose.foundation.layout.WindowInsetsSides.Horizontal + androidx.compose.foundation.layout.WindowInsetsSides.Top) +
                        (if (presented) androidx.compose.foundation.layout.WindowInsetsSides.Horizontal else androidx.compose.foundation.layout.WindowInsetsSides.Bottom),
                )
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.consumeWindowInsets(consumed)) {
                    if (key == MainDestination) {
                        val link by IosDeepLinks.pending.collectAsState()
                        ChangeDetectionRootApp(
                            platformActions = platformActions,
                            initialSiteId = link?.first,
                            initialSnapId = link?.second,
                            onInitialSiteIdHandled = IosDeepLinks::consume,
                            startDestination = key,
                        )
                    } else {
                        ChangeDetectionRootApp(platformActions = platformActions, startDestination = key)
                    }
                }
            }
        }
        // Compose lays out inside the bars instead of under them, so nothing hides behind the tab or navigation bar.
        controller.edgesForExtendedLayout = platform.UIKit.UIRectEdgeNone
        host.controller = controller
        return controller
    }

    private inner class Host(private val presented: Boolean) : NativeNavigation {
        lateinit var controller: UIViewController

        /** Whether this screen hides the native bar (then Compose handles the status bar inset itself). */
        val barHidden = androidx.compose.runtime.mutableStateOf(false)
        private var signature: String? = null
        private var actions: List<BarAction> = emptyList()
        private var menu: List<BarAction> = emptyList()

        /** Screens opened from a sheet land on the visible tab's stack. */
        private fun stack(): UINavigationController? = controller.navigationController ?: (root as UITabBarController).selectedViewController as? UINavigationController

        override fun push(key: NavKey) {
            stack()?.pushViewController(screen(key, presented = false), animated = true)
        }

        override fun present(key: NavKey) {
            val sheet = screen(key, presented = true).apply { modalPresentationStyle = UIModalPresentationPageSheet }
            (controller.presentedViewController ?: controller).presentViewController(sheet, animated = true, completion = null)
        }

        override fun close() {
            if (presented && controller.presentingViewController != null) {
                controller.dismissViewControllerAnimated(true, completion = null)
            } else {
                controller.navigationController?.popViewControllerAnimated(true)
            }
        }

        override fun setBackground(argb: Int) {
            val color = UIColor(red = ((argb shr 16) and 255) / 255.0, green = ((argb shr 8) and 255) / 255.0, blue = (argb and 255) / 255.0, alpha = 1.0)
            controller.view.backgroundColor = color
            controller.navigationController?.view?.backgroundColor = color
            root.view.backgroundColor = color
        }

        override fun setHomeBadge(count: Int) {
            homeBadge = count
            refreshHomeBadge()
        }

        override fun setBar(title: String?, subtitle: String?, actions: List<BarAction>, menu: List<BarAction>, hidden: Boolean) {
            // Handlers always run the latest lambdas; the bar is only rebuilt when what it shows changes.
            this.actions = actions
            this.menu = menu
            val next = buildString {
                append(hidden).append('|').append(title).append('|').append(subtitle)
                (actions + listOf(null) + menu).forEach { append('|').append(it?.label).append(it?.iosSymbol).append(it?.enabled).append(it?.destructive) }
            }
            if (next == signature) return
            signature = next
            barVisibility[controller] = hidden
            barHidden.value = hidden
            controller.navigationController?.takeIf { it.topViewController == controller }?.setNavigationBarHidden(hidden, animated = false)
            val item = controller.navigationItem
            item.title = title
            item.subtitle = subtitle
            val buttons = mutableListOf<UIBarButtonItem>()
            if (menu.isNotEmpty()) {
                val elements = menu.indices.map { index -> action(menu[index]) { this.menu.getOrNull(index)?.onClick?.invoke() } }
                buttons += UIBarButtonItem(image = UIImage.systemImageNamed("ellipsis"), menu = UIMenu.menuWithTitle("", children = elements))
            }
            // Right-to-left: the first action sits next to the menu.
            actions.indices.reversed().forEach { index ->
                buttons += UIBarButtonItem(primaryAction = action(actions[index]) { this.actions.getOrNull(index)?.onClick?.invoke() })
            }
            item.setRightBarButtonItems(buttons, animated = false)
        }

        private fun action(action: BarAction, onClick: () -> Unit): UIAction = UIAction.actionWithTitle(action.label, image = UIImage.systemImageNamed(action.iosSymbol), identifier = null) { _ -> onClick() }.apply {
            attributes = (if (action.destructive) UIMenuElementAttributesDestructive else 0uL) or (if (action.enabled) 0uL else UIMenuElementAttributesDisabled)
        }
    }

    private class TabDelegate(private val onSelect: () -> Unit) :
        NSObject(),
        platform.UIKit.UITabBarControllerDelegateProtocol {
        override fun tabBarController(tabBarController: UITabBarController, didSelectViewController: UIViewController) = onSelect()
    }

    /** Applies each screen's bar visibility as it appears, so Home (own header) and other screens can differ. */
    private class StackDelegate(private val visibility: MutableMap<UIViewController, Boolean>) :
        NSObject(),
        UINavigationControllerDelegateProtocol {
        override fun navigationController(navigationController: UINavigationController, willShowViewController: UIViewController, animated: Boolean) {
            navigationController.setNavigationBarHidden(visibility[willShowViewController] ?: false, animated = animated)
            // Forget screens that were popped or dismissed.
            visibility.keys.removeAll { it.navigationController == null && it.presentingViewController == null }
        }
    }
}
