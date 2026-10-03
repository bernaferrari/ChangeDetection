/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.ui.components.AppLoadingIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The same bounded text layouts serve baseline snapshots and native rendered previews. */
@Composable
internal fun SnapshotTextContent(content: String, modifier: Modifier = Modifier, monospace: Boolean = false) {
    val fragments by produceState<List<DiffRowModel>?>(null, content) {
        value = null
        value = withContext(Dispatchers.Default) {
            displayDiffRows(listOf(DiffRowModel(1, content, DiffLineType.UNCHANGED)))
        }
    }
    val ready = fragments
    if (ready == null) {
        Box(modifier, contentAlignment = Alignment.Center) { AppLoadingIndicator() }
    } else {
        LazyColumn(modifier.testTag("snapshot-text"), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)) {
            items(ready.size, key = { "snapshot-fragment-$it" }) { index ->
                SelectionContainer {
                    Text(
                        ready[index].content,
                        style = if (monospace) MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace) else MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
