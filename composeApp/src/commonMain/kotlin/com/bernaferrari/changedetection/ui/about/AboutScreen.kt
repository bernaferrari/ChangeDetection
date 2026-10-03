/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.about

import com.bernaferrari.changedetection.shared.generated.resources.about_story
import com.bernaferrari.changedetection.shared.generated.resources.settings_feedback
import com.bernaferrari.changedetection.shared.generated.resources.about_elsewhere

import androidx.compose.material.icons.filled.Description

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
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.a11y_navigate_back)) }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 600.dp).fillMaxWidth(), contentPadding = PaddingValues(bottom = 32.dp)) {
                item {
                    Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        com.bernaferrari.changedetection.ui.kit.AppMark(Modifier.size(72.dp))
                        Text(stringResource(Res.string.app_name), Modifier.padding(top = 16.dp), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(Res.string.brand_tagline), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(Res.string.about_story), Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyLarge)
                        Text("— " + stringResource(Res.string.about_developer_name), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    com.bernaferrari.changedetection.ui.kit.ListGroup(Modifier.padding(top = 24.dp)) {
                        com.bernaferrari.changedetection.ui.kit.GroupRow(
                            title = stringResource(Res.string.about_view_on_github),
                            supporting = stringResource(Res.string.about_open_source_disclaimer),
                            icon = Icons.Default.Code,
                            onClick = { onGitHubClick("https://github.com/bernaferrari/ChangeDetection") },
                        ) { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline) }
                        com.bernaferrari.changedetection.ui.kit.GroupRow(
                            title = stringResource(Res.string.settings_feedback),
                            icon = Icons.Default.Email,
                            showDivider = true,
                            onClick = onContactClick,
                        ) { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline) }
                        com.bernaferrari.changedetection.ui.kit.GroupRow(
                            title = stringResource(Res.string.about_view_licenses),
                            icon = Icons.Default.Description,
                            showDivider = true,
                            onClick = { showLicensesDialog = true },
                        )
                    }
                }
                item {
                    com.bernaferrari.changedetection.ui.kit.GroupLabel(stringResource(Res.string.about_elsewhere))
                    androidx.compose.foundation.layout.FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "GitHub" to "https://github.com/bernaferrari",
                            "X" to "https://x.com/bernaferrari",
                            "Reddit" to "https://www.reddit.com/user/bernaferrari",
                            "LinkedIn" to "https://www.linkedin.com/in/bernaferrari",
                        ).forEach { (label, url) ->
                            androidx.compose.material3.AssistChip(onClick = { onSocialClick(url) }, label = { Text(label) }, shape = MaterialTheme.shapes.medium)
                        }
                    }
                }
            }
        }
    }

    if (showLicensesDialog) {
        AboutLicensesDialog(onDismiss = { showLicensesDialog = false }, onUrlClick = onLicenseUrlClick)
    }
}
