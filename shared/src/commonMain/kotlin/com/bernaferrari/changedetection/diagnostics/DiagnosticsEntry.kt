package com.bernaferrari.changedetection.diagnostics

import androidx.compose.runtime.Composable

/** Android-only, local report preview and explicit system sharing. */
@Composable expect fun DiagnosticsEntry(siteId: String? = null)
