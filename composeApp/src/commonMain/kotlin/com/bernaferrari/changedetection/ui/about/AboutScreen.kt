/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.theme.spacing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onSocialClick: (String) -> Unit,
    onGitHubClick: (String) -> Unit,
    onContactClick: () -> Unit,
    onLicenseUrlClick: (String) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    var showLicensesDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.about_title),
                        style = MaterialTheme.typography.displaySmall,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.a11y_navigate_back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { Spacer(modifier = Modifier.height(spacing.md)) }

            // App Info Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(spacing.md),
                    ) {
                        Text(
                            text = stringResource(Res.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )

                        Text(
                            text = "${stringResource(Res.string.about_designed_by)}\n" +
                                stringResource(Res.string.about_developer_name),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )

                        // Social Links
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    MaterialTheme.shapes.medium,
                                )
                                .padding(spacing.md),
                        ) {
                            SocialLink("GitHub", Res.drawable.github_logo) {
                                onSocialClick("https://github.com/bernaferrari")
                            }
                            SocialLink("X", Res.drawable.x_logo) {
                                onSocialClick("https://x.com/bernaferrari")
                            }
                            SocialLink("Reddit", Res.drawable.reddit_logo) {
                                onSocialClick("https://www.reddit.com/user/bernaferrari")
                            }
                            SocialLink("LinkedIn", Res.drawable.linkedin_logo) {
                                onSocialClick("https://www.linkedin.com/in/bernaferrari")
                            }
                        }

                        AboutOpenSourceLink(
                            label = stringResource(Res.string.about_open_source_disclaimer),
                            onClick = {
                                onGitHubClick("https://github.com/bernaferrari/ChangeDetection")
                            },
                        )
                    }
                }
            }

            // Action Buttons
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(
                        onClick = onContactClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(spacing.touchTarget),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            text = stringResource(Res.string.about_contact_developer),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }

                    OutlinedButton(
                        onClick = { showLicensesDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(spacing.touchTarget),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(spacing.xs))
                        Text(
                            text = stringResource(Res.string.about_view_licenses),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(spacing.md)) }
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
private fun AboutOpenSourceLink(
    label: String,
    onClick: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Composable
private fun SocialLink(
    text: String,
    iconRes: org.jetbrains.compose.resources.DrawableResource,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = text,
            modifier = Modifier.size(20.dp),
        )
    }
}
