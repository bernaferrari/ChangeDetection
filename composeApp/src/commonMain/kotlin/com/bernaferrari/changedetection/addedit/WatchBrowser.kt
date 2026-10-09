/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.addedit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import org.jetbrains.compose.resources.stringResource

/** A part the user tapped in the live page: its CSS selector and how far it has been widened. */
@Serializable
internal data class PickedPart(val selector: String, val depth: Int = 0, val maxDepth: Int = 0, val text: String = "", val tag: String = "")

/**
 * Watching starts in a real browser: go to the exact page (search, menus, signing in all work, and the
 * session is reused by later checks), optionally pick the parts that matter or that are noise, then watch.
 */
@Composable
internal fun WatchBrowser(
    startUrl: String,
    only: String,
    ignored: List<String>,
    onPicksChange: (only: String, ignored: List<String>) -> Unit,
    onPageChange: (String) -> Unit,
    onWatch: () -> Unit,
    onClose: () -> Unit,
    watchLabel: String,
) {
    val state = rememberWebViewState(startUrl)
    val navigator = rememberWebViewNavigator()
    val bridge = rememberWebViewJsBridge(navigator)
    var picking by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf<PickedPart?>(null) }
    var editingAddress by remember { mutableStateOf(false) }
    val currentOnPageChange by rememberUpdatedState(onPageChange)

    DisposableEffect(bridge) {
        val handler = object : IJsMessageHandler {
            override fun methodName() = "cdPick"
            override fun handle(message: JsMessage, navigator: WebViewNavigator?, callback: (String) -> Unit) {
                current = runCatching { pickJson.decodeFromString<PickedPart>(message.params) }.getOrNull()
                callback("")
            }
        }
        bridge.register(handler)
        onDispose { bridge.unregister(handler) }
    }
    val loaded = state.loadingState is LoadingState.Finished
    LaunchedEffect(state.lastLoadedUrl) { state.lastLoadedUrl?.takeIf { it.startsWith("http") }?.let(currentOnPageChange) }
    // The picker script also outlines saved picks, so it is installed on every page, but only intercepts taps while picking.
    LaunchedEffect(loaded, state.lastLoadedUrl) { if (loaded) navigator.evaluateJavaScript(PICKER_SCRIPT) }
    LaunchedEffect(loaded, picking, only, ignored) {
        if (!loaded) return@LaunchedEffect
        navigator.evaluateJavaScript("window.__cdPick&&window.__cdPick.mark(${PageCleanup.js(only)},${PageCleanup.js(ignored.joinToString(", "))});window.__cdPick&&window.__cdPick.${if (picking) "enable" else "disable"}();")
        if (!picking) current = null
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(Res.string.a11y_close)) }
                AddressPill(
                    url = state.lastLoadedUrl ?: startUrl,
                    editing = editingAddress,
                    onEdit = { editingAddress = it },
                    onGo = { typed ->
                        editingAddress = false
                        typed.normalizeUrl()?.let { navigator.loadUrl(it) }
                    },
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { navigator.reload() }) { Icon(Icons.Rounded.Refresh, stringResource(Res.string.refresh)) }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            BrowserRuntimeHost(Modifier.fillMaxSize()) {
                WebView(state = state, modifier = Modifier.fillMaxSize(), navigator = navigator, webViewJsBridge = bridge)
            }
            val loading = state.loadingState as? LoadingState.Loading
            if (loading != null) {
                LinearWavyProgressIndicator(progress = { loading.progress.coerceAtLeast(0.05f) }, modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
            Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp)) {
                AnimatedContent(
                    targetState = picking,
                    transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { it / 2 } + fadeOut()) },
                    label = "browser-tools",
                ) { isPicking ->
                    if (isPicking) {
                        PickerPanel(
                            current = current,
                            only = only,
                            ignored = ignored,
                            onDepth = { depth -> if (depth != current?.depth) navigator.evaluateJavaScript("window.__cdPick&&window.__cdPick.setDepth($depth)") },
                            onWatchOnly = { part ->
                                onPicksChange(part.selector, ignored - part.selector)
                                current = null
                                navigator.evaluateJavaScript("window.__cdPick&&window.__cdPick.clear()")
                            },
                            onIgnore = { part ->
                                onPicksChange(only.takeUnless { it == part.selector }.orEmpty(), (ignored + part.selector).distinct())
                                current = null
                                navigator.evaluateJavaScript("window.__cdPick&&window.__cdPick.clear()")
                            },
                            onRemoveOnly = { onPicksChange("", ignored) },
                            onRemoveIgnored = { selector -> onPicksChange(only, ignored - selector) },
                            onDone = { picking = false },
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.88f)) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.Lock, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.inverseOnSurface)
                                Text(stringResource(Res.string.browser_session_note), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.inverseOnSurface)
                            }
                        }
                        BrowseToolbar(
                            canBack = navigator.canGoBack,
                            canForward = navigator.canGoForward,
                            picks = (if (only.isNotBlank()) 1 else 0) + ignored.size,
                            onBack = { navigator.navigateBack() },
                            onForward = { navigator.navigateForward() },
                            onPick = { picking = true },
                            onWatch = onWatch,
                            watchLabel = watchLabel,
                        )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressPill(url: String, editing: Boolean, onEdit: (Boolean) -> Unit, onGo: (String) -> Unit, modifier: Modifier = Modifier) {
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
        Surface(onClick = { onEdit(true) }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = modifier.heightIn(min = 44.dp)) {
            Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (url.startsWith("https://")) Icon(Icons.Rounded.Lock, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(display, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun BrowseToolbar(
    canBack: Boolean,
    canForward: Boolean,
    picks: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onPick: () -> Unit,
    onWatch: () -> Unit,
    watchLabel: String,
) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 6.dp) {
        Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            IconButton(onClick = onBack, enabled = canBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(Res.string.a11y_navigate_back)) }
            IconButton(onClick = onForward, enabled = canForward) { Icon(Icons.AutoMirrored.Rounded.ArrowForward, stringResource(Res.string.browser_forward)) }
            BadgedBox(badge = { if (picks > 0) Badge { Text(picks.toString()) } }) {
                FilledTonalIconButton(onClick = onPick, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Rounded.TouchApp, stringResource(Res.string.picker_title)) }
            }
            Spacer(Modifier.width(6.dp))
            Button(onClick = onWatch, shapes = ButtonDefaults.shapes(), modifier = Modifier.heightIn(min = 48.dp)) {
                Text(watchLabel, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/** uBlock-style picking: tap a part, widen it with the slider, then watch only it or ignore it. */
@Composable
private fun PickerPanel(
    current: PickedPart?,
    only: String,
    ignored: List<String>,
    onDepth: (Int) -> Unit,
    onWatchOnly: (PickedPart) -> Unit,
    onIgnore: (PickedPart) -> Unit,
    onRemoveOnly: () -> Unit,
    onRemoveIgnored: (String) -> Unit,
    onDone: () -> Unit,
) {
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 8.dp, modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.picker_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        current?.text?.takeIf { it.isNotBlank() }?.let { "“$it”" } ?: stringResource(Res.string.picker_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FilledTonalButton(onClick = onDone, shapes = ButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.done))
                }
            }
            AnimatedVisibility(current != null) {
                val part = current ?: return@AnimatedVisibility
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (part.maxDepth > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(Res.string.picker_smaller), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Slider(
                                value = part.depth.toFloat(),
                                onValueChange = { onDepth(it.toInt()) },
                                valueRange = 0f..part.maxDepth.toFloat(),
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
  var base = null, depth = 0, on = false;
  function stop(e){ return !e || !e.parentElement || e.parentElement === document.body || e.parentElement === document.documentElement; }
  function target(){ var e = base; for (var i = 0; i < depth && !stop(e); i++) e = e.parentElement; return e; }
  function maxDepth(){ var n = 0, e = base; while (!stop(e) && n < 12) { n++; e = e.parentElement; } return n; }
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
    base = document.elementFromPoint(ev.clientX, ev.clientY); depth = 0; show(); report();
  }, true);
  window.__cdPick = {
    enable: function(){ on = true; return 0; },
    disable: function(){ on = false; base = null; box.style.display = 'none'; return 0; },
    clear: function(){ base = null; box.style.display = 'none'; return 0; },
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
