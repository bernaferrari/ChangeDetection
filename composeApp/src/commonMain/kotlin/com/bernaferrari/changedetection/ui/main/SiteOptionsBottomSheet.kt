/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.ui.theme.ExpressiveSpring
import kotlinx.coroutines.delay
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.refresh
import com.bernaferrari.changedetection.shared.generated.resources.edit
import com.bernaferrari.changedetection.shared.generated.resources.delete
import com.bernaferrari.changedetection.shared.generated.resources.open_in_browser
import com.bernaferrari.changedetection.shared.generated.resources.remove
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteOptionsBottomSheet(
    site: Site,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenInBrowser: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Site colors for gradient header
    val siteColor = remember(site.colors.first) { Color(site.colors.first) }
    val siteColor2 = remember(site.colors.second) { Color(site.colors.second) }
    val gradient = remember(siteColor, siteColor2) {
        Brush.linearGradient(listOf(siteColor, siteColor2))
    }

    // Staggered animation states
    var showHeader by remember { mutableStateOf(false) }
    var showRefresh by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showOpenInBrowser by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showHeader = true
        delay(50)
        showRefresh = true
        delay(50)
        showEdit = true
        if (onOpenInBrowser != null) {
            delay(50)
            showOpenInBrowser = true
        }
        delay(50)
        showDelete = true
    }

    val displayTitle = site.title?.takeIf { it.isNotBlank() }
        ?: site.url.replace(Regex("^https?://(www\\.)?"), "").substringBefore("/")
    val displayDomain = site.url.replace(Regex("^https?://(www\\.)?"), "").substringBefore("/")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            // Gradient header with animation
            AnimatedVisibility(
                visible = showHeader,
                enter = fadeIn(animationSpec = tween(200)) + slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = spring(
                        dampingRatio = ExpressiveSpring.GENTLE_DAMPING,
                        stiffness = ExpressiveSpring.GENTLE_STIFFNESS,
                    ),
                ),
            ) {
                // Site header with gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradient)
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Site icon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = displayTitle.firstOrNull()?.uppercase() ?: "W",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (site.title?.isNotBlank() == true) {
                                Text(
                                    text = displayDomain,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedBottomSheetOption(
                visible = showRefresh,
                icon = Icons.Outlined.Refresh,
                text = stringResource(Res.string.refresh),
                onClick = onRefresh,
            )

            AnimatedBottomSheetOption(
                visible = showEdit,
                icon = Icons.Outlined.Edit,
                text = stringResource(Res.string.edit),
                onClick = onEdit,
            )

            if (onOpenInBrowser != null) {
                AnimatedBottomSheetOption(
                    visible = showOpenInBrowser,
                    icon = Icons.Outlined.OpenInBrowser,
                    text = stringResource(Res.string.open_in_browser),
                    onClick = onOpenInBrowser,
                )
            }

            // Divider before delete
            AnimatedVisibility(visible = showDelete) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }

            AnimatedBottomSheetOption(
                visible = showDelete,
                icon = Icons.Outlined.Delete,
                text = stringResource(Res.string.remove),
                tint = MaterialTheme.colorScheme.error,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun AnimatedBottomSheetOption(
    visible: Boolean,
    icon: ImageVector,
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)) + slideInVertically(
            initialOffsetY = { 20 },
            animationSpec = spring(
                dampingRatio = ExpressiveSpring.GENTLE_DAMPING,
                stiffness = ExpressiveSpring.GENTLE_STIFFNESS,
            ),
        ),
    ) {
        BottomSheetOption(
            icon = icon,
            text = text,
            tint = tint,
            onClick = onClick,
        )
    }
}

@Composable
private fun BottomSheetOption(
    icon: ImageVector,
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon with subtle background
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = tint,
            )
        }
    }
}
