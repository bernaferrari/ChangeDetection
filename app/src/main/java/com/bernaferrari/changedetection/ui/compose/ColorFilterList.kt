package com.bernaferrari.changedetection.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.ColorGroup

@Composable
fun ColorFilterList(
    modifier: Modifier = Modifier,
    gradientList: List<ColorGroup>,
    onSelectionChanged: (List<ColorGroup>) -> Unit,
) {
    var selectedColors by remember { mutableStateOf(setOf<ColorGroup>()) }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(gradientList) { index, colorGroup ->
            ColorPickerButton(
                colorGroup = colorGroup,
                isSelected = colorGroup in selectedColors,
                allowReselection = true,
                onClick = { selected ->
                    selectedColors = if (selected) {
                        selectedColors + colorGroup
                    } else {
                        selectedColors - colorGroup
                    }
                    onSelectionChanged(selectedColors.toList())
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ColorFilterListPreview() {
    MaterialTheme {
        Surface {
            ColorFilterList(
                gradientList = listOf(
                    ColorGroup(0xFF2196F3.toInt(), 0xFF21CBF3.toInt()),
                    ColorGroup(0xFF4CAF50.toInt(), 0xFF8BC34A.toInt()),
                    ColorGroup(0xFFFF5722.toInt(), 0xFFFF9800.toInt()),
                    ColorGroup(0xFF9C27B0.toInt(), 0xFFE91E63.toInt())
                ),
                onSelectionChanged = { }
            )
        }
    }
}
