package com.bernaferrari.changedetection.detailsWebview

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.WebAsset
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.repo.Snap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualComparisonScreen(
    siteId: String,
    onNavigateUp: () -> Unit,
    onShowTextDiff: (VersionComparison) -> Unit = {},
    viewModel: ComparisonViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedPreviousVersion by remember { mutableStateOf<Snap?>(null) }
    var selectedCurrentVersion by remember { mutableStateOf<Snap?>(null) }
    var showVersionSelector by remember { mutableStateOf(false) }
    var selectingVersionFor by remember { mutableStateOf<String?>(null) }
    var currentlyShowing by remember { mutableStateOf(ShowingVersion.CURRENT) }
    var overlayOpacity by remember { mutableStateOf(0.5f) }

    // Auto-load versions when screen opens
    LaunchedEffect(siteId) {
        viewModel.loadVersionsForSite(siteId)
    }

    // Auto-select the two most recent versions
    LaunchedEffect(uiState.comparison) {
        uiState.comparison?.let { comparison ->
            selectedCurrentVersion = comparison.currentVersion
            selectedPreviousVersion = comparison.previousVersion
        }
    }

    // Fix system bars with proper WindowInsets
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 8.dp
            ) {
                Column {
                    Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

                    TopAppBar(
                        title = {
                            Text(
                                "Visual Comparison",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateUp) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        actions = {
                            // Text diff button - now navigates to our new TextDiffScreen
                            IconButton(
                                onClick = {
                                    uiState.comparison?.let { onShowTextDiff(it) }
                                },
                                enabled = uiState.comparison != null
                            ) {
                                Icon(Icons.Default.Code, contentDescription = "HTML Diff")
                            }

                            // Version selector button
                            if (uiState.comparison?.allVersions?.size ?: 0 > 2) {
                                IconButton(
                                    onClick = { showVersionSelector = true }
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = "Version History"
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }

                uiState.error != null -> {
                    ErrorState(uiState.error!!)
                }

                uiState.comparison != null -> {
                    ComparisonContent(
                        comparison = uiState.comparison!!,
                        selectedPreviousVersion = selectedPreviousVersion,
                        selectedCurrentVersion = selectedCurrentVersion,
                        currentlyShowing = currentlyShowing,
                        overlayOpacity = overlayOpacity,
                        onCurrentlyShowingChange = { currentlyShowing = it },
                        onOverlayOpacityChange = { overlayOpacity = it },
                        onVersionSelect = { side, version ->
                            when (side) {
                                "previous" -> selectedPreviousVersion = version
                                "current" -> selectedCurrentVersion = version
                            }
                        },
                        onShowVersionSelector = { side ->
                            selectingVersionFor = side
                            showVersionSelector = true
                        }
                    )
                }
            }
        }
    }

    // Version selector bottom sheet
    if (showVersionSelector && uiState.comparison != null) {
        VersionSelectorBottomSheet(
            versions = uiState.comparison!!.allVersions,
            selectingFor = selectingVersionFor,
            onVersionSelected = { version ->
                when (selectingVersionFor) {
                    "previous" -> selectedPreviousVersion = version
                    "current" -> selectedCurrentVersion = version
                    else -> selectedCurrentVersion = version
                }
                showVersionSelector = false
                selectingVersionFor = null
            },
            onDismiss = {
                showVersionSelector = false
                selectingVersionFor = null
            }
        )
    }
}

enum class ShowingVersion {
    PREVIOUS, CURRENT, OVERLAY
}

@Composable
private fun ComparisonContent(
    comparison: VersionComparison,
    selectedPreviousVersion: Snap?,
    selectedCurrentVersion: Snap?,
    currentlyShowing: ShowingVersion,
    overlayOpacity: Float,
    onCurrentlyShowingChange: (ShowingVersion) -> Unit,
    onOverlayOpacityChange: (Float) -> Unit,
    onVersionSelect: (String, Snap) -> Unit,
    onShowVersionSelector: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main content area - takes most space
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            StackedWebViewContent(
                comparison = comparison,
                selectedPreviousVersion = selectedPreviousVersion,
                selectedCurrentVersion = selectedCurrentVersion,
                currentlyShowing = currentlyShowing,
                overlayOpacity = overlayOpacity,
                onCurrentlyShowingChange = onCurrentlyShowingChange
            )
        }

        // Simple control panel
        SimpleControlPanel(
            currentlyShowing = currentlyShowing,
            overlayOpacity = overlayOpacity,
            onCurrentlyShowingChange = onCurrentlyShowingChange,
            onOverlayOpacityChange = onOverlayOpacityChange,
            hasVersions = selectedPreviousVersion != null && selectedCurrentVersion != null
        )

        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun SimpleControlPanel(
    currentlyShowing: ShowingVersion,
    overlayOpacity: Float,
    onCurrentlyShowingChange: (ShowingVersion) -> Unit,
    onOverlayOpacityChange: (Float) -> Unit,
    hasVersions: Boolean
) {
    if (!hasVersions) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Simple mode buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = { onCurrentlyShowingChange(ShowingVersion.PREVIOUS) },
                    label = { Text("Before") },
                    selected = currentlyShowing == ShowingVersion.PREVIOUS,
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    onClick = { onCurrentlyShowingChange(ShowingVersion.OVERLAY) },
                    label = { Text("Overlay") },
                    selected = currentlyShowing == ShowingVersion.OVERLAY,
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    onClick = { onCurrentlyShowingChange(ShowingVersion.CURRENT) },
                    label = { Text("After") },
                    selected = currentlyShowing == ShowingVersion.CURRENT,
                    modifier = Modifier.weight(1f)
                )
            }

            // Opacity slider - only show in overlay mode
            if (currentlyShowing == ShowingVersion.OVERLAY) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Blend",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Slider(
                        value = overlayOpacity,
                        onValueChange = onOverlayOpacityChange,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${(overlayOpacity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StackedWebViewContent(
    comparison: VersionComparison,
    selectedPreviousVersion: Snap?,
    selectedCurrentVersion: Snap?,
    currentlyShowing: ShowingVersion,
    overlayOpacity: Float,
    onCurrentlyShowingChange: (ShowingVersion) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        when (currentlyShowing) {
                            ShowingVersion.PREVIOUS -> onCurrentlyShowingChange(ShowingVersion.CURRENT)
                            ShowingVersion.CURRENT -> onCurrentlyShowingChange(ShowingVersion.PREVIOUS)
                            ShowingVersion.OVERLAY -> onCurrentlyShowingChange(ShowingVersion.CURRENT)
                        }
                    }
                )
            }
    ) {
        // Previous version WebView (bottom layer)
        if (selectedPreviousVersion != null) {
            WebViewContainer(
                snap = selectedPreviousVersion,
                content = comparison.previousContent,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(
                        when {
                            currentlyShowing == ShowingVersion.OVERLAY -> 1f - overlayOpacity
                            currentlyShowing == ShowingVersion.PREVIOUS -> 1f
                            else -> 0f
                        }
                    )
            )
        }

        // Current version WebView (top layer)
        if (selectedCurrentVersion != null) {
            WebViewContainer(
                snap = selectedCurrentVersion,
                content = comparison.currentContent,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(
                        when {
                            currentlyShowing == ShowingVersion.OVERLAY -> overlayOpacity
                            currentlyShowing == ShowingVersion.CURRENT -> 1f
                            else -> 0f
                        }
                    )
            )
        }

        // Version indicator
        AnimatedVisibility(
            visible = currentlyShowing != ShowingVersion.OVERLAY,
            enter = slideInHorizontally() + fadeIn(),
            exit = slideOutHorizontally() + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            VersionIndicator(
                version = currentlyShowing,
                modifier = Modifier.padding(20.dp)
            )
        }

        // Empty state
        if (selectedPreviousVersion == null && selectedCurrentVersion == null) {
            EmptyVersionsState()
        }
    }
}

@Composable
private fun VersionIndicator(
    version: ShowingVersion,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (version) {
        ShowingVersion.PREVIOUS -> "Before" to MaterialTheme.colorScheme.error
        ShowingVersion.CURRENT -> "After" to MaterialTheme.colorScheme.primary
        ShowingVersion.OVERLAY -> "Overlay" to MaterialTheme.colorScheme.tertiary
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.9f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(8.dp)
            ) {}

            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Text(
                text = "Loading comparison...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Text(
                text = "Oops! Something went wrong",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyVersionsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.WebAsset,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Text(
                text = "No versions to compare",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Please select versions to compare",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun WebViewContainer(
    snap: Snap?,
    content: String?,
    modifier: Modifier = Modifier
) {
    if (snap != null && content != null) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                }
            },
            update = { webView ->
                // Load content the same way as LocalBrowser
                webView.loadDataWithBaseURL("", content, "text/html", "UTF-8", "")
            },
            modifier = modifier
        )
    } else {
        // Simple empty state
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.WebAsset,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No content",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun createRichDemoContent(snap: Snap): String {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Demo Content</title>
            <style>
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', system-ui, sans-serif;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    min-height: 100vh;
                    padding: 20px;
                    color: white;
                }
                .header {
                    background: rgba(255, 255, 255, 0.1);
                    backdrop-filter: blur(10px);
                    border-radius: 16px;
                    padding: 24px;
                    margin-bottom: 20px;
                    text-align: center;
                    border: 1px solid rgba(255, 255, 255, 0.2);
                }
                .content-section {
                    background: rgba(255, 255, 255, 0.1);
                    backdrop-filter: blur(10px);
                    border-radius: 12px;
                    padding: 20px;
                    margin: 16px 0;
                    border: 1px solid rgba(255, 255, 255, 0.2);
                }
                h1 {
                    font-size: 24px;
                    font-weight: 700;
                    margin-bottom: 8px;
                }
                h2 {
                    font-size: 18px;
                    font-weight: 600;
                    margin-bottom: 12px;
                    color: rgba(255, 255, 255, 0.9);
                }
                p {
                    line-height: 1.6;
                    margin-bottom: 12px;
                    color: rgba(255, 255, 255, 0.8);
                }
                .timestamp {
                    font-size: 14px;
                    color: rgba(255, 255, 255, 0.7);
                    margin-top: 16px;
                }
                .nav {
                    display: flex;
                    gap: 16px;
                    margin: 16px 0;
                    flex-wrap: wrap;
                }
                .nav-item {
                    background: rgba(255, 255, 255, 0.2);
                    padding: 8px 16px;
                    border-radius: 20px;
                    text-decoration: none;
                    color: white;
                    font-weight: 500;
                    transition: background 0.2s;
                }
                .nav-item:hover {
                    background: rgba(255, 255, 255, 0.3);
                }
                .card {
                    background: rgba(255, 255, 255, 0.05);
                    border-radius: 8px;
                    padding: 16px;
                    margin: 12px 0;
                    border-left: 4px solid rgba(255, 255, 255, 0.3);
                }
                .footer {
                    text-align: center;
                    margin-top: 32px;
                    padding: 20px;
                    background: rgba(0, 0, 0, 0.2);
                    border-radius: 12px;
                    font-size: 14px;
                    color: rgba(255, 255, 255, 0.6);
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .content-section {
                    animation: fadeIn 0.6s ease-out;
                }
            </style>
        </head>
        <body>
            <div class="header">
                <h1>📱 Demo Website Content</h1>
                <p>Interactive preview of captured content</p>
                <div class="timestamp">
                    📅 Captured: ${
        java.text.SimpleDateFormat("EEEE, MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(snap.timestamp))
    }
                </div>
            </div>
            
            <div class="nav">
                <a href="#" class="nav-item">🏠 Home</a>
                <a href="#" class="nav-item">📰 News</a>
                <a href="#" class="nav-item">🛍️ Products</a>
                <a href="#" class="nav-item">📞 Contact</a>
                <a href="#" class="nav-item">ℹ️ About</a>
            </div>
            
            <div class="content-section">
                <h2>🚀 Welcome Section</h2>
                <p>This is a demo representation of dynamic website content. In a real implementation, you would see the actual HTML content that was captured at this timestamp.</p>
                <div class="card">
                    <strong>Feature Highlight</strong><br>
                    Real-time change detection with visual comparisons
                </div>
            </div>
            
            <div class="content-section">
                <h2>📊 Content Stats</h2>
                <div class="card">
                    <strong>Capture Size:</strong> ${formatBytes(snap.contentSize)}<br>
                    <strong>Content Type:</strong> ${snap.contentType}<br>
                    <strong>Processing:</strong> Complete ✅
                </div>
            </div>
            
            <div class="content-section">
                <h2>🔄 Dynamic Content</h2>
                <p>This section would typically contain:</p>
                <div class="card">• Live news updates</div>
                <div class="card">• Product listings</div>
                <div class="card">• User-generated content</div>
                <div class="card">• Real-time data feeds</div>
            </div>
            
            <div class="footer">
                <p>💡 This is demo content. The actual captured HTML would be rendered here in production.</p>
            </div>
        </body>
        </html>
    """.trimIndent()
}

private fun formatBytes(bytes: Int): String {
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return "${(bytes / 1024.0).toFloat().format(1)} KB"
    return "${(bytes / (1024.0 * 1024.0)).toFloat().format(1)} MB"
}

private fun Float.format(digits: Int) = "%.${digits}f".format(this)

@Composable
private fun BeautifulStatsCard(
    diffPercentage: Float,
    previousVersion: Snap?,
    currentVersion: Snap?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shadowElevation = 8.dp,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Content Changed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${diffPercentage.format(1)}% size difference",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
