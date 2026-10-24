@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.addedit

import androidx.compose.material3.SliderState
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.browser.BrowserRuntimeHost
import com.bernaferrari.changedetection.browser.PageCleanup
import com.bernaferrari.changedetection.extensions.normalizeUrl
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.rememberWebViewJsBridge
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.bernaferrari.changedetection.localization.stringResource

/** A part the user tapped in the live page: its CSS selector and how far it has been widened. */
@Serializable
internal data class PickedPart(val selector: String, val depth: Int = 0, val maxDepth: Int = 0, val text: String = "", val tag: String = "")

/** Page scripts share this bridge. Bound every value before it reaches native UI state. */
internal fun decodePickedPart(payload: String, picking: Boolean): PickedPart? {
    if (!picking || payload.length > 8_192) return null
    val part = runCatching { pickJson.decodeFromString<PickedPart>(payload) }.getOrNull() ?: return null
    return part.takeIf {
        it.maxDepth in 0..9 && it.depth in 0..it.maxDepth &&
            it.selector.isNotBlank() && it.selector.length <= 4_096 &&
            it.selector.none { char -> char.code < 32 } && it.text.length <= 120 && it.tag.length <= 32
    }
}

internal fun decodeBrowserUrl(value: String): String? {
    val url = runCatching { pickJson.decodeFromString<String>(value) }.getOrElse { value }
    return url.takeIf { it.length <= 8_192 && (it.startsWith("https://") || it.startsWith("http://")) }?.normalizeUrl()
}

/** What the open page already says about itself, read when watching starts so nothing waits on a reload. */
internal data class LivePage(val url: String, val title: String?, val icon: String?, val capturePreview: LivePreviewCapture? = null)

private const val LIVE_PAGE_SCRIPT = "JSON.stringify([window.location.href,${PageCleanup.TITLE},${PageCleanup.ICON}])"

/** WebViews hand script results back JSON-encoded (Android) or raw (iOS); accepts both. */
internal fun decodeLivePage(value: String): LivePage? {
    val raw = runCatching { pickJson.decodeFromString<String>(value) }.getOrElse { value }
    val parts = runCatching { pickJson.decodeFromString<List<String>>(raw) }.getOrNull()
        ?: return decodeBrowserUrl(value)?.let { LivePage(it, null, null) }
    val url = parts.getOrNull(0)?.let(::decodeBrowserUrl) ?: return null
    return LivePage(
        url = url,
        title = parts.getOrNull(1)?.takeIf { it.isNotBlank() && it.length <= 2_000 },
        icon = parts.getOrNull(2)?.takeIf { it.length <= 8_192 && (it.startsWith("https://") || it.startsWith("http://")) },
    )
}

/**
 * Watching starts in a real browser: go to the exact page (search, menus, signing in all work, and the
 * saved cookies are reused by later Android text and screenshot checks on this device), optionally pick the parts that matter or that are noise, then watch.
 */
