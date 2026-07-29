/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.comparison_single_snapshot_hint
import com.bernaferrari.changedetection.shared.generated.resources.comparison_single_snapshot_title
import com.bernaferrari.changedetection.shared.generated.resources.comparison_empty_subtitle
import com.bernaferrari.changedetection.shared.generated.resources.comparison_empty_title
import com.bernaferrari.changedetection.shared.generated.resources.failed_to_load_content
import com.bernaferrari.changedetection.shared.generated.resources.retry
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.ui.theme.spacing
import com.bernaferrari.changedetection.util.formatTimestamp
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EmptySnapshotState(modifier: Modifier = Modifier) {
    SharedEmptyState(
        icon = Icons.Default.Schedule,
        title = stringResource(Res.string.comparison_empty_title),
        description = stringResource(Res.string.comparison_empty_subtitle),
        modifier = modifier,
    )
}

/** Presents one stored snapshot with content-appropriate loading, failure, and empty states. */
@Composable
@ExperimentalMaterial3Api
internal fun SingleSnapshotState(
    snap: Snap,
    content: String?,
    bytes: ByteArray?,
    isLoadingContent: Boolean,
    hasError: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    if (snap.isImageSnapshot() && bytes != null) {
        Box(modifier = modifier) {
            ImageViewport(
                bytes = bytes,
                label = null,
                modifier = Modifier.fillMaxSize(),
            )

            SnapshotTimestampLabel(
                timestamp = snap.timestamp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = spacing.xl),
            )
        }
    } else if (content != null) {
        val isHtml = remember(snap.contentType) {
            snap.isHtmlSnapshot()
        }

        Box(modifier = modifier) {
            if (isHtml) {
                val webViewState = rememberWebViewStateWithHTMLData(data = content)
                WebView(
                    state = webViewState,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = spacing.md),
                ) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(spacing.md),
                    )
                }
            }

            SnapshotTimestampLabel(
                timestamp = snap.timestamp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = spacing.xl),
            )
        }
    } else if (isLoadingContent) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    } else if (hasError) {
        Column(
            modifier = modifier.padding(spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(Res.string.failed_to_load_content))
            Spacer(Modifier.height(spacing.sm))
            Button(onClick = onRetry) {
                Text(stringResource(Res.string.retry))
            }
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(spacing.xl),
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Spacer(modifier = Modifier.height(spacing.lg))
                Text(
                    text = stringResource(Res.string.comparison_single_snapshot_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = stringResource(Res.string.comparison_single_snapshot_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(spacing.md))
                Text(
                    text = formatTimestamp(snap.timestamp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
internal fun SnapshotTimestampLabel(
    timestamp: Long,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
        tonalElevation = spacing.xxs,
    ) {
        Text(
            text = formatTimestamp(timestamp),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
