@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.loading
import com.bernaferrari.changedetection.localization.stringResource
import kotlinx.coroutines.delay

/** A consistently labelled expressive indicator for short, indeterminate waits. */
@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    label: String = stringResource(Res.string.loading),
) {
    LoadingIndicator(
        modifier = modifier
            .semantics { contentDescription = label }
            .size(size),
        color = color,
    )
}

/**
 * Local reads usually finish within a frame or two; flashing a spinner for them reads as a glitch.
 * The indicator only appears once the wait is long enough to be noticed.
 */
@Composable
fun DelayedLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    delayMillis: Long = 400,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) + scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec(), initialScale = 0.8f),
    ) {
        AppLoadingIndicator(size = size)
    }
}
