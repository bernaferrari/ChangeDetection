package com.bernaferrari.changedetection.ui.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme

@Composable
fun EmptyStateComponent(
    modifier: Modifier = Modifier,
    onAddSite: () -> Unit,
    onLearnMore: () -> Unit = {}
) {

    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(16.dp)

        ) {
            // Animated icon with rotating dashed border
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                // Rotating dashed border
                val infiniteTransition = rememberInfiniteTransition(label = "border")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(16000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )

                Canvas(modifier = Modifier.size(100.dp)) {
                    rotate(rotation) {
                        drawCircle(
                            color = primary,
                            radius = size.minDimension / 2,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(12.dp.toPx(), 8.dp.toPx()),
                                    0f
                                )
                            )
                        )
                    }
                }

                // Background decoration
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                                    MaterialTheme.colorScheme.surface
                                ),
                                radius = 80f
                            ),
                            shape = RoundedCornerShape(40.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Start Monitoring",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Track changes on any website and get notified when content updates.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(0.8f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onAddSite,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Your First Site",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onLearnMore,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Learn How It Works",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
fun EmptyStatePreview() {
    MaterialTheme {
        EmptyStateComponent(
            modifier = Modifier.fillMaxSize(),
            onAddSite = {}
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmptyStateDarkPreview() {
    ChangeDetectionTheme(darkTheme = true) {
        EmptyStateComponent(
            modifier = Modifier.fillMaxSize(),
            onAddSite = {}
        )
    }
}
