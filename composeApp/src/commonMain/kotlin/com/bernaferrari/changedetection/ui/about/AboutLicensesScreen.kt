/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.about

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

data class License(
    val name: String,
    val copyright: String,
    val author: String,
    val licenseType: String,
    val url: String? = null,
    val category: String = "General",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutLicensesDialog(
    onDismiss: () -> Unit,
    onUrlClick: (String) -> Unit,
) {
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
            category = "Android Framework",
        ),
        License(
            name = "Jetpack Compose",
            copyright = "2021",
            author = "Google Inc.",
            licenseType = "Apache License 2.0",
            category = "Android Framework",
        ),
        License(
            name = "Material 3",
            copyright = "2021",
            author = "Google Inc.",
            licenseType = "Apache License 2.0",
            category = "Android Framework",
        ),
        License(
            name = "Koin",
            copyright = "2017-2024",
            author = "Kotzilla",
            licenseType = "Apache License 2.0",
            category = "Dependency Injection",
        ),
        License(
            name = "Room",
            copyright = "2017",
            author = "Google Inc.",
            licenseType = "Apache License 2.0",
            category = "Android Framework",
        ),
        License(
            name = "Kotlin",
            copyright = "2010-2024",
            author = "JetBrains",
            licenseType = "Apache License 2.0",
            category = "Language & Runtime",
        ),
        License(
            name = "Ktor",
            copyright = "2018-2024",
            author = "JetBrains",
            licenseType = "Apache License 2.0",
            category = "Networking",
        ),
        License(
            name = "Ksoup",
            copyright = "2024",
            author = "Fleek Soft",
            licenseType = "Apache License 2.0",
            category = "Networking",
        ),
        License(
            name = "Coil",
            copyright = "2021",
            author = "Colin White",
            licenseType = "Apache License 2.0",
            category = "Media & Graphics",
        ),
        License(
            name = "fast-kotlin-diff-core",
            copyright = "2024",
            author = "Bernardo Ferrari",
            licenseType = "Apache License 2.0",
            category = "Utilities",
        ),
    )

    val groupedLicenses = licenses.groupBy { it.category }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Minimal Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = stringResource(Res.string.about_licenses_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(Res.string.about_licenses_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(Res.string.a11y_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Stats Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatCard(
                        number = "${licenses.size}",
                        label = stringResource(Res.string.about_stat_libraries),
                        icon = Icons.Default.Book,
                    )
                    StatCard(
                        number = "${groupedLicenses.size}",
                        label = stringResource(Res.string.about_stat_categories),
                        icon = Icons.Default.Category,
                    )
                    StatCard(
                        number = "${licenses.count { it.licenseType == "Apache License 2.0" }}",
                        label = stringResource(Res.string.about_stat_apache),
                        icon = Icons.Default.Shield,
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )

                // License List
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    groupedLicenses.forEach { (category, categoryLicenses) ->
                        item {
                            CategoryHeader(category = category)
                        }

                        items(
                            items = categoryLicenses,
                            key = { it.name },
                        ) { license ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(300)) +
                                    slideInHorizontally(animationSpec = tween(300)),
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
                                            onUrlClick(url)
                                        }
                                    },
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
    icon: ImageVector,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = number,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CategoryHeader(category: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp),
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        )
    }
}

@Composable
private fun EnhancedLicenseCard(
    license: License,
    onClick: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale",
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
            pressedElevation = 12.dp,
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = license.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (license.copyright.isNotEmpty() && license.author.isNotEmpty()) {
                    Text(
                        text = "© ${license.copyright} ${license.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                } else if (license.author.isNotEmpty()) {
                    Text(
                        text = license.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            // License badge - compact
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Text(
                    text = license.licenseType.replace(" License", "").replace(" 2.0", ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            // Arrow indicator
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = stringResource(Res.string.a11y_open_license),
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
