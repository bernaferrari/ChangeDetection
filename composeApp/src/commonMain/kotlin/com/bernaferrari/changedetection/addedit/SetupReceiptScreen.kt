/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.ui.detail.SavedPagePreview
import com.bernaferrari.changedetection.ui.detail.rememberSavedPagePreview
import com.bernaferrari.changedetection.ui.comparison.SnapshotImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.components.CheckFailureNotice
import com.bernaferrari.changedetection.ui.main.watchlistReader
import com.bernaferrari.changedetection.util.displayPageTitle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Save confirmation observes durable evidence; a queued check is never presented as a baseline. */
@Composable
internal fun SetupReceiptScreen(
    savedSite: Site,
    onOpenPage: () -> Unit,
    onDone: () -> Unit,
    repository: SitesRepository = koinInject(),
    snapsRepository: SnapsRepository = koinInject(),
) {
    var page by remember(savedSite.id) { mutableStateOf<SiteAndLastSnap?>(null) }
    var unavailable by remember(savedSite.id) { mutableStateOf(false) }
    var loaded by remember(savedSite.id) { mutableStateOf(false) }
    LaunchedEffect(savedSite.id, repository) {
        watchlistReader(repository, onFailure = { unavailable = true }, onLoaded = { unavailable = false }).collect { rows ->
            page = rows.firstOrNull { it.site.id == savedSite.id }
            loaded = true
        }
    }
    SetupReceiptContent(savedSite, page, unavailable, loaded && page == null, onOpenPage, onDone, rememberSavedPagePreview(page?.snap, snapsRepository), isLoading = !loaded && !unavailable)
}

@Composable
internal fun SetupReceiptContent(
    savedSite: Site,
    page: SiteAndLastSnap?,
    statusUnavailable: Boolean,
    removed: Boolean,
    onOpenPage: () -> Unit,
    onDone: () -> Unit,
    preview: SavedPagePreview = SavedPagePreview.Loading,
    isLoading: Boolean = false,
) {
    Scaffold(
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Button(
                        onClick = onDone,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Outlined.Home, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(Res.string.setup_receipt_done))
                    }
                }
            }
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val topSpace = (maxHeight * .12f).coerceIn(24.dp, 88.dp)
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(topSpace))
                ReceiptMark()
                Spacer(Modifier.height(28.dp))
                Text(
                    stringResource(if (removed || isLoading || statusUnavailable || page?.site?.isSyncEnabled == false) Res.string.setup_receipt_added else Res.string.setup_receipt_title),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = (-0.8).sp),
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    savedSite.displayPageTitle(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(36.dp))
                if (statusUnavailable && !removed) {
                    ReceiptNotice { Text(stringResource(Res.string.setup_receipt_status_unavailable)) }
                    Spacer(Modifier.height(16.dp))
                }
                when {
                    isLoading -> ReceiptNotice { Text(stringResource(Res.string.loading)) }

                    removed -> ReceiptNotice { Text(stringResource(Res.string.detail_missing)) }

                    statusUnavailable && page == null -> TextButton(onClick = onOpenPage) { Text(stringResource(Res.string.setup_receipt_open)) }

                    page?.snap != null -> {
                        Card(
                            onClick = onOpenPage,
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            ReceiptPreview(preview)
                            FlowRow(
                                Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text(stringResource(Res.string.setup_receipt_baseline), style = MaterialTheme.typography.labelMedium)
                                }
                                Text(
                                    stringResource(Res.string.setup_receipt_open),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            stringResource(Res.string.setup_receipt_baseline_detail),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }

                    else -> {
                        ReceiptNotice {
                            if (page?.site?.isSuccessful == false) {
                                CheckFailureNotice(page.site.lastFailureCode)
                            } else {
                                Text(stringResource(Res.string.turbo_waiting_check), style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                stringResource(Res.string.setup_receipt_waiting_detail),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = onOpenPage) { Text(stringResource(Res.string.setup_receipt_open)) }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ReceiptMark() {
    Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Surface(Modifier.size(80.dp).rotate(-9f), shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {}
        Surface(Modifier.size(76.dp).rotate(5f), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Description, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Surface(
            Modifier.align(Alignment.BottomEnd).size(30.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary,
            border = BorderStroke(3.dp, MaterialTheme.colorScheme.background),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }
        }
    }
}

@Composable
private fun ReceiptNotice(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

/** Text captures are presented as readable documents, never invented website screenshots. */
@Composable
private fun ReceiptPreview(preview: SavedPagePreview) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, contentColor = MaterialTheme.colorScheme.onSurface) {
        Box(Modifier.fillMaxWidth().then(if (preview is SavedPagePreview.Image) Modifier.height(180.dp) else Modifier.heightIn(min = 180.dp)), contentAlignment = Alignment.Center) {
            when (preview) {
                is SavedPagePreview.Image -> SnapshotImage(preview.bytes, null, Modifier.fillMaxSize(), ContentScale.Crop)

                is SavedPagePreview.Text -> Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Description, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(Res.string.detail_saved_document), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val lines = preview.excerpt.trim().lines()
                    Text(
                        lines.firstOrNull().orEmpty().ifBlank { stringResource(Res.string.detail_empty_text) },
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (lines.size > 1) {
                        Text(
                            lines.drop(1).joinToString("\n").trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Description, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        stringResource(
                            when (preview) {
                                SavedPagePreview.Loading -> Res.string.loading
                                SavedPagePreview.Unavailable -> Res.string.detail_preview_unavailable
                                else -> Res.string.detail_saved_document
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
