package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.bernaferrari.changedetection.repo.source.WebResult
import kotlinx.coroutines.launch

@Composable
fun CompareDialog(
    url: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var directResult by remember { mutableStateOf<String?>(null) }
    var directLoading by remember { mutableStateOf(true) }

    var browserResult by remember { mutableStateOf<String?>(null) }
    var browserLoading by remember { mutableStateOf(true) }

    var webView by remember { mutableStateOf<CustomWebView?>(null) }

    LaunchedEffect(url) {
        // Direct request
        scope.launch {
//            val result = fetchFromServer(url)
//            directResult = when {
//                result.second.isNotEmpty() -> result.second.size.readableFileSize()
//                else -> result.first
//            }
            directLoading = false
        }

        // WebView request
        scope.launch {
            webView = CustomWebView(context)
            val result = fetchFromWebView(url, context, webView!!)
            browserResult = when (result) {
                is WebResult.Success -> result.data.size.readableFileSize()
                is WebResult.Error -> result.description
            }
            browserLoading = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Compare Results",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Direct request section
                Column {
                    Text(
                        text = "Direct Request",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (directLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Text("Loading...")
                        }
                    } else {
                        Text(
                            text = directResult ?: "No result",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Browser request section
                Column {
                    Text(
                        text = "Browser Request",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (browserLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Text("Loading...")
                        }
                    } else {
                        Text(
                            text = browserResult ?: "No result",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

object CompareDialogHelper {
    @Composable
    fun ShowCompareDialog(
        url: String?,
        onDismiss: () -> Unit
    ) {
        url?.let {
            CompareDialog(
                url = it,
                onDismiss = onDismiss
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CompareDialogPreview() {
    MaterialTheme {
        CompareDialogContent(
            directResult = "1.2 MB",
            directLoading = false,
            browserResult = "1.5 MB",
            browserLoading = false,
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CompareDialogLoadingPreview() {
    MaterialTheme {
        CompareDialogContent(
            directResult = null,
            directLoading = true,
            browserResult = "Error: Network timeout",
            browserLoading = false,
            onDismiss = {}
        )
    }
}

@Composable
private fun CompareDialogContent(
    directResult: String?,
    directLoading: Boolean,
    browserResult: String?,
    browserLoading: Boolean,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Compare Results",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Direct request section
            Column {
                Text(
                    text = "Direct Request",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (directLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Text("Loading...")
                    }
                } else {
                    Text(
                        text = directResult ?: "No result",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Browser request section
            Column {
                Text(
                    text = "Browser Request",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (browserLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Text("Loading...")
                    }
                } else {
                    Text(
                        text = browserResult ?: "No result",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}
