package com.bernaferrari.changedetection.ui.home

import com.bernaferrari.changedetection.shared.generated.resources.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.preview.EditSegment
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.empty_body
import com.bernaferrari.changedetection.shared.generated.resources.empty_clipboard_label
import com.bernaferrari.changedetection.shared.generated.resources.empty_title
import com.bernaferrari.changedetection.shared.generated.resources.empty_watch
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works
import com.bernaferrari.changedetection.shared.generated.resources.home_watch_page
import com.bernaferrari.changedetection.ui.kit.DiffText
import com.bernaferrari.changedetection.ui.kit.PageAvatar
import com.bernaferrari.changedetection.ui.kit.SerifSnippet
import com.bernaferrari.changedetection.ui.kit.editText
import com.bernaferrari.changedetection.ui.theme.marks
import com.bernaferrari.changedetection.util.readText
import com.bernaferrari.changedetection.localization.stringResource

/** The empty state shows the product instead of describing it, and offers a copied link. */
@Composable
fun HomeEmptyState(onAddPage: (String?) -> Unit, onHowItWorks: () -> Unit, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboard.current
    var copied by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        copied = runCatching { clipboard.readText() }.getOrNull()?.trim()
            ?.takeIf { it.length < 2_000 && (it.startsWith("http://") || it.startsWith("https://")) && ' ' !in it }
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.size(12.dp))
            DemoCards()
            Spacer(Modifier.size(36.dp))
            Text(stringResource(Res.string.empty_title), style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.size(12.dp))
            Text(stringResource(Res.string.empty_body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(28.dp))
            AnimatedVisibility(copied != null, enter = fadeIn() + expandVertically()) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.padding(bottom = 10.dp)) {
                    Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.ContentPaste, null, Modifier.size(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(Res.string.empty_clipboard_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(copied.orEmpty().removePrefix("https://").removePrefix("http://"), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Button(onClick = { onAddPage(copied) }) { Text(stringResource(Res.string.empty_watch)) }
                    }
                }
            }
            Button(
                onClick = { onAddPage(null) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                shapes = ButtonDefaults.shapes(),
            ) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.home_watch_page), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.size(24.dp))
        }
    }
}

/** Two little "change cards" that settle into place, so the first screen already looks alive. */
@Composable
private fun DemoCards() {
    val settle = remember { Animatable(0f) }
    LaunchedEffect(Unit) { settle.animateTo(1f, tween(700)) }
    val marks = MaterialTheme.marks
    Column(Modifier.fillMaxWidth().clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy((-8).dp)) {
        DemoCard(
            Modifier.padding(end = 24.dp).rotate(-2.5f * settle.value).graphicsLayer {
                translationY = (1f - settle.value) * 40f
                alpha = settle.value
            },
            "S",
            "store.example",
        ) {
            DiffText(
                editText(listOf(EditSegment(stringResource(com.bernaferrari.changedetection.shared.generated.resources.Res.string.demo_price) + " ", EditSegment.Kind.Same), EditSegment("\$1,599", EditSegment.Kind.Removed), EditSegment(" ", EditSegment.Kind.Same), EditSegment("\$1,349", EditSegment.Kind.Added)), marks),
                style = SerifSnippet.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
            )
        }
        DemoCard(
            Modifier.align(Alignment.End).padding(start = 40.dp).rotate(2f * settle.value).graphicsLayer {
                translationY = (1f - settle.value) * 80f
                alpha = settle.value
            },
            "F",
            "opencall.example",
        ) {
            DiffText(editText(listOf(EditSegment(stringResource(com.bernaferrari.changedetection.shared.generated.resources.Res.string.demo_applications_close) + " ", EditSegment.Kind.Same), EditSegment(stringResource(com.bernaferrari.changedetection.shared.generated.resources.Res.string.demo_new_date), EditSegment.Kind.Added)), marks))
        }
    }
}

@Composable
private fun DemoCard(modifier: Modifier, letter: String, domain: String, content: @Composable () -> Unit) {
    Surface(
        modifier.shadow(10.dp, MaterialTheme.shapes.large, ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.08f), spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.12f)),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PageAvatar(letter, domain, size = 28.dp)
                Text(domain, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}
