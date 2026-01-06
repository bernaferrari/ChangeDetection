package com.bernaferrari.changedetection.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.ui.theme.ExpressiveSpring

@Composable
fun SharedEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(500)) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = spring(
                        dampingRatio = ExpressiveSpring.GENTLE_DAMPING,
                        stiffness = ExpressiveSpring.GENTLE_STIFFNESS
                    )
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(500, delayMillis = 100)) +
                        slideInVertically(
                            initialOffsetY = { 20 },
                            animationSpec = spring(
                                dampingRatio = ExpressiveSpring.GENTLE_DAMPING,
                                stiffness = ExpressiveSpring.GENTLE_STIFFNESS
                            )
                        )
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(500, delayMillis = 150)) +
                        slideInVertically(
                            initialOffsetY = { 20 },
                            animationSpec = spring(
                                dampingRatio = ExpressiveSpring.GENTLE_DAMPING,
                                stiffness = ExpressiveSpring.GENTLE_STIFFNESS
                            )
                        )
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (primaryActionLabel != null && onPrimaryAction != null) {
                Spacer(modifier = Modifier.height(24.dp))
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(500, delayMillis = 200)) +
                            scaleIn(
                                initialScale = 0.9f,
                                animationSpec = spring(
                                    dampingRatio = ExpressiveSpring.GENTLE_DAMPING,
                                    stiffness = ExpressiveSpring.GENTLE_STIFFNESS
                                )
                            )
                ) {
                    Button(
                        onClick = onPrimaryAction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(primaryActionLabel)
                    }
                }
            }

            if (secondaryActionLabel != null && onSecondaryAction != null) {
                if (primaryActionLabel == null) Spacer(modifier = Modifier.height(24.dp)) else Spacer(
                    modifier = Modifier.height(8.dp)
                )
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(500, delayMillis = 250))
                ) {
                    TextButton(onClick = onSecondaryAction) {
                        Text(secondaryActionLabel)
                    }
                }
            }
        }
    }
}
