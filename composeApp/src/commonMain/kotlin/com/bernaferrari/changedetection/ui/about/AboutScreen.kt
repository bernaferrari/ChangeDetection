/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.a11y_navigate_back
import com.bernaferrari.changedetection.shared.generated.resources.about_contact_developer
import com.bernaferrari.changedetection.shared.generated.resources.about_developer_name
import com.bernaferrari.changedetection.shared.generated.resources.about_designed_by
import com.bernaferrari.changedetection.shared.generated.resources.about_open_source_disclaimer
import com.bernaferrari.changedetection.shared.generated.resources.about_title
import com.bernaferrari.changedetection.shared.generated.resources.about_view_licenses
import com.bernaferrari.changedetection.shared.generated.resources.about_view_on_github
import com.bernaferrari.changedetection.shared.generated.resources.about_hero_label
import com.bernaferrari.changedetection.shared.generated.resources.about_hero_title
import com.bernaferrari.changedetection.shared.generated.resources.about_local_badge
import com.bernaferrari.changedetection.shared.generated.resources.about_signal_badge
import com.bernaferrari.changedetection.shared.generated.resources.app_name
import com.bernaferrari.changedetection.shared.generated.resources.brand_tagline
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionBrandMark
import com.bernaferrari.changedetection.ui.theme.spacing
import com.bernaferrari.changedetection.ui.components.SignalHeroShape
import com.bernaferrari.changedetection.ui.components.SegmentPosition
import com.bernaferrari.changedetection.ui.components.segmentShape
import org.jetbrains.compose.resources.stringResource

/**
 * Product information follows the same grouped, action-first structure as Settings. It avoids
 * turning a maintenance screen into a promotional landing page while keeping the useful links
 * easy to find and comfortable on wide screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onSocialClick: (String) -> Unit,
    onGitHubClick: (String) -> Unit,
    onContactClick: () -> Unit,
    onLicenseUrlClick: (String) -> Unit,
) {
    var showLicensesDialog by remember { mutableStateOf(false) }
    val spacing = MaterialTheme.spacing

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.a11y_navigate_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = spacing.lg,
                    vertical = spacing.lg,
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = SignalHeroShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ) {
                        Column(
                            modifier = Modifier.padding(spacing.xl),
                            verticalArrangement = Arrangement.spacedBy(spacing.md),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = stringResource(Res.string.about_hero_label),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.72f),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f).padding(end = spacing.md),
                                )
                                ChangeDetectionBrandMark(
                                    modifier = Modifier.size(72.dp),
                                    containerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    signalColor = MaterialTheme.colorScheme.primaryContainer,
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                                Text(
                                    text = stringResource(Res.string.about_hero_title),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = stringResource(Res.string.brand_tagline),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                            ) {
                                AboutBadge(
                                    icon = Icons.Outlined.Lock,
                                    label = stringResource(Res.string.about_local_badge),
                                    modifier = Modifier.weight(1f),
                                )
                                AboutBadge(
                                    icon = Icons.Outlined.NotificationsActive,
                                    label = stringResource(Res.string.about_signal_badge),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Text(
                                text = "${stringResource(Res.string.about_designed_by)} ${stringResource(Res.string.about_developer_name)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.68f),
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        AboutActionRow(
                            icon = Icons.Default.Code,
                            title = stringResource(Res.string.about_view_on_github),
                            subtitle = stringResource(Res.string.about_open_source_disclaimer),
                            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                            onClick = {
                                onGitHubClick("https://github.com/bernaferrari/ChangeDetection")
                            },
                            position = SegmentPosition.First,
                        )
                        AboutActionRow(
                            icon = Icons.Default.Email,
                            title = stringResource(Res.string.about_contact_developer),
                            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                            onClick = onContactClick,
                            position = SegmentPosition.Middle,
                        )
                        AboutActionRow(
                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                            title = stringResource(Res.string.about_view_licenses),
                            onClick = { showLicensesDialog = true },
                            position = SegmentPosition.Last,
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Text(
                            text = stringResource(Res.string.about_developer_name),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                            ) {
                                SocialLink(
                                    label = "GitHub",
                                    modifier = Modifier.weight(1f),
                                ) {
                                    onSocialClick("https://github.com/bernaferrari")
                                }
                                SocialLink(
                                    label = "X",
                                    modifier = Modifier.weight(1f),
                                ) {
                                    onSocialClick("https://x.com/bernaferrari")
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                            ) {
                                SocialLink(
                                    label = "Reddit",
                                    modifier = Modifier.weight(1f),
                                ) {
                                    onSocialClick("https://www.reddit.com/user/bernaferrari")
                                }
                                SocialLink(
                                    label = "LinkedIn",
                                    modifier = Modifier.weight(1f),
                                ) {
                                    onSocialClick("https://www.linkedin.com/in/bernaferrari")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLicensesDialog) {
        AboutLicensesDialog(
            onDismiss = { showLicensesDialog = false },
            onUrlClick = onLicenseUrlClick,
        )
    }
}

@Composable
private fun AboutBadge(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 52.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.sm, vertical = MaterialTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun AboutActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    trailingIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowForward,
    position: SegmentPosition = SegmentPosition.Only,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.spacing.touchTarget),
        shape = segmentShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SocialLink(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.heightIn(min = MaterialTheme.spacing.touchTarget),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
