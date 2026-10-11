/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.a11y_close
import com.bernaferrari.changedetection.shared.generated.resources.a11y_open_license
import com.bernaferrari.changedetection.shared.generated.resources.about_licenses_subtitle
import com.bernaferrari.changedetection.shared.generated.resources.about_licenses_title
import com.bernaferrari.changedetection.ui.theme.spacing
import com.bernaferrari.changedetection.localization.stringResource

private data class License(
    val name: String,
    val author: String,
    val licenseType: String,
    val category: String,
)

private val licenses = listOf(
    License("Android Open Source Project", "Google", "Apache License 2.0", "Android"),
    License("Jetpack Compose", "Google", "Apache License 2.0", "Android"),
    License("Material 3", "Google", "Apache License 2.0", "Android"),
    License("Koin", "Kotzilla", "Apache License 2.0", "Core"),
    License("Kotlin", "JetBrains", "Apache License 2.0", "Core"),
    License("Ktor", "JetBrains", "Apache License 2.0", "Core"),
    License("Room 3", "Android Open Source Project", "Apache License 2.0", "Data"),
    License("AndroidX SQLite", "Android Open Source Project", "Apache License 2.0", "Data"),
    License("Ksoup", "Fleek Soft", "Apache License 2.0", "Networking"),
    License("MaterialKolor", "Jordond", "Apache License 2.0", "UI"),
    License("Noto Sans (Devanagari, Bengali, Simplified Chinese)", "The Noto Project", "SIL Open Font License 1.1", "UI"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutLicensesDialog(
    onDismiss: () -> Unit,
    onUrlClick: (String) -> Unit,
) {
    val groupedLicenses = licenses.groupBy { it.category }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.widthIn(max = 640.dp).heightIn(max = 720.dp).fillMaxSize(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = MaterialTheme.spacing.lg, end = MaterialTheme.spacing.xs, top = MaterialTheme.spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.about_licenses_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(Res.string.about_licenses_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.a11y_close))
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = MaterialTheme.spacing.md))
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(MaterialTheme.spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                    ) {
                        groupedLicenses.forEach { (category, categoryLicenses) ->
                            item(key = category) {
                                Text(
                                    text = if (category == "Android") {
                                        category
                                    } else {
                                        stringResource(
                                            when (category) {
                                                "Core" -> com.bernaferrari.changedetection.shared.generated.resources.Res.string.license_category_core
                                                "Data" -> com.bernaferrari.changedetection.shared.generated.resources.Res.string.license_category_data
                                                "Networking" -> com.bernaferrari.changedetection.shared.generated.resources.Res.string.license_category_networking
                                                else -> com.bernaferrari.changedetection.shared.generated.resources.Res.string.license_category_ui
                                            },
                                        )
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = MaterialTheme.spacing.sm, bottom = MaterialTheme.spacing.xs),
                                )
                            }
                            items(categoryLicenses, key = { it.name }) { license ->
                                LicenseRow(
                                    license = license,
                                    onClick = {
                                        onUrlClick(
                                            if (license.licenseType == "Apache License 2.0") {
                                                "https://www.apache.org/licenses/LICENSE-2.0"
                                            } else {
                                                "https://opensource.org/licenses"
                                            },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LicenseRow(
    license: License,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = license.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${license.author} · ${license.licenseType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = stringResource(Res.string.a11y_open_license),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
