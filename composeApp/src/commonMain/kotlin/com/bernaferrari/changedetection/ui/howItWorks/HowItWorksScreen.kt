/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.howItWorks

import androidx.compose.ui.draw.clip

import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.height
import com.bernaferrari.changedetection.background.MonitoringCapabilities
import com.bernaferrari.changedetection.background.MonitoringExecution
import com.bernaferrari.changedetection.background.monitoringCapabilities
import com.bernaferrari.changedetection.ui.components.segmentPosition
import com.bernaferrari.changedetection.ui.components.segmentShape
import com.bernaferrari.changedetection.ui.components.SegmentPosition
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.components.SignalHeroShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
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
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works
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
fun HowItWorksScreen(onBackClick: () -> Unit) = HowItWorksContent(onBackClick, monitoringCapabilities)

@Composable
internal fun HowItWorksContent(onBackClick: () -> Unit, capabilities: MonitoringCapabilities) {
    val steps = listOf(
        HowItWorksStep(
            title = stringResource(Res.string.how_it_works_step_add_title),
            description = stringResource(Res.string.how_it_works_step_add_description),
            detail = stringResource(Res.string.how_it_works_step_add_highlight),
            icon = Icons.Rounded.AddLink,
        ),
        HowItWorksStep(
            title = stringResource(Res.string.how_it_works_step_monitor_title),
            description = stringResource(
                when (capabilities.execution) {
                    MonitoringExecution.Background -> Res.string.how_it_works_step_monitor_description
                    MonitoringExecution.WhileOpen -> Res.string.settings_checks_while_open
                    MonitoringExecution.Unavailable -> Res.string.settings_checks_unavailable
                },
            ),
            detail = stringResource(if (capabilities.constraints) Res.string.how_it_works_step_monitor_highlight else Res.string.how_it_works_schedule_range),
            icon = Icons.Rounded.NotificationsActive,
        ),
        HowItWorksStep(
            title = stringResource(Res.string.how_it_works_step_focus_title),
            description = stringResource(if (capabilities.turbo) Res.string.how_it_works_step_focus_description else Res.string.turbo_unavailable_description),
            detail = stringResource(if (capabilities.turbo) Res.string.how_it_works_step_focus_highlight else Res.string.turbo_unavailable_title),
            icon = Icons.Rounded.FlashOn,
        ),
        HowItWorksStep(
            title = stringResource(Res.string.how_it_works_step_alerts_title),
            description = stringResource(Res.string.how_it_works_step_alerts_description),
            detail = stringResource(Res.string.how_it_works_step_alerts_highlight),
            icon = Icons.Rounded.Visibility,
        ),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.a11y_navigate_back))
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 640.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "title") {
                    Text(stringResource(Res.string.how_it_works), style = MaterialTheme.typography.displaySmall)
                    Text(
                        stringResource(Res.string.how_intro),
                        Modifier.padding(top = 10.dp, bottom = 20.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item(key = "example") {
                    androidx.compose.material3.Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceBright) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(Res.string.how_example_label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            com.bernaferrari.changedetection.ui.kit.DiffText(
                                com.bernaferrari.changedetection.ui.kit.editText(
                                    listOf(
                                        com.bernaferrari.changedetection.preview.EditSegment("Applications close ", com.bernaferrari.changedetection.preview.EditSegment.Kind.Same),
                                        com.bernaferrari.changedetection.preview.EditSegment("31 October", com.bernaferrari.changedetection.preview.EditSegment.Kind.Removed),
                                        com.bernaferrari.changedetection.preview.EditSegment(" ", com.bernaferrari.changedetection.preview.EditSegment.Kind.Same),
                                        com.bernaferrari.changedetection.preview.EditSegment("14 November", com.bernaferrari.changedetection.preview.EditSegment.Kind.Added),
                                    ),
                                    com.bernaferrari.changedetection.ui.theme.LocalMarks.current,
                                ),
                            )
                            Text(stringResource(Res.string.how_example_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(steps.size, key = { "step-$it" }) { index ->
                    val step = steps[index]
                    Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(com.bernaferrari.changedetection.ui.theme.LocalMarks.current.highlight),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text((index + 1).toString(), style = MaterialTheme.typography.titleMedium, color = com.bernaferrari.changedetection.ui.theme.LocalMarks.current.onHighlight)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(step.title, style = MaterialTheme.typography.titleLarge)
                            Text(step.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(step.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                item(key = "privacy") {
                    androidx.compose.material3.Surface(Modifier.padding(top = 28.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(Res.string.how_it_works_footer_title), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(Res.string.settings_privacy_line), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
