package com.bernaferrari.changedetection.diagnostics

import androidx.compose.runtime.Composable

@Composable actual fun DiagnosticsEntry(siteId: String?, captureDetails: Boolean, onTryText: (suspend () -> Boolean)?) = Unit

@Composable actual fun DiagnosticsRow() = Unit
