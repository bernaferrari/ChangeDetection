package com.bernaferrari.changedetection.addedit

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.extensions.fixUrlIfNecessary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    initialUrl: String = "https://google.com",
    onNavigateUp: () -> Unit,
    onUrlSelected: (String) -> Unit,
    viewModel: SharedViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var displayUrl by remember { mutableStateOf(initialUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { Text("Web View") },
            navigationIcon = {
                IconButton(onClick = onNavigateUp) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            },
            actions = {
                IconButton(
                    onClick = { webView?.goBack() },
                    enabled = canGoBack
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                TextButton(
                    onClick = {
                        onUrlSelected(currentUrl)
                        onNavigateUp()
                    }
                ) {
                    Text("Select")
                }
            }
        )

        // URL Input
        OutlinedTextField(
            value = displayUrl,
            onValueChange = { displayUrl = it },
            label = { Text("URL") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(
                onGo = {
                    keyboardController?.hide()
                    val fixedUrl = displayUrl.fixUrlIfNecessary()
                    currentUrl = fixedUrl
                    webView?.loadUrl(fixedUrl)
                }
            ),
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        )

        // WebView
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            isLoading = true
                            url?.let { 
                                currentUrl = it
                                displayUrl = it
                            }
                            canGoBack = view?.canGoBack() == true
                            super.onPageStarted(view, url, favicon)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            url?.let { 
                                currentUrl = it
                                displayUrl = it
                            }
                            canGoBack = view?.canGoBack() == true
                            super.onPageFinished(view, url)
                        }
                    }
                    
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    loadUrl(initialUrl)
                }
            },
            update = { view ->
                webView = view
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
        }
    }
}
