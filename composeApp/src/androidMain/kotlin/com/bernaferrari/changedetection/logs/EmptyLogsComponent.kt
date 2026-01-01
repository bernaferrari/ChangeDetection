package com.bernaferrari.changedetection.logs

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import kotlin.math.*

@Composable
fun EmptyLogsComponent(
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(32.dp)
        ) {
            // Animated clock with wave effect and floating particles
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "clock_animation")
                
                // Wave animation for background
                val waveOffset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 2 * PI.toFloat(),
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "wave"
                )

                // Floating particles animation
                val particleOffset1 by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 2 * PI.toFloat(),
                    animationSpec = infiniteRepeatable(
                        animation = tween(6000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "particle1"
                )

                val particleOffset2 by infiniteTransition.animateFloat(
                    initialValue = PI.toFloat(),
                    targetValue = 3 * PI.toFloat(),
                    animationSpec = infiniteRepeatable(
                        animation = tween(8000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "particle2"
                )

                // Pulsing scale for center icon
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Canvas(modifier = Modifier.size(140.dp)) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val radius = size.minDimension / 2

                    // Draw wave rings
                    for (i in 0..2) {
                        val waveRadius = (radius * 0.7f) + (sin(waveOffset + i * PI.toFloat() / 3) * 15f)
                        drawCircle(
                            color = primary.copy(alpha = 0.2f - i * 0.05f),
                            radius = waveRadius,
                            center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                            style = Stroke(width = (2f - i * 0.5f).dp.toPx())
                        )
                    }

                    // Draw floating particles
                    val particles = listOf(
                        Triple(particleOffset1, 0.6f, 0.3f),
                        Triple(particleOffset1 + PI.toFloat() / 2, 0.75f, 0.25f),
                        Triple(particleOffset2, 0.8f, 0.2f),
                        Triple(particleOffset2 + PI.toFloat() / 3, 0.65f, 0.28f)
                    )

                    particles.forEach { (offset, radiusMultiplier, alpha) ->
                        val particleRadius = radius * radiusMultiplier
                        val x = centerX + cos(offset) * particleRadius
                        val y = centerY + sin(offset) * particleRadius
                        
                        drawCircle(
                            color = secondary.copy(alpha = alpha),
                            radius = 4.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(x, y)
                        )
                    }
                }

                // Central clock icon with animated background
                Box(
                    modifier = Modifier
                        .size((100 * scale).dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                ),
                                radius = 120f
                            ),
                            shape = RoundedCornerShape(50.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size((56 * scale).dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "No Changes Yet",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your tracked websites haven't changed recently. We're continuously monitoring them and changes will appear here when detected.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(0.9f)
                    .padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Info card with enhanced styling
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "We're actively monitoring your sites for changes",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
fun EmptyLogsPreview() {
    MaterialTheme {
        EmptyLogsComponent(
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmptyLogsDarkPreview() {
    ChangeDetectionTheme(darkTheme = true) {
        EmptyLogsComponent(
            modifier = Modifier.fillMaxSize()
        )
    }
}
