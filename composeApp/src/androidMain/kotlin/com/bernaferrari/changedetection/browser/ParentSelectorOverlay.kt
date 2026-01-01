package com.bernaferrari.changedetection.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.spring
import com.bernaferrari.changedetection.ui.theme.ExpressiveSpring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bernaferrari.changedetection.R
import kotlin.math.roundToInt

@Composable
fun ParentSelectorOverlay(
    isVisible: Boolean,
    selectedElement: SelectedElement?,
    parentChain: List<ParentElement>,
    mode: InteractionMode,
    onDismiss: () -> Unit,
    onSelectLevel: (Int) -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible && selectedElement != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        selectedElement?.let { element ->
            val interactionSource = remember { MutableInteractionSource() }
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
                        .clickable(
                            indication = null,
                            interactionSource = interactionSource
                        ) { onDismiss() }
                )

                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    ParentSelectorContent(
                        element = element,
                        parentChain = if (parentChain.isNotEmpty()) parentChain else listOf(
                            ParentElement(
                                selector = element.selector,
                                displayName = element.description,
                                tagName = element.selector.substringBefore(' ').ifBlank { "element" },
                                className = "",
                                id = "",
                                index = 0
                            )
                        ),
                        mode = mode,
                        onDismiss = onDismiss,
                        onSelectLevel = onSelectLevel,
                        onConfirm = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.6f)
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ParentSelectorContent(
    element: SelectedElement,
    parentChain: List<ParentElement>,
    mode: InteractionMode,
    onDismiss: () -> Unit,
    onSelectLevel: (Int) -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    var selectedLevel by remember { mutableStateOf(0) }

    LaunchedEffect(parentChain) {
        selectedLevel = 0
        if (parentChain.isNotEmpty()) {
            onSelectLevel(0)
        }
    }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            ParentSelectorHeader(
                mode = mode,
                onClose = onDismiss
            )
            
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            
            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // Slider Section
                ParentLevelSlider(
                    currentLevel = selectedLevel,
                    maxLevel = (parentChain.size - 1).coerceAtLeast(0),
                    onLevelChange = { level ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedLevel = level
                        onSelectLevel(level)
                    }
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
                
                // Preview Section
                ElementPreview(
                    selectedElement = parentChain.getOrNull(selectedLevel),
                    mode = mode
                )
                
                // Hierarchy Visualization
                HierarchyVisualization(
                    hierarchy = parentChain,
                    selectedLevel = selectedLevel,
                    onSelectLevel = { level ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedLevel = level
                        onSelectLevel(level)
                    }
                )
            }
            
            // Bottom Actions
            BottomActionBar(
                mode = mode,
                onCancel = onDismiss,
                onConfirm = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onConfirm(parentChain.getOrNull(selectedLevel)?.selector ?: element.selector)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun ParentSelectorHeader(
    mode: InteractionMode,
    onClose: () -> Unit
) {
    val accent = if (mode == InteractionMode.ELEMENT_ZAP) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val title = stringResource(
        id = if (mode == InteractionMode.ELEMENT_ZAP) {
            R.string.browser_parent_title_block
        } else {
            R.string.browser_parent_title_monitor
        }
    )
    val subtitle = stringResource(id = R.string.browser_parent_subtitle)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = 0.15f),
                        accent.copy(alpha = 0.04f)
                    )
                )
            )
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (mode == InteractionMode.ELEMENT_ZAP) {
                    Icons.Default.Block
                } else {
                    Icons.Default.Visibility
                },
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                )
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(id = R.string.close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ParentLevelSlider(
    currentLevel: Int,
    maxLevel: Int,
    onLevelChange: (Int) -> Unit
) {
    val safeMax = maxLevel.coerceAtLeast(0)
    var sliderValue by remember { mutableFloatStateOf(currentLevel.toFloat()) }

    LaunchedEffect(currentLevel) {
        sliderValue = currentLevel.toFloat()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.browser_parent_slider_label_narrow),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(id = R.string.browser_parent_slider_label_broad),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Slider(
            value = sliderValue,
            onValueChange = { value ->
                sliderValue = value
                onLevelChange(value.roundToInt().coerceIn(0, safeMax))
            },
            valueRange = 0f..safeMax.toFloat(),
            steps = (safeMax - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                activeTickColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                inactiveTickColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
private fun ElementPreview(
    selectedElement: ParentElement?,
    mode: InteractionMode
) {
    selectedElement?.let { element ->
        val accentColor = if (mode == InteractionMode.ELEMENT_ZAP) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }
        val title = if (mode == InteractionMode.ELEMENT_ZAP) {
            stringResource(id = R.string.browser_parent_preview_title_block)
        } else {
            stringResource(id = R.string.browser_parent_preview_title_monitor)
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (mode == InteractionMode.ELEMENT_ZAP) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                }
            ),
            border = BorderStroke(
                1.dp,
                if (mode == InteractionMode.ELEMENT_ZAP) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (mode == InteractionMode.ELEMENT_ZAP) {
                            Icons.Default.Block
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = accentColor
                    )
                }

                // Tag and attributes
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TagChip(element.tagName)

                    element.id.takeIf { it.isNotBlank() }?.let {
                        AttributeChip("#$it", MaterialTheme.colorScheme.tertiary)
                    }
                    
                    element.className.split(" ")
                        .firstOrNull { it.isNotBlank() }
                        ?.let {
                            AttributeChip(".$it", MaterialTheme.colorScheme.secondary)
                        }
                }
                
                // Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(id = R.string.browser_parent_preview_selector_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = element.selector,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Text preview if available
                element.displayName.takeIf { it.isNotBlank() }?.let { text ->
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(id = R.string.browser_parent_preview_text_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(tag: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    ) {
        Text(
            text = tag.lowercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun AttributeChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = color
        )
    }
}

@Composable
private fun HierarchyVisualization(
    hierarchy: List<ParentElement>,
    selectedLevel: Int,
    onSelectLevel: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.browser_parent_hierarchy_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            )
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                hierarchy.forEachIndexed { index, element ->
                    HierarchyItem(
                        element = element,
                        isSelected = index == selectedLevel,
                        depth = element.index,
                        onClick = { onSelectLevel(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HierarchyItem(
    element: ParentElement,
    isSelected: Boolean,
    depth: Int,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        } else {
            Color.Transparent
        },
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS
        ),
        label = "hierarchyBackground"
    )
    
    Surface(
        onClick = onClick,
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = (16 + depth * 16).dp,
                    top = 8.dp,
                    end = 8.dp,
                    bottom = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Connector line
            if (depth > 0) {
                Icon(
                    Icons.Default.SubdirectoryArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            }
            
            // Element info
            Text(
                text = buildString {
                    append(element.tagName.lowercase())
                    if (element.id.isNotBlank()) {
                        append('#')
                        append(element.id)
                    }
                    element.className.split(" ")
                        .firstOrNull { it.isNotBlank() }
                        ?.let {
                            append('.')
                            append(it)
                        }
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            if (element.index == 0) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = stringResource(id = R.string.browser_parent_badge_target).uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomActionBar(
    mode: InteractionMode,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = stringResource(id = R.string.browser_parent_action_cancel))
            }
            
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(2f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (mode == InteractionMode.ELEMENT_ZAP) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                Icon(
                    imageVector = if (mode == InteractionMode.ELEMENT_ZAP) {
                        Icons.Default.Block
                    } else {
                        Icons.Default.Visibility
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (mode == InteractionMode.ELEMENT_ZAP) {
                        stringResource(id = R.string.browser_parent_action_block)
                    } else {
                        stringResource(id = R.string.browser_parent_action_monitor)
                    },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
