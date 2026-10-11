/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.comparison_mode_after
import com.bernaferrari.changedetection.shared.generated.resources.comparison_mode_before
import com.bernaferrari.changedetection.shared.generated.resources.comparison_version_switcher
import org.jetbrains.compose.resources.stringResource

/** Full-canvas rendered comparison with a stable before/after split instead of a slider. */
@Composable
internal fun RenderedHtmlComparison(
    oldContent: String,
    newContent: String,
    modifier: Modifier = Modifier,
) {
    val useNativeText by produceState(true, oldContent, newContent) {
        value = withContext(Dispatchers.Default) {
            !oldContent.hasRenderedMarkup() && !newContent.hasRenderedMarkup()
        }
    }
    var showAfter by remember(oldContent, newContent) { mutableStateOf(true) }

    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (useNativeText) {
                Surface(
                    modifier = Modifier.fillMaxSize().heightIn(min = 168.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    SnapshotTextContent(if (showAfter) newContent else oldContent, Modifier.fillMaxSize())
                }
            } else {
                key(showAfter) {
                    SecureSnapshotWebView(
                        html = if (showAfter) newContent else oldContent,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        RenderedVersionSwitcher(
            showAfter = showAfter,
            onShowAfterChange = { showAfter = it },
        )
    }
}

@Composable
private fun RenderedVersionSwitcher(
    showAfter: Boolean,
    onShowAfterChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val switcherLabel = stringResource(Res.string.comparison_version_switcher)
    Surface(
        modifier = modifier
            .widthIn(max = 280.dp)
            .semantics { contentDescription = switcherLabel }
            .selectableGroup(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Row(modifier = Modifier.padding(5.dp)) {
            listOf(false, true).forEachIndexed { index, after ->
                ToggleButton(
                    checked = showAfter == after,
                    onCheckedChange = { checked -> if (checked) onShowAfterChange(after) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shapes = if (index == 0) {
                        ButtonGroupDefaults.connectedLeadingButtonShapes()
                    } else {
                        ButtonGroupDefaults.connectedTrailingButtonShapes()
                    },
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Text(
                        text = stringResource(
                            if (after) Res.string.comparison_mode_after else Res.string.comparison_mode_before,
                        ),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

private val renderedHtmlElementPattern = Regex("<[a-zA-Z][^>]*>")

private fun String.hasRenderedMarkup(): Boolean = renderedHtmlElementPattern.containsMatchIn(this)
