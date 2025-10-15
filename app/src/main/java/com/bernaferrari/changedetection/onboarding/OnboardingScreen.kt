package com.bernaferrari.changedetection.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.bernaferrari.changedetection.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    // Auto-advance animation
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage < onboardingPages.size - 1) {
            delay(5000) // Auto-advance after 5 seconds
            if (pagerState.currentPage < onboardingPages.size - 1) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSkip()
                    }
                ) {
                Text(
                    text = stringResource(id = R.string.skip),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPage(
                    page = onboardingPages[page],
                    isActive = pagerState.currentPage == page
                )
            }
            
            // Bottom section with indicators and navigation
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(onboardingPages.size) { index ->
                        OnboardingIndicator(
                            isActive = index == pagerState.currentPage,
                            isCompleted = index < pagerState.currentPage
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    if (pagerState.currentPage > 0) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(56.dp))
                    }
                    
                    // Continue/Finish button
                    OnboardingActionButton(
                        isLastPage = pagerState.currentPage == onboardingPages.size - 1,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (pagerState.currentPage == onboardingPages.size - 1) {
                                onOnboardingComplete()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    page: OnboardingPageData,
    isActive: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.3f,
        animationSpec = tween(500),
        label = "alpha"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .scale(scale)
            .alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated icon with floating effect
        val infiniteTransition = rememberInfiniteTransition(label = "floating")
        val floatingOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 10f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "floating"
        )
        
        Card(
            modifier = Modifier
                .size(120.dp)
                .offset(y = floatingOffset.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = page.iconBackgroundColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = page.iconColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Title with animated typing effect
        AnimatedContent(
            targetState = stringResource(id = page.titleRes),
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "title"
        ) { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description with staggered animation
        AnimatedContent(
            targetState = stringResource(id = page.descriptionRes),
            transitionSpec = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(400, delayMillis = 200)
                ) + fadeIn(animationSpec = tween(400, delayMillis = 200)) togetherWith
                        slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
            },
            label = "description"
        ) { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Feature highlights with animated appearance
        page.highlightsRes?.let { highlightsRes ->
            val highlights = stringArrayResource(id = highlightsRes)
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                highlights.forEachIndexed { index, highlight ->
                    var visible by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(isActive) {
                        if (isActive) {
                            delay(300L + index * 100L)
                            visible = true
                        } else {
                            visible = false
                        }
                    }
                    
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        ) + fadeIn()
                    ) {
                        OnboardingHighlight(highlight)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingHighlight(highlight: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = highlight,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun OnboardingIndicator(
    isActive: Boolean,
    isCompleted: Boolean
) {
    val color by animateColorAsState(
        targetValue = when {
            isActive -> MaterialTheme.colorScheme.primary
            isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        },
        animationSpec = tween(300),
        label = "indicator_color"
    )
    
    val width by animateDpAsState(
        targetValue = if (isActive) 32.dp else 8.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "indicator_width"
    )
    
    Box(
        modifier = Modifier
            .width(width)
            .height(8.dp)
            .background(color, RoundedCornerShape(4.dp))
    )
}

@Composable
private fun OnboardingActionButton(
    isLastPage: Boolean,
    onClick: () -> Unit
) {
    val buttonText = stringResource(
        id = if (isLastPage) R.string.onboarding_button_get_started else R.string.continue_label
    )
    val buttonIcon = if (isLastPage) Icons.Default.RocketLaunch else Icons.AutoMirrored.Filled.ArrowForward
    
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(56.dp)
            .widthIn(min = 140.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isLastPage) 
                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
        )
    ) {
        AnimatedContent(
            targetState = buttonIcon to buttonText,
            transitionSpec = {
                fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
            },
            label = "button_content"
        ) { (icon, text) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

data class OnboardingPageData(
    val icon: ImageVector,
    val iconColor: Color,
    val iconBackgroundColor: Color,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @ArrayRes val highlightsRes: Int? = null
)

val onboardingPages = listOf(
    OnboardingPageData(
        icon = Icons.Default.Visibility,
        iconColor = Color.White,
        iconBackgroundColor = Color(0xFF2196F3),
        titleRes = R.string.onboarding_title_welcome,
        descriptionRes = R.string.onboarding_description_welcome,
        highlightsRes = R.array.onboarding_highlights_welcome
    ),
    OnboardingPageData(
        icon = Icons.Default.TouchApp,
        iconColor = Color.White,
        iconBackgroundColor = Color(0xFF4CAF50),
        titleRes = R.string.onboarding_title_element_selection,
        descriptionRes = R.string.onboarding_description_element_selection,
        highlightsRes = R.array.onboarding_highlights_element_selection
    ),
    OnboardingPageData(
        icon = Icons.Default.Block,
        iconColor = Color.White,
        iconBackgroundColor = Color(0xFFFF5722),
        titleRes = R.string.onboarding_title_zap,
        descriptionRes = R.string.onboarding_description_zap,
        highlightsRes = R.array.onboarding_highlights_zap
    ),
    OnboardingPageData(
        icon = Icons.Default.Bolt,
        iconColor = Color.White,
        iconBackgroundColor = Color(0xFFFF9800),
        titleRes = R.string.onboarding_title_turbo,
        descriptionRes = R.string.onboarding_description_turbo,
        highlightsRes = R.array.onboarding_highlights_turbo
    ),
    OnboardingPageData(
        icon = Icons.Default.Analytics,
        iconColor = Color.White,
        iconBackgroundColor = Color(0xFF9C27B0),
        titleRes = R.string.onboarding_title_analytics,
        descriptionRes = R.string.onboarding_description_analytics,
        highlightsRes = R.array.onboarding_highlights_analytics
    ),
    OnboardingPageData(
        icon = Icons.Default.RocketLaunch,
        iconColor = Color.White,
        iconBackgroundColor = Color(0xFF673AB7),
        titleRes = R.string.onboarding_title_ready,
        descriptionRes = R.string.onboarding_description_ready,
        highlightsRes = R.array.onboarding_highlights_ready
    )
)