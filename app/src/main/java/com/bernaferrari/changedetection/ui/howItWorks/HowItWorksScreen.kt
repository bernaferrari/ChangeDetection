package com.bernaferrari.changedetection.ui.howItWorks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.ui.design.CanvasHero
import com.bernaferrari.changedetection.ui.design.CanvasPrimaryButton
import com.bernaferrari.changedetection.ui.design.CanvasSectionHeader
import androidx.compose.material3.surfaceColorAtElevation

data class HowItWorksStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color,
    val highlight: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowItWorksScreen(
    onBackClick: () -> Unit
) {
    val steps = listOf(
        HowItWorksStep(
            icon = Icons.Outlined.Add,
            title = stringResource(id = R.string.how_it_works_step_add_title),
            description = stringResource(id = R.string.how_it_works_step_add_description),
            color = MaterialTheme.colorScheme.primary,
            highlight = stringResource(id = R.string.how_it_works_step_add_highlight)
        ),
        HowItWorksStep(
            icon = Icons.Outlined.CloudSync,
            title = stringResource(id = R.string.how_it_works_step_monitor_title),
            description = stringResource(id = R.string.how_it_works_step_monitor_description),
            color = MaterialTheme.colorScheme.secondary,
            highlight = stringResource(id = R.string.how_it_works_step_monitor_highlight)
        ),
        HowItWorksStep(
            icon = Icons.Outlined.Search,
            title = stringResource(id = R.string.how_it_works_step_focus_title),
            description = stringResource(id = R.string.how_it_works_step_focus_description),
            color = MaterialTheme.colorScheme.tertiary,
            highlight = stringResource(id = R.string.how_it_works_step_focus_highlight)
        ),
        HowItWorksStep(
            icon = Icons.Outlined.Notifications,
            title = stringResource(id = R.string.how_it_works_step_alerts_title),
            description = stringResource(id = R.string.how_it_works_step_alerts_description),
            color = MaterialTheme.colorScheme.error,
            highlight = stringResource(id = R.string.how_it_works_step_alerts_highlight)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.how_it_works_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.close)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                CanvasHero(
                    modifier = Modifier.fillMaxWidth(),
                    badge = stringResource(id = R.string.how_it_works_hero_badge),
                    title = stringResource(id = R.string.how_it_works_hero_title),
                    subtitle = stringResource(id = R.string.how_it_works_hero_subtitle)
                )
            }

            item {
                CanvasSectionHeader(
                    title = stringResource(id = R.string.how_it_works_steps_header)
                )
            }

            items(steps) { step ->
                HowItWorksStepCard(step = step)
            }

            item {
                HowItWorksFooter(onBackClick = onBackClick)
            }
        }
    }
}

@Composable
private fun HowItWorksStepCard(step: HowItWorksStep) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = step.color.copy(alpha = 0.12f)
                )
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = null,
                            tint = step.color
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = step.color.copy(alpha = 0.08f)
                )
            ) {
                Text(
                    text = step.highlight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = step.color
                )
            }
        }
    }
}

@Composable
private fun HowItWorksFooter(onBackClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.how_it_works_footer_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(id = R.string.how_it_works_footer_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            CanvasPrimaryButton(
                label = stringResource(id = R.string.how_it_works_primary_cta),
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
