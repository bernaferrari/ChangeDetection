package com.bernaferrari.changedetection

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import com.bernaferrari.changedetection.ui.theme.SharedAppTheme
import com.bernaferrari.changedetection.ui.components.SharedLoadingIndicator
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    return ComposeUIViewController {
        AppContent()
    }
}

@Composable
private fun AppContent() {
    SharedAppTheme {
        SharedLoadingIndicator()
    }
}
