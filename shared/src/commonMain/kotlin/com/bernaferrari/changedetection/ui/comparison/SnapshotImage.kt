@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.ui.components.DelayedLoadingIndicator

@Composable
internal expect fun SnapshotImage(
    bytes: ByteArray,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
)

/** False for a capture stacked under another one, so overlapping layers show a single indicator. */
internal val LocalShowsImageLoading = staticCompositionLocalOf { true }

/**
 * While a capture decodes: the canvas stays calm, and only a wait long enough to notice shows one centered
 * indicator. Inside a scrolling viewport the height is unbounded, so a minimum keeps it off the top edge.
 */
@Composable
internal fun SnapshotImageLoading(modifier: Modifier) {
    Box(modifier.fillMaxSize().heightIn(min = 280.dp), contentAlignment = Alignment.Center) {
        if (LocalShowsImageLoading.current) DelayedLoadingIndicator(size = 40.dp)
    }
}

/** The decoded capture eases in over the canvas instead of popping over the placeholder. */
@Composable
internal fun Modifier.snapshotFadeIn(key: Any): Modifier {
    val opacity = remember(key) { Animatable(0f) }
    val spec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    LaunchedEffect(opacity) { opacity.animateTo(1f, spec) }
    return graphicsLayer { alpha = opacity.value }
}
