package com.bernaferrari.changedetection.detailsvisual

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.bernaferrari.changedetection.extensions.convertTimestampToDate
import com.bernaferrari.changedetection.repo.Snap
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.graphics.createBitmap

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VisualScreen(
    siteId: String,
    contentType: String,
    url: String,
    onNavigateUp: () -> Unit,
    onShareSnap: (Snap) -> Unit = {},
    viewModel: VisualViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snapsPagingData = viewModel.getSnapsPaged(siteId, contentType).collectAsLazyPagingItems()
    val snapsFiltered by viewModel.getSnapsFiltered(siteId, contentType)
        .collectAsStateWithLifecycle(emptyList())

    val isPdf = contentType == "application/pdf"
    val pagerState = rememberPagerState(pageCount = { snapsPagingData.itemCount })
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedSnapIndex by remember { mutableStateOf(0) }
    var showControls by remember { mutableStateOf(true) }
    var isHighQuality by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSnapIndex) {
        if (snapsFiltered.isNotEmpty()) {
            listState.animateScrollToItem(selectedSnapIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        selectedSnapIndex = pagerState.currentPage
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = snapsPagingData[pagerState.currentPage]?.let {
                        VisualViewModel.formatTimestamp(it.timestamp)
                    } ?: "Visual Changes"
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateUp) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showControls = !showControls }) {
                    Icon(
                        imageVector = if (showControls) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showControls) "Hide controls" else "Show controls"
                    )
                }

                IconButton(onClick = { showBottomSheet = true }) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Show list")
                }

                if (isPdf) {
                    IconButton(onClick = { isHighQuality = !isHighQuality }) {
                        Icon(
                            imageVector = Icons.Default.HighQuality,
                            contentDescription = "High quality",
                            tint = if (isHighQuality) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                IconButton(
                    onClick = {
                        snapsPagingData[selectedSnapIndex]?.let { snap ->
                            viewModel.shareSnap(snap, context)
                        }
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }

                IconButton(onClick = { viewModel.openInBrowser(url, context) }) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in browser")
                }
            }
        )

        // Main content pager
        Box(
            modifier = Modifier.weight(1f)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val snap = snapsPagingData[page]
                if (snap != null) {
                    if (isPdf) {
                        PdfViewer(
                            snap = snap,
                            isHighQuality = isHighQuality,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        ImageViewer(
                            snap = snap,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    CircularProgressIndicator()
                }
            }

            // Control bar at bottom
            if (showControls && snapsPagingData.itemCount > 0) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous button
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (pagerState.currentPage > 0) {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage > 0
                        ) {
                            Icon(Icons.Default.ArrowBackIos, contentDescription = "Previous")
                        }

                        // Page indicator
                        Text(
                            text = "${pagerState.currentPage + 1} / ${snapsPagingData.itemCount}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Next button
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (pagerState.currentPage < snapsPagingData.itemCount - 1) {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage < snapsPagingData.itemCount - 1
                        ) {
                            Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next")
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Delete button
                        IconButton(
                            onClick = {
                                snapsPagingData[pagerState.currentPage]?.let { snap ->
                                    viewModel.removeSnap(snap.snapId)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Bottom thumbnail strip
        if (showControls) {
            LazyRow(
                state = listState,
                modifier = Modifier.height(80.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(snapsPagingData.itemCount) { index ->
                    val snap = snapsPagingData[index]
                    if (snap != null) {
                        ThumbnailItem(
                            snap = snap,
                            isSelected = index == pagerState.currentPage,
                            isPdf = isPdf,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Bottom sheet with full list
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(snapsFiltered) { snap ->
                    SnapListItem(
                        snap = snap,
                        onClick = {
                            val index = snapsFiltered.indexOf(snap)
                            if (index >= 0) {
                                selectedSnapIndex = index
                                showBottomSheet = false
                            }
                        },
                        onDelete = { viewModel.removeSnap(snap.snapId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThumbnailItem(
    snap: Snap,
    isSelected: Boolean,
    isPdf: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .size(64.dp)
            .clickable { onClick() },
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        if (isPdf) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = "PDF",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            val file = File(context.filesDir, snap.snapId)
            val contentUri = FileProvider.getUriForFile(
                context,
                "com.bernaferrari.changedetection.files",
                file
            )

            AsyncImage(
                model = contentUri,
                contentDescription = "Thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun PdfViewer(
    snap: Snap,
    isHighQuality: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(snap.snapId, isHighQuality) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(snap.snapId, isHighQuality) { mutableStateOf(true) }
    var error by remember(snap.snapId, isHighQuality) { mutableStateOf<String?>(null) }

    LaunchedEffect(snap.snapId, isHighQuality) {
        isLoading = true
        error = null
        try {
            val renderedBitmap = withContext(Dispatchers.IO) {
                renderPdfPage(context, snap.snapId, isHighQuality)
            }
            bitmap = renderedBitmap
        } catch (e: Exception) {
            error = "Failed to render PDF: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> CircularProgressIndicator()
            error != null -> Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error
            )

            bitmap != null -> {
                AndroidView(
                    factory = { context ->
                        SubsamplingScaleImageView(context).apply {
                            setImage(ImageSource.bitmap(bitmap!!))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private suspend fun renderPdfPage(
    context: Context,
    snapId: String,
    isHighQuality: Boolean
): Bitmap {
    val file = File(context.filesDir, snapId)
    val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    val pdfRenderer = PdfRenderer(fileDescriptor)

    return try {
        val page = pdfRenderer.openPage(0)
        val qualityMultiplier = if (isHighQuality) 6 else 4
        val renderedBitmap =
            createBitmap(page.width * qualityMultiplier, page.height * qualityMultiplier)

        Canvas(renderedBitmap).apply {
            drawColor(androidx.compose.ui.graphics.Color.White.toArgb())
        }

        page.render(renderedBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        renderedBitmap
    } finally {
        pdfRenderer.close()
        fileDescriptor.close()
    }
}

@Composable
private fun ImageViewer(
    snap: Snap,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = File(context.filesDir, snap.snapId)
    val contentUri = FileProvider.getUriForFile(
        context,
        "com.bernaferrari.changedetection.files",
        file
    )

    AndroidView(
        factory = { context ->
            SubsamplingScaleImageView(context).apply {
                setImage(ImageSource.uri(contentUri))
            }
        },
        modifier = modifier
    )
}

@Composable
private fun SnapListItem(
    snap: Snap,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = snap.timestamp.convertTimestampToDate(),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${snap.contentSize} bytes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove snapshot?") },
            text = { Text("Are you sure you want to remove this snapshot?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
