package com.bernaferrari.changedetection.util

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op on iOS as standard back gesture/navigation is handled by the system or navigation controller
}
