/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.*
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
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // App Info Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Text(
                            text = "Change Detection",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )

                        Text(
                            text = buildAnnotatedString {
                                append(stringResource(Res.string.about_designed_by))
                                append("\n")
                                pushStyle(
                                    SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                                append(stringResource(Res.string.about_developer_name))
                                pop()
                            },
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        // Social Links
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(16.dp),
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

                        Text(
                            text = buildAnnotatedString {
                                append(stringResource(Res.string.about_open_source_disclaimer))
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clickable {
                                    onGitHubClick("https://github.com/bernaferrari/ChangeDetection")
                                }
                                .alpha(0.9f),
                        )
                    }
                }
                AssistChip(
                    onClick = {
                        onGitHubClick("https://github.com/bernaferrari/ChangeDetection")
                    },
                    label = { Text(stringResource(Res.string.about_view_on_github)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            // Action Buttons
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(
                        onClick = onContactClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
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
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.about_view_licenses),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
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
private fun SocialLink(
    text: String,
    iconRes: org.jetbrains.compose.resources.DrawableResource,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = IconButtonColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.12f),
        ),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = text,
            modifier = Modifier.size(20.dp),
        )
    }
}
