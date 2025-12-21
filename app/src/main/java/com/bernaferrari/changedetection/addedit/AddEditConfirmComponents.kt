package com.bernaferrari.changedetection.addedit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.browser.BrowserSessionData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditConfirmScreen(
    url: String,
    title: String,
    selectedColors: Pair<Int, Int>,
    isSaving: Boolean,
    saveError: String?,
    browserData: BrowserSessionData?,
    useCookies: Boolean,
    cookieBytes: Long,
    onTitleChange: (String) -> Unit,
    onColorChange: (Pair<Int, Int>) -> Unit,
    onUseCookiesChange: (Boolean) -> Unit,
    onNavigateUp: () -> Unit,
    onSave: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(id = R.string.complete_setup),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateUp()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            OverviewBanner(
                monitoredCount = browserData?.selectedElements?.size ?: 0,
                zappedCount = browserData?.zappedElements?.size ?: 0,
                hasCookies = browserData?.cookies?.isNotBlank() == true
            )

            SitePreviewCard(
                url = url,
                title = title,
                selectedColors = selectedColors
            )

            if (browserData != null && (browserData.selectedElements.isNotEmpty() || browserData.zappedElements.isNotEmpty())) {
                MonitorZapSection(
                    monitoredElements = browserData.selectedElements,
                    zappedElements = browserData.zappedElements
                )
            } else {
                FullPageMonitoringCard()
            }

            TitleSection(
                title = title,
                onTitleChange = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTitleChange(it)
                }
            )

            ColorSelectionSection(
                selectedColors = selectedColors,
                availableColors = defaultAvailableColors,
                onColorChange = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onColorChange(it)
                }
            )

            AuthenticationSection(
                useCookies = useCookies,
                cookieBytes = cookieBytes,
                onUseCookiesChange = onUseCookiesChange
            )

            if (browserData != null) {
                FeaturesSummaryCard(browserData = browserData, cookieBytes = cookieBytes)
            }

            ActionButtons(
                isSaving = isSaving,
                titleFilled = title.isNotBlank(),
                onNavigateUp = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateUp()
                },
                onSave = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave()
                }
            )

            AnimatedVisibility(visible = saveError != null) {
                ErrorCard(saveError)
            }
        }
    }
}

private val defaultAvailableColors = listOf(
    Pair(0xFF2196F3.toInt(), 0xFF1976D2.toInt()),
    Pair(0xFF4CAF50.toInt(), 0xFF388E3C.toInt()),
    Pair(0xFFFF5722.toInt(), 0xFFD84315.toInt()),
    Pair(0xFF9C27B0.toInt(), 0xFF7B1FA2.toInt()),
    Pair(0xFFFFC107.toInt(), 0xFFF57C00.toInt()),
    Pair(0xFF00BCD4.toInt(), 0xFF0097A7.toInt()),
    Pair(0xFFE91E63.toInt(), 0xFFC2185B.toInt()),
    Pair(0xFF795548.toInt(), 0xFF5D4037.toInt())
)

