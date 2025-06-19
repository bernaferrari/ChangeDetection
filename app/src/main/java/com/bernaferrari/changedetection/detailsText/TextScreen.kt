package com.bernaferrari.changedetection.detailsText

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.extensions.convertTimestampToDate
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.bernaferrari.changedetection.repo.Snap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextScreen(
    siteId: String,
    url: String,
    contentType: String,
    onNavigateUp: () -> Unit,
    viewModel: TextViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val diffViewMode by viewModel.diffViewMode.collectAsStateWithLifecycle()

    LaunchedEffect(siteId) {
        viewModel.loadSnaps(siteId)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopBar(
                onNavigateUp = onNavigateUp,
                onOpenInBrowser = { viewModel.openInBrowser(url, context) },
                diffViewMode = diffViewMode,
                onToggleView = {
                    val newMode = if (diffViewMode == DiffViewMode.TEXT)
                        DiffViewMode.VISUAL else DiffViewMode.TEXT
                    viewModel.setDiffViewMode(newMode)
                },
                hasComparison = uiState.currentSnap != null && uiState.previousSnap != null
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> LoadingState()
                uiState.error != null -> ErrorState(uiState.error!!)
                uiState.snaps.isEmpty() -> EmptyState()
                uiState.currentSnap == null -> DualListComparisonState(
                    snaps = uiState.snaps,
                    onCompareVersions = { beforeSnap, afterSnap ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.loadDiff(afterSnap, beforeSnap)
                    }
                )

                else -> {
                    when (diffViewMode) {
                        DiffViewMode.TEXT -> TraditionalDiffView(
                            currentSnap = uiState.currentSnap!!,
                            previousSnap = uiState.previousSnap,
                            diffHtml = uiState.diffHtml,
                            onChangeComparison = {
                                // Navigate back to selection screen to change comparison
                                viewModel.clearComparison()
                            }
                        )

                        DiffViewMode.VISUAL -> VisualDiffView(
                            currentSnap = uiState.currentSnap!!,
                            previousSnap = uiState.previousSnap,
                            onChangeComparison = {
                                // Navigate back to selection screen to change comparison
                                viewModel.clearComparison()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onNavigateUp: () -> Unit,
    onOpenInBrowser: () -> Unit,
    diffViewMode: DiffViewMode,
    onToggleView: () -> Unit,
    hasComparison: Boolean
) {
    CenterAlignedTopAppBar(
        title = {
            if (hasComparison) {
                ModeSwitch(
                    diffViewMode = diffViewMode,
                    onToggleView = onToggleView
                )
            } else {
                Text(
                    text = "Changes",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onNavigateUp,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            IconButton(
                onClick = onOpenInBrowser,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.OpenInBrowser,
                    contentDescription = "Open in browser",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent
        )
    )
}

@Composable
private fun ModeSwitch(
    diffViewMode: DiffViewMode,
    onToggleView: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            ModeButton(
                text = "Visual",
                icon = Icons.Default.Visibility,
                isSelected = diffViewMode == DiffViewMode.VISUAL,
                onClick = { if (diffViewMode != DiffViewMode.VISUAL) onToggleView() }
            )
            ModeButton(
                text = "Text Diff",
                icon = Icons.AutoMirrored.Filled.CompareArrows,
                isSelected = diffViewMode == DiffViewMode.TEXT,
                onClick = { if (diffViewMode != DiffViewMode.TEXT) onToggleView() }
            )
        }
    }
}

@Composable
private fun ModeButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "mode_button_background"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "mode_button_content"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "mode_button_scale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        modifier = Modifier.scale(scale)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.1.sp
                ),
                color = contentColor
            )
        }
    }
}

@Composable
private fun TraditionalDiffView(
    currentSnap: Snap,
    previousSnap: Snap?,
    diffHtml: String,
    onChangeComparison: (Snap) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ComparisonHeader(
            currentSnap = currentSnap,
            previousSnap = previousSnap,
            onChangeComparison = onChangeComparison
        )

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = false
                    settings.domStorageEnabled = false
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(null, diffHtml, "text/html", "UTF-8", null)
            },
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
        )
    }
}

