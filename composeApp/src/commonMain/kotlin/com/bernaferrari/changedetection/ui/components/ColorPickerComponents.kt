/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.ColorGroup

/**
 * A calm, touch-sized swatch picker. Selection is communicated by both a border and check mark;
 * colour is never the only indicator of state.
 */
@Composable
fun ColorPickerList(
    currentColors: ColorGroup,
    colorGroups: List<ColorGroup>,
    onColorSelected: (ColorGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(colorGroups) { index, colorGroup ->
            ColorPickerButton(
                colorGroup = colorGroup,
                isSelected = currentColors == colorGroup,
                onClick = { onColorSelected(colorGroup) },
                modifier = Modifier.semantics {
                    contentDescription = "Color theme ${index + 1}"
                },
            )
        }
    }
}

@Composable
fun ColorPickerButton(
    colorGroup: ColorGroup,
    isSelected: Boolean,
    onClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    allowReselection: Boolean = false,
) {
    val selectionColor = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier
            .size(48.dp)
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = {
                    if (!isSelected || allowReselection) {
                        onClick(!isSelected)
                    }
                },
            ),
        shape = CircleShape,
        border = if (isSelected) BorderStroke(2.dp, selectionColor) else null,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Box(
            modifier = Modifier
                .padding(4.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(colorGroup.first)),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
