/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.image_comparison_unavailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import com.bernaferrari.changedetection.localization.stringResource
import org.jetbrains.skia.Image as SkiaImage

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal actual fun SnapshotImage(
    bytes: ByteArray,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    val state by produceState<IosImageState>(IosImageState.Loading, bytes) {
        value = IosImageState.Loading
        value = withContext(Dispatchers.IO) {
            runCatching { SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap() }
                .map { IosImageState.Ready(it) }
                .getOrDefault(IosImageState.Failed)
        }
    }
    when (val current = state) {
        is IosImageState.Ready -> Image(current.image, contentDescription, modifier, contentScale = contentScale)

        IosImageState.Loading -> Box(modifier, contentAlignment = Alignment.Center) { LoadingIndicator() }

        IosImageState.Failed -> Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                stringResource(Res.string.image_comparison_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private sealed interface IosImageState {
    data object Loading : IosImageState
    data class Ready(val image: ImageBitmap) : IosImageState
    data object Failed : IosImageState
}