@Composable
internal fun WatchBrowser(
    startUrl: String,
    only: String,
    ignored: List<String>,
    onPicksChange: (only: String, ignored: List<String>) -> Unit,
    onPageChange: (String) -> Unit,
    onWatch: (LivePage) -> Unit,
    onClose: () -> Unit,
    watchLabel: String,
    captureMode: com.bernaferrari.changedetection.repo.CaptureMode = com.bernaferrari.changedetection.repo.CaptureMode.SCREENSHOT,
) {
    val state = rememberWebViewState(startUrl).apply {
        webSettings.androidWebSettings.domStorageEnabled = true
        webSettings.androidWebSettings.useWideViewPort = true
        // Match fresh browser captures; transparent page CSS uses the normal white browser canvas.
        webSettings.backgroundColor = androidx.compose.ui.graphics.Color.White
    }
    val navigator = rememberWebViewNavigator()
    val bridge = rememberWebViewJsBridge(navigator)
    var picking by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf<PickedPart?>(null) }
    var editingAddress by remember { mutableStateOf(false) }
    val currentOnPageChange by rememberUpdatedState(onPageChange)
    val currentOnWatch by rememberUpdatedState(onWatch)
    val scope = rememberCoroutineScope()
    val captureMutex = remember(state) { kotlinx.coroutines.sync.Mutex() }
    var liveUrl by remember(startUrl) { mutableStateOf(startUrl) }
    var resolvingWatch by remember { mutableStateOf(false) }
    var watchFailed by remember { mutableStateOf(false) }

    fun watchLivePage() {
        if (resolvingWatch || state.loadingState !is LoadingState.Finished || state.errorsForCurrentRequest.any { it.isFromMainFrame }) return
        scope.launch {
            resolvingWatch = true
            watchFailed = false
            try {
                val raw = kotlinx.coroutines.withTimeoutOrNull(3_000) {
                    kotlinx.coroutines.suspendCancellableCoroutine<String> { continuation ->
                        navigator.evaluateJavaScript(LIVE_PAGE_SCRIPT) { value ->
                            if (continuation.isActive) continuation.resumeWith(Result.success(value))
                        }
                    }
                }
                val page = raw?.let(::decodeLivePage)
                if (page == null || state.errorsForCurrentRequest.any { it.isFromMainFrame }) {
                    watchFailed = true
                } else {
                    liveUrl = page.url
                    // Open review immediately. Its preview job captures this still-mounted browser.
                    val mode = captureMode
                    val region = com.bernaferrari.changedetection.detection.TextRegion(only, ignored.joinToString(", "))
                    currentOnWatch(
                        page.copy(capturePreview = { onPreview ->
                            captureMutex.withLock { captureLivePagePreview(state, navigator, page.url, mode, region, onPreview) }
                        }),
                    )
                }
            } finally {
                resolvingWatch = false
            }
        }
    }

    DisposableEffect(bridge) {
        val handler = object : IJsMessageHandler {
            override fun methodName() = "cdPick"
            override fun handle(message: JsMessage, navigator: WebViewNavigator?, callback: (String) -> Unit) {
                decodePickedPart(message.params, picking)?.let { current = it }
                callback("")
            }
        }
        bridge.register(handler)
        onDispose { bridge.unregister(handler) }
    }
    // Back steps out one level at a time: picking, then the page's own history, then back to the address.
    com.bernaferrari.changedetection.util.BackHandler {
        when {
            picking -> picking = false
            navigator.canGoBack -> navigator.navigateBack()
            else -> onClose()
        }
    }
    val loadFailed = state.errorsForCurrentRequest.any { it.isFromMainFrame }
    val loaded = state.loadingState is LoadingState.Finished && !loadFailed
    LaunchedEffect(state.lastLoadedUrl) {
        state.lastLoadedUrl?.takeIf { it.startsWith("http") }?.let {
            liveUrl = it
            currentOnPageChange(it)
        }
    }
    // The picker script also outlines saved picks, so it is installed on every page, but only intercepts taps while picking.
    LaunchedEffect(loaded, state.lastLoadedUrl) { if (loaded) navigator.evaluateJavaScript(PICKER_SCRIPT) }
    LaunchedEffect(loaded, picking, only, ignored) {
        if (!loaded) return@LaunchedEffect
        navigator.evaluateJavaScript("window.__cdPick&&window.__cdPick.mark(${PageCleanup.js(only)},${PageCleanup.js(ignored.joinToString(", "))});window.__cdPick&&window.__cdPick.${if (picking) "enable" else "disable"}();")
        if (!picking) current = null
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            // Browser controls sit where browsers put them: close, back, forward and the address (with reload) on top.
            topBar = {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(Res.string.a11y_close)) }
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.padding(start = 4.dp)) {
                        // A tight pair, like a browser's: each button is half of one 72×48 target, so the arrows sit together.
                        Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                IconButton(onClick = { navigator.navigateBack() }, enabled = navigator.canGoBack, modifier = Modifier.size(width = 36.dp, height = 48.dp)) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(Res.string.a11y_navigate_back), Modifier.size(20.dp))
                                }
                                IconButton(onClick = { navigator.navigateForward() }, enabled = navigator.canGoForward, modifier = Modifier.size(width = 36.dp, height = 48.dp)) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, stringResource(Res.string.browser_forward), Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                    AddressPill(
                        url = liveUrl,
                        editing = editingAddress,
                        onEdit = { editingAddress = it },
                        onGo = { typed ->
                            editingAddress = false
                            typed.normalizeUrl()?.let { navigator.loadUrl(it) }
                        },
                        onReload = { navigator.reload() },
                        modifier = Modifier.weight(1f).padding(start = 8.dp, end = 8.dp),
                    )
                }
            },
            // Docked, not floating: many pages have their own bottom bar, and it must stay reachable.
            bottomBar = {
                Column {
                    if (watchFailed) Text(stringResource(Res.string.failed_to_load_content), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
                    BrowseToolbar(
                        picks = (if (only.isNotBlank()) 1 else 0) + ignored.size,
                        onPick = { picking = true },
                        onWatch = ::watchLivePage,
                        enabled = loaded && !resolvingWatch,
                        watchLabel = watchLabel,
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                BrowserRuntimeHost(Modifier.fillMaxSize()) {
                    WebView(state = state, modifier = Modifier.fillMaxSize().testTag("watch-browser-content").then(com.bernaferrari.changedetection.browser.watchCaptureViewportModifier()), navigator = navigator, webViewJsBridge = bridge, onCreated = { _ -> }, factory = ::createWatchWebView, platformWebViewParams = com.bernaferrari.changedetection.browser.rememberBrowserPlatformParams())
                }
                if (loadFailed) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                        com.bernaferrari.changedetection.ui.components.SharedEmptyState(
                            icon = Icons.Rounded.Refresh,
                            title = stringResource(Res.string.local_browser_error_title),
                            description = stringResource(Res.string.browser_load_error_body),
                            primaryActionLabel = stringResource(Res.string.retry),
                            onPrimaryAction = { navigator.reload() },
                        )
                    }
                }
                // A page load has real progress: a plain browser bar that eases between updates, fills, then fades.
                val loading = state.loadingState as? LoadingState.Loading
                val progress = remember { androidx.compose.animation.core.Animatable(0f) }
                val progressSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                val target = loading?.progress?.coerceIn(0.05f, 1f)
                LaunchedEffect(target) {
                    // A new navigation restarts from the left instead of sliding back from the last page's full bar.
                    if (target != null && target < progress.value) progress.snapTo(0f)
                    progress.animateTo(target ?: 1f, progressSpec)
                }
                AnimatedVisibility(
                    visible = loading != null,
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    enter = androidx.compose.animation.fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
                    exit = androidx.compose.animation.fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
                ) {
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { progress.value },
                        modifier = Modifier.fillMaxWidth(),
                        trackColor = androidx.compose.ui.graphics.Color.Transparent,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                }
            }
        }
        // The picker slides up over the toolbar, covering it entirely, so the page does not reflow while choosing.
        AnimatedVisibility(
            visible = picking,
            enter = slideInVertically(androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)) { it },
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            PickerPanel(
                current = current,
                only = only,
                ignored = ignored,
                onDepth = { depth -> if (depth != current?.depth) navigator.evaluateJavaScript("window.__cdPick&&window.__cdPick.setDepth($depth)") },
                // Watching one part completes the choice, so picking ends; ignoring can go on for several parts.
                onWatchOnly = { part ->
                    onPicksChange(part.selector, ignored - part.selector)
                    current = null
                    picking = false
                },
                onIgnore = { part ->
                    onPicksChange(only.takeUnless { it == part.selector }.orEmpty(), (ignored + part.selector).distinct())
                    current = null
                    navigator.evaluateJavaScript("window.__cdPick&&window.__cdPick.clear()")
                },
                onRemoveOnly = { onPicksChange("", ignored) },
                onRemoveIgnored = { selector -> onPicksChange(only, ignored - selector) },
                onBack = { picking = false },
            )
        }
    }
}

