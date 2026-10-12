@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.bernaferrari.changedetection.util

import androidx.compose.runtime.Composable

/** The iOS edge-swipe back gesture closes in-screen states (selection, search) like it does on Android. */
@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    androidx.compose.ui.backhandler.BackHandler(enabled = enabled, onBack = onBack)
}
