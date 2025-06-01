package com.bernaferrari.changedetection.localBrowser

import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.extensions.replaceRelativePathWithAbsolute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalBrowserScreen(
    snapId: String,
    url: String,
    onDismiss: () -> Unit,
    viewModel: LocalBrowserViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    LaunchedEffect(snapId) {
        viewModel.fetchData(snapId)
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top bar
        TopAppBar(
            title = { Text("Local Browser") },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        )
        
        // Content
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                        }
                    },
                    update = { webView ->
                        if (state.content.isNotBlank()) {
                            val processedContent = state.content.replaceRelativePathWithAbsolute(url)
                            webView.loadDataWithBaseURL("", processedContent, "text/html", "UTF-8", "")
                        } else {
                            webView.loadUrl("about:blank")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
