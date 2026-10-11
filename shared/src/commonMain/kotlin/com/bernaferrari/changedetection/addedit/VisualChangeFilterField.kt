/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.kit.ConnectedChoice
import org.jetbrains.compose.resources.stringResource

/** How much of a screenshot must change before a new version is saved, as one row of choices. */
@Composable
internal fun VisualChangeFilterField(basisPoints: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    val options = listOf(0, 10, 100, 500).let { if (basisPoints in 1..10_000 && basisPoints !in it) (it + basisPoints).sorted() else it }
    val off = stringResource(Res.string.ignore_small_off)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.ignore_small_title), style = MaterialTheme.typography.titleSmall)
        ConnectedChoice(options, basisPoints.takeIf { it in options } ?: 0, onChange, Modifier, enabled = enabled) { value ->
            if (value == 0) off else percent(value)
        }
        Text(
            stringResource(if (basisPoints == 0) Res.string.ignore_small_off_body else Res.string.ignore_small_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Basis points as a short percentage: 10 → "0.1%", 100 → "1%". */
private fun percent(basisPoints: Int): String = com.bernaferrari.changedetection.util.localizedPercent(basisPoints / 100.0, maxFractionDigits = 2)
