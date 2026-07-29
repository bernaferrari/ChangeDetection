/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.comparison_no_changes_html
import com.bernaferrari.changedetection.shared.generated.resources.comparison_no_changes_title
import com.bernaferrari.changedetection.shared.generated.resources.newer
import com.bernaferrari.changedetection.shared.generated.resources.older
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.util.HtmlSanitizer
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import org.jetbrains.compose.resources.stringResource

/** Rendered old/new HTML layers with a deliberate, user-controlled blend control. */
@Composable
internal fun RenderedHtmlComparison(
    oldContent: String,
    newContent: String,
    modifier: Modifier = Modifier,
) {
    var overlayOpacity by remember { mutableFloatStateOf(0.5f) }
    val similarityPercent = remember(oldContent, newContent) {
        calculateRenderedSimilarity(oldContent, newContent)
    }
    val sanitizedOldContent = remember(oldContent) { HtmlSanitizer.sanitize(oldContent) }
    val sanitizedNewContent = remember(newContent) { HtmlSanitizer.sanitize(newContent) }
    val oldWebViewState = rememberWebViewStateWithHTMLData(data = sanitizedOldContent)
    val newWebViewState = rememberWebViewStateWithHTMLData(data = sanitizedNewContent)

    Column(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val similarityColor = when {
                    similarityPercent >= 90 -> MaterialTheme.colorScheme.tertiary
                    similarityPercent >= 70 -> MaterialTheme.colorScheme.primary
                    similarityPercent >= 50 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.error
                }
                Text(
                    text = "${100 - similarityPercent}% changed",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = similarityColor,
                )
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "$similarityPercent% similar",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (similarityPercent == 100) {
                SharedEmptyState(
                    icon = Icons.Default.CheckCircle,
                    title = stringResource(Res.string.comparison_no_changes_title),
                    description = stringResource(Res.string.comparison_no_changes_html),
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                )
            } else {
                WebView(state = oldWebViewState, modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = overlayOpacity },
                ) {
                    WebView(state = newWebViewState, modifier = Modifier.fillMaxSize())
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = overlayOpacity == 0f,
                    onClick = { overlayOpacity = 0f },
                    label = { Text(stringResource(Res.string.older)) },
                    leadingIcon = if (overlayOpacity == 0f) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else {
                        null
                    },
                )
                Slider(
                    value = overlayOpacity,
                    onValueChange = { overlayOpacity = it },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                )
                FilterChip(
                    selected = overlayOpacity == 1f,
                    onClick = { overlayOpacity = 1f },
                    label = { Text(stringResource(Res.string.newer)) },
                    leadingIcon = if (overlayOpacity == 1f) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
