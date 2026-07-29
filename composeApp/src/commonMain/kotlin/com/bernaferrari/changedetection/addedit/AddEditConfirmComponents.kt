/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.extensions.normalizeUrl
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.color_theme
import com.bernaferrari.changedetection.shared.generated.resources.complete_setup
import com.bernaferrari.changedetection.shared.generated.resources.save
import com.bernaferrari.changedetection.shared.generated.resources.site_name
import com.bernaferrari.changedetection.shared.generated.resources.site_name_placeholder
import com.bernaferrari.changedetection.shared.generated.resources.start_monitoring
import com.bernaferrari.changedetection.shared.generated.resources.a11y_navigate_back
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.ui.components.ColorPickerList
import com.bernaferrari.changedetection.ui.theme.spacing
import io.ktor.http.Url
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditConfirmScreen(
    url: String,
    title: String,
    isEditing: Boolean = false,
    selectedColors: Pair<Int, Int>,
    isSaving: Boolean,
    saveError: String?,
    onTitleChange: (String) -> Unit,
    onColorChange: (Pair<Int, Int>) -> Unit,
    onNavigateUp: () -> Unit,
    onSave: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val domain = remember(url) { extractDomain(url) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.complete_setup)) },
                navigationIcon = {
                    IconButton(onClick = {
                        onNavigateUp()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.a11y_navigate_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
            ) {
                Box(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.ime)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Button(
                        onClick = onSave,
                        enabled = title.isNotBlank() && !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MaterialTheme.spacing.touchTarget),
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.size(MaterialTheme.spacing.xs))
                            Text(stringResource(if (isEditing) Res.string.save else Res.string.start_monitoring))
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SitePreviewCard(
                title = title,
                domain = domain,
                selectedColors = selectedColors,
            )

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(Res.string.site_name)) },
                placeholder = { Text(stringResource(Res.string.site_name_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(selectedColors.first),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )

            saveError?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(Res.string.color_theme),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                ColorThemeSelector(
                    selectedColors = selectedColors,
                    availableColors = defaultAvailableColors,
                    onColorChange = { colors -> onColorChange(colors.first to colors.second) },
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SitePreviewCard(
    title: String,
    domain: String,
    selectedColors: Pair<Int, Int>,
) {
    val displayTitle = title.ifBlank { domain }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color(selectedColors.first).copy(alpha = 0.18f),
                contentColor = Color(selectedColors.first),
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Spacer(modifier = Modifier.size(MaterialTheme.spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (domain.isNotEmpty()) {
                    Text(
                        text = domain,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorThemeSelector(
    selectedColors: Pair<Int, Int>,
    availableColors: List<ColorGroup>,
    onColorChange: (ColorGroup) -> Unit,
) {
    val selectedGroup = ColorGroup(selectedColors.first, selectedColors.second)
    ColorPickerList(
        currentColors = selectedGroup,
        colorGroups = availableColors,
        onColorSelected = onColorChange,
        modifier = Modifier.fillMaxWidth(),
    )
}

private val defaultAvailableColors = listOf(
    ColorGroup(0xff40C4FF.toInt(), 0xff536DFE.toInt()), // Light Blue A200 to Indigo A200
    ColorGroup(0xfffeb47b.toInt(), 0xffff7e5f.toInt()),
    ColorGroup(0xffF54EA2.toInt(), 0xffFF7676.toInt()), // Pink -> Pink
    ColorGroup(0xff17EAD9.toInt(), 0xff448AFF.toInt()), // Cyan -> Blue
    ColorGroup(0xff20e281.toInt(), 0xff00bf8f.toInt()), // Green -> Light-Green
    ColorGroup(0xff7117EA.toInt(), 0xffEA6060.toInt()), // Purple -> Instagram-Purple
    ColorGroup(0xffFBB03B.toInt(), 0xffD4145A.toInt()), // Sanguine
    ColorGroup(0xffFEAC5E.toInt(), 0xff5d0058.toInt()), // Yellow -> Purple
    ColorGroup(0xffffb88c.toInt(), 0xffde6262.toInt()), // A Lost Memory
    ColorGroup(0xff1e3c72.toInt(), 0xff2a5298.toInt()), // Eternal Constance
    ColorGroup(0xff3AA17E.toInt(), 0xff00537E.toInt()), // New Leaf
    ColorGroup(0xff29ABE2.toInt(), 0xff4F00BC.toInt()), // Deep Sea
    ColorGroup(0xffEBC08D.toInt(), 0xffF24645.toInt()), // Fizzy Peach
    ColorGroup(0xff622774.toInt(), 0xffC53364.toInt()), // Dark-Purple -> Light-Purple
)

private fun extractDomain(url: String): String {
    return runCatching {
        val sanitized = url.trim()
        if (sanitized.isEmpty()) return@runCatching ""
        val normalized = sanitized.normalizeUrl() ?: "https://$sanitized"
        val host = Url(normalized).host
        host.removePrefix("www.")
    }.getOrDefault("")
}
