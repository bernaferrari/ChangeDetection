/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.elementselector

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme

class ElementSelectorActivity : ComponentActivity() {

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_SITE_ID = "site_id"
        const val RESULT_CSS_SELECTOR = "css_selector"
        const val RESULT_XPATH = "xpath"
        const val RESULT_ELEMENT_TEXT = "element_text"

        fun createIntent(context: Context, url: String, siteId: String): Intent = Intent(context, ElementSelectorActivity::class.java).apply {
            putExtra(EXTRA_URL, url)
            putExtra(EXTRA_SITE_ID, siteId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL) ?: ""
        val siteId = intent.getStringExtra(EXTRA_SITE_ID) ?: ""

        setContent {
            ChangeDetectionTheme {
                ElementSelectorScreen(
                    url = url,
                    onElementSelected = { cssSelector, xpath, elementText ->
                        val resultIntent = Intent().apply {
                            putExtra(RESULT_CSS_SELECTOR, cssSelector)
                            putExtra(RESULT_XPATH, xpath)
                            putExtra(RESULT_ELEMENT_TEXT, elementText)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    },
                    onCancel = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElementSelectorScreen(
    url: String,
    onElementSelected: (cssSelector: String, xpath: String, elementText: String) -> Unit,
    onCancel: () -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    var selectedElement by remember { mutableStateOf<ElementInfo?>(null) }
    var elementHierarchy by remember { mutableStateOf<List<ElementInfo>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showInstructions by remember { mutableStateOf(true) }

    // Cleanup WebView and JavaScript interface when activity finishes
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            try {
                webView?.stopLoading()
                webView?.removeJavascriptInterface("Android")
                webView?.clearHistory()
                webView?.clearCache(true)
                webView?.destroy()
                webView = null
            } catch (e: Exception) {
                io.github.aakira.napier.Napier.e("❌ Error cleaning up ElementSelectorActivity WebView: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Select Element",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = "Choose what to monitor",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showInstructions = true },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
                    }
                },
            )
        },
        bottomBar = {
            if (selectedElement != null) {
                ElementSelectionBottomBar(
                    elementHierarchy = elementHierarchy,
                    selectedIndex = selectedIndex,
                    onIndexChanged = { index ->
                        selectedIndex = index
                        webView?.evaluateJavascript(
                            "highlightElementByIndex($index)",
                            null,
                        )
                    },
                    onConfirm = {
                        val element = elementHierarchy.getOrNull(selectedIndex)
                        if (element != null) {
                            onElementSelected(element.cssSelector, element.xpath, element.text)
                        }
                    },
                    onCancel = {
                        selectedElement = null
                        elementHierarchy = emptyList()
                        webView?.evaluateJavascript("clearHighlights()", null)
                    },
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false

                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun onElementSelected(hierarchyJson: String) {
                                    // Parse hierarchy JSON and update UI
                                    val elements = parseElementHierarchy(hierarchyJson)
                                    elementHierarchy = elements
                                    selectedElement = elements.firstOrNull()
                                    selectedIndex = 0
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading page...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showInstructions) {
        InstructionsDialog(
            onDismiss = { showInstructions = false },
        )
    }
}

@Composable
private fun ElementSelectionBottomBar(
    elementHierarchy: List<ElementInfo>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Element hierarchy slider
            if (elementHierarchy.size > 1) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Adjust Selection (${selectedIndex + 1}/${elementHierarchy.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Slider(
                        value = selectedIndex.toFloat(),
                        onValueChange = { onIndexChanged(it.toInt()) },
                        valueRange = 0f..(elementHierarchy.size - 1).toFloat(),
                        steps = elementHierarchy.size - 2,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }

            // Selected element info
            val selectedElement = elementHierarchy.getOrNull(selectedIndex)
            if (selectedElement != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = selectedElement.tagName.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (selectedElement.text.isNotBlank()) {
                            Text(
                                text = selectedElement.text.take(100) +
                                    if (selectedElement.text.length > 100) "..." else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            text = selectedElement.cssSelector,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Monitor This")
                }
            }
        }
    }
}

@Composable
private fun InstructionsDialog(
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "How to Select Elements",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    InstructionStep(
                        number = "1",
                        title = "Tap any element",
                        description = "Click on the content you want to monitor",
                    )
                    InstructionStep(
                        number = "2",
                        title = "Adjust selection",
                        description = "Use the slider to select parent elements for broader monitoring",
                    )
                    InstructionStep(
                        number = "3",
                        title = "Confirm choice",
                        description = "Tap 'Monitor This' when you're happy with the selection",
                    )
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Got it!")
                }
            }
        }
    }
}

@Composable
private fun InstructionStep(
    number: String,
    title: String,
    description: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = Color.White,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun parseElementHierarchy(hierarchyJson: String): List<ElementInfo> {
    // Basic parsing implementation - in a real app this would use a proper JSON parser
    return try {
        if (hierarchyJson.isBlank()) {
            emptyList()
        } else {
            // For now, return a mock element structure
            // In practice, this would parse actual HTML element hierarchy
            listOf(
                ElementInfo(
                    tagName = "div",
                    cssSelector = "#main-content",
                    xpath = "//div[@id='main-content']",
                    text = "Main content area",
                    className = "content",
                    id = "main-content",
                ),
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

data class ElementInfo(
    val tagName: String,
    val cssSelector: String,
    val xpath: String,
    val text: String,
    val className: String = "",
    val id: String = "",
)

private val elementSelectionScript = """
(function() {
    let isSelectionMode = true;
    let selectedElements = [];
    let highlightedElement = null;
    
    const primaryColor = '#2196F3';
    const highlightOpacity = '0.3';
    
    // Create overlay div for highlighting
    const overlay = document.createElement('div');
    overlay.style.position = 'absolute';
    overlay.style.pointerEvents = 'none';
    overlay.style.zIndex = '10000';
    overlay.style.backgroundColor = primaryColor + '80';
    overlay.style.border = '2px solid ' + primaryColor;
    overlay.style.borderRadius = '4px';
    overlay.style.display = 'none';
    overlay.style.transition = 'all 0.2s ease';
    document.body.appendChild(overlay);
    
    function generateCssSelector(element) {
        if (element.id) return '#' + element.id;
        
        let path = [];
        let current = element;
        
        while (current && current.nodeType === Node.ELEMENT_NODE) {
            let selector = current.nodeName.toLowerCase();
            
            if (current.className) {
                const classes = current.className.split(' ').filter(c => c.trim());
                if (classes.length > 0) {
                    selector += '.' + classes.slice(0, 2).join('.');
                }
            }
            
            // Add nth-child if needed
            let sibling = current;
            let nth = 1;
            while ((sibling = sibling.previousElementSibling) !== null) {
                if (sibling.nodeName.toLowerCase() === current.nodeName.toLowerCase()) nth++;
            }
            if (nth > 1) {
                selector += ':nth-child(' + nth + ')';
            }
            
            path.unshift(selector);
            current = current.parentNode;
            
            if (current.nodeName.toLowerCase() === 'body' || path.length > 6) break;
        }
        
        return path.join(' > ');
    }
    
    function generateXPath(element) {
        if (element.id) return "//*[@id='" + element.id + "']";
        
        let path = '';
        let current = element;
        
        while (current && current.nodeType === Node.ELEMENT_NODE) {
            let index = 1;
            let sibling = current.previousSibling;
            
            while (sibling) {
                if (sibling.nodeType === Node.ELEMENT_NODE && 
                    sibling.nodeName === current.nodeName) {
                    index++;
                }
                sibling = sibling.previousSibling;
            }
            
            const tagName = current.nodeName.toLowerCase();
            path = '/' + tagName + '[' + index + ']' + path;
            current = current.parentNode;
            
            if (current.nodeName.toLowerCase() === 'body') break;
        }
        
        return '//body' + path;
    }
    
    function getElementHierarchy(element) {
        const hierarchy = [];
        let current = element;
        
        while (current && current !== document.body) {
            const elementInfo = {
                tagName: current.nodeName.toLowerCase(),
                cssSelector: generateCssSelector(current),
                xpath: generateXPath(current),
                text: current.textContent.trim().slice(0, 200),
                className: current.className || '',
                id: current.id || ''
            };
            hierarchy.push(elementInfo);
            current = current.parentElement;
        }
        
        return hierarchy;
    }
    
    function highlightElement(element) {
        if (!element) return;
        
        const rect = element.getBoundingClientRect();
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
        const scrollLeft = window.pageXOffset || document.documentElement.scrollLeft;
        
        overlay.style.display = 'block';
        overlay.style.left = (rect.left + scrollLeft) + 'px';
        overlay.style.top = (rect.top + scrollTop) + 'px';
        overlay.style.width = rect.width + 'px';
        overlay.style.height = rect.height + 'px';
        
        highlightedElement = element;
    }
    
    function clearHighlights() {
        overlay.style.display = 'none';
        highlightedElement = null;
        selectedElements = [];
    }
    
    // Global functions for external control
    window.highlightElementByIndex = function(index) {
        if (selectedElements[index]) {
            highlightElement(selectedElements[index].element);
        }
    };
    
    window.clearHighlights = clearHighlights;
    
    // Event handlers
    function handleMouseOver(event) {
        if (!isSelectionMode) return;
        event.stopPropagation();
        highlightElement(event.target);
    }
    
    function handleClick(event) {
        if (!isSelectionMode) return;
        event.preventDefault();
        event.stopPropagation();
        
        const hierarchy = getElementHierarchy(event.target);
        selectedElements = hierarchy.map(info => ({
            info: info,
            element: event.target // Keep reference for highlighting
        }));
        
        // Send hierarchy to Android
        if (typeof Android !== 'undefined') {
            Android.onElementSelected(JSON.stringify(hierarchy));
        }
    }
    
    // Add event listeners
    document.addEventListener('mouseover', handleMouseOver, true);
    document.addEventListener('click', handleClick, true);
    
    // Prevent default link behavior
    document.addEventListener('click', function(e) {
        if (isSelectionMode) {
            e.preventDefault();
        }
    }, true);
    
    console.log('Element selector initialized');
})();
""".trimIndent()
