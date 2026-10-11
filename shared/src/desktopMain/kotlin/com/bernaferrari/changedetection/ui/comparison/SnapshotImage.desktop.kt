/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import com.bernaferrari.changedetection.image.desktopImageDispatcher

import com.bernaferrari.changedetection.image.decodeDesktopImage

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.stringResource
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.image_comparison_unavailable

@Composable
internal actual fun SnapshotImage(
    bytes: ByteArray,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    val state by produceState<DesktopImageLoadState>(DesktopImageLoadState.Loading, bytes) {
        value = DesktopImageLoadState.Loading
        value = withContext(desktopImageDispatcher) {
            decodeDesktopImage(bytes, 4_000_000, 4096)?.let { DesktopImageLoadState.Ready(it.image) } ?: DesktopImageLoadState.Failed
        }
    }
    val image = (state as? DesktopImageLoadState.Ready)?.image

    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else if (state == DesktopImageLoadState.Loading) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                stringResource(Res.string.image_comparison_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private sealed interface DesktopImageLoadState {
    data object Loading : DesktopImageLoadState
    data class Ready(val image: ImageBitmap) : DesktopImageLoadState
    data object Failed : DesktopImageLoadState
}