@Composable
private fun SitePreviewCard(
    url: String,
    title: String,
    selectedColors: Pair<Int, Int>
) {
    val displayTitle = remember(title, url) { resolveTitle(title, url) }
    val domain = remember(url) { extractDomain(url) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(selectedColors.first), Color(selectedColors.second))
                        )
                    ),
                contentAlignment = Alignment.BottomStart
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.96f),
                    modifier = Modifier
                        .padding(20.dp)
                        .size(36.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (domain.isNotEmpty()) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text(
                                text = domain,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Language, contentDescription = "Language selection", modifier = Modifier.size(16.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSelectionSection(
    selectedColors: Pair<Int, Int>,
    availableColors: List<Pair<Int, Int>>,
    onColorChange: (Pair<Int, Int>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = stringResource(id = R.string.theme_color_preview_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = stringResource(id = R.string.theme_color_preview_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                availableColors.forEach { colors ->
                    AnimatedColorOption(
                        colors = colors,
                        isSelected = colors == selectedColors,
                        onClick = { onColorChange(colors) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OverviewBanner(
    monitoredCount: Int,
    zappedCount: Int,
    hasCookies: Boolean
) {
    val highlightItems = buildList {
        if (monitoredCount > 0) {
            add(Icons.Default.Visibility to pluralStringResource(R.plurals.monitoring_elements_selected, monitoredCount, monitoredCount))
        }
        if (zappedCount > 0) {
            add(Icons.Default.Block to pluralStringResource(R.plurals.monitoring_elements_zapped, zappedCount, zappedCount))
        }
        if (hasCookies) {
            add(Icons.Default.Cookie to stringResource(id = R.string.cookies_section_title))
        }
        if (isEmpty()) {
            add(Icons.Default.Language to stringResource(id = R.string.browser_full_page_monitoring))
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(horizontal = 26.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(id = R.string.confirmation_ready_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = stringResource(id = R.string.confirmation_ready_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                highlightItems.forEach { (icon, text) ->
                    StatusChip(icon = icon, text = text)
                }
            }
        }
    }
}

@Composable
private fun StatusChip(icon: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TitleSection(
    title: String,
    onTitleChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(id = R.string.site_details_title)) },
            placeholder = { Text("My Awesome Website") },
            leadingIcon = {
                Icon(
                    Icons.Default.Title,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun MonitorZapSection(
    monitoredElements: List<com.bernaferrari.changedetection.browser.SelectedElement>,
    zappedElements: List<com.bernaferrari.changedetection.browser.ZappedElement>
) {
    val monitorLabels = monitoredElements.map { it.description.ifBlank { it.selector } }.distinct()
    val zapLabels = zappedElements.map { it.description.ifBlank { it.selector } }.distinct()

    if (monitorLabels.isEmpty() && zapLabels.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Preview section", tint = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(id = R.string.preview),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = stringResource(id = R.string.review_and_start_monitoring),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (monitorLabels.isNotEmpty()) {
                MonitorZapGroup(
                    title = stringResource(id = R.string.elements_to_monitor),
                    subtitle = pluralStringResource(R.plurals.monitoring_elements_selected, monitorLabels.size, monitorLabels.size),
                    icon = Icons.Default.Visibility,
                    chipIcon = Icons.Default.Check,
                    elements = monitorLabels,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (zapLabels.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                MonitorZapGroup(
                    title = stringResource(id = R.string.elements_to_hide),
                    subtitle = pluralStringResource(R.plurals.monitoring_elements_zapped, zapLabels.size, zapLabels.size),
                    icon = Icons.Default.Block,
                    chipIcon = Icons.Default.HighlightOff,
                    elements = zapLabels,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun FullPageMonitoringCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(id = R.string.browser_full_page_monitoring),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = stringResource(id = R.string.review_and_start_monitoring),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = stringResource(id = R.string.full_page_monitoring_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class FeatureHighlight(
    val icon: ImageVector,
    val title: String,
    val subtitle: String?
)

@Composable
private fun FeaturesSummaryCard(
    browserData: BrowserSessionData,
    cookieBytes: Long
) {
    val monitoredCount = browserData.selectedElements.size
    val zappedCount = browserData.zappedElements.size
    val hasCookies = cookieBytes > 0

    val highlights = buildList {
        if (monitoredCount > 0) {
            add(
                FeatureHighlight(
                    icon = Icons.Default.Visibility,
                    title = pluralStringResource(
                        R.plurals.monitoring_elements_selected,
                        monitoredCount,
                        monitoredCount
                    ),
                    subtitle = stringResource(id = R.string.element_monitoring_enabled)
                )
            )
        } else {
            add(
                FeatureHighlight(
                    icon = Icons.Default.Visibility,
                    title = stringResource(id = R.string.browser_full_page_monitoring),
                    subtitle = stringResource(id = R.string.review_and_start_monitoring)
                )
            )
        }

        if (zappedCount > 0) {
            add(
                FeatureHighlight(
                    icon = Icons.Default.Block,
                    title = pluralStringResource(
                        R.plurals.monitoring_elements_zapped,
                        zappedCount,
                        zappedCount
                    ),
                    subtitle = stringResource(id = R.string.elements_blocked_count, zappedCount)
                )
            )
        }

        if (hasCookies) {
            add(
                FeatureHighlight(
                    icon = Icons.Default.Cookie,
                    title = stringResource(id = R.string.cookies_section_title),
                    subtitle = stringResource(
                        id = R.string.cookies_size_label,
                        formatBytes(cookieBytes)
                    )
                )
            )
        }

        add(
            FeatureHighlight(
                icon = Icons.Default.Notifications,
                title = stringResource(id = R.string.you_will_be_notified),
                subtitle = stringResource(id = R.string.taking_initial_snapshot)
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = stringResource(id = R.string.site_statistics),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            highlights.forEach { highlight ->
                FeatureHighlightRow(highlight)
            }
        }
    }
}

@Composable
private fun FeatureHighlightRow(highlight: FeatureHighlight) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = highlight.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .padding(10.dp)
                    .size(20.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = highlight.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            highlight.subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun resolveTitle(title: String, url: String): String {
    if (title.isNotBlank()) return title
    val domain = extractDomain(url)
    return domain.ifBlank { url.ifBlank { "Website" } }
}

private fun extractDomain(url: String): String {
    return runCatching {
        val sanitized = url.trim()
        if (sanitized.isEmpty()) return@runCatching ""
        val host = java.net.URI(sanitized).host.orEmpty()
        host.removePrefix("www.")
    }.getOrDefault("")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonitorZapGroup(
    title: String,
    subtitle: String,
    icon: ImageVector,
    chipIcon: ImageVector,
    elements: List<String>,
    containerColor: Color,
    contentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = contentColor)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = contentColor
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            elements.forEach { element ->
                ElementChip(
                    text = element,
                    icon = chipIcon,
                    containerColor = containerColor,
                    contentColor = contentColor
                )
            }
        }
    }
}

@Composable
private fun ElementChip(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = contentColor,
            leadingIconContentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledLabelColor = contentColor,
            disabledLeadingIconContentColor = contentColor
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimatedColorOption(
    colors: Pair<Int, Int>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "color_option_scale"
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier
            .size(48.dp)
            .scale(scale),
        border = if (isSelected) {
            BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(colors.first), Color(colors.second))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(visible = isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun AuthenticationSection(
    useCookies: Boolean,
    cookieBytes: Long,
    onUseCookiesChange: (Boolean) -> Unit
) {
    val cookieSizeLabel = remember(cookieBytes) {
        if (cookieBytes > 0) formatBytes(cookieBytes) else null
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    tonalElevation = 4.dp,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Cookie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(id = R.string.cookies_section_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = stringResource(id = R.string.cookies_section_caption),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(id = R.string.cookies_switch_label),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = cookieSizeLabel?.let { stringResource(id = R.string.cookies_size_label, it) }
                            ?: stringResource(id = R.string.cookies_none),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(id = R.string.cookies_switch_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                Switch(
                    checked = useCookies,
                    onCheckedChange = { onUseCookiesChange(it) }
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    isSaving: Boolean,
    titleFilled: Boolean,
    onNavigateUp: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onNavigateUp,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit settings")
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(id = R.string.edit_label))
        }

        Button(
            onClick = onSave,
            enabled = titleFilled && !isSaving,
            modifier = Modifier
                .weight(2f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 4.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.RocketLaunch, contentDescription = "Start monitoring")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(id = R.string.start_monitoring),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(saveError: String?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Error, contentDescription = "Error occurred", tint = MaterialTheme.colorScheme.onErrorContainer)
            Text(text = saveError.orEmpty(), color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024f
    if (kb < 1f) return "$bytes B"
    val mb = kb / 1024f
    return if (mb < 1f) "%.1f KB".format(java.util.Locale.getDefault(), kb)
    else "%.1f MB".format(java.util.Locale.getDefault(), mb)
}
