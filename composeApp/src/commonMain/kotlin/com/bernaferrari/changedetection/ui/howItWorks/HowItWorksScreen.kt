/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.howItWorks

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.design.CanvasHero
import com.bernaferrari.changedetection.ui.design.CanvasPrimaryButton
import com.bernaferrari.changedetection.ui.design.CanvasSectionHeader
import org.jetbrains.compose.resources.stringResource

data class HowItWorksStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val highlight: String,
    val color: Color,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowItWorksScreen(
    onBackClick: () -> Unit,
) {
    val steps = listOf(
        HowItWorksStep(
            title = stringResource(Res.string.how_it_works_step_add_title),
            description = stringResource(Res.string.how_it_works_step_add_description),
            icon = Icons.Rounded.AddLink,
            highlight = stringResource(Res.string.how_it_works_step_add_highlight),
            color = MaterialTheme.colorScheme.primary,
        ),
        HowItWorksStep(
            title = stringResource(Res.string.how_it_works_step_monitor_title),
            description = stringResource(Res.string.how_it_works_step_monitor_description),
            icon = Icons.Rounded.NotificationsActive,
            highlight = stringResource(Res.string.how_it_works_step_monitor_highlight),
            color = MaterialTheme.colorScheme.secondary,
        ),
        HowItWorksStep(
            title = stringResource(Res.string.how_it_works_step_focus_title),
            description = stringResource(Res.string.how_it_works_step_focus_description),
            icon = Icons.Rounded.Visibility,
            highlight = stringResource(Res.string.how_it_works_step_focus_highlight),
            color = MaterialTheme.colorScheme.tertiary,
        ),
        HowItWorksStep(
            title = stringResource(Res.string.how_it_works_step_alerts_title),
            description = stringResource(Res.string.how_it_works_step_alerts_description),
            icon = Icons.Rounded.FlashOn,
            highlight = stringResource(Res.string.how_it_works_step_alerts_highlight),
            color = MaterialTheme.colorScheme.error,
        ),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.a11y_navigate_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            item {
                CanvasHero(
                    title = stringResource(Res.string.how_it_works_hero_title),
                    subtitle = stringResource(Res.string.how_it_works_hero_subtitle),
                    badge = stringResource(Res.string.how_it_works_hero_badge),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }

            item {
                CanvasSectionHeader(
                    title = stringResource(Res.string.how_it_works_steps_header),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                )
            }

            itemsIndexed(steps) { index, step ->
                StepItem(index + 1, step)
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                FooterSection(onBackClick)
            }
        }
    }
}

@Composable
private fun StepItem(number: Int, step: HowItWorksStep) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(step.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        tint = step.color,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Column {
                    Text(
                        text = stringResource(Res.string.step_number, number),
                        style = MaterialTheme.typography.labelMedium,
                        color = step.color,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp,
                        ),
                    )
                }
            }

            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
                color = step.color.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = step.color,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = step.highlight,
                        style = MaterialTheme.typography.labelLarge,
                        color = step.color,
                    )
                }
            }
        }
    }
}

@Composable
private fun FooterSection(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.how_it_works_footer_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.how_it_works_footer_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        CanvasPrimaryButton(
            label = stringResource(Res.string.how_it_works_primary_cta),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            onClick = onBackClick,
        )
    }
}
