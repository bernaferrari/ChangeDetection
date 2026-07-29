/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.howItWorks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.a11y_navigate_back
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works_footer_description
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works_footer_title
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works_hero_subtitle
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works_hero_title
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works_primary_cta
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works_step_add_description
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works_step_add_highlight
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works_step_add_title
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works_step_alerts_description
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works_step_alerts_highlight
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works_step_alerts_title
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works_step_focus_description
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works_step_focus_highlight
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works_step_focus_title
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works_step_monitor_description
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works_step_monitor_highlight
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works_step_monitor_title
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works_steps_header
import com.bernaferrari.changedetection.shared.generated.resources.step_number
import com.bernaferrari.changedetection.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

private data class HowItWorksStep(
    val title: String,
    val description: String,
    val detail: String,
    val icon: ImageVector,
)

@Composable
fun HowItWorksScreen(onBackClick: () -> Unit) {
    val steps = listOf(
        HowItWorksStep(
            title = stringResource(Res.string.how_it_works_step_add_title),
            description = stringResource(Res.string.how_it_works_step_add_description),
            detail = stringResource(Res.string.how_it_works_step_add_highlight),
            icon = Icons.Rounded.AddLink,
        ),
        HowItWorksStep(
            title = stringResource(Res.string.how_it_works_step_monitor_title),
            description = stringResource(Res.string.how_it_works_step_monitor_description),
            detail = stringResource(Res.string.how_it_works_step_monitor_highlight),
            icon = Icons.Rounded.NotificationsActive,
        ),
        HowItWorksStep(
            title = stringResource(Res.string.how_it_works_step_focus_title),
            description = stringResource(Res.string.how_it_works_step_focus_description),
            detail = stringResource(Res.string.how_it_works_step_focus_highlight),
            icon = Icons.Rounded.Visibility,
        ),
        HowItWorksStep(
            title = stringResource(Res.string.how_it_works_step_alerts_title),
            description = stringResource(Res.string.how_it_works_step_alerts_description),
            detail = stringResource(Res.string.how_it_works_step_alerts_highlight),
            icon = Icons.Rounded.FlashOn,
        ),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.how_it_works_hero_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.a11y_navigate_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentPadding = PaddingValues(
                    start = MaterialTheme.spacing.lg,
                    top = MaterialTheme.spacing.md,
                    end = MaterialTheme.spacing.lg,
                    bottom = MaterialTheme.spacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                        Text(
                            text = stringResource(Res.string.how_it_works_hero_subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.size(MaterialTheme.spacing.sm))
                        Text(
                            text = stringResource(Res.string.how_it_works_steps_header),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                itemsIndexed(steps) { index, step ->
                    HowItWorksStepCard(number = index + 1, step = step)
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MaterialTheme.spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                    ) {
                        Text(
                            text = stringResource(Res.string.how_it_works_footer_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(Res.string.how_it_works_footer_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        FilledTonalButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = MaterialTheme.spacing.xs),
                        ) {
                            Text(stringResource(Res.string.how_it_works_primary_cta))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HowItWorksStepCard(number: Int, step: HowItWorksStep) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
            ) {
                Text(
                    text = stringResource(Res.string.step_number, number),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = step.detail,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.xxs),
                )
            }
        }
    }
}
