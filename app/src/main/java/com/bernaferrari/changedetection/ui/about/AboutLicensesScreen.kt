package com.bernaferrari.changedetection.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import kotlinx.coroutines.delay

data class License(
    val name: String,
    val copyright: String,
    val author: String,
    val licenseType: String,
    val url: String? = null,
    val category: String = "General"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutLicensesDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        isVisible = true
    }

    val licenses = listOf(
        License(
            name = "Android Open Source Project",
            copyright = "2018",
            author = "Google Inc.",
            licenseType = "Apache License 2.0",
            category = "Android Framework"
        ),
        License(
            name = "Jetpack Compose",
            copyright = "2021",
            author = "Google Inc.",
            licenseType = "Apache License 2.0",
            category = "Android Framework"
        ),
        License(
            name = "Material 3",
            copyright = "2021",
            author = "Google Inc.",
            licenseType = "Apache License 2.0",
            category = "Android Framework"
        ),
        License(
            name = "Hilt",
            copyright = "2020",
            author = "Google Inc.",
            licenseType = "Apache License 2.0",
            category = "Android Framework"
        ),
        License(
            name = "Room",
            copyright = "2017",
            author = "Google Inc.",
            licenseType = "Apache License 2.0",
            category = "Android Framework"
        ),
        License(
            name = "DataStore",
            copyright = "2020",
            author = "Google Inc.",
            licenseType = "Apache License 2.0",
            category = "Android Framework"
        ),
        License(
            name = "Navigation Compose",
            copyright = "2021",
            author = "Google Inc.",
            licenseType = "Apache License 2.0",
            category = "Android Framework"
        ),
        License(
            name = "Kotlin",
            copyright = "2010-2017",
            author = "JetBrains",
            licenseType = "Apache License 2.0",
            category = "Language & Runtime"
        ),
        License(
            name = "RxJava",
            copyright = "2016-present",
            author = "RxJava Contributors",
            licenseType = "Apache License 2.0",
            category = "Language & Runtime"
        ),
        License(
            name = "RxAndroid",
            copyright = "2015",
            author = "The RxAndroid authors",
            licenseType = "Apache License 2.0",
            category = "Language & Runtime"
        ),
        License(
            name = "OkHttp",
            copyright = "2019",
            author = "Square, Inc.",
            licenseType = "Apache License 2.0",
            category = "Networking"
        ),
        License(
            name = "Jsoup",
            copyright = "2009-2018",
            author = "Jonathan Hedley",
            licenseType = "MIT License",
            category = "Networking"
        ),
        License(
            name = "Coil",
            copyright = "2021",
            author = "Colin White",
            licenseType = "Apache License 2.0",
            category = "Media & Graphics"
        ),
        License(
            name = "Dagger",
            copyright = "2012",
            author = "The Dagger Authors",
            licenseType = "Apache License 2.0",
            category = "Dependency Injection"
        ),
        License(
            name = "java-diff-utils",
            copyright = "2018",
            author = "Tobias (wumpz)",
            licenseType = "Apache License 2.0",
            category = "Utilities"
        ),
        License(
            name = "Android Iconics",
            copyright = "2017",
            author = "Mike Penz",
            licenseType = "Apache License 2.0",
            category = "UI Components"
        ),
        License(
            name = "Groupie",
            copyright = "2016",
            author = "Lisa Wray",
            licenseType = "MIT License",
            category = "UI Components"
        ),
        License(
            name = "material-about-library",
            copyright = "2016",
            author = "Daniel Stone",
            licenseType = "Apache License 2.0",
            category = "UI Components"
        ),
        License(
            name = "Material Dialogs",
            copyright = "2018",
            author = "Aidan Michael Follestad",
            licenseType = "Apache License 2.0",
            category = "UI Components"
        ),
        License(
            name = "Logger",
            copyright = "2017",
            author = "Orhan Obut",
            licenseType = "Apache License 2.0",
            category = "Development Tools"
        )
    )

    val groupedLicenses = licenses.groupBy { it.category }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Hero Header with Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Open Source",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = "Built with love using amazing libraries",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                modifier = Modifier.padding(start = 44.dp, top = 4.dp)
                            )
                        }

                        FilledTonalIconButton(
                            onClick = onDismiss,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            )
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Stats Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCard(
                        number = "${licenses.size}",
                        label = "Libraries",
                        icon = Icons.Default.Book
                    )
                    StatCard(
                        number = "${groupedLicenses.size}",
                        label = "Categories",
                        icon = Icons.Default.Category
                    )
                    StatCard(
                        number = "${licenses.count { it.licenseType == "Apache License 2.0" }}",
                        label = "Apache 2.0",
                        icon = Icons.Default.Shield
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // License List
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupedLicenses.forEach { (category, categoryLicenses) ->
                        item {
                            CategoryHeader(category = category)
                        }

                        items(
                            items = categoryLicenses,
                            key = { it.name }
                        ) { license ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(300)) +
                                        slideInHorizontally(animationSpec = tween(300))
                            ) {
                                EnhancedLicenseCard(
                                    license = license,
                                    onClick = {
                                        val licenseUrl = when (license.licenseType) {
                                            "Apache License 2.0" -> "https://www.apache.org/licenses/LICENSE-2.0"
                                            "MIT License" -> "https://opensource.org/licenses/MIT"
                                            else -> null
                                        }

                                        licenseUrl?.let { url ->
                                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                            context.startActivity(intent)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

            }
        }
    }
}

@Composable
private fun StatCard(
    number: String,
    label: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = number,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryHeader(category: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun EnhancedLicenseCard(
    license: License,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        onClick = {
            isPressed = true
            onClick()
        },
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 12.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        LaunchedEffect(isPressed) {
            if (isPressed) {
                delay(100)
                isPressed = false
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // License Type Badge
            val licenseIcon = when (license.licenseType) {
                "Apache License 2.0" -> Icons.Default.Shield
                "MIT License" -> Icons.Default.Security
                else -> Icons.Default.Description
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = licenseIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = license.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (license.copyright.isNotEmpty() && license.author.isNotEmpty()) {
                    Text(
                        text = "© ${license.copyright} ${license.author}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else if (license.author.isNotEmpty()) {
                    Text(
                        text = license.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = license.licenseType,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Arrow indicator
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open license",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
