package com.bernaferrari.changedetection.addedit

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.input.nestedscroll.nestedScroll
import android.content.ClipDescription
import android.content.ClipboardManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.extensions.isValidUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeautifulUrlInputStep(
    url: String,
    urlError: String?,
    onUrlChange: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onContinue: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val nativeClipboard = remember { clipboard.nativeClipboard }
    
    // WebView state
    var webView by remember { mutableStateOf<android.webkit.WebView?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var currentWebUrl by remember { mutableStateOf(url) }
    var urlBarText by remember { mutableStateOf(url) }
    var isFocused by remember { mutableStateOf(false) }
    
    // Sync URL bar when external URL changes
    LaunchedEffect(url) {
        if (url != urlBarText && url.isNotBlank()) {
            urlBarText = url
        }
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        kotlinx.coroutines.delay(100)
        keyboardController?.show()
    }

    fun readClipboardText(): String? {
        val manager = nativeClipboard ?: return null
        val description = manager.primaryClipDescription
        val supportsText = description?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true ||
                description?.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) == true ||
                description?.hasMimeType(ClipDescription.MIMETYPE_TEXT_URILIST) == true
        if (!supportsText) return null

        val clip = manager.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        val text = clip.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
        return text?.takeIf { it.isNotEmpty() }
    }

    val isValid = remember(url, urlError) {
        url.isNotBlank() && url.isValidUrl() && urlError == null
    }
    
    // Show WebView only when URL is valid
    val showPreview = url.isNotBlank() && url.isValidUrl()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            // Compact browser-style address bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Close button
                        IconButton(
                            onClick = onNavigateUp,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // URL input field - pill shaped
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Lock/Globe icon
                                if (url.startsWith("https")) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                androidx.compose.foundation.text.BasicTextField(
                                    value = urlBarText,
                                    onValueChange = { newValue ->
                                        val trimmed = newValue.trim()
                                        urlBarText = trimmed
                                        onUrlChange(trimmed)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { isFocused = it.isFocused },
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Go,
                                        keyboardType = KeyboardType.Uri
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onGo = {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                            // Ensure we try to load what the user typed
                                            val trimmed = urlBarText.trim()
                                            if (trimmed != urlBarText) {
                                                urlBarText = trimmed
                                            }
                                            val finalUrl = if (!trimmed.startsWith("http") && trimmed.isNotEmpty()) "https://$trimmed" else trimmed
                                            onUrlChange(finalUrl)
                                            // Update currentWebUrl BEFORE loading to prevent race condition
                                            currentWebUrl = finalUrl
                                            webView?.loadUrl(finalUrl)
                                        }
                                    ),
                                    decorationBox = { innerTextField ->
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            if (urlBarText.isEmpty()) {
                                                Text(
                                                    text = "Enter website URL",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                                
                                // Clear button
                                androidx.compose.animation.AnimatedVisibility(visible = urlBarText.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            urlBarText = ""
                                            onUrlChange("")
                                            focusRequester.requestFocus()
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                
                                // Loading indicator
                                if (isLoading) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        // Paste button (compact)
                        IconButton(
                            onClick = {
                                val text = readClipboardText()?.trim()
                                if (!text.isNullOrBlank()) {
                                    urlBarText = text
                                    onUrlChange(text)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // Auto-load if valid URL
                                    if (text.isValidUrl()) {
                                        val finalUrl = if (!text.startsWith("http")) "https://$text" else text
                                        // Update currentWebUrl BEFORE loading to prevent race condition
                                        currentWebUrl = finalUrl
                                        webView?.loadUrl(finalUrl)
                                    }
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = androidx.compose.animation.slideInVertically { it } + androidx.compose.animation.fadeIn(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                val trimmed = urlBarText.trim()
                                if (trimmed != urlBarText) {
                                    urlBarText = trimmed
                                    onUrlChange(trimmed)
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onContinue()
                            },
                            enabled = isValid,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(text = stringResource(id = R.string.continue_label))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showPreview) {
                // Live WebView preview
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.webkit.WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            
                            webViewClient = object : android.webkit.WebViewClient() {
                                override fun onPageStarted(
                                    view: android.webkit.WebView?,
                                    loadingUrl: String?,
                                    favicon: android.graphics.Bitmap?
                                ) {
                                    super.onPageStarted(view, loadingUrl, favicon)
                                    isLoading = true
                                }
                                
                                override fun onPageFinished(view: android.webkit.WebView?, finishedUrl: String?) {
                                    super.onPageFinished(view, finishedUrl)
                                    isLoading = false
                                    // Update URL when user navigates
                                    finishedUrl?.let { newUrl ->
                                        if (newUrl != currentWebUrl && newUrl != "about:blank") {
                                            currentWebUrl = newUrl
                                            // Only update bar if not focused (prevent overwriting user typing)
                                            if (!isFocused) {
                                                urlBarText = newUrl
                                                onUrlChange(newUrl)
                                            }
                                        }
                                    }
                                }
                                
                                override fun shouldOverrideUrlLoading(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?
                                ): Boolean {
                                    // Allow navigation and update the URL
                                    request?.url?.toString()?.let { newUrl ->
                                        currentWebUrl = newUrl
                                        if (!isFocused) {
                                            urlBarText = newUrl
                                            onUrlChange(newUrl)
                                        }
                                    }
                                    return false // Let WebView handle navigation
                                }
                            }
                            
                            // Load initial URL
                            val initialUrl = if (!url.startsWith("http")) "https://$url" else url
                            loadUrl(initialUrl)
                            webView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { wv ->
                        // Only reload if URL changed externally (not from WebView navigation)
                        if (url != currentWebUrl && url.isValidUrl()) {
                            val loadUrl = if (!url.startsWith("http")) "https://$url" else url
                            if (loadUrl != wv.url) {
                                currentWebUrl = loadUrl
                                wv.loadUrl(loadUrl)
                            }
                        }
                    }
                )
            } else {
                // Empty state - show hint to enter URL
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Enter a website URL",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Type or paste a URL above to see a preview",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    // Show error if any
                    urlError?.let { error ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
    
    // Cleanup WebView
    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }
}

// UrlHero removed - was unused

@Composable
fun SuccessDialog(
    accentColors: Pair<Int, Int>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(accentColors.first).copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color(accentColors.first),
                                        Color(accentColors.second)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(id = R.string.success_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = stringResource(id = R.string.site_added_successfully),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FilledTonalButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(text = stringResource(id = R.string.done))
                }
            }
        }
    }
}
