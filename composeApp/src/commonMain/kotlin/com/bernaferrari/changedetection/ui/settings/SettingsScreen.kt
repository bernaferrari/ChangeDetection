/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bernaferrari.changedetection.model.ThemeMode
import com.bernaferrari.changedetection.repo.ComparisonMethod
import com.bernaferrari.changedetection.ui.theme.ExpressiveSpring
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.bernaferrari.changedetection.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onAboutClick: () -> Unit = {},
    onHowItWorksClick: () -> Unit = {},
    onBackgroundSyncClick: () -> Unit = {},
    onContactClick: () -> Unit = {},
    onRecreateActivity: () -> Unit = {},
    appVersion: String = "",
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBackgroundSyncDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        contentWindowInsets = WindowInsets(0), // Remove system insets
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
        ) {
            item {
                SectionHeader(
                    title = stringResource(Res.string.settings_section_appearance),
                    icon = Icons.Outlined.Palette,
                )
                SettingsGroup {
                    ThemeSelector(
                        selectedTheme = uiState.themeMode,
                        onThemeSelected = { viewModel.updateThemeMode(it) },
                    )
                }
            }

            // Sync Settings
            item {
                SectionHeader(
                    title = stringResource(Res.string.settings_monitoring_section),
                    icon = Icons.Outlined.Sync,
                )
                SettingsGroup {
                    ActionItem(
                        icon = Icons.Outlined.Sync,
                        title = stringResource(Res.string.settings_background_sync_title),
                        value = if (uiState.backgroundSync) {
                            stringResource(Res.string.settings_background_sync_every, uiState.syncInterval.toInt())
                        } else {
                            stringResource(Res.string.settings_background_sync_disabled)
                        },
                        onClick = { showBackgroundSyncDialog = true },
                    )
                }
            }

            if (uiState.comparisonMethod == ComparisonMethod.SCREENSHOT) item {
                SettingsGroup {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_comparison_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(Res.string.settings_comparison_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ComparisonMethodSelector(
                            selectedMethod = uiState.comparisonMethod,
                            onMethodSelected = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.updateComparisonMethod(it)
                            },
                        )
                    }
                }
            }

            // Browser delay setting - separate card with explanation
            item {
                SettingsGroup {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.settings_browser_delay_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(Res.string.settings_browser_delay_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Tap-to-cycle value buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val delays = listOf(2000L, 5000L, 10000L, 15000L)
                            delays.forEach { delay ->
                                val isSelected = uiState.browserDelayMs == delay
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.updateBrowserDelay(delay)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = formatDelay(delay),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            ),
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // About Section
            item {
                SectionHeader(
                    title = stringResource(Res.string.settings_about_section),
                    icon = Icons.Outlined.Info,
                )
                SettingsGroup {
                    ActionItem(
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        title = stringResource(Res.string.how_it_works),
                        onClick = onHowItWorksClick,
                    )
                    ItemDivider()
                    ActionItem(
                        icon = Icons.Outlined.Info,
                        title = stringResource(Res.string.settings_about_title),
                        onClick = onAboutClick,
                    )
                    ItemDivider()
                    ActionItem(
                        icon = Icons.Outlined.Code,
                        title = stringResource(Res.string.settings_version),
                        value = appVersion.ifBlank { "—" },
                        showChevron = false,
                        onClick = {},
                    )
                    ItemDivider()
                    ActionItem(
                        icon = Icons.Outlined.Email,
                        title = stringResource(Res.string.settings_action_contact),
                        onClick = onContactClick,
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showBackgroundSyncDialog) {
        BackgroundSyncDialog(
            onDismiss = { showBackgroundSyncDialog = false },
            viewModel = viewModel,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector? = null,
) {
    Row(
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun ThemeSelector(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ThemeMode.entries.chunked(2).forEach { rowModes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowModes.forEach { mode ->
                    ThemeModeToggle(
                        themeMode = mode,
                        isSelected = selectedTheme == mode.name,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onThemeSelected(mode.name)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS,
        ),
        label = "toggleScale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon with colored background
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    title: String,
    value: String? = null,
    showChevron: Boolean = true,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS,
        ),
        label = "actionScale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon with colored background
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )

        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (showChevron) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@Composable
private fun ComparisonMethodSelector(
    selectedMethod: ComparisonMethod,
    onMethodSelected: (ComparisonMethod) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ComparisonMethod.values().forEach { method ->
            val isSelected = method == selectedMethod

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onMethodSelected(method) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        if (method == ComparisonMethod.HTML) {
                            Res.string.settings_comparison_page_text
                        } else {
                            Res.string.settings_comparison_screenshots
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun nextDelay(current: Long): Long {
    val options = listOf(2000L, 5000L, 10000L, 15000L)
    val index = options.indexOf(current).takeIf { it >= 0 } ?: 1
    return options[(index + 1) % options.size]
}

private fun formatDelay(delayMs: Long): String {
    val seconds = (delayMs / 1000L).coerceAtLeast(1L)
    return "${seconds}s"
}