@Composable
private fun AddressPill(url: String, editing: Boolean, onEdit: (Boolean) -> Unit, onGo: (String) -> Unit, onReload: () -> Unit, modifier: Modifier = Modifier) {
    val display = remember(url) { url.removePrefix("https://").removePrefix("http://").removePrefix("www.").trimEnd('/') }
    if (editing) {
        var typed by remember(url) { mutableStateOf(url) }
        val focus = remember { androidx.compose.ui.focus.FocusRequester() }
        LaunchedEffect(Unit) { focus.requestFocus() }
        TextField(
            value = typed,
            onValueChange = { typed = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onGo = { onGo(typed) }),
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            modifier = modifier.heightIn(min = 48.dp).focusRequester(focus).onFocusChanged { if (!it.hasFocus) onEdit(false) },
        )
    } else {
        Surface(onClick = { onEdit(true) }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = modifier.heightIn(min = 48.dp)) {
            Row(Modifier.padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (url.startsWith("https://")) Icon(Icons.Rounded.Lock, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(display, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = onReload, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Refresh, stringResource(Res.string.refresh), Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
private fun BrowseToolbar(picks: Int, onPick: () -> Unit, onWatch: () -> Unit, watchLabel: String, enabled: Boolean) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BadgedBox(badge = { if (picks > 0) Badge { Text(com.bernaferrari.changedetection.util.localizedInteger(picks)) } }) {
                FilledTonalButton(onClick = onPick, shapes = ButtonDefaults.shapes(), contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Rounded.TouchApp, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.picker_title_short))
                }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onWatch, enabled = enabled, shapes = ButtonDefaults.shapes(), modifier = Modifier.heightIn(min = 48.dp)) {
                Text(watchLabel, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/** uBlock-style picking: tap a part, widen it with the slider, then watch only it or ignore it. */
@Composable
internal fun PickerPanel(
    current: PickedPart?,
    only: String,
    ignored: List<String>,
    onDepth: (Int) -> Unit,
    onWatchOnly: (PickedPart) -> Unit,
    onIgnore: (PickedPart) -> Unit,
    onRemoveOnly: () -> Unit,
    onRemoveIgnored: (String) -> Unit,
    onBack: () -> Unit,
) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().wrapContentWidth().widthIn(max = 560.dp).fillMaxWidth()
                .heightIn(min = 96.dp).animateContentSize(androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow))
                .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Picks apply as soon as they are made, so leaving is a plain back, not a "Done" that might save something.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.offset(x = (-8).dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(Res.string.a11y_navigate_back))
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.picker_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            current == null -> stringResource(Res.string.picker_hint)
                            current.text.isNotBlank() -> "“${current.text.trim()}”"
                            current.tag in setOf("img", "picture", "video", "svg", "canvas") -> stringResource(Res.string.picker_selected_image)
                            current.tag == "iframe" -> stringResource(Res.string.picker_selected_frame)
                            else -> stringResource(Res.string.picker_selected_area)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            val part = current
            if (part != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (part.maxDepth > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(Res.string.picker_smaller), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            // One state per element; the thumb moves as it is dragged and follows the page when the picked part changes.
                            val slider = remember(part.maxDepth) { SliderState(part.depth.toFloat(), (part.maxDepth - 1).coerceAtLeast(0), 0f..part.maxDepth.toFloat()) }
                            LaunchedEffect(slider, part.depth) { slider.value = part.depth.toFloat() }
                            Slider(
                                state = slider,
                                onValueChange = {
                                    slider.value = it
                                    onDepth(kotlin.math.round(it).toInt())
                                },
                                modifier = Modifier.weight(1f),
                            )
                            Text(stringResource(Res.string.picker_larger), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onWatchOnly(part) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) { Text(stringResource(Res.string.picker_watch_only)) }
                        OutlinedButton(onClick = { onIgnore(part) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.VisibilityOff, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(Res.string.picker_ignore))
                        }
                    }
                }
            }
            if (only.isNotBlank() || ignored.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (only.isNotBlank()) {
                        item {
                            InputChip(selected = true, onClick = onRemoveOnly, label = { Text(stringResource(Res.string.picker_watching_part)) }, trailingIcon = { Icon(Icons.Rounded.Close, stringResource(Res.string.picker_remove), Modifier.size(16.dp)) })
                        }
                    }
                    items(ignored) { selector ->
                        InputChip(selected = false, onClick = { onRemoveIgnored(selector) }, label = { Text(stringResource(Res.string.picker_ignored_part)) }, leadingIcon = { Icon(Icons.Rounded.VisibilityOff, null, Modifier.size(16.dp)) }, trailingIcon = { Icon(Icons.Rounded.Close, stringResource(Res.string.picker_remove), Modifier.size(16.dp)) })
                    }
                }
            }
        }
    }
}

