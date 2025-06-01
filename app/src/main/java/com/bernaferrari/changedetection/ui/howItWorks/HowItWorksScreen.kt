package com.bernaferrari.changedetection.ui.howItWorks

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*

data class TimelineStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowItWorksScreen(
    onBackClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error

    val steps = listOf(
        TimelineStep(
            icon = Icons.Outlined.Add,
            title = "Add Website",
            description = "Enter any website URL you want to monitor for changes",
            color = primary
        ),
        TimelineStep(
            icon = Icons.Outlined.CloudSync,
            title = "We Monitor",
            description = "Our system regularly checks your websites in the background",
            color = secondary
        ),
        TimelineStep(
            icon = Icons.Outlined.Search,
            title = "Detect Changes",
            description = "Advanced algorithms compare content and identify modifications",
            color = tertiary
        ),
        TimelineStep(
            icon = Icons.Outlined.Notifications,
            title = "Get Notified",
            description = "Instant notifications when changes are detected on your sites",
            color = error
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How It Works") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            item {
                HeroSection()
            }

            itemsIndexed(steps) { index, step ->
                TimelineItem(
                    step = step,
                    isLast = index == steps.size - 1,
                    animationDelay = index * 200L
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                GetStartedCard()
            }
        }
    }
}

@Composable
private fun HeroSection() {
    val infiniteTransition = rememberInfiniteTransition(label = "hero")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            val primary = MaterialTheme.colorScheme.primary
            val secondary = MaterialTheme.colorScheme.secondary

            // Animated background circles
            Canvas(modifier = Modifier.size(120.dp)) {
                val center = center
                drawCircle(
                    color = primary.copy(alpha = 0.1f),
                    radius = size.minDimension / 2 * scale,
                    center = center
                )
                drawCircle(
                    color = secondary.copy(alpha = 0.08f),
                    radius = size.minDimension / 3 * scale,
                    center = center
                )
            }

            Icon(
                imageVector = Icons.Outlined.Visibility,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .scale(scale),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Website Change Detection",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Never miss important updates. We monitor your favorite websites 24/7 and notify you instantly when content changes.",
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 24.sp
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(0.9f)
        )
    }
}

@Composable
private fun TimelineItem(
    step: TimelineStep,
    isLast: Boolean,
    animationDelay: Long
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(animationDelay)
        visible = true
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .alpha(animatedAlpha)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Timeline node
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                step.color.copy(alpha = 0.2f),
                                step.color.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(36.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            step.color.copy(alpha = 0.1f),
                            RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = step.color
                    )
                }
            }

            // Timeline line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(60.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    step.color.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            ),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp)
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(0.85f)
            )
        }
    }
}

@Composable
private fun GetStartedCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Rocket,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ready to Start?",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add your first website and experience real-time change detection",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(0.8f)
            )
        }
    }
}
