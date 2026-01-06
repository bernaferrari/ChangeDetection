package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.statusBarsPadding
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import com.bernaferrari.changedetection.R

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.util.formatTimestamp
import com.bernaferrari.changedetection.util.UnifiedDiffUtils
import com.bernaferrari.changedetection.ui.theme.ExpressiveSpring
import com.orhanobut.logger.Logger
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ComparisonScreen(
    siteId: String,
    onNavigateUp: () -> Unit,
    viewModel: ComparisonViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(siteId) {
        viewModel.loadSiteAndSnaps(siteId)
    }

    // Auto-select first pair when snaps are loaded
    LaunchedEffect(uiState.snaps) {
        if (uiState.snaps.size > 1 && uiState.selectedPairIndex == null) {
            viewModel.selectPairForComparison(0)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.site?.title?.takeIf { it.isNotBlank() }
                                ?: stringResource(id = R.string.site_details_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        uiState.site?.url?.let { url ->
                            Text(
                                text = url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator()
                }
            }

            uiState.site == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.site_not_found),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            uiState.snaps.isEmpty() -> {
                EmptySnapshotState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            uiState.snaps.size == 1 -> {
                SingleSnapshotState(
                    snap = uiState.snaps.first(),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            else -> {
                DiffFirstContent(
                    snaps = uiState.snaps,
                    selectedPairIndex = uiState.selectedPairIndex ?: 0,
                    content1 = uiState.snapContent1,
                    content2 = uiState.snapContent2,
                    isLoadingDiff = uiState.isLoadingDiff,
                    onSelectPair = { viewModel.selectPairForComparison(it) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun EmptySnapshotState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.comparison_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.comparison_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SingleSnapshotState(
    snap: Snap,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "1 snapshot captured",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.comparison_single_snapshot_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = formatTimestamp(snap.timestamp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DiffFirstContent(
    snaps: List<Snap>,
    selectedPairIndex: Int,
    content1: String?,
    content2: String?,
    isLoadingDiff: Boolean,
    onSelectPair: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier) {
        // Version selector - horizontal scrollable chips
        VersionSelector(
            snaps = snaps,
            selectedIndex = selectedPairIndex,
            onSelectPair = onSelectPair
        )

        // Diff mode selector
        var mode by remember { mutableStateOf("TEXT") }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = mode == "TEXT",
                onClick = { mode = "TEXT" },
                label = { Text(stringResource(id = R.string.text_diff)) }
            )
            FilterChip(
                selected = mode == "HTML",
                onClick = { mode = "HTML" },
                label = { Text(stringResource(id = R.string.rendered_html)) }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Share button
            if (content1 != null && content2 != null) {
                IconButton(onClick = {
                    shareUnifiedDiff(context, snaps[selectedPairIndex], snaps[selectedPairIndex + 1], content1, content2)
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share diff")
                }
            }
        }

        // Diff content
        if (isLoadingDiff) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularWavyProgressIndicator()
            }
        } else if (content1 != null && content2 != null) {
            when (mode) {
                "TEXT" -> {
                    SimpleDiffView(
                        oldContent = content1,
                        newContent = content2,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                "HTML" -> {
                    RenderedHtmlComparison(
                        oldContent = content1,
                        newContent = content2,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.failed_to_load_content),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun VersionSelector(
    snaps: List<Snap>,
    selectedIndex: Int,
    onSelectPair: (Int) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "${snaps.size} versions",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 0 until snaps.size - 1) {
                val isSelected = i == selectedIndex
                val snap1 = snaps[i]
                
                VersionChip(
                    label = "v${snaps.size - i} → v${snaps.size - i - 1}",
                    subtitle = formatTimestamp(snap1.timestamp),
                    isSelected = isSelected,
                    onClick = { onSelectPair(i) }
                )
            }
        }
    }
}

@Composable
private fun VersionChip(
    label: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS
        ),
        label = "chipColor"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SimpleDiffView(
    oldContent: String,
    newContent: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showOnlyChanges by remember { mutableStateOf(false) }

    val oldLines = remember(oldContent) { oldContent.lines() }
    val newLines = remember(newContent) { newContent.lines() }
    val maxLines = remember(oldLines, newLines) { maxOf(oldLines.size, newLines.size) }
    val changeStats = remember(oldLines, newLines) { calculateChangeStats(oldLines, newLines) }
    val primaryLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val addedLineColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)

    Row(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Stats and filter row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ComparisonStatsRow(stats = changeStats)
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !showOnlyChanges,
                        onClick = { showOnlyChanges = false },
                        label = { Text(stringResource(id = R.string.filter_all)) }
                    )
                    FilterChip(
                        selected = showOnlyChanges,
                        onClick = { showOnlyChanges = true },
                        label = { Text(stringResource(id = R.string.filter_only_changes)) }
                    )
                }
            }

            // Diff lines
            for (i in 0 until maxLines) {
                val oldLine = oldLines.getOrNull(i) ?: ""
                val newLine = newLines.getOrNull(i) ?: ""

                if (!showOnlyChanges || oldLine != newLine) {
                    when {
                        oldLine == newLine -> {
                            DiffLine(i + 1, oldLine, DiffLineType.UNCHANGED)
                        }
                        oldLine.isNotEmpty() && newLine.isNotEmpty() -> {
                            DiffLine(i + 1, oldLine, DiffLineType.REMOVED)
                            DiffLine(i + 1, newLine, DiffLineType.ADDED)
                        }
                        oldLine.isNotEmpty() -> {
                            DiffLine(i + 1, oldLine, DiffLineType.REMOVED)
                        }
                        newLine.isNotEmpty() -> {
                            DiffLine(i + 1, newLine, DiffLineType.ADDED)
                        }
                    }
                }
            }
        }

        // Minimap
        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .padding(top = 16.dp, bottom = 16.dp, end = 8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val total = maxLines.coerceAtLeast(1)
                val h = size.height
                val step = h / total
                var y = 0f
                for (idx in 0 until total) {
                    val oldLine = oldLines.getOrNull(idx) ?: ""
                    val newLine = newLines.getOrNull(idx) ?: ""
                    val color = when {
                        oldLine == newLine -> Color.Transparent
                        oldLine.isNotEmpty() && newLine.isNotEmpty() -> addedLineColor
                        else -> primaryLineColor
                    }
                    if (color.alpha > 0f) drawLine(
                        color = color,
                        start = Offset(0f, y),
                        end = Offset(size.width, y)
                    )
                    y += step
                }
            }
        }
    }
}

@Composable
private fun RenderedHtmlComparison(
    oldContent: String,
    newContent: String,
    modifier: Modifier = Modifier
) {
    val (removedKeywords, addedKeywords) = remember(oldContent, newContent) {
        extractChangedKeywords(oldContent, newContent)
    }
    val removedJsArray = remember(removedKeywords) { 
        removedKeywords.joinToString(prefix = "[\"", postfix = "\"]", separator = "\",\"") 
    }
    val addedJsArray = remember(addedKeywords) { 
        addedKeywords.joinToString(prefix = "[\"", postfix = "\"]", separator = "\",\"") 
    }
    val addColor = "rgba(76,175,80,0.18)"
    val remColor = "rgba(244,67,54,0.18)"

    var oldWebView by remember { mutableStateOf<WebView?>(null) }
    var newWebView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                oldWebView?.stopLoading()
                oldWebView?.clearHistory()
                oldWebView?.clearCache(true)
                oldWebView?.destroy()
                oldWebView = null
            } catch (e: Exception) {
                Logger.e("Error cleaning up old WebView: ${e.message}")
            }
            try {
                newWebView?.stopLoading()
                newWebView?.clearHistory()
                newWebView?.clearCache(true)
                newWebView?.destroy()
                newWebView = null
            } catch (e: Exception) {
                Logger.e("Error cleaning up new WebView: ${e.message}")
            }
        }
    }

    Row(modifier = modifier) {
        val sanitizedOldContent = remember(oldContent) {
            com.bernaferrari.changedetection.util.HtmlSanitizer.sanitize(oldContent)
        }
        val sanitizedNewContent = remember(newContent) {
            com.bernaferrari.changedetection.util.HtmlSanitizer.sanitize(newContent)
        }

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            val js = buildHighlighterJs()
                            view?.evaluateJavascript(js, null)
                            view?.evaluateJavascript(
                                "window.applyChangeHighlights($removedJsArray, '$remColor');",
                                null
                            )
                        }
                    }
                }.also { oldWebView = it }
            },
            update = { it.loadDataWithBaseURL(null, sanitizedOldContent, "text/html", "utf-8", null) },
            modifier = Modifier.weight(1f)
        )

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            val js = buildHighlighterJs()
                            view?.evaluateJavascript(js, null)
                            view?.evaluateJavascript(
                                "window.applyChangeHighlights($addedJsArray, '$addColor');",
                                null
                            )
                        }
                    }
                }.also { newWebView = it }
            },
            update = { it.loadDataWithBaseURL(null, sanitizedNewContent, "text/html", "utf-8", null) },
            modifier = Modifier.weight(1f)
        )
    }
}