private val pickJson = Json { ignoreUnknownKeys = true }

/**
 * Installed into the live page. Taps (while picking) choose the element under the finger; setDepth widens
 * it to an ancestor; mark() outlines the saved picks. Selectors prefer unique ids, then nth-of-type paths.
 */
private val PICKER_SCRIPT = """
(function(){
  if (window.__cdPick) return 0;
  var box = document.createElement('div');
  box.style.cssText = 'position:absolute;z-index:2147483647;pointer-events:none;border:3px solid #4F5BD5;background:rgba(79,91,213,.18);border-radius:8px;display:none;box-sizing:border-box;transition:all .15s ease';
  document.documentElement.appendChild(box);
  var marks = document.createElement('style');
  document.documentElement.appendChild(marks);
  var base = null, steps = [], depth = 0, on = false;
  function stop(e){ return !e || !e.parentElement || e.parentElement === document.body || e.parentElement === document.documentElement; }
  // Each slider step is the next ancestor that is visibly bigger; wrappers with the same box are skipped.
  function chain(e){
    var out = [e], last = e.getBoundingClientRect();
    while (!stop(e) && out.length < 10) {
      e = e.parentElement; var r = e.getBoundingClientRect();
      if (r.width - last.width > 6 || r.height - last.height > 6) { out.push(e); last = r; }
    }
    return out;
  }
  function target(){ return steps[Math.min(depth, steps.length - 1)] || null; }
  function maxDepth(){ return Math.max(0, steps.length - 1); }
  function esc(s){ return window.CSS && CSS.escape ? CSS.escape(s) : s.replace(/[^a-zA-Z0-9_-]/g, '\\${'$'}&'); }
  function unique(sel){ try { return document.querySelectorAll(sel).length === 1; } catch (x) { return false; } }
  function selector(e){
    if (e.id && unique('#' + esc(e.id))) return '#' + esc(e.id);
    var parts = [];
    while (e && e.nodeType === 1 && e !== document.body && e !== document.documentElement) {
      if (e.id && unique('#' + esc(e.id))) { parts.unshift('#' + esc(e.id)); return parts.join(' > '); }
      var i = 1, s = e; while ((s = s.previousElementSibling)) if (s.tagName === e.tagName) i++;
      parts.unshift(e.tagName.toLowerCase() + ':nth-of-type(' + i + ')');
      e = e.parentElement;
    }
    parts.unshift('body');
    return parts.join(' > ');
  }
  function show(){
    var e = target(); if (!e) { box.style.display = 'none'; return; }
    var r = e.getBoundingClientRect();
    box.style.display = 'block'; box.style.left = (r.left + window.scrollX) + 'px'; box.style.top = (r.top + window.scrollY) + 'px';
    box.style.width = r.width + 'px'; box.style.height = r.height + 'px';
  }
  function report(){
    var e = target(); if (!e || !window.kmpJsBridge) return;
    window.kmpJsBridge.callNative('cdPick', JSON.stringify({ selector: selector(e), depth: depth, maxDepth: maxDepth(), text: (e.innerText || e.alt || '').trim().replace(/\s+/g, ' ').slice(0, 120), tag: e.tagName.toLowerCase() }), function(){});
  }
  document.addEventListener('click', function(ev){
    if (!on) return;
    ev.preventDefault(); ev.stopPropagation();
    base = document.elementFromPoint(ev.clientX, ev.clientY); steps = base ? chain(base) : []; depth = 0; show(); report();
  }, true);
  window.__cdPick = {
    enable: function(){ on = true; return 0; },
    disable: function(){ on = false; base = null; steps = []; box.style.display = 'none'; return 0; },
    clear: function(){ base = null; steps = []; box.style.display = 'none'; return 0; },
    setDepth: function(d){ depth = Math.max(0, Math.min(d, maxDepth())); show(); report(); return 0; },
    mark: function(only, ignored){
      var css = '';
      if (only) css += only + '{outline:3px solid #4F5BD5 !important;outline-offset:2px !important;}';
      if (ignored) css += ignored + '{outline:2px dashed #B23A24 !important;opacity:.35 !important;}';
      marks.textContent = css; return 0;
    }
  };
  return 0;
})();
""".trimIndent()
