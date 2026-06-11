/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.setup

import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
actual fun ElementSelectorDialog(
    url: String,
    onDismiss: () -> Unit,
    onElementSelected: (cssSelector: String, elementPreview: String) -> Unit,
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var selectedElement by remember { mutableStateOf<ElementInfo?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Cleanup WebView and JavaScript interface when dialog is dismissed
    DisposableEffect(Unit) {
        onDispose {
            try {
                webView?.stopLoading()
                webView?.removeJavascriptInterface("Android")
                webView?.clearHistory()
                webView?.clearCache(true)
                webView?.destroy()
                webView = null
            } catch (e: Exception) {
                io.github.aakira.napier.Napier.e("❌ Error cleaning up ElementSelectorDialog WebView: ${e.message}")
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Select Element to Monitor",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Tap any element on the page to monitor it",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Selection info
                selectedElement?.let { element ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Selected: ${element.tagName}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = element.selector,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (element.preview.isNotEmpty()) {
                                Text(
                                    text = element.preview.take(100) + if (element.preview.length > 100) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // WebView
                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true

                                // Add JavaScript interface for element selection
                                addJavascriptInterface(
                                    object {
                                        @JavascriptInterface
                                        fun onElementSelected(
                                            selector: String,
                                            tagName: String,
                                            preview: String,
                                        ) {
                                            selectedElement = ElementInfo(selector, tagName, preview)
                                        }
                                    },
                                    "Android",
                                )

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false

                                        // Inject element selection JavaScript
                                        view?.evaluateJavascript(elementSelectionScript, null)
                                    }
                                }

                                loadUrl(url)
                                webView = this
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )

                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                // Bottom Actions
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                selectedElement?.let { element ->
                                    onElementSelected(element.selector, element.preview)
                                }
                            },
                            enabled = selectedElement != null,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Monitor Element")
                        }
                    }
                }
            }
        }
    }
}

private val elementSelectionScript = """
(function() {
    let selectedElement = null;
    
    // Style for highlighting elements
    const highlightStyle = `
        outline: 3px solid #2196F3 !important;
        outline-offset: 2px !important;
        background-color: rgba(33, 150, 243, 0.1) !important;
        cursor: pointer !important;
        z-index: 999999 !important;
    `;
    
    // Remove existing highlights
    function clearHighlights() {
        document.querySelectorAll('[data-element-selector]').forEach(el => {
            el.removeAttribute('data-element-selector');
            el.style.cssText = el.style.cssText.replace(/outline[^;]*;?/g, '').replace(/background-color[^;]*;?/g, '');
        });
    }
    
    // Generate CSS selector for element
    function generateSelector(element) {
        if (element.id) {
            return '#' + element.id;
        }
        
        let path = [];
        while (element && element.nodeType === Node.ELEMENT_NODE) {
            let selector = element.nodeName.toLowerCase();
            
            if (element.className) {
                const classes = element.className.split(' ').filter(c => c.trim()).slice(0, 2);
                if (classes.length > 0) {
                    selector += '.' + classes.join('.');
                }
            }
            
            // Add nth-child if needed for uniqueness
            let sibling = element;
            let nth = 1;
            while ((sibling = sibling.previousElementSibling) !== null) {
                if (sibling.nodeName.toLowerCase() === element.nodeName.toLowerCase()) nth++;
            }
            
            if (nth > 1) {
                selector += ':nth-child(' + nth + ')';
            }
            
            path.unshift(selector);
            element = element.parentNode;
            
            // Stop at body or if we have enough specificity
            if (element.nodeName.toLowerCase() === 'body' || path.length > 4) break;
        }
        
        return path.join(' > ');
    }
    
    // Handle element hover
    function handleMouseOver(event) {
        event.stopPropagation();
        clearHighlights();
        
        event.target.setAttribute('data-element-selector', 'hover');
        event.target.style.cssText += highlightStyle;
    }
    
    // Handle element click
    function handleClick(event) {
        event.preventDefault();
        event.stopPropagation();
        
        clearHighlights();
        selectedElement = event.target;
        
        const selector = generateSelector(selectedElement);
        const tagName = selectedElement.tagName.toLowerCase();
        const preview = selectedElement.textContent.trim();
        
        // Permanently highlight selected element
        selectedElement.style.cssText += highlightStyle.replace('#2196F3', '#4CAF50');
        
        // Send to Android
        if (typeof Android !== 'undefined') {
            Android.onElementSelected(selector, tagName, preview);
        }
    }
    
    // Add event listeners to all elements
    document.querySelectorAll('*').forEach(element => {
        element.addEventListener('mouseover', handleMouseOver);
        element.addEventListener('click', handleClick);
    });
    
    // Prevent default link clicks
    document.addEventListener('click', function(e) {
        e.preventDefault();
    }, true);
    
    console.log('Element selector activated');
})();
""".trimIndent()

private data class ElementInfo(
    val selector: String,
    val tagName: String,
    val preview: String,
)
