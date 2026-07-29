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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
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
import com.bernaferrari.changedetection.shared.generated.resources.app_name
import com.bernaferrari.changedetection.ui.theme.spacing
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
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text(
                            text = stringResource(Res.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${stringResource(Res.string.about_designed_by)} ${stringResource(Res.string.about_developer_name)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item {
                    AboutActionGroup {
                        AboutActionRow(
                            icon = Icons.Default.Code,
                            title = stringResource(Res.string.about_view_on_github),
                            subtitle = stringResource(Res.string.about_open_source_disclaimer),
                            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                            onClick = {
                                onGitHubClick("https://github.com/bernaferrari/ChangeDetection")
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        AboutActionRow(
                            icon = Icons.Default.Email,
                            title = stringResource(Res.string.about_contact_developer),
                            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                            onClick = onContactClick,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        AboutActionRow(
                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                            title = stringResource(Res.string.about_view_licenses),
                            onClick = { showLicensesDialog = true },
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
private fun AboutActionGroup(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column { content() }
    }
}

@Composable
private fun AboutActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    trailingIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowForward,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.spacing.touchTarget),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
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
