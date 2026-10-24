package com.bernaferrari.changedetection.diagnostics

import androidx.compose.runtime.Composable

/** Android-only, local report preview and explicit system sharing. */
@Composable expect fun DiagnosticsEntry(siteId: String? = null)

/** The same report as a settings row, so it sits with Help instead of floating above the first section. */
@Composable expect fun DiagnosticsRow()
