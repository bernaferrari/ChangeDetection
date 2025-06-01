package com.bernaferrari.changedetection.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bernaferrari.changedetection.R

@Composable
fun TagPickerList(
    tags: List<String>,
    selectedTags: Set<String>,
    onTagToggled: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tags) { tag ->
            TagChip(
                name = tag,
                isSelected = tag in selectedTags,
                onClick = { onTagToggled(tag) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(name) },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = Color.White,
            containerColor = Color.Transparent,
            labelColor = Color(ContextCompat.getColor(context, R.color.unchecked_chip_text))
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = if (isSelected) MaterialTheme.colorScheme.primary 
                         else Color(ContextCompat.getColor(context, R.color.unchecked_chip_text)),
            selectedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun TagPickerListPreview() {
    MaterialTheme {
        Surface {
            TagPickerList(
                tags = listOf("Technology", "News", "Sports", "Entertainment", "Science"),
                selectedTags = setOf("Technology", "Science"),
                onTagToggled = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TagChipSelectedPreview() {
    MaterialTheme {
        Surface {
            TagChip(
                name = "Technology",
                isSelected = true,
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TagChipUnselectedPreview() {
    MaterialTheme {
        Surface {
            TagChip(
                name = "Sports",
                isSelected = false,
                onClick = { }
            )
        }
    }
}
