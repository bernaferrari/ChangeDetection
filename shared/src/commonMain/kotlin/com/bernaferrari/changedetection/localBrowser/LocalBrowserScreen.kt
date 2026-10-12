package com.bernaferrari.changedetection.localBrowser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import com.bernaferrari.changedetection.browser.BrowserRuntimeHost
import com.bernaferrari.changedetection.browser.PlatformBrowserView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.a11y_close
import com.bernaferrari.changedetection.shared.generated.resources.a11y_saved_screenshot
import com.bernaferrari.changedetection.shared.generated.resources.a11y_saved_screenshot_source
import com.bernaferrari.changedetection.shared.generated.resources.a11y_saved_screenshot_source_only
import com.bernaferrari.changedetection.shared.generated.resources.local_browser_empty_snapshot
import com.bernaferrari.changedetection.shared.generated.resources.local_browser_error_body
import com.bernaferrari.changedetection.shared.generated.resources.local_browser_error_title
import com.bernaferrari.changedetection.shared.generated.resources.local_browser_title_live
import com.bernaferrari.changedetection.shared.generated.resources.local_browser_title_snapshot
import com.bernaferrari.changedetection.shared.generated.resources.retry
import com.bernaferrari.changedetection.ui.comparison.SnapshotImage
import com.bernaferrari.changedetection.ui.comparison.SecureSnapshotWebView
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.util.formatTimestamp
import com.bernaferrari.changedetection.util.readableFileSize
import com.bernaferrari.changedetection.util.displayPageAddress
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import com.bernaferrari.changedetection.localization.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LocalBrowserScreen(
    snapId: String,
    url: String,
    onDismiss: () -> Unit,
    viewModel: LocalBrowserViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLocalSnapshot = snapId.isNotBlank()
    val safeUrl = remember(url) { url.trim() }

    LaunchedEffect(snapId) {
        if (isLocalSnapshot) viewModel.fetchData(snapId) else viewModel.reset()
    }

    Scaffold(
        topBar = {
            com.bernaferrari.changedetection.ui.navigation.AppTopBar(
                title = stringResource(if (isLocalSnapshot) Res.string.local_browser_title_snapshot else Res.string.local_browser_title_live),
                onBack = onDismiss,
            )
        },
    ) { paddingValues ->
        when {
            (isLocalSnapshot && state.hasError) || (!isLocalSnapshot && safeUrl.isBlank()) -> SharedEmptyState(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(Res.string.local_browser_error_title),
                description = stringResource(Res.string.local_browser_error_body),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                primaryActionLabel = if (isLocalSnapshot) stringResource(Res.string.retry) else null,
                onPrimaryAction = if (isLocalSnapshot) ({ viewModel.fetchData(snapId) }) else null,
            )

            isLocalSnapshot && state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator()
            }

            isLocalSnapshot && state.content.isBlank() && state.bytes?.isEmpty() != false -> SharedEmptyState(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(Res.string.local_browser_empty_snapshot),
                description = stringResource(Res.string.local_browser_error_body),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )

            isLocalSnapshot -> {
                val content = state.content
                val isImage = state.mimeType.startsWith("image/", ignoreCase = true)
                val isHtmlDocument = state.mimeType.startsWith("text/html", ignoreCase = true) && content.hasHtmlMarkup()
                val isShortText = !isImage && !isHtmlDocument && state.content.length < 800
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                ) {
                    SnapshotContextHeader(url = safeUrl, state = state)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isShortText) Modifier.heightIn(min = 160.dp) else Modifier.weight(1f))
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        shape = RoundedCornerShape(26.dp, 26.dp, 10.dp, 26.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        when {
                            isImage && state.bytes?.isNotEmpty() == true -> {
                                val source = safeUrl.displayPageAddress()
                                val capturedAt = state.timestamp?.let(::formatTimestamp)
                                val screenshotDescription = when {
                                    source.isNotBlank() && capturedAt != null -> stringResource(
                                        Res.string.a11y_saved_screenshot_source,
                                        source,
                                        capturedAt,
                                    )

                                    source.isNotBlank() -> stringResource(
                                        Res.string.a11y_saved_screenshot_source_only,
                                        source,
                                    )

                                    else -> stringResource(Res.string.a11y_saved_screenshot)
                                }
                                SnapshotImage(
                                    bytes = requireNotNull(state.bytes),
                                    contentDescription = screenshotDescription,
                                    modifier = Modifier.fillMaxSize().padding(8.dp).clip(RoundedCornerShape(20.dp)),
                                    contentScale = ContentScale.Fit,
                                )
                            }

                            isHtmlDocument -> SecureSnapshotWebView(
                                html = content,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp, 26.dp, 10.dp, 26.dp)),
                            )

                            else -> SelectionContainer {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(24.dp),
                                ) {
                                    Text(
                                        text = state.content,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> BrowserRuntimeHost(Modifier.fillMaxSize().padding(paddingValues)) {
                PlatformBrowserView(state = rememberWebViewState(url = safeUrl), modifier = Modifier.fillMaxSize().padding(paddingValues))
            }
        }
    }
}

@Composable
private fun SnapshotContextHeader(url: String, state: BrowserState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(15.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Article,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = url.displayPageAddress(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            val details = buildList {
                state.timestamp?.let { add(formatTimestamp(it)) }
                if (state.contentSize > 0) add(state.contentSize.toLong().readableFileSize())
            }.joinToString(" · ")
            if (details.isNotBlank()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

private val htmlElementPattern = Regex("<[a-zA-Z][^>]*>")

private fun String.hasHtmlMarkup(): Boolean = htmlElementPattern.containsMatchIn(this)
