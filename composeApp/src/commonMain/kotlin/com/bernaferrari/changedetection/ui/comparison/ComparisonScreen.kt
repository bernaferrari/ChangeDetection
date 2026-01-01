package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material3.*
import androidx.compose.material3.surfaceColorAtElevation
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import com.bernaferrari.changedetection.R
import com.bernaferrari.difflib.DiffUtils

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.Site
import java.text.SimpleDateFormat
import java.util.*
import com.bernaferrari.changedetection.util.formatTimestamp
import com.bernaferrari.changedetection.util.UnifiedDiffUtils
import com.bernaferrari.changedetection.ui.comparison.ComparisonHeroSection
import com.bernaferrari.changedetection.ui.comparison.ComparisonStatsRow
import com.bernaferrari.changedetection.ui.comparison.ComparisonPairCard
import com.bernaferrari.changedetection.ui.comparison.SnapshotCard
import com.orhanobut.logger.Logger
import com.bernaferrari.changedetection.ui.design.CanvasEmptyState
import com.bernaferrari.changedetection.ui.design.CanvasHero
import com.bernaferrari.changedetection.ui.design.CanvasSectionHeader
import com.bernaferrari.changedetection.ui.design.InsightCard
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(
    siteId: String,
    onNavigateUp: () -> Unit,
    viewModel: ComparisonViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(siteId) {
        viewModel.loadSiteAndSnaps(siteId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.site?.title
                                ?: androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.site_details_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        uiState.site?.let { site ->
                            Text(
                                text = site.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                }
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
                    CircularProgressIndicator()
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
                        text = androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.site_not_found),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            uiState.snaps.isEmpty() -> {
                val site = uiState.site ?: return@Scaffold
                ComparisonEmptyContent(
                    paddingValues = paddingValues,
                    site = site
                )
            }

            else -> {
                val site = uiState.site ?: return@Scaffold
                val selectedPairIndex = uiState.selectedPairIndex
                if (selectedPairIndex != null && uiState.snaps.size > selectedPairIndex + 1) {
                    DiffComparisonView(
                        snap1 = uiState.snaps[selectedPairIndex],
                        snap2 = uiState.snaps[selectedPairIndex + 1],
                        content1 = uiState.snapContent1,
                        content2 = uiState.snapContent2,
                        isLoading = uiState.isLoadingDiff,
                        onBack = { viewModel.clearSelection() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                } else {
                    ComparisonLoadedContent(
                        paddingValues = paddingValues,
                        site = site,
                        uiState = uiState,
                        onSelectPair = { index -> viewModel.selectPairForComparison(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonLoadedContent(
    paddingValues: PaddingValues,
    site: Site,
    uiState: ComparisonUiState,
    onSelectPair: (Int) -> Unit
) {
    val snaps = uiState.snaps
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            ComparisonHeroSection(site = site, totalSnapshots = snaps.size)
        }
        item {
            ComparisonHeaderStats(
                totalSnapshots = snaps.size,
                latestTimestamp = snaps.firstOrNull()?.timestamp,
                isElementMonitoring = false
            )
        }

        if (snaps.size > 1) {
            item {
                CanvasSectionHeader(
                    title = stringResource(id = R.string.comparison_section_snapshots)
                )
            }
            item {
                Text(
                    text = stringResource(id = R.string.comparison_quick_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(snaps.size - 1) { index ->
                ComparisonPairCard(
                    snap1 = snaps[index],
                    snap2 = snaps[index + 1],
                    pairIndex = index,
                    isSelected = uiState.selectedPairIndex == index,
                    onClick = { onSelectPair(index) }
                )
            }
        } else {
            item {
                CanvasSectionHeader(
                    title = stringResource(id = R.string.comparison_section_snapshots)
                )
            }
            item {
                SnapshotCard(
                    snap = snaps.first(),
                    onClick = {}
                )
            }
            item {
                Text(
                    text = stringResource(id = R.string.comparison_single_snapshot_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
private fun ComparisonEmptyContent(
    paddingValues: PaddingValues,
    site: Site
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            ComparisonHeroSection(site = site, totalSnapshots = 0)
        }
        item {
            CanvasEmptyState(
                icon = Icons.Default.Schedule,
                title = stringResource(id = R.string.comparison_empty_title),
                subtitle = stringResource(id = R.string.comparison_empty_subtitle),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ComparisonHeroSection(
    site: Site,
    totalSnapshots: Int
) {
    val title = site.title?.takeIf { it.isNotBlank() } ?: site.url
    CanvasHero(
        title = title,
        subtitle = site.url,
        badge = stringResource(id = R.string.comparison_hero_badge),
        modifier = Modifier.fillMaxWidth(),
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = totalSnapshots.toString(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(id = R.string.comparison_stats_snapshots_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun ComparisonHeaderStats(
    totalSnapshots: Int,
    latestTimestamp: Long?,
    isElementMonitoring: Boolean
) {
    val latestDisplay = latestTimestamp?.let { formatTimestamp(it) }
    val latestSupporting = latestDisplay?.let {
        stringResource(id = R.string.comparison_stats_latest_supporting, it)
    }
    val scopeValue = if (isElementMonitoring) {
        stringResource(id = R.string.comparison_stats_scope_element)
    } else {
        stringResource(id = R.string.comparison_stats_scope_page)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        InsightCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Schedule,
            label = stringResource(id = R.string.comparison_stats_snapshots_label),
            value = totalSnapshots.toString()
        )
        InsightCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.History,
            label = stringResource(id = R.string.comparison_stats_latest_label),
            value = latestDisplay ?: stringResource(id = R.string.comparison_stats_unavailable),
            supportingText = latestSupporting ?: stringResource(id = R.string.comparison_stats_unavailable)
        )
        InsightCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Visibility,
            label = stringResource(id = R.string.comparison_stats_scope_label),
            value = scopeValue
        )
    }
}


@Composable
private fun DiffComparisonView(
    snap1: Snap,
    snap2: Snap,
    content1: String?,
    content2: String?,
    isLoading: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                        }
                        Text(
                            text = androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.diff_comparison),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    if (content1 != null && content2 != null) {
                        Row {
                            IconButton(onClick = {
                                shareUnifiedDiff(context, snap1, snap2, content1, content2)
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share diff")
                            }
                            TextButton(onClick = onBack) { Text(androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.done)) }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = androidx.compose.ui.res.stringResource(
                                id = com.bernaferrari.changedetection.R.string.older_label,
                                formatTimestamp(snap1.timestamp)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(
                                id = com.bernaferrari.changedetection.R.string.size_label,
                                formatBytes(snap1.contentSize)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Comparison arrow",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(
                            text = androidx.compose.ui.res.stringResource(
                                id = com.bernaferrari.changedetection.R.string.newer_label,
                                formatTimestamp(snap2.timestamp)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(
                                id = com.bernaferrari.changedetection.R.string.size_label,
                                formatBytes(snap2.contentSize)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Diff content
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (content1 != null && content2 != null) {
            var mode by remember { mutableStateOf("VISUAL") }
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = mode == "TEXT",
                        onClick = { mode = "TEXT" },
                        label = { Text(androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.text_diff)) },
                        leadingIcon = if (mode == "TEXT") {
                            { Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null) }
                        } else null
                    )
                    FilterChip(
                        selected = mode == "HTML",
                        onClick = { mode = "HTML" },
                        label = { Text(androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.rendered_html)) },
                        leadingIcon = if (mode == "HTML") {
                            { Icon(Icons.Default.Language, contentDescription = null) }
                        } else null
                    )
                    FilterChip(
                        selected = mode == "VISUAL",
                        onClick = { mode = "VISUAL" },
                        label = { Text("Visual Slider") },
                        leadingIcon = if (mode == "VISUAL") {
                            { Icon(Icons.Default.Compare, contentDescription = null) }
                        } else null
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                when (mode) {
                    "TEXT" -> {
                        SimpleDiffView(
                            oldContent = content1,
                            newContent = content2,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 0.dp)
                        )
                    }

                    "HTML" -> {
                        RenderedHtmlComparison(
                            oldContent = content1,
                            newContent = content2,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    "VISUAL" -> {
                        VisualDiffView(
                            oldContent = content1,
                            newContent = content2,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.failed_to_load_content),
                    color = MaterialTheme.colorScheme.error
                )
            }
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

    // Precompute lines and stats once so both main column and minimap can use them
    val oldLines = remember(oldContent) { oldContent.lines() }
    val newLines = remember(newContent) { newContent.lines() }
    val maxLines = remember(oldLines, newLines) { maxOf(oldLines.size, newLines.size) }
    val changeStats = remember(oldLines, newLines) { calculateChangeStats(oldLines, newLines) }
    val primaryLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val addedLineColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // Enhanced diff header with statistics

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Content Comparison",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ComparisonStatsRow(stats = changeStats)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !showOnlyChanges,
                        onClick = { showOnlyChanges = false },
                        label = { Text(androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.filter_all)) }
                    )
                    FilterChip(
                        selected = showOnlyChanges,
                        onClick = { showOnlyChanges = true },
                        label = { Text(androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.filter_only_changes)) }
                    )
                }

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
    val removedJsArray =
        remember(removedKeywords) { removedKeywords.joinToString(prefix = "[\"", postfix = "\"]", separator = "\",\"") }
    val addedJsArray =
        remember(addedKeywords) { addedKeywords.joinToString(prefix = "[\"", postfix = "\"]", separator = "\",\"") }
    val addColor = "rgba(76,175,80,0.18)" // subtle green
    val remColor = "rgba(244,67,54,0.18)" // subtle red

    // Track WebView references for proper cleanup
    var oldWebView by remember { mutableStateOf<WebView?>(null) }
    var newWebView by remember { mutableStateOf<WebView?>(null) }

    // Cleanup WebViews when composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            try {
                oldWebView?.stopLoading()
                oldWebView?.clearHistory()
                oldWebView?.clearCache(true)
                oldWebView?.destroy()
                oldWebView = null
            } catch (e: Exception) {
                Logger.e("❌ Error cleaning up old WebView: ${e.message}")
            }

            try {
                newWebView?.stopLoading()
                newWebView?.clearHistory()
                newWebView?.clearCache(true)
                newWebView?.destroy()
                newWebView = null
            } catch (e: Exception) {
                Logger.e("❌ Error cleaning up new WebView: ${e.message}")
            }
        }
    }

    Row(modifier = modifier) {
        // Sanitize HTML before rendering to prevent XSS attacks
        val sanitizedOldContent = remember(oldContent) {
            com.bernaferrari.changedetection.util.HtmlSanitizer.sanitize(
                oldContent
            )
        }
        val sanitizedNewContent = remember(newContent) {
            com.bernaferrari.changedetection.util.HtmlSanitizer.sanitize(
                newContent
            )
        }

        /**
         * OPTIMIZATION: First WebView for old/removed content.
         * Lifecycle: Created in factory, stored for cleanup in DisposableEffect,
         * destroyed when composable leaves composition.
         */
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

        /**
         * OPTIMIZATION: Second WebView for new/added content.
         * Lifecycle: Created in factory, stored for cleanup in DisposableEffect,
         * destroyed when composable leaves composition.
         */
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
                            view?.evaluateJavascript("window.applyChangeHighlights($addedJsArray, '$addColor');", null)
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
            "the",
            "and",
            "with",
            "from",
            "that",
            "this",
            "have",
            "has",
            "for",
            "you",
            "your",
            "are",
            "was",
            "were",
            "will",
            "been",
            "into",
            "about",
            "over",
            "under",
            "more",
            "less",
            "only",
            "very",
            "also",
            "http",
            "https",
            "www",
            "com"
        )
        return s.lowercase()
            .replace("<[^>]+>".toRegex(), " ")
            .split("[^a-z0-9]+".toRegex())
            .asSequence()
            .filter { it.length >= 5 }
            .filter { it !in stop }
            .toSet()
    }

    val oldTokens = tokenize(oldHtml)
    val newTokens = tokenize(newHtml)
    val added = (newTokens - oldTokens).take(20) // limit
    val removed = (oldTokens - newTokens).take(20)
    return Pair(removed.toList(), added.toList())
}

private fun buildHighlighterJs(): String {
    return """
        (function(){
          if (window.applyChangeHighlights) return;
          window.applyChangeHighlights = function(keywords, bgColor){
            try {
              const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_ELEMENT, null);
              const seen = new WeakSet();
              while (walker.nextNode()){
                const el = walker.currentNode; if (!el || seen.has(el)) continue;
                const text = (el.innerText||'').toLowerCase();
                for (var i=0;i<keywords.length;i++){
                  const kw = keywords[i];
                  if (kw && text.indexOf(kw) !== -1){
                    el.style.transition = 'background-color 200ms ease';
                    el.style.backgroundImage = 'none';
                    el.style.backgroundColor = bgColor;
                    el.style.borderRadius = '6px';
                    seen.add(el);
                    break;
                  }
                }
              }
            } catch(e) { /* no-op */ }
          };
        })();
    """.trimIndent()
}

private fun shareUnifiedDiff(
    context: android.content.Context,
    older: Snap,
    newer: Snap,
    oldContent: String,
    newContent: String
) {
    try {
        val original = oldContent.lines()
        val revised = newContent.lines()
        val patch = DiffUtils.diff(original, revised)
        val unified = UnifiedDiffUtils.generateUnifiedDiff(
            "older_${older.snapId}",
            "newer_${newer.snapId}",
            original,
            patch,
            3
        )
        val text = unified.joinToString("\n")

        // Write to cache file for sharing
        val cacheDir = File(context.cacheDir, "diffs").apply { mkdirs() }
        val outFile = File(cacheDir, "diff_${older.snapId}_${newer.snapId}.patch")
        outFile.writeText(text)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", outFile)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Changes for ${newer.siteId}")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share diff"))
    } catch (_: Exception) {
        // best-effort
    }
}
