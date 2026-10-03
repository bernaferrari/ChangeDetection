/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import com.bernaferrari.changedetection.image.decodeSampledBitmap

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal actual fun SnapshotImage(
    bytes: ByteArray,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    val state by produceState<ImageLoadState>(ImageLoadState.Loading, bytes) {
        value = ImageLoadState.Loading
        value = withContext(Dispatchers.Default) {
            decodeSampledBitmap(
                bytes = bytes,
                maxPixels = MAX_DISPLAY_PIXELS,
                maxDimension = MAX_DISPLAY_DIMENSION,
            )?.let { ImageLoadState.Ready(it.bitmap) } ?: ImageLoadState.Failed
        }
    }

    when (val current = state) {
        ImageLoadState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is ImageLoadState.Ready -> {
            DisposableEffect(current.bitmap) {
                onDispose { current.bitmap.recycle() }
            }
            Image(
                bitmap = current.bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
            )
        }

        ImageLoadState.Failed -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private sealed interface ImageLoadState {
    data object Loading : ImageLoadState
    data class Ready(val bitmap: Bitmap) : ImageLoadState
    data object Failed : ImageLoadState
}

private const val MAX_DISPLAY_PIXELS = 4_000_000
private const val MAX_DISPLAY_DIMENSION = 4_096
