package com.bernaferrari.changedetection.localBrowser

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun LiveBrowserPage(url: String, modifier: Modifier) = LegacyLiveBrowserPage(url, modifier)
