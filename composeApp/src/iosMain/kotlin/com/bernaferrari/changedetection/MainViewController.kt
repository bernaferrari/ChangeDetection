/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection

import androidx.compose.ui.window.ComposeUIViewController
import com.bernaferrari.changedetection.di.initKoinIos
import com.bernaferrari.changedetection.platform.openEmail
import com.bernaferrari.changedetection.platform.openUrl
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoinIos()
    return ComposeUIViewController {
        ChangeDetectionRootApp(
            platformActions = PlatformActions(
                openUrl = ::openUrl,
                openEmail = ::openEmail,
                appVersion = platform.Foundation.NSBundle.mainBundle
                    .objectForInfoDictionaryKey("CFBundleShortVersionString")
                    ?.toString()
                    .orEmpty(),
            ),
        )
    }
}
