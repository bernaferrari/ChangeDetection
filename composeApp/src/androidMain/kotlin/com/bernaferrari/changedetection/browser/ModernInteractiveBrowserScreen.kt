package com.bernaferrari.changedetection.browser

import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import com.bernaferrari.changedetection.extensions.normalizeUrl
import com.bernaferrari.changedetection.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernInteractiveBrowserScreen(
    initialUrl: String,
    initialSession: BrowserSessionData? = null,
    onNavigateUp: () -> Unit,
    onSaveComplete: (BrowserSessionData) -> Unit,
    viewModel: InteractiveBrowserViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var webView by remember { mutableStateOf<WebView?>(null) }
    
    LaunchedEffect(initialUrl) {
        if (initialUrl.isNotBlank()) {
            viewModel.setInitialUrl(initialUrl)
        }
    }

    LaunchedEffect(initialSession) {
        initialSession?.let { viewModel.hydrateFromSession(it) }
    }

    LaunchedEffect(uiState.interactionMode) {
        viewModel.reapplyInteractionMode()
    }
    
    var showModeHint by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.interactionMode, uiState.isLoading) {
        if (!uiState.isLoading && uiState.interactionMode != InteractionMode.NORMAL) {
            showModeHint = true
            delay(2000)
            showModeHint = false
        } else if (uiState.interactionMode == InteractionMode.NORMAL) {
            showModeHint = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            TopAppBar(
                title = {
                    Text(
                        text = androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.configure_site),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
            
            // Browser Controls
            BrowserControlsBar(
                currentUrl = uiState.currentUrl,
                isLoading = uiState.isLoading,
                webView = webView,
                onUrlSubmit = { url ->
                    val normalized = url.normalizeUrl()
                    val finalUrl = normalized ?: url.trim()
                    if (finalUrl.isNotBlank()) {
                        viewModel.setCurrentUrl(finalUrl)
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            )
            
            // WebView Container
            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webView = this
                            viewModel.setWebView(this)
                            setupWebView(this, viewModel)
                        }
                    },
                    update = { view ->
                        if (uiState.currentUrl.isNotBlank() && view.url != uiState.currentUrl) {
                            view.loadUrl(uiState.currentUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                DisposableEffect(webView) {
                    onDispose {
                        webView?.apply {
                            try {
                                stopLoading()
                                clearHistory()
                                clearCache(true)
                                setWebChromeClient(WebChromeClient())
                                setWebViewClient(WebViewClient())
                                destroy()
                            } catch (_: Exception) {
                            } finally {
                                webView = null
                            }
                        }
                    }
                }
                
                if (uiState.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                }
                
                // Mode overlay
            }
            
            // Bottom Control Bar with ZAP manager
            BottomControlBar(
                uiState = uiState,
                onElementSelectToggle = viewModel::toggleElementSelection,
                onElementZapToggle = viewModel::toggleElementZapping,
                onShowZapManager = viewModel::showZapManager
            )
        }

        // ZAP Manager Overlay
        ZapManagerOverlay(
            uiState = uiState,
            onDismiss = viewModel::showZapManager,
            onRemoveZappedElement = viewModel::removeZappedElement,
            onClearAllZapped = viewModel::clearAllZappedElements
        )

        // Parent selector overlay for refining targets
        val overlaySelection: SelectedElement? = uiState.parentChain
            .takeIf { uiState.showParentSelector && it.isNotEmpty() }
            ?.getOrNull(uiState.selectedParentIndex.coerceIn(0, uiState.parentChain.lastIndex))
            ?.let { parent ->
                SelectedElement(
                    selector = parent.selector,
                    description = parent.displayName,
                    timestamp = parent.index.toLong()
                )
            }
            ?: uiState.pendingElementSelector
                ?.takeIf { uiState.showParentSelector }
                ?.let {
                    SelectedElement(
                        selector = it,
                        description = uiState.pendingElementDescription,
                        timestamp = 0L
                    )
                }

        ParentSelectorOverlay(
            isVisible = uiState.showParentSelector,
            selectedElement = overlaySelection,
            parentChain = uiState.parentChain,
            mode = uiState.pendingInteractionMode ?: uiState.interactionMode,
            onDismiss = viewModel::hideParentSelector,
            onSelectLevel = viewModel::selectParentAtIndex,
            onConfirm = { viewModel.confirmParentSelection() }
        )
        
        // Floating Action Button for Continue
        AnimatedVisibility(
            visible = uiState.hasSessionData && !uiState.showParentSelector,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 112.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = { 
                    onSaveComplete(
                        BrowserSessionData(
                            url = uiState.currentUrl,
                            title = uiState.pageTitle,
                            cookies = uiState.capturedCookies,
                            userAgent = uiState.userAgent,
                            selectedElements = uiState.selectedElements,
                            zappedElements = uiState.zappedElements
                        )
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.continue_label),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        ModeOverlay(
            mode = uiState.interactionMode,
            isVisible = showModeHint && !uiState.isLoading
        )
    }
}

@Composable
private fun BrowserControlsBar(
    currentUrl: String,
    isLoading: Boolean,
    webView: WebView?,
    onUrlSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var urlText by remember { mutableStateOf(currentUrl) }
    var isUrlEditing by remember { mutableStateOf(false) }
    
    LaunchedEffect(currentUrl) {
        if (!isUrlEditing) {
            urlText = currentUrl
        }
    }
    
    val submitUrl: (String) -> Unit = { input ->
        val normalized = input.normalizeUrl()
        val finalUrl = normalized ?: input.trim()
        if (finalUrl.isNotBlank()) {
            onUrlSubmit(finalUrl)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { webView?.let { if (it.canGoBack()) it.goBack() } },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = { webView?.let { if (it.canGoForward()) it.goForward() } },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = {
                        webView?.let {
                            if (isLoading) it.stopLoading() else it.reload()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                        contentDescription = if (isLoading) "Stop" else "Refresh",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            OutlinedTextField(
                value = urlText,
                onValueChange = {
                    urlText = it
                    isUrlEditing = true
                },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(text = androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.enter_website_url_ellipsis))
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (urlText != currentUrl && urlText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                submitUrl(urlText)
                                isUrlEditing = false
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        submitUrl(urlText)
                        isUrlEditing = false
                    }
                )
            )
        }
    }
}

@Composable
private fun BoxScope.ModeOverlay(
    mode: InteractionMode,
    isVisible: Boolean
) {
    val overlayData = when (mode) {
        InteractionMode.ELEMENT_SELECT -> ModeOverlayData(
            icon = Icons.Default.TouchApp,
            accent = MaterialTheme.colorScheme.primary,
            title = stringResource(id = R.string.browser_mode_select_title),
            subtitle = stringResource(id = R.string.browser_mode_select_subtitle)
        )
        InteractionMode.ELEMENT_ZAP -> ModeOverlayData(
            icon = Icons.Default.Block,
            accent = MaterialTheme.colorScheme.error,
            title = stringResource(id = R.string.browser_mode_zap_title),
            subtitle = stringResource(id = R.string.browser_mode_zap_subtitle)
        )
        InteractionMode.LOGIN_CAPTURE -> ModeOverlayData(
            icon = Icons.Default.Info,
            accent = MaterialTheme.colorScheme.tertiary,
            title = stringResource(id = R.string.browser_mode_login_title),
            subtitle = stringResource(id = R.string.browser_mode_login_subtitle)
        )
        else -> null
    }

    AnimatedVisibility(
        visible = isVisible && overlayData != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        overlayData?.let { data ->
            Surface(
                shape = RoundedCornerShape(22.dp),
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = data.accent.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            imageVector = data.icon,
                            contentDescription = null,
                            tint = data.accent,
                            modifier = Modifier
                                .size(40.dp)
                                .padding(8.dp)
                        )
                    }
                    Column {
                        Text(
                            text = data.title,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = data.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private data class ModeOverlayData(
    val icon: ImageVector,
    val accent: Color,
    val title: String,
    val subtitle: String
)

private fun setupWebView(webView: WebView, viewModel: InteractiveBrowserViewModel) {
    webView.apply {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true // Enable database for login persistence
            allowFileAccess = false
            allowContentAccess = false
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        
        // Enable automatic cookie and login persistence
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        
        // Add JavaScript interface for element interaction
        addJavascriptInterface(
            ModernElementInteractionInterface(viewModel),
            "AndroidInterface"
        )
        
        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                viewModel.setLoading(true)
                url?.let { viewModel.setCurrentUrl(it) }
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                viewModel.setLoading(false)
                
                    // Extract page title
                view?.title?.let { title ->
                    if (title.isNotBlank() && title != url) {
                        viewModel.setPageTitle(title)
                    }
                }
                
                // Auto-capture and persist cookies globally
                    url?.let { 
                    val cookies = CookieManager.getInstance().getCookie(it) ?: ""
                    viewModel.setCapturedCookies(cookies)
                    
                    // Auto-save cookies for future use across all sites
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(view, true)
                }
                
                    // Capture user agent from WebView settings
                    try {
                        val ua = view?.settings?.userAgentString ?: ""
                        if (ua.isNotBlank()) {
                            viewModel.setUserAgent(ua)
                        }
                    } catch (_: Exception) {}

                // Inject interaction JavaScript
                view?.evaluateJavascript(getModernInteractionJavaScript(), null)
                viewModel.applyZappedElements(viewModel.uiState.value.zappedElements)
                viewModel.reapplyInteractionMode()
            }
        }
        
        webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                title?.let { 
                    if (it.isNotBlank()) {
                        viewModel.setPageTitle(it)
                    }
                }
            }
        }
    }
}

class ModernElementInteractionInterface(private val viewModel: InteractiveBrowserViewModel) {
    @JavascriptInterface
    fun onElementClicked(selector: String, elementInfo: String) {
        viewModel.handleElementInteraction(selector, elementInfo)
    }
}

private fun getModernInteractionJavaScript(): String {
    return """
        (function() {
            if (window.changeDetectionListenersAdded) {
                window.setInteractionMode = setInteractionMode;
                return;
            }
            window.changeDetectionListenersAdded = true;

            window.interactionMode = 'normal';
            window.selectedElements = new Set();
            window.zappedElements = new Set();
            window.previewState = {
                currentChain: [],
                highlightEl: null,
                originalOutline: null,
                originalBackground: null
            };

            function storeOriginalDisplay(element) {
                if (!element.dataset.zapOriginalDisplay) {
                    const computed = window.getComputedStyle(element).display;
                    element.dataset.zapOriginalDisplay = (element.style.display && element.style.display !== 'none')
                        ? element.style.display
                        : (computed !== 'none' ? computed : '');
                }
            }

            function restoreOriginalDisplay(element) {
                if (element.dataset.zapOriginalDisplay !== undefined) {
                    element.style.display = element.dataset.zapOriginalDisplay || '';
                    delete element.dataset.zapOriginalDisplay;
                } else {
                    element.style.display = '';
                }
            }

            function captureStyles(element, prefix) {
                if (!element.dataset[prefix + 'Outline']) {
                    element.dataset[prefix + 'Outline'] = element.style.outline || '';
                }
                if (!element.dataset[prefix + 'OutlineOffset']) {
                    element.dataset[prefix + 'OutlineOffset'] = element.style.outlineOffset || '';
                }
                if (!element.dataset[prefix + 'Background']) {
                    element.dataset[prefix + 'Background'] = element.style.backgroundColor || '';
                }
                if (!element.dataset[prefix + 'Transition']) {
                    element.dataset[prefix + 'Transition'] = element.style.transition || '';
                }
            }

            function restoreStyles(element, prefix) {
                if (element.dataset[prefix + 'Outline'] !== undefined) {
                    element.style.outline = element.dataset[prefix + 'Outline'];
                    delete element.dataset[prefix + 'Outline'];
                } else {
                    element.style.outline = '';
                }

                if (element.dataset[prefix + 'OutlineOffset'] !== undefined) {
                    element.style.outlineOffset = element.dataset[prefix + 'OutlineOffset'];
                    delete element.dataset[prefix + 'OutlineOffset'];
                } else {
                    element.style.outlineOffset = '';
                }

                if (element.dataset[prefix + 'Background'] !== undefined) {
                    element.style.backgroundColor = element.dataset[prefix + 'Background'];
                    delete element.dataset[prefix + 'Background'];
                } else {
                    element.style.backgroundColor = '';
                }

                if (element.dataset[prefix + 'Transition'] !== undefined) {
                    element.style.transition = element.dataset[prefix + 'Transition'];
                    delete element.dataset[prefix + 'Transition'];
                } else {
                    element.style.transition = '';
                }
            }

            function applySelectionHighlight(element) {
                captureStyles(element, 'selected');
                element.style.transition = 'outline 0.2s ease, background-color 0.2s ease';
                element.style.outline = '3px solid #1E88E5';
                element.style.outlineOffset = '3px';
                element.style.backgroundColor = 'rgba(30, 136, 229, 0.18)';
                element.setAttribute('data-selected', 'true');
            }

            function removeSelectionHighlight(element) {
                restoreStyles(element, 'selected');
                element.removeAttribute('data-selected');
            }

            function applyZapHighlight(element) {
                captureStyles(element, 'zapped');
                element.style.transition = 'outline 0.2s ease, background-color 0.2s ease';
                element.style.outline = '2px dashed #FF3B30';
                element.style.outlineOffset = '3px';
                element.style.backgroundColor = 'rgba(255, 59, 48, 0.16)';
                element.setAttribute('data-zapped', 'true');
            }

            function removeZapHighlight(element) {
                restoreStyles(element, 'zapped');
                element.removeAttribute('data-zapped');
            }

            window.clearPreviewHighlight = function() {
                if (window.previewState.highlightEl) {
                    const el = window.previewState.highlightEl;
                    if (window.previewState.originalOutline !== null) {
                        el.style.outline = window.previewState.originalOutline;
                        el.style.outlineOffset = '';
                    } else {
                        el.style.outline = '';
                    }
                    if (window.previewState.originalBackground !== null) {
                        el.style.backgroundColor = window.previewState.originalBackground;
                    }
                }
                window.previewState.highlightEl = null;
                window.previewState.originalOutline = null;
                window.previewState.originalBackground = null;
            };

            window.previewParentAtIndex = function(index) {
                window.clearPreviewHighlight();
                const item = window.previewState.currentChain[index];
                if (!item || !item.element) return;
                const el = item.element;
                window.previewState.highlightEl = el;
                window.previewState.originalOutline = el.style.outline;
                window.previewState.originalBackground = el.style.backgroundColor;
                el.style.outline = '3px dashed #7C4DFF';
                el.style.outlineOffset = '2px';
                el.style.backgroundColor = 'rgba(124, 77, 255, 0.12)';
            };

            window.applyZappedElements = function(selectors) {
                selectors.forEach(selector => {
                    const element = document.querySelector(selector);
                    if (!element) return;
                    window.zappedElements.add(selector);
                    storeOriginalDisplay(element);
                    element.style.display = 'none';
                    element.setAttribute('data-zapped', 'true');
                });
            };

            function addElementInteraction() {
                let hoveredElement = null;
                const originalStyles = new Map();

                document.addEventListener('mouseover', function(e) {
                    if (window.interactionMode === 'select' || window.interactionMode === 'zap') {
                        e.preventDefault();
                        clearHover();

                        hoveredElement = e.target;
                        const color = window.interactionMode === 'select' ? '#007AFF' : '#FF3B30';
                        const bgColor = window.interactionMode === 'select' ? 'rgba(0, 122, 255, 0.15)' : 'rgba(255, 59, 48, 0.15)';

                        originalStyles.set(hoveredElement, {
                            outline: hoveredElement.style.outline,
                            backgroundColor: hoveredElement.style.backgroundColor,
                            transition: hoveredElement.style.transition
                        });

                        hoveredElement.style.transition = 'all 0.2s ease';
                        hoveredElement.style.outline = '2px solid ' + color;
                        hoveredElement.style.outlineOffset = '2px';
                        hoveredElement.style.backgroundColor = bgColor;
                        hoveredElement.style.cursor = 'pointer';
                    }
                }, true);

                document.addEventListener('mouseout', function() {
                    if (window.interactionMode === 'select' || window.interactionMode === 'zap') {
                        clearHover();
                    }
                }, true);

                function clearHover() {
                    if (
                        hoveredElement &&
                        (hoveredElement.hasAttribute('data-selected') || hoveredElement.hasAttribute('data-zapped'))
                    ) {
                        originalStyles.delete(hoveredElement);
                        hoveredElement = null;
                        return;
                    }

                    if (hoveredElement && originalStyles.has(hoveredElement)) {
                        const original = originalStyles.get(hoveredElement);
                        hoveredElement.style.outline = original.outline;
                        hoveredElement.style.backgroundColor = original.backgroundColor;
                        hoveredElement.style.transition = original.transition;
                        hoveredElement.style.cursor = '';
                        originalStyles.delete(hoveredElement);
                    }
                    hoveredElement = null;
                }

                document.addEventListener('click', function(e) {
                    if (window.interactionMode === 'select' || window.interactionMode === 'zap') {
                        e.preventDefault();
                        e.stopPropagation();

                        const element = e.target;
                        const selector = generateCssSelector(element);

                        clearHover();

                        if (window.interactionMode === 'select') {
                            handleElementSelection(element, selector);
                        } else {
                            handleElementZapping(element, selector);
                        }
                    }
                }, true);

                const navigationEvents = ['mousedown', 'mouseup', 'touchstart', 'touchend'];
                navigationEvents.forEach(eventType => {
                    document.addEventListener(eventType, function(event) {
                        const target = event.target;
                        if ((window.interactionMode === 'select' || window.interactionMode === 'zap') && target && target.closest && target.closest('a')) {
                            event.preventDefault();
                            event.stopPropagation();
                        }
                    }, true);
                });
            }

            function handleElementSelection(element, selector) {
                window.clearPreviewHighlight();

                if (window.selectedElements.has(selector)) {
                    window.selectedElements.delete(selector);
                    removeSelectionHighlight(element);
                } else {
                    window.selectedElements.add(selector);
                    applySelectionHighlight(element);
                }

                const elementInfo = generateElementInfo(element, selector);
                AndroidInterface.onElementClicked(selector, elementInfo);
            }

            function handleElementZapping(element, selector) {
                window.clearPreviewHighlight();

                if (window.zappedElements.has(selector)) {
                    window.zappedElements.delete(selector);
                    removeZapHighlight(element);
                    restoreOriginalDisplay(element);
                } else {
                    window.zappedElements.add(selector);
                    applyZapHighlight(element);
                    storeOriginalDisplay(element);
                }

                const elementInfo = generateElementInfo(element, selector);
                AndroidInterface.onElementClicked(selector, elementInfo);
            }

            function getElementIndex(element) {
                let index = 1;
                let sibling = element.previousElementSibling;
                while (sibling) {
                    if (sibling.tagName === element.tagName) {
                        index++;
                    }
                    sibling = sibling.previousElementSibling;
                }
                return index;
            }

            function generateCssSelector(element) {
                if (element.id) {
                    return '#' + CSS.escape(element.id);
                }

                const segments = [];
                let current = element;
                while (current && current.nodeType === 1 && segments.length < 6) {
                    let segment = current.tagName.toLowerCase();

                    if (current.id) {
                        segment = '#' + CSS.escape(current.id);
                        segments.unshift(segment);
                        break;
                    }

                    const classes = Array.from(current.classList).filter(Boolean).slice(0, 2);
                    if (classes.length) {
                        segment += '.' + classes.map(c => CSS.escape(c)).join('.');
                    }

                    const nth = getElementIndex(current);
                    if (nth > 1) {
                        segment += ':nth-of-type(' + nth + ')';
                    }

                    segments.unshift(segment);
                    current = current.parentElement;
                }

                return segments.join(' > ');
            }

            function buildParentChain(element) {
                const chain = [];
                let current = element;
                let depth = 0;
                while (current && current.nodeType === 1 && depth < 8) {
                    const selector = generateCssSelector(current);
                    chain.push({
                        element: current,
                        selector,
                        displayName: buildDisplayName(current),
                        tagName: current.tagName,
                        className: current.className || '',
                        id: current.id || '',
                        index: depth
                    });
                    current = current.parentElement;
                    depth++;
                }
                return chain;
            }

            function buildDisplayName(element) {
                const text = (element.textContent || '').trim();
                if (text.length > 0) {
                    return text.substring(0, 60);
                }
                if (element.id) {
                    return '#' + element.id;
                }
                if (element.classList.length > 0) {
                    return element.tagName + '.' + element.classList[0];
                }
                return element.tagName.toLowerCase();
            }

            function generateElementInfo(element, selector) {
                const rect = element.getBoundingClientRect();
                const parentChain = buildParentChain(element);
                window.previewState.currentChain = parentChain;

                const chainForJson = parentChain.map(item => ({
                    selector: item.selector,
                    displayName: item.displayName,
                    tagName: item.tagName,
                    className: item.className,
                    id: item.id,
                    index: item.index
                }));

                return JSON.stringify({
                    selector,
                    tagName: element.tagName,
                    className: element.className || '',
                    id: element.id || '',
                    text: element.textContent ? element.textContent.substring(0, 140) : '',
                    bounds: {
                        top: rect.top,
                        left: rect.left,
                        width: rect.width,
                        height: rect.height
                    },
                    parentChain: chainForJson
                });
            }

            function setInteractionMode(mode) {
                window.interactionMode = mode;
                window.clearPreviewHighlight();
                console.log('Interaction mode changed to:', mode);
            }

            window.setInteractionMode = setInteractionMode;
            window.applySelectionHighlight = applySelectionHighlight;
            window.removeSelectionHighlight = removeSelectionHighlight;
            window.applyZapHighlight = applyZapHighlight;
            window.removeZapHighlight = removeZapHighlight;
            window.storeOriginalDisplay = storeOriginalDisplay;
            window.restoreOriginalDisplay = restoreOriginalDisplay;

            addElementInteraction();
            console.log('✅ Change Detection browser integration loaded successfully');
        })();
    """.trimIndent()
}
