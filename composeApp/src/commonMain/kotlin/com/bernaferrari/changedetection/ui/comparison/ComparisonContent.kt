/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.failed_to_load_content
import com.bernaferrari.changedetection.shared.generated.resources.retry
import com.bernaferrari.changedetection.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

/** Coordinates version selection, comparison modes, and the content viewport for a snapshot pair. */
@Composable
@ExperimentalMaterial3Api
internal fun DiffFirstContent(
    snaps: List<Snap>,
    selectedPairIndex: Int,
    content1: String?,
    content2: String?,
    bytes1: ByteArray?,
    bytes2: ByteArray?,
    imageDiff: SnapshotImageDiff?,
    isLoadingDiff: Boolean,
    onSelectPair: (Int) -> Unit,
    onRetry: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val newerSnap = snaps.getOrNull(selectedPairIndex)
    val olderSnap = snaps.getOrNull(selectedPairIndex + 1)
    val isImagePair = newerSnap?.isImageSnapshot() == true && olderSnap?.isImageSnapshot() == true
    var mode by remember(selectedPairIndex, isImagePair) {
        mutableStateOf(if (isImagePair) ComparisonMode.ImageDiff else ComparisonMode.Html)
    }
    val modes = remember(isImagePair) {
        if (isImagePair) {
            listOf(
                ComparisonMode.ImageDiff,
                ComparisonMode.ImageSwipe,
                ComparisonMode.ImageBefore,
                ComparisonMode.ImageAfter,
            )
        } else {
            listOf(ComparisonMode.Text, ComparisonMode.Html)
        }
    }

    Column(modifier = modifier) {
        VersionSelector(
            snaps = snaps,
            selectedIndex = selectedPairIndex,
            onSelectPair = onSelectPair,
        )
        ComparisonModeSelector(
            modes = modes,
            selectedMode = mode,
            onModeSelected = { mode = it },
            canShare = (isImagePair && imageDiff != null) || (content1 != null && content2 != null),
            onShare = onShare,
        )

        when {
            isLoadingDiff -> Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(spacing.lg))
            }

            isImagePair -> ImageComparisonContent(
                beforeSnap = olderSnap,
                afterSnap = newerSnap,
                beforeBytes = bytes2,
                afterBytes = bytes1,
                imageDiff = imageDiff,
                mode = mode,
                modifier = Modifier.weight(1f),
            )

            content1 != null && content2 != null -> when (mode) {
                ComparisonMode.Text -> SimpleDiffView(
                    oldContent = content2,
                    newContent = content1,
                    modifier = Modifier.weight(1f),
                )

                ComparisonMode.Html -> RenderedHtmlComparison(
                    oldContent = content2,
                    newContent = content1,
                    modifier = Modifier.weight(1f),
                )

                else -> Unit
            }

            else -> Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(Res.string.failed_to_load_content),
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(spacing.sm))
                Button(onClick = onRetry) {
                    Text(stringResource(Res.string.retry))
                }
            }
        }
    }
}
