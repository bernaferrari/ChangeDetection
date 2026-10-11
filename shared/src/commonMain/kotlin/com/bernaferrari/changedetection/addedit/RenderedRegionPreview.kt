/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.bernaferrari.changedetection.browser.*
import com.bernaferrari.changedetection.detection.*
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import kotlinx.coroutines.launch

internal class RegionPagePosition {
    var viewport = RegionViewport()
}

internal typealias RegionPageRenderer = @Composable (SelectableTextDocument, String?, (String) -> Unit, RegionPagePosition, Modifier) -> Unit

@Composable
internal fun RenderedRegionPreview(document: SelectableTextDocument, initiallySelected: String?, onSelect: (String) -> Unit, position: RegionPagePosition, modifier: Modifier) {
    // Restore the highlight when returning from the text list; clicks update the existing DOM without reloading it.
    val html = remember(document.sourceHtml ?: document.selectionHtml, document.regions) { selectionDocumentWithInitialRegion(document, initiallySelected, position.viewport) }
    val state = rememberWebViewStateWithHTMLData(html)
    val currentSelect by rememberUpdatedState(onSelect)
    val scope = rememberCoroutineScope()
    // Only the hash-authorized picker script runs; source scripts, navigation and resources are removed.
    state.webSettings.isJavaScriptEnabled = true
    state.webSettings.desktopWebSettings.disablePopupWindows = true
    BrowserRuntimeHost(modifier) {
        PlatformBrowserView(state, modifier, highlightUpdate = remember(document, initiallySelected) { regionHighlightUpdate(document, initiallySelected) }, onNavigationRequest = { url ->
            val viewport = regionViewportFromNavigation(url)
            val region = selectionFromNavigation(url, document.regions)
            if (viewport != null || region != null) {
                scope.launch {
                    if (viewport != null) position.viewport = viewport
                    if (region != null) currentSelect(region.selector)
                }
            }
            url == "about:blank" || url.startsWith("data:text/html")
        })
    }
}