private fun extractChangedKeywords(oldHtml: String, newHtml: String): Pair<List<String>, List<String>> {
    fun tokenize(s: String): Set<String> {
        val stop = setOf(
            "the", "and", "with", "from", "that", "this", "have", "has", "for",
            "you", "your", "are", "was", "were", "will", "been", "into", "about",
            "over", "under", "more", "less", "only", "very", "also", "http",
            "https", "www", "com"
        )
        val text = s.replace(Regex("<[^>]+>"), " ")
        return text.split(Regex("[\\s\\p{Punct}]+"))
            .filter { it.length >= 3 && it !in stop }
            .map { it.lowercase() }
            .toSet()
    }

    val oldTokens = tokenize(oldHtml)
    val newTokens = tokenize(newHtml)
    val removed = (oldTokens - newTokens).take(30)
    val added = (newTokens - oldTokens).take(30)
    return removed.toList() to added.toList()
}

private fun buildHighlighterJs(): String = """
(function() {
    window.applyChangeHighlights = function(keywords, bgColor) {
        if (!keywords || keywords.length === 0) return;
        var pattern = keywords.join('|');
        var regex = new RegExp('\\b(' + pattern + ')\\b', 'gi');
        var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null, false);
        var nodes = [];
        while(walker.nextNode()) nodes.push(walker.currentNode);
        nodes.forEach(function(node) {
            if (regex.test(node.nodeValue)) {
                var span = document.createElement('span');
                span.innerHTML = node.nodeValue.replace(regex, '<mark style="background:' + bgColor + '">$1</mark>');
                node.parentNode.replaceChild(span, node);
            }
        });
    };
})();
""".trimIndent()

private fun shareUnifiedDiff(
    context: android.content.Context,
    snap1: Snap,
    snap2: Snap,
    content1: String,
    content2: String
) {
    try {
        val oldLines = content1.lines()
        val newLines = content2.lines()
        val patch = com.bernaferrari.difflib.DiffUtils.diff(oldLines, newLines)
        val diff = UnifiedDiffUtils.generateUnifiedDiff(
            originalFileName = "v${snap1.timestamp}",
            revisedFileName = "v${snap2.timestamp}",
            originalLines = oldLines,
            patch = patch,
            contextSize = 3
        )
        val diffText = diff.joinToString("\n")
        
        val cacheDir = File(context.cacheDir, "diffs")
        cacheDir.mkdirs()
        val diffFile = File(cacheDir, "diff_${System.currentTimeMillis()}.diff")
        diffFile.writeText(diffText)
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            diffFile
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share diff"))
    } catch (e: Exception) {
        Logger.e("Failed to share diff: ${e.message}")
    }
}