@Composable
private fun VisualDiffView(
    currentSnap: Snap,
    previousSnap: Snap?,
    onChangeComparison: (Snap) -> Unit,
    viewModel: TextViewModel = hiltViewModel()
) {
    val webViewContent by viewModel.webViewContent.collectAsStateWithLifecycle()
    val visualViewMode by viewModel.visualViewMode.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ComparisonHeader(
            currentSnap = currentSnap,
            previousSnap = previousSnap,
            onChangeComparison = onChangeComparison
        )

        if (previousSnap != null) {
            VisualViewToggle(
                visualViewMode = visualViewMode,
                onModeChange = { mode -> viewModel.setVisualViewMode(mode) }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                previousSnap == null -> {
                    SimpleWebView(content = webViewContent.currentContent)
                }

                visualViewMode == VisualViewMode.BEFORE -> {
                    SimpleWebView(content = webViewContent.previousContent)
                }

                visualViewMode == VisualViewMode.AFTER -> {
                    SimpleWebView(content = webViewContent.currentContent)
                }

                visualViewMode == VisualViewMode.OVERLAY -> {
                    OverlayWebView(
                        previousContent = webViewContent.previousContent,
                        currentContent = webViewContent.currentContent
                    )
                }
            }
        }
    }
}

@Composable
private fun VisualViewToggle(
    visualViewMode: VisualViewMode,
    onModeChange: (VisualViewMode) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shadowElevation = 6.dp,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            VisualModeButton(
                text = "Overlay",
                isSelected = visualViewMode == VisualViewMode.OVERLAY,
                onClick = { onModeChange(VisualViewMode.OVERLAY) },
                accentColor = MaterialTheme.colorScheme.tertiary
            )
            VisualModeButton(
                text = "Before",
                isSelected = visualViewMode == VisualViewMode.BEFORE,
                onClick = { onModeChange(VisualViewMode.BEFORE) },
                accentColor = MaterialTheme.colorScheme.error
            )
            VisualModeButton(
                text = "After",
                isSelected = visualViewMode == VisualViewMode.AFTER,
                onClick = { onModeChange(VisualViewMode.AFTER) },
                accentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun VisualModeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            accentColor
        } else {
            Color.Transparent
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "visual_mode_background"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "visual_mode_text"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "visual_mode_scale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        modifier = Modifier.scale(scale)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.1.sp
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun SimpleWebView(content: String) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, content, "text/html", "UTF-8", null)
        },
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
    )
}

@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            strokeWidth = 2.5.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ErrorState(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            )
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(48.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = "No content",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "No History Yet",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Start monitoring to see how this content changes over time",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

@Composable
fun DualListComparisonState(
    snaps: List<Snap>,
    onCompareVersions: (beforeSnap: Snap, afterSnap: Snap) -> Unit
) {
    var selectedBeforeSnap by remember { mutableStateOf<Snap?>(null) }
    var selectedAfterSnap by remember { mutableStateOf<Snap?>(null) }

    // Auto-select most recent as "after" if available
    LaunchedEffect(snaps) {
        if (snaps.isNotEmpty() && selectedAfterSnap == null) {
            selectedAfterSnap = snaps.first()
        }
    }

    // Trigger comparison when both are selected
    LaunchedEffect(selectedBeforeSnap, selectedAfterSnap) {
        if (selectedBeforeSnap != null && selectedAfterSnap != null) {
            onCompareVersions(selectedBeforeSnap!!, selectedAfterSnap!!)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            // Header
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Compare Versions",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Select one version from each list to compare",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Before List (older versions)
                VersionSelectionList(
                    title = "Before",
                    snaps = snaps.drop(1), // Skip the most recent
                    selectedSnap = selectedBeforeSnap,
                    onSnapSelected = { selectedBeforeSnap = it },
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                    defaultExpanded = false
                )

                // After List (recent versions, most recent pre-selected)
                VersionSelectionList(
                    title = "After",
                    snaps = snaps.take(5), // Show top 5 recent
                    selectedSnap = selectedAfterSnap,
                    onSnapSelected = { selectedAfterSnap = it },
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    defaultExpanded = true
                )
            }
        }
    }
}

@Composable
private fun VersionSelectionList(
    title: String,
    snaps: List<Snap>,
    selectedSnap: Snap?,
    onSnapSelected: (Snap) -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
    defaultExpanded: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(defaultExpanded) }
    val displaySnaps = if (isExpanded) snaps else snaps.take(3)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // List Header
        Surface(
            onClick = { isExpanded = !isExpanded },
            shape = RoundedCornerShape(16.dp),
            color = color.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = color,
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = color
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${snaps.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = color
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp),
                        tint = color
                    )
                }
            }
        }

        // Snap List
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            displaySnaps.forEach { snap ->
                SelectableSnapCard(
                    snap = snap,
                    isSelected = snap == selectedSnap,
                    onClick = { onSnapSelected(snap) },
                    accentColor = color
                )
            }

            // Show more indicator
            if (!isExpanded && snaps.size > 3) {
                TextButton(
                    onClick = { isExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "+${snaps.size - 3} more versions",
                        style = MaterialTheme.typography.labelMedium,
                        color = color
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableSnapCard(
    snap: Snap,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            accentColor.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_background"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            accentColor
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_border"
    )

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelected) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor,
                        modifier = Modifier.size(6.dp)
                    ) {}
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = snap.contentSize.readableFileSize(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = snap.timestamp.convertTimestampToDate(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun OverlayWebView(
    previousContent: String,
    currentContent: String
) {
    var showingCurrent by remember { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Dual WebView setup with smooth transitions
        DualWebViewRenderer(
            previousContent = previousContent,
            currentContent = currentContent,
            showingCurrent = showingCurrent
        )

        // Elegant floating control at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        ) {
            ElegantFloatingControl(
                showingCurrent = showingCurrent,
                onToggle = { newState ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showingCurrent = newState
                }
            )
        }

        // Subtle indicator at top right
        VersionIndicator(
            showingCurrent = showingCurrent,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        // Double-tap gesture for quick switching
        TapGestureArea(
            onDoubleTap = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showingCurrent = !showingCurrent
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun DualWebViewRenderer(
    previousContent: String,
    currentContent: String,
    showingCurrent: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Previous content WebView
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    alpha = if (showingCurrent) 0f else 1f
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(null, previousContent, "text/html", "UTF-8", null)
                webView.animate()
                    .alpha(if (showingCurrent) 0f else 1f)
                    .setDuration(300)
                    .start()
            },
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
        )

        // Current content WebView
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    alpha = if (showingCurrent) 1f else 0f
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(null, currentContent, "text/html", "UTF-8", null)
                webView.animate()
                    .alpha(if (showingCurrent) 1f else 0f)
                    .setDuration(300)
                    .start()
            },
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
        )
    }
}

@Composable
private fun ElegantFloatingControl(
    showingCurrent: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 12.dp,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FloatingVersionButton(
                text = "Before",
                isSelected = !showingCurrent,
                onClick = { onToggle(false) },
                color = MaterialTheme.colorScheme.error
            )

            FloatingVersionButton(
                text = "After",
                isSelected = showingCurrent,
                onClick = { onToggle(true) },
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun FloatingVersionButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            color
        } else {
            Color.Transparent
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "floating_button_background"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "floating_button_text"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "floating_button_scale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = backgroundColor,
        modifier = Modifier.scale(scale)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.1.sp
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
        )
    }
}

@Composable
private fun VersionIndicator(
    showingCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val indicatorColor = if (showingCurrent)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.error

    val backgroundColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "indicator_background"
    )

    Surface(
        modifier = modifier.padding(20.dp),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = indicatorColor,
                modifier = Modifier.size(8.dp)
            ) {}

            Text(
                text = if (showingCurrent) "After" else "Before",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TapGestureArea(
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = { onDoubleTap() }
            )
        }
    )
}