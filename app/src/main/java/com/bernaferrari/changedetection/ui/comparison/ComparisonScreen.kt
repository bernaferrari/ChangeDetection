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
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import com.bernaferrari.diffutils.diffs.DiffUtils
import com.bernaferrari.diffutils.diffs.UnifiedDiffUtils
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bernaferrari.changedetection.repo.Snap
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(
    siteId: String,
    onNavigateUp: () -> Unit,
    viewModel: ComparisonViewModel = hiltViewModel()
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
                            text = uiState.site?.title ?: androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.site_details_title),
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        // Beautiful animated empty state
                        val infiniteTransition = rememberInfiniteTransition(label = "empty_snapshots")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = pulseAlpha * 0.3f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha)
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.first_snapshot_title),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.first_snapshot_desc),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "💡 " + androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.what_happens_next),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                listOf(
                                    "📸 " + androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.taking_initial_snapshot),
                                    "🔄 " + androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.future_changes_detected),
                                    "📊 " + androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.comparison_will_show_differences),
                                    "🔔 " + androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.you_will_be_notified)
                                ).forEach { step ->
                                    Text(
                                        text = step,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            else -> {
                val selectedPairIndex = uiState.selectedPairIndex
                if (selectedPairIndex != null && uiState.snaps.size > selectedPairIndex + 1) {
                    // Show diff view
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
                    // Show snapshot list with pairs
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "📊 " + androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.site_statistics),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    uiState.site?.let { site ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            StatsItem(androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.total_snapshots), uiState.snaps.size.toString())
                                            StatsItem(androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.status), if (site.isSuccessful) "✅ Active" else "❌ Error")
                                        }
                                        
                                        if (site.elementMonitoring) {
                                            Text(
                                                text = "🎯 " + androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.element_monitoring_enabled),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        
                                        if (site.requiresAuthentication) {
                                            Text(
                                                text = "🔐 " + androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.authentication_configured),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (uiState.snaps.size > 1) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Compare,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.version_comparisons),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        Text(
                                            text = androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.version_comparisons_desc),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (uiState.snaps.size > 1) {
                            item {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.version_history, uiState.snaps.size),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            
                            // Show comparison pairs
                            items(uiState.snaps.size - 1) { index ->
                                ComparisonPairCard(
                                    newerSnap = uiState.snaps[index],
                                    olderSnap = uiState.snaps[index + 1],
                                    pairIndex = index,
                                    isSelected = uiState.selectedPairIndex == index,
                                    onClick = { viewModel.selectPairForComparison(index) }
                                )
                            }
                        } else {
                            item {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.snapshots_count, uiState.snaps.size),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            
                            items(uiState.snaps) { snap ->
                                SnapshotCard(
                                    snap = snap,
                                    isSelected = false,
                                    selectionNumber = null,
                                    onClick = { }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SnapshotCard(
    snap: Snap,
    isSelected: Boolean = false,
    selectionNumber: Int? = null,
    onClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectionNumber != null) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectionNumber.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTimestamp(snap.timestamp),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.type_label, snap.contentType),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.size_label, formatBytes(snap.contentSize)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatBytes(bytes: Int): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${"%.1f".format(kb)} KB"
    val mb = kb / 1024.0
    return "${"%.1f".format(mb)} MB"
}

enum class DiffLineType {
    UNCHANGED, ADDED, REMOVED, MODIFIED
}

data class ChangeStats(
    val added: Int,
    val removed: Int,
    val modified: Int,
    val unchanged: Int
)

@Composable
private fun DiffLine(
    lineNumber: Int,
    content: String,
    type: DiffLineType,
    modifier: Modifier = Modifier
) {
    val (prefix, backgroundColor, textColor) = when (type) {
        DiffLineType.UNCHANGED -> Triple(
            "  ",
            Color.Transparent,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        DiffLineType.ADDED -> Triple(
            "+ ",
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.primary
        )
        DiffLineType.REMOVED -> Triple(
            "- ",
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.error
        )
        DiffLineType.MODIFIED -> Triple(
            "~ ",
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.tertiary
        )
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Line number
        Text(
            text = String.format("%4d", lineNumber),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(end = 8.dp)
        )
        
        // Prefix indicator
        Text(
            text = prefix,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = textColor,
            modifier = Modifier.padding(end = 4.dp)
        )
        
        // Content
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = textColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ChangeStatChip(
    text: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = color
            )
        }
    }
}

private fun calculateChangeStats(oldLines: List<String>, newLines: List<String>): ChangeStats {
    val oldSet = oldLines.toSet()
    val newSet = newLines.toSet()
    
    val added = newSet.minus(oldSet).size
    val removed = oldSet.minus(newSet).size
    val unchanged = oldSet.intersect(newSet).size
    val modified = maxOf(0, (oldLines.size + newLines.size - added - removed - unchanged * 2) / 2)
    
    return ChangeStats(
        added = added,
        removed = removed,
        modified = modified,
        unchanged = unchanged
    )
}

@Composable
private fun ComparisonPairCard(
    newerSnap: Snap,
    olderSnap: Snap,
    pairIndex: Int,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Comparison header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Compare,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Version ${pairIndex + 1} → ${pairIndex + 2}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View comparison",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Time diff
            val timeDiff = newerSnap.timestamp - olderSnap.timestamp
            val timeDiffText = formatTimeDiff(timeDiff)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Newer",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatTimestamp(newerSnap.timestamp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formatBytes(newerSnap.contentSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterVertically),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Older",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTimestamp(olderSnap.timestamp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formatBytes(olderSnap.contentSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Time difference chip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "⏱ $timeDiffText between versions",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

private fun formatTimeDiff(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        days > 0 -> "$days day${if (days > 1) "s" else ""}"
        hours > 0 -> "$hours hour${if (hours > 1) "s" else ""}"
        minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""}"
        else -> "$seconds second${if (seconds != 1L) "s" else ""}"
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                                Icon(Icons.Default.Share, contentDescription = null)
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
                            text = androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.older_label, formatTimestamp(snap1.timestamp)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.size_label, formatBytes(snap1.contentSize)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(
                            text = androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.newer_label, formatTimestamp(snap2.timestamp)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(id = com.bernaferrari.changedetection.R.string.size_label, formatBytes(snap2.contentSize)),
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
            var mode by remember { mutableStateOf("TEXT") }
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
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (mode == "TEXT") {
            SimpleDiffView(
                        oldContent = content1,
                        newContent = content2,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 0.dp)
                    )
                } else {
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
                    ChangeStatChip(
                        text = "+${changeStats.added}",
                        color = MaterialTheme.colorScheme.primary,
                        icon = Icons.Default.Add
                    )
                    ChangeStatChip(
                        text = "-${changeStats.removed}",
                        color = MaterialTheme.colorScheme.error,
                        icon = Icons.Default.Remove
                    )
                    ChangeStatChip(
                        text = "~${changeStats.modified}",
                        color = MaterialTheme.colorScheme.tertiary,
                        icon = Icons.Default.Edit
                    )
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
                        if (color.alpha > 0f) drawLine(color = color, start = Offset(0f, y), end = Offset(size.width, y))
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
    val removedJsArray = remember(removedKeywords) { removedKeywords.joinToString(prefix = "[\"", postfix = "\"]", separator = "\",\"") }
    val addedJsArray = remember(addedKeywords) { addedKeywords.joinToString(prefix = "[\"", postfix = "\"]", separator = "\",\"") }
    val addColor = "rgba(76,175,80,0.18)" // subtle green
    val remColor = "rgba(244,67,54,0.18)" // subtle red

    Row(modifier = modifier) {
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
                            view?.evaluateJavascript("window.applyChangeHighlights($removedJsArray, '$remColor');", null)
                        }
                    }
                }
            },
            update = { it.loadDataWithBaseURL(null, oldContent, "text/html", "utf-8", null) },
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
                            view?.evaluateJavascript("window.applyChangeHighlights($addedJsArray, '$addColor');", null)
                        }
                    }
                }
            },
            update = { it.loadDataWithBaseURL(null, newContent, "text/html", "utf-8", null) },
            modifier = Modifier.weight(1f)
        )
    }
}

private fun extractChangedKeywords(oldHtml: String, newHtml: String): Pair<List<String>, List<String>> {
    fun tokenize(s: String): Set<String> {
        val stop = setOf(
            "the","and","with","from","that","this","have","has","for","you","your","are","was","were","will","been","into","about","over","under","more","less","only","very","also","http","https","www","com"
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