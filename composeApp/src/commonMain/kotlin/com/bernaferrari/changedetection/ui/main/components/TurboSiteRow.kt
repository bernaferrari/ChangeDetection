/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.main.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.ui.main.SiteRowPosition
import com.bernaferrari.changedetection.ui.theme.spacing

/** A standard, full-row checkbox target for the sites included in a Turbo session. */
@Composable
internal fun TurboSiteRow(
    site: Site,
    isSelected: Boolean,
    enabled: Boolean,
    isSyncing: Boolean,
    position: SiteRowPosition = SiteRowPosition.Single,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val siteColor = remember(site.colors.first) { Color(site.colors.first) }
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            lerp(
                MaterialTheme.colorScheme.surfaceContainerLow,
                MaterialTheme.colorScheme.primaryContainer,
                0.16f,
            )
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "turboRowColor",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (position == SiteRowPosition.First || position == SiteRowPosition.Single) 0.dp else 2.dp),
        shape = position.turboRowShape(),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = isSelected,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = { onClick() },
                )
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(siteColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                if (isSyncing) {
                    LoadingIndicator(modifier = Modifier.size(24.dp), color = siteColor)
                } else {
                    Text(
                        text = site.title?.firstOrNull()?.uppercase() ?: "W",
                        style = MaterialTheme.typography.titleSmall,
                        color = siteColor.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = site.title?.takeIf { it.isNotBlank() } ?: site.url,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = site.url.replace(Regex("^https?://"), ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Crossfade(
                targetState = isSelected,
                animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                label = "turboCheck",
            ) { selected ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        )
                        .then(
                            if (selected) {
                                Modifier
                            } else {
                                Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun SiteRowPosition.turboRowShape() = when (this) {
    SiteRowPosition.Single -> RoundedCornerShape(20.dp)
    SiteRowPosition.First -> RoundedCornerShape(20.dp, 20.dp, 6.dp, 6.dp)
    SiteRowPosition.Middle -> RoundedCornerShape(6.dp)
    SiteRowPosition.Last -> RoundedCornerShape(6.dp, 6.dp, 20.dp, 20.dp)
}
