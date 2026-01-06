package com.bernaferrari.changedetection.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.ui.design.CanvasHero
import com.bernaferrari.changedetection.ui.design.CanvasSectionHeader
import kotlinx.coroutines.launch
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
    val currentPage = pagerState.currentPage.coerceIn(0, onboardingPages.lastIndex)
    val currentPageData = onboardingPages[currentPage]

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            CanvasHero(
                title = stringResource(id = currentPageData.titleRes),
                subtitle = stringResource(id = currentPageData.descriptionRes),
                badge = stringResource(
                    id = R.string.onboarding_badge_step,
                    currentPage + 1,
                    onboardingPages.size
                ),
                modifier = Modifier.fillMaxWidth()
            )

            CanvasSectionHeader(
                title = stringResource(id = R.string.onboarding_section_title)
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) { pageIndex ->
                OnboardingPage(page = onboardingPages[pageIndex])
            }

            OnboardingIndicators(
                total = onboardingPages.size,
                current = currentPage
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPage > 0) {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                pagerState.animateScrollToPage(currentPage - 1)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(id = R.string.back))
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                OnboardingActionButton(
                    isLastPage = currentPage == onboardingPages.lastIndex,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (currentPage == onboardingPages.lastIndex) {
                            onOnboardingComplete()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(currentPage + 1)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    page: OnboardingPageData
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(page.iconBackgroundColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = page.iconBackgroundColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = stringResource(id = page.titleRes),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Text(
                text = stringResource(id = page.descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            page.highlightsRes?.let { highlightsRes ->
                val highlights = stringArrayResource(id = highlightsRes)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    highlights.forEach { text ->
                        HighlightRow(text = text)
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun OnboardingIndicators(total: Int, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val isActive = index == current
            val width = if (isActive) 32.dp else 8.dp
            val color = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            }
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
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
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Icon(
            imageVector = buttonIcon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = buttonText,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

data class OnboardingPageData(
    val icon: ImageVector,
    val iconColor: Color,
    val iconBackgroundColor: Color,
    @field:StringRes val titleRes: Int,
    @field:StringRes val descriptionRes: Int,
    @field:ArrayRes val highlightsRes: Int? = null
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
