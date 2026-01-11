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
import com.bernaferrari.changedetection.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.util.formatTimestamp
import com.bernaferrari.changedetection.ui.theme.ExpressiveSpring
import io.github.aakira.napier.Napier
import org.koin.compose.viewmodel.koinViewModel
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.multiplatform.webview.web.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(
    siteId: String,
    onNavigateUp: () -> Unit,
    onSiteEdit: (String) -> Unit,
    onSiteDelete: () -> Unit,
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
                                ?: stringResource(Res.string.app_name),
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
                actions = {
                    var showMenu by remember { mutableStateOf(false) }

                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Reload") },
                            onClick = {
                                viewModel.reloadSite()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                onSiteEdit(siteId)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Prune") },
                            onClick = {
                                viewModel.pruneSite()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                viewModel.deleteSite()
                                onSiteDelete()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error
                            )
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
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
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
                        text = "Site not found",
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
                    content = uiState.singleSnapContent,
                    isLoadingContent = uiState.isLoadingDiff,
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
                    onShare = {
                        val idx = uiState.selectedPairIndex ?: 0
                        if (idx + 1 < uiState.snaps.size && uiState.snapContent1 != null && uiState.snapContent2 != null) {
                            viewModel.shareDiff(
                                snap1 = uiState.snaps[idx],
                                snap2 = uiState.snaps[idx + 1],
                                content1 = uiState.snapContent1!!,
                                content2 = uiState.snapContent2!!
                            )
                        }
                    },
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
                text = stringResource(Res.string.comparison_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.comparison_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
@ExperimentalMaterial3Api
private fun SingleSnapshotState(
    snap: Snap,
    content: String?,
    isLoadingContent: Boolean,
    modifier: Modifier = Modifier
) {
    if (content != null) {
        val isHtml = remember(snap.contentType) {
            snap.contentType.contains("html", ignoreCase = true)
        }

        Box(modifier = modifier) {
            if (isHtml) {
                val webViewState = rememberWebViewStateWithHTMLData(
                    data = content
                )
                WebView(
                    state = webViewState,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Floating label for timestamp
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                tonalElevation = 4.dp
            ) {
                Text(
                    text = formatTimestamp(snap.timestamp),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    } else if (isLoadingContent) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
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
                    text = stringResource(Res.string.comparison_single_snapshot_hint),
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
}

@Composable
private fun DiffFirstContent(
    snaps: List<Snap>,
    selectedPairIndex: Int,
    content1: String?,
    content2: String?,
    isLoadingDiff: Boolean,
    onSelectPair: (Int) -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Version selector - horizontal scrollable chips
        VersionSelector(
            snaps = snaps,
            selectedIndex = selectedPairIndex,
            onSelectPair = onSelectPair
        )

        // Diff mode selector
        var mode by remember { mutableStateOf("HTML") }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = mode == "TEXT",
                onClick = { mode = "TEXT" },
                label = { Text(stringResource(Res.string.text_diff)) }
            )
            FilterChip(
                selected = mode == "HTML",
                onClick = { mode = "HTML" },
                label = { Text(stringResource(Res.string.rendered_html)) }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Share button
            if (content1 != null && content2 != null) {
                IconButton(onClick = onShare) {
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
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
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
                    text = stringResource(Res.string.failed_to_load_content),
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
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = showOnlyChanges,
                        onClick = { showOnlyChanges = true },
                        label = { Text("Changes") }
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
    var overlayOpacity by remember { mutableFloatStateOf(0.5f) }

    // Calculate similarity percentage
    val similarityPercent = remember(oldContent, newContent) {
        calculateSimilarity(oldContent, newContent)
    }

    val sanitizedOldContent = remember(oldContent) {
        com.bernaferrari.changedetection.util.HtmlSanitizer.sanitize(oldContent)
    }
    val sanitizedNewContent = remember(newContent) {
        com.bernaferrari.changedetection.util.HtmlSanitizer.sanitize(newContent)
    }

    val oldWebViewState = rememberWebViewStateWithHTMLData(data = sanitizedOldContent)
    val newWebViewState = rememberWebViewStateWithHTMLData(data = sanitizedNewContent)

    Column(modifier = modifier) {
        // Similarity indicator at top
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val similarityColor = when {
                    similarityPercent >= 90 -> MaterialTheme.colorScheme.tertiary
                    similarityPercent >= 70 -> MaterialTheme.colorScheme.primary
                    similarityPercent >= 50 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.error
                }

                Text(
                    text = "${100 - similarityPercent}% changed",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = similarityColor
                )
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${similarityPercent}% similar",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Overlay comparison area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Base layer: Old content (always fully visible as background)
            WebView(
                state = oldWebViewState,
                modifier = Modifier.fillMaxSize()
            )

            // Overlay layer: New content with adjustable opacity
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = overlayOpacity }
            ) {
                WebView(
                    state = newWebViewState,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Control bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Old button
                FilterChip(
                    selected = overlayOpacity == 0f,
                    onClick = { overlayOpacity = 0f },
                    label = { Text("Old") },
                    leadingIcon = if (overlayOpacity == 0f) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )

                // Blend slider
                Slider(
                    value = overlayOpacity,
                    onValueChange = { overlayOpacity = it },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )

                // New button
                FilterChip(
                    selected = overlayOpacity == 1f,
                    onClick = { overlayOpacity = 1f },
                    label = { Text("New") },
                    leadingIcon = if (overlayOpacity == 1f) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
    }
}

/**
 * Calculate similarity percentage between two strings using a simple comparison.
 * Returns a value from 0-100 representing how similar the texts are.
 */
private fun calculateSimilarity(old: String, new: String): Int {
    if (old == new) return 100
    if (old.isEmpty() || new.isEmpty()) return 0

    // Strip HTML tags for comparison
    val oldText = old.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
    val newText = new.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()

    if (oldText == newText) return 100

    // Use word-level comparison
    val oldWords = oldText.lowercase().split(Regex("\\s+")).toSet()
    val newWords = newText.lowercase().split(Regex("\\s+")).toSet()

    if (oldWords.isEmpty() && newWords.isEmpty()) return 100
    if (oldWords.isEmpty() || newWords.isEmpty()) return 0

    val intersection = oldWords.intersect(newWords).size
    val union = oldWords.union(newWords).size

    return ((intersection.toDouble() / union.toDouble()) * 100).toInt()
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

// Use calculateChangeStats and ComparisonStatsRow from ComparisonComponents.kt


