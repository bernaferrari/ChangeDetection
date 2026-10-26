@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.bernaferrari.changedetection.repo.PageIconShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.bernaferrari.changedetection.ui.components.rememberPageIconMotion
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.extensions.normalizeUrl
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.supportsVisualMonitoring
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.kit.ConnectedChoice
import com.bernaferrari.changedetection.ui.kit.PageAvatar
import com.bernaferrari.changedetection.ui.kit.tileColor
import com.bernaferrari.changedetection.ui.theme.marks
import com.bernaferrari.changedetection.util.readText
import kotlinx.coroutines.delay
import com.bernaferrari.changedetection.localization.pluralStringResource
import com.bernaferrari.changedetection.localization.stringResource

/**
 * Watching a page is one screen: paste a link, the preview appears by itself (and names the page),
 * pick Text or Screenshot, then start. Technical choices wait behind "More options".
 */
@Composable
fun AddEditConfirmScreen(
    url: String,
    urlError: AddEditError?,
    title: String,
    iconUrl: String = "",
    iconShape: PageIconShape = PageIconShape.AUTOMATIC,
    onIconShapeChange: (PageIconShape) -> Unit = {},
    iconColors: com.bernaferrari.changedetection.repo.ColorGroup = 0 to 0,
    onIconColorChange: (com.bernaferrari.changedetection.repo.ColorGroup) -> Unit = {},
    captureMode: CaptureMode = CaptureMode.DEFAULT,
    onCaptureModeChange: (CaptureMode) -> Unit = {},
    preview: CapturePreviewState = CapturePreviewState.Idle,
    legacyThreshold: Int = 0,
    visualChangeThresholdBps: Int = 0,
    onVisualChangeThresholdChange: (Int) -> Unit = {},
    textRegion: com.bernaferrari.changedetection.detection.TextRegion = com.bernaferrari.changedetection.detection.TextRegion(),
    regionError: Boolean = false,
    onTextRegionChange: (com.bernaferrari.changedetection.detection.TextRegion) -> Unit = {},
    onRemoveLegacyThreshold: () -> Unit = {},
    onPreview: () -> Unit = {},
    onClosePreview: () -> Unit = {},
    isEditing: Boolean = false,
    isSaving: Boolean,
    saveError: AddEditError?,
    duplicateSiteId: String? = null,
    onOpenDuplicate: (String) -> Unit = {},
    onDismissDuplicate: () -> Unit = {},
    onUrlChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onLivePage: (title: String?, icon: String?) -> Unit = { _, _ -> },
    onLivePreviewRequest: (LivePreviewCapture) -> Unit = {},
    onNavigateUp: () -> Unit,
    onSave: () -> Unit,
    showUrlChangeDialog: Boolean = false,
    onCancel: () -> Unit = {},
    onKeepHistory: () -> Unit = {},
    onStartFresh: () -> Unit = {},
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    // iOS habit: scrolling or tapping away puts the keyboard down.
    val dismissKeyboardOnScroll = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput && kotlin.math.abs(available.y) > 2f) focusManager.clearFocus()
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }
    val urlFocus = remember { FocusRequester() }
    val clipboard = LocalClipboard.current
    var copiedLink by remember { mutableStateOf<String?>(null) }
    var moreOptions by rememberSaveable { mutableStateOf(false) }
    // These steps belong to the in-memory draft; never restore them without that draft.
    var browsing by remember { mutableStateOf(false) }
    var reviewing by remember { mutableStateOf(false) }
    val validUrl = url.normalizeUrl() != null
    val textMode = captureMode == CaptureMode.TEXT
    val visualMode = captureMode == CaptureMode.SCREENSHOT || captureMode == CaptureMode.DEFAULT
    // Typed selectors refresh the preview once typing pauses; choices refresh it right away.
    var typedRegion by remember { mutableStateOf(false) }
    LaunchedEffect(textRegion) {
        if (!typedRegion) return@LaunchedEffect
        delay(900)
        typedRegion = false
        if (validUrl && (reviewing || isEditing) && !regionError) onPreview()
    }

    LaunchedEffect(url) {
        if (!isEditing && url.isBlank()) {
            copiedLink = clipboard.readText()?.trim()?.takeIf { it.length < 2_000 && ' ' !in it && (it.startsWith("http://") || it.startsWith("https://") || it.contains('.')) }
        }
    }
    LaunchedEffect(Unit) { if (!isEditing && url.isBlank()) urlFocus.requestFocus() }
    // The preview follows the address: once typing pauses on something that looks like a page, load it.
    LaunchedEffect(url, captureMode) {
        if (isEditing && validUrl && preview is CapturePreviewState.Idle) onPreview()
    }

    fun changeMode(mode: CaptureMode) {
        if (isSaving || mode == captureMode) return
        onCaptureModeChange(mode)
        if (validUrl && (reviewing || isEditing)) onPreview()
    }

    val browserAvailable = LocalBrowserAvailable.current
    val nativeChrome = com.bernaferrari.changedetection.ui.navigation.usesNativeNavigation()
    val ignoredParts = remember(textRegion.exclude) { textRegion.exclude.split(", ").map { it.trim() }.filter { it.isNotEmpty() } }
    // Reviewing is a step of its own that slides over the browser (a sheet inside the iOS sheet fought its
    // swipe-to-dismiss); back returns to the page with everything picked so far.
    val reviewStep: @Composable () -> Unit = {
        com.bernaferrari.changedetection.util.BackHandler {
            if (!isSaving) {
                onClosePreview()
                reviewing = false
            }
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(if (isEditing) Res.string.editor_edit_title else Res.string.watch_this_page)) },
                    navigationIcon = {
                        IconButton(enabled = !isSaving, onClick = {
                            onClosePreview()
                            reviewing = false
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.a11y_navigate_back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            },
            bottomBar = {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = onSave,
                            // Never blocked on the snapshot: a ready one becomes the first version, otherwise the first check takes it.
                            enabled = !isSaving,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().heightIn(min = 56.dp),
                        ) {
                            if (isSaving) {
                                LoadingIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.size(8.dp))
                                Text(stringResource(Res.string.saving), style = MaterialTheme.typography.titleMedium)
                            } else {
                                Text(stringResource(if (isEditing) Res.string.save else Res.string.editor_start), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).nestedScroll(dismissKeyboardOnScroll), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PagePreviewCard(
                        url = url, title = title, iconUrl = iconUrl, iconShape = iconShape, onIconShapeChange = onIconShapeChange, iconColors = iconColors, onIconColorChange = onIconColorChange, onTitleChange = onTitleChange, preview = preview, enabled = !isSaving,
                        onPickRegion = if (browserAvailable) {
                            (
                                {
                                    onClosePreview()
                                    reviewing = false
                                    browsing = true
                                }
                                )
                        } else {
                            null
                        },
                        onRetry = onPreview, onEditAddress = {
                            onClosePreview()
                            reviewing = false
                            browsing = false
                        }, onSelectRegion = null, textRegion = textRegion, screenshot = visualMode,
                    )
                    if (browserAvailable) {
                        PartsSummary(textRegion.include, ignoredParts.size, onEdit = {
                            onClosePreview()
                            reviewing = false
                            browsing = true
                        })
                    }
                    OptionsCard(
                        captureMode,
                        ::changeMode,
                        textRegion,
                        {
                            onTextRegionChange(it)
                            onPreview()
                        },
                        enabled = !isSaving,
                        expanded = moreOptions,
                        onExpandedChange = { moreOptions = it },
                        isEditing = isEditing,
                        advanced = {
                            AdvancedOptions(
                                captureMode, textRegion, regionError, visualChangeThresholdBps, onVisualChangeThresholdChange, {
                                    onTextRegionChange(it)
                                    onPreview()
                                }, {
                                    typedRegion = true
                                    onTextRegionChange(it)
                                }, isSaving,
                                onPick = if (browserAvailable) {
                                    (
                                        {
                                            onClosePreview()
                                            reviewing = false
                                            browsing = true
                                        }
                                        )
                                } else {
                                    null
                                },
                            )
                        },
                    )
                    if (duplicateSiteId != null) {
                        DuplicatePageWarning(duplicateSiteId, onOpenDuplicate, enabled = !isSaving) {
                            onDismissDuplicate()
                            reviewing = false
                            browsing = false
                        }
                    }
                    saveError?.localizedMessage()?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                    FootNote(Icons.Outlined.Schedule, stringResource(Res.string.monitor_editor_cadence_hint))
                    // Explained where it matters: right before watching a page you may have signed in to.
                    if (captureMode == CaptureMode.TEXT && !com.bernaferrari.changedetection.repo.supportsRenderedTextMonitoring) {
                        FootNote(Icons.Outlined.Info, stringResource(Res.string.text_http_capability_note))
                    } else if (browserAvailable && (visualMode || captureMode == CaptureMode.TEXT)) {
                        FootNote(Icons.Outlined.Lock, stringResource(Res.string.browser_session_note))
                    }
                }
            }
        }
    }
    val reviewOverlay: @Composable (Boolean) -> Unit = { visible ->
        AnimatedVisibility(
            visible = visible,
            enter = androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)) { it } + fadeIn(),
            exit = androidx.compose.animation.slideOutHorizontally { it } + fadeOut(),
        ) { reviewStep() }
    }

    if (browsing && validUrl) {
        Box(Modifier.fillMaxSize()) {
            WatchBrowser(
                startUrl = remember { url.normalizeUrl() ?: url },
                only = textRegion.include,
                ignored = ignoredParts,
                captureMode = captureMode,
                onPicksChange = { only, ignored -> onTextRegionChange(com.bernaferrari.changedetection.detection.TextRegion(only, ignored.joinToString(", "))) },
                onPageChange = { page -> if (!isEditing && page.trimEnd('/') != (url.normalizeUrl() ?: url).trimEnd('/')) onUrlChange(page) },
                onWatch = { page ->
                    onUrlChange(page.url)
                    onLivePage(page.title, page.icon)
                    reviewing = true
                    page.capturePreview?.let(onLivePreviewRequest) ?: onPreview()
                },
                onClose = {
                    onClosePreview()
                    browsing = false
                    reviewing = false
                },
                watchLabel = stringResource(if (isEditing) Res.string.done else Res.string.watch_this_page),
            )
            reviewOverlay(reviewing)
        }
        return
    }

    if (reviewing) {
        reviewStep()
        return
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    // A new page has its own headline below, so the bar only carries Close.
                    title = { if (isEditing) Text(stringResource(Res.string.editor_edit_title)) },
                    navigationIcon = {
                        IconButton(enabled = !isSaving, onClick = onNavigateUp) {
                            Icon(
                                if (isEditing) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                                contentDescription = stringResource(if (isEditing) Res.string.a11y_navigate_back else Res.string.a11y_close),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            },
            bottomBar = {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Box(
                        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)).padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Button(
                            onClick = {
                                keyboard?.hide()
                                when {
                                    isEditing -> onSave()

                                    browserAvailable -> browsing = true

                                    else -> {
                                        reviewing = true
                                        onPreview()
                                    }
                                }
                            },
                            enabled = !isSaving && validUrl && (!isEditing || preview !is CapturePreviewState.Loading),
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().heightIn(min = 56.dp),
                        ) {
                            if (isSaving) {
                                LoadingIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.size(8.dp))
                                Text(stringResource(Res.string.saving), style = MaterialTheme.typography.titleMedium)
                            } else {
                                Text(stringResource(if (isEditing) Res.string.save else Res.string.editor_open_page), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            },
        ) { padding ->
            Box(
                Modifier.fillMaxSize().padding(padding)
                    .nestedScroll(dismissKeyboardOnScroll)
                    .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!isEditing) {
                        Text(
                            stringResource(Res.string.editor_paste_title),
                            Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            stringResource(Res.string.editor_paste_body),
                            Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AddressField(
                        url = url,
                        error = urlError?.takeIf { url.isNotBlank() }?.localizedMessage(),
                        enabled = !isSaving,
                        focus = urlFocus,
                        onChange = onUrlChange,
                        onGo = {
                            keyboard?.hide()
                            if (validUrl) {
                                when {
                                    isEditing -> onPreview()

                                    browserAvailable -> browsing = true

                                    else -> {
                                        reviewing = true
                                        onPreview()
                                    }
                                }
                            }
                        },
                    )
                    val link = copiedLink
                    AnimatedVisibility(!isEditing && url.isBlank() && link != null) {
                        if (link != null) {
                            SuggestionChip(
                                onClick = {
                                    onUrlChange(link)
                                    keyboard?.hide()
                                    browsing = true
                                },
                                icon = { Icon(Icons.Default.ContentPaste, null, Modifier.size(18.dp)) },
                                label = { Text(stringResource(Res.string.paste) + " · " + link.removePrefix("https://").removePrefix("http://").removePrefix("www."), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                shape = CircleShape,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }

                    if (duplicateSiteId != null) DuplicatePageWarning(duplicateSiteId, onOpenDuplicate, enabled = !isSaving, onDismissDuplicate)

                    AnimatedVisibility(isEditing, enter = fadeIn(), exit = fadeOut()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            PagePreviewCard(
                                url = url,
                                title = title,
                                iconUrl = iconUrl,
                                iconShape = iconShape,
                                onIconShapeChange = onIconShapeChange,
                                iconColors = iconColors,
                                onIconColorChange = onIconColorChange,
                                onTitleChange = onTitleChange,
                                preview = preview,
                                enabled = !isSaving,
                                onPickRegion = if (browserAvailable) ({ browsing = true }) else null,
                                onRetry = onPreview,
                                onEditAddress = { urlFocus.requestFocus() },
                                onSelectRegion = if (textMode && !isSaving) {
                                    { selected ->
                                        onTextRegionChange(selected)
                                        onPreview()
                                    }
                                } else {
                                    null
                                },
                                textRegion = textRegion,
                                screenshot = visualMode,
                            )
                            val ready = (preview as? CapturePreviewState.Ready)?.content as? CapturePreview.Html
                            if (textMode && ready != null && ready.text.split(Regex("\\s+")).count { it.isNotBlank() } < 25 && supportsVisualMonitoring) {
                                com.bernaferrari.changedetection.ui.kit.Callout(
                                    title = stringResource(Res.string.editor_little_text_title),
                                    body = stringResource(Res.string.editor_little_text_body),
                                    icon = Icons.Outlined.Warning,
                                    container = MaterialTheme.marks.warningSoft,
                                    iconTint = MaterialTheme.marks.warning,
                                    modifier = Modifier.offset(x = (-16).dp).fillMaxWidth(),
                                    actions = { Button(enabled = !isSaving, onClick = { changeMode(CaptureMode.SCREENSHOT) }) { Text(stringResource(Res.string.editor_use_screenshot)) } },
                                )
                            }
                            if (browserAvailable) PartsSummary(textRegion.include, ignoredParts.size, onEdit = { browsing = true })
                            OptionsCard(
                                captureMode = captureMode,
                                onMode = ::changeMode,
                                textRegion = textRegion,
                                onRegion = {
                                    onTextRegionChange(it)
                                    onPreview()
                                },
                                enabled = !isSaving,
                                expanded = moreOptions,
                                onExpandedChange = { moreOptions = it },
                                isEditing = isEditing,
                                advanced = {
                                    AdvancedOptions(
                                        captureMode,
                                        textRegion,
                                        regionError,
                                        visualChangeThresholdBps,
                                        onVisualChangeThresholdChange,
                                        onRegion = {
                                            onTextRegionChange(it)
                                            onPreview()
                                        },
                                        onRegionTyped = {
                                            typedRegion = true
                                            onTextRegionChange(it)
                                        },
                                        isSaving = isSaving,
                                        onPick = if (browserAvailable) ({ browsing = true }) else null,
                                    )
                                },
                            )
                            if (legacyThreshold > 0 && !(visualMode && visualChangeThresholdBps > 0) && captureMode != CaptureMode.TEXT && captureMode != CaptureMode.SOURCE) {
                                LegacyThresholdNotice(legacyThreshold, !isSaving, onRemoveLegacyThreshold)
                            }
                            Text(
                                stringResource(if (isEditing) Res.string.editor_baseline_note else Res.string.monitor_editor_cadence_hint),
                                Modifier.padding(horizontal = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    saveError?.localizedMessage()?.let { message ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.error,
                        ) {
                            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, null)
                                Text(message, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        reviewOverlay(reviewing && !browsing)
    }
    if (showUrlChangeDialog) AddressChangeDialog(onCancel, onKeepHistory, onStartFresh)
}

/** A quiet line of context under the options, with an icon saying what it is about. */
@Composable
private fun FootNote(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, Modifier.padding(top = 1.dp).size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** What the picks add up to, with a way back into the page to change them. */
@Composable
private fun PartsSummary(only: String, ignored: Int, onEdit: () -> Unit) {
    Surface(onClick = onEdit, shape = MaterialTheme.shapes.extraLarge, color = tileColor, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.TouchApp, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(if (only.isNotBlank()) Res.string.parts_one else Res.string.parts_whole), style = MaterialTheme.typography.titleSmall)
                Text(
                    if (ignored > 0) pluralStringResource(Res.plurals.parts_ignored, ignored, ignored) else stringResource(Res.string.parts_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun AddressField(url: String, error: String?, enabled: Boolean, focus: FocusRequester, onChange: (String) -> Unit, onGo: () -> Unit) {
    OutlinedTextField(
        value = url,
        onValueChange = onChange,
        enabled = enabled,
        label = { Text(stringResource(Res.string.website_url)) },
        placeholder = { Text(stringResource(Res.string.website_url_placeholder)) },
        supportingText = error?.let { { Text(it) } },
        isError = error != null,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go, autoCorrectEnabled = false, capitalization = KeyboardCapitalization.None),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        leadingIcon = { Icon(Icons.Default.Language, null) },
        trailingIcon = if (url.isNotBlank()) {
            { IconButton(enabled = enabled, onClick = { onChange("") }) { Icon(Icons.Default.Clear, stringResource(Res.string.clear_page_address)) } }
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth().semantics { error?.let { error(it) } }.focusRequester(focus),
        shape = MaterialTheme.shapes.extraLarge,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            focusedContainerColor = tileColor,
            unfocusedContainerColor = tileColor,
            disabledContainerColor = tileColor,
        ),
    )
}

/** The page as it will be watched: its name, and exactly the text or picture we'll compare. */
@Composable
private fun PagePreviewCard(
    url: String,
    title: String,
    iconUrl: String,
    iconShape: PageIconShape,
    onIconShapeChange: (PageIconShape) -> Unit,
    iconColors: com.bernaferrari.changedetection.repo.ColorGroup,
    onIconColorChange: (com.bernaferrari.changedetection.repo.ColorGroup) -> Unit,
    onTitleChange: (String) -> Unit,
    preview: CapturePreviewState,
    enabled: Boolean,
    onRetry: () -> Unit,
    onEditAddress: () -> Unit,
    onPickRegion: (() -> Unit)? = null,
    onSelectRegion: ((com.bernaferrari.changedetection.detection.TextRegion) -> Unit)?,
    textRegion: com.bernaferrari.changedetection.detection.TextRegion,
    /** Screenshots preview at a fixed height; loading reserves it so the sheet does not jump when it arrives. */
    screenshot: Boolean = false,
) {
    val host = remember(url) { url.removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore('/') }
    var choosingShape by remember { mutableStateOf(false) }
    if (choosingShape) {
        PageIconShapePicker(
            name = title.ifBlank { host },
            key = url.normalizeUrl() ?: url,
            icon = iconUrl.ifBlank { null },
            selected = iconShape,
            onSelect = onIconShapeChange,
            colors = iconColors,
            onColorSelect = onIconColorChange,
            onDismiss = { choosingShape = false },
        )
    }
    Surface(shape = MaterialTheme.shapes.extraLarge, color = tileColor, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val iconInteractions = remember { MutableInteractionSource() }
                val iconMotion = rememberPageIconMotion(iconInteractions)
                Box(
                    Modifier.size(60.dp).clickable(enabled = enabled, interactionSource = iconInteractions, indication = null, role = androidx.compose.ui.semantics.Role.Button, onClickLabel = stringResource(Res.string.editor_icon_shape_title)) { choosingShape = true },
                    contentAlignment = Alignment.Center,
                ) {
                    PageAvatar(title.ifBlank { host }, url.normalizeUrl() ?: url, modifier = iconMotion.modifier, size = 52.dp, icon = iconUrl.ifBlank { null }, iconShape = iconShape, colors = iconColors, selectionProgress = iconMotion.outlineProgress, selectionColor = if (iconMotion.focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    // Says the icon is customizable; without it the shape and color picker is a hidden tap.
                    if (enabled) {
                        Box(
                            Modifier.align(Alignment.BottomEnd).size(22.dp).clip(CircleShape).background(tileColor).padding(2.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Palette, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
                PageNameField(title, onTitleChange, host, enabled, Modifier.weight(1f))
            }
            AnimatedContent(
                targetState = preview,
                contentKey = { it::class },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "preview",
            ) { state ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (state) {
                        // Says what is happening and that nothing waits on it, instead of a bare "loading". It holds the
                        // ready preview's exact frame (for text: header, eight lines and "Show more", plus the parts button
                        // below), built from the same invisible pieces, so the content swaps in without the card resizing.
                        CapturePreviewState.Idle, CapturePreviewState.Loading -> {
                            Box(
                                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerLow).then(if (screenshot) Modifier.height(320.dp) else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!screenshot) {
                                    Column(Modifier.fillMaxWidth().graphicsLayer { alpha = 0f }.clearAndSetSemantics {}.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(" ", style = MaterialTheme.typography.labelLarge)
                                        Text("", minLines = 8, maxLines = 8, style = com.bernaferrari.changedetection.ui.theme.Reading.small)
                                        TextButton(onClick = {}, enabled = false, contentPadding = PaddingValues(horizontal = 0.dp)) { Text(" ") }
                                    }
                                }
                                Column(
                                    Modifier.padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    LoadingIndicator(Modifier.size(48.dp))
                                    Text(stringResource(Res.string.capture_first_version_title), style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Text(
                                        stringResource(Res.string.capture_first_version_body),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.widthIn(max = 320.dp),
                                    )
                                }
                            }
                            if (!screenshot && onSelectRegion != null) {
                                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = 0f }.clearAndSetSemantics {}) { Text(" ") }
                            }
                        }

                        is CapturePreviewState.Failed -> Column(
                            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f)).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                                Text(stringResource(Res.string.editor_preview_failed_title), style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                stringResource(
                                    when (state.reason) {
                                        PreviewFailure.TIMEOUT -> Res.string.check_failure_timeout_detail
                                        PreviewFailure.CAPTURE_INCOMPLETE -> Res.string.check_failure_capture_incomplete_detail
                                        PreviewFailure.NO_MATCH -> Res.string.preview_region_no_match
                                        PreviewFailure.REGION -> Res.string.check_failure_region_detail
                                        PreviewFailure.MULTIPLE_REGIONS -> Res.string.preview_single_visual_region
                                        PreviewFailure.TLS -> Res.string.check_failure_tls_detail
                                        PreviewFailure.CONNECTION -> Res.string.check_failure_connection_detail
                                        PreviewFailure.UNAVAILABLE -> Res.string.check_failure_unknown_detail
                                    },
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (state.reason in setOf(PreviewFailure.REGION, PreviewFailure.NO_MATCH, PreviewFailure.MULTIPLE_REGIONS) && onPickRegion != null) {
                                    Button(onClick = onPickRegion, enabled = enabled) { Text(stringResource(Res.string.picker_title)) }
                                }
                                Button(onClick = onRetry, enabled = enabled) { Text(stringResource(Res.string.retry)) }
                                OutlinedButton(onClick = onEditAddress, enabled = enabled) { Text(stringResource(Res.string.editor_change_address)) }
                                com.bernaferrari.changedetection.diagnostics.DiagnosticsEntry()
                            }
                        }

                        is CapturePreviewState.Ready -> when (val content = state.content) {
                            is CapturePreview.Html -> {
                                val words = remember(content.text) { content.text.split(Regex("\\s+")).count { it.isNotBlank() } }
                                var expanded by remember(content.text) { mutableStateOf(false) }
                                Column(
                                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(stringResource(Res.string.editor_preview), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                        Text(pluralStringResource(Res.plurals.editor_words, words, words), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        content.text.ifBlank { stringResource(Res.string.capture_preview_empty) }.let { if (expanded) it.take(6_000) else it.take(1_200) },
                                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                                        maxLines = if (expanded) Int.MAX_VALUE else 8,
                                        overflow = TextOverflow.Ellipsis,
                                        style = com.bernaferrari.changedetection.ui.theme.Reading.small.copy(color = MaterialTheme.colorScheme.onSurface),
                                    )
                                    if (words > 40) {
                                        TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                                            Text(stringResource(if (expanded) Res.string.show_less else Res.string.show_more))
                                        }
                                    }
                                }
                                if (onSelectRegion != null && content.selectableDocument != null) {
                                    TextRegionPickerAction(content.selectableDocument, textRegion, onSelectRegion)
                                }
                            }

                            is CapturePreview.Image -> CapturePreviewPanel(state, enabled, onRetry, {}, bare = true)
                        }
                    }
                }
            }
        }
    }
}

/** How to compare, as one plain choice; everything technical folds out under "More options", in place. */
@Composable
private fun OptionsCard(
    captureMode: CaptureMode,
    onMode: (CaptureMode) -> Unit,
    @Suppress("UNUSED_PARAMETER") textRegion: com.bernaferrari.changedetection.detection.TextRegion,
    @Suppress("UNUSED_PARAMETER") onRegion: (com.bernaferrari.changedetection.detection.TextRegion) -> Unit,
    enabled: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    isEditing: Boolean = false,
    advanced: @Composable () -> Unit,
) {
    val spring = androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntSize>(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
    Surface(shape = MaterialTheme.shapes.extraLarge, color = tileColor, modifier = Modifier.fillMaxWidth().animateContentSize(spring)) {
        Column(Modifier.padding(top = 16.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(Res.string.editor_compare), style = MaterialTheme.typography.titleSmall)
                CompareChoice(captureMode, onMode, enabled, screenshotAvailable = supportsVisualMonitoring, isEditing = isEditing)
            }
            HorizontalDivider(Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.surfaceContainer)
            val rotation by androidx.compose.animation.core.animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
            Row(
                Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = { onExpandedChange(!expanded) }).heightIn(min = 52.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Tune, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(Res.string.editor_more_options), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Icon(Icons.Rounded.ExpandMore, null, Modifier.graphicsLayer { rotationZ = rotation }, tint = MaterialTheme.colorScheme.outline)
            }
            if (expanded) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) { advanced() }
            }
        }
    }
}

/** The technical choices: which part of the page, and how small a visual change to ignore. */
@Composable
private fun AdvancedOptions(
    captureMode: CaptureMode,
    textRegion: com.bernaferrari.changedetection.detection.TextRegion,
    regionError: Boolean,
    visualChangeThresholdBps: Int,
    onVisualChangeThresholdChange: (Int) -> Unit,
    onRegion: (com.bernaferrari.changedetection.detection.TextRegion) -> Unit,
    onRegionTyped: (com.bernaferrari.changedetection.detection.TextRegion) -> Unit,
    isSaving: Boolean,
    onPick: (() -> Unit)?,
) {
    TextRegionFields(textRegion, regionError, !isSaving, onChoose = onRegion, onType = onRegionTyped, onPick = onPick)
    if (captureMode == CaptureMode.SCREENSHOT || captureMode == CaptureMode.DEFAULT) {
        VisualChangeFilterField(visualChangeThresholdBps, !isSaving, onVisualChangeThresholdChange)
    }
}

@Composable
private fun DuplicatePageWarning(
    siteId: String,
    onOpen: (String) -> Unit,
    enabled: Boolean = true,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Icon(imageVector = Icons.Outlined.Info, contentDescription = null)
                Text(stringResource(Res.string.monitor_editor_duplicate_warning), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(enabled = enabled, onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
                TextButton(enabled = enabled, onClick = { onOpen(siteId) }) { Text(stringResource(Res.string.monitor_editor_duplicate_open)) }
            }
        }
    }
}

@Composable
private fun AddEditError.localizedMessage(): String = stringResource(
    when (this) {
        AddEditError.InvalidUrl -> Res.string.monitor_editor_error_invalid_url
        AddEditError.SiteNotFound -> Res.string.monitor_editor_error_not_found
        AddEditError.LoadFailed -> Res.string.monitor_editor_error_load
        AddEditError.SaveFailed -> Res.string.monitor_editor_error_save
    },
)

/** Whether this platform can embed a browser for navigating, signing in and picking parts. Tests turn it off. */
val LocalBrowserAvailable = androidx.compose.runtime.staticCompositionLocalOf { com.bernaferrari.changedetection.browser.supportsPageRegionSelection }

/** The page name as a soft filled field: no label or underline, a primary outline only while editing; the pencil says it is editable. */
@Composable
internal fun PageNameField(value: String, onValueChange: (String) -> Unit, placeholder: String, enabled: Boolean, modifier: Modifier = Modifier) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val scheme = MaterialTheme.colorScheme
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val border by animateColorAsState(if (focused) scheme.primary else Color.Transparent, label = "name-border")
    val container by animateColorAsState(if (focused) scheme.surface else scheme.surfaceContainerHigh, label = "name-container")
    val label = stringResource(Res.string.editor_name)
    // The field owns its text while typing; the page's own title (or an edit elsewhere) still replaces it. The last value
    // sent is remembered so an older echo of the user's own typing can never overwrite newer keystrokes.
    val state = androidx.compose.foundation.text.input.rememberTextFieldState(value)
    var lastSent by remember { mutableStateOf(value) }
    val latestOnValueChange by androidx.compose.runtime.rememberUpdatedState(onValueChange)
    LaunchedEffect(value) {
        if (value != lastSent && value != state.text.toString()) state.setTextAndPlaceCursorAtEnd(value)
        lastSent = value
    }
    LaunchedEffect(state) {
        androidx.compose.runtime.snapshotFlow { state.text.toString() }.collect { text ->
            if (text != lastSent) {
                lastSent = text
                latestOnValueChange(text)
            }
        }
    }
    androidx.compose.foundation.text.BasicTextField(
        state = state,
        enabled = enabled,
        lineLimits = androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine,
        textStyle = MaterialTheme.typography.titleLarge.copy(color = scheme.onSurface),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(scheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Sentences),
        onKeyboardAction = { focusManager.clearFocus() },
        interactionSource = interactions,
        // No visible label; screen readers still hear what the field is.
        modifier = modifier.semantics { contentDescription = label },
        decorator = { inner ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(container)
                    .border(2.dp, border, RoundedCornerShape(16.dp))
                    .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    if (state.text.isEmpty()) Text(placeholder, style = MaterialTheme.typography.titleLarge, color = scheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1)
                    inner()
                }
                if (!focused) Icon(Icons.Outlined.Edit, null, Modifier.padding(start = 8.dp).size(18.dp), tint = scheme.onSurfaceVariant)
            }
        },
    )
}
