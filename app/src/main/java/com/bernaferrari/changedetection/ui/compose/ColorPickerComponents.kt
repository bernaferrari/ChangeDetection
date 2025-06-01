package com.bernaferrari.changedetection.ui.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.ColorGroup

@Composable
fun ColorPickerList(
    currentColors: ColorGroup,
    gradientList: List<ColorGroup>,
    onColorSelected: (ColorGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(gradientList) { index, colorGroup ->
            ColorPickerButton(
                colorGroup = colorGroup,
                isSelected = currentColors == colorGroup,
                isFirst = index == 0,
                onClick = { _ ->
                    onColorSelected(colorGroup)
                }
            )
        }
    }
}

@Composable
fun ColorPickerButton(
    colorGroup: ColorGroup,
    isSelected: Boolean,
    isFirst: Boolean = false,
    allowReselection: Boolean = false,
    onClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 0.dp,
        label = "border"
    )
    
    Box(
        modifier = modifier
            .size(32.dp)
            .scale(scale)
            .clip(CircleShape)
            .border(borderWidth, Color.White, CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(colorGroup.first),
                        Color(colorGroup.second)
                    )
                ),
                shape = CircleShape
            )
            .clickable {
                if (!isSelected || allowReselection) {
                    onClick(!isSelected)
                }
            }
    )
}

@Composable
fun EpoxyColorPickerItem(
    colorGroup: ColorGroup,
    switchIsOn: Boolean,
    allowDeselection: Boolean = false,
    onColorSelected: (ColorGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    ColorPickerButton(
        colorGroup = colorGroup,
        isSelected = switchIsOn,
        allowReselection = allowDeselection,
        onClick = { selected ->
            if (selected || allowDeselection) {
                onColorSelected(colorGroup)
            }
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun ColorPickerListPreview() {
    MaterialTheme {
        Surface {
            ColorPickerList(
                currentColors = ColorGroup(0xFF2196F3.toInt(), 0xFF21CBF3.toInt()),
                gradientList = listOf(
                    ColorGroup(0xFF2196F3.toInt(), 0xFF21CBF3.toInt()),
                    ColorGroup(0xFF4CAF50.toInt(), 0xFF8BC34A.toInt()),
                    ColorGroup(0xFFFF5722.toInt(), 0xFFFF9800.toInt()),
                    ColorGroup(0xFF9C27B0.toInt(), 0xFFE91E63.toInt()),
                    ColorGroup(0xFFFFEB3B.toInt(), 0xFFFFC107.toInt())
                ),
                onColorSelected = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ColorPickerButtonSelectedPreview() {
    MaterialTheme {
        Surface {
            ColorPickerButton(
                colorGroup = ColorGroup(0xFF2196F3.toInt(), 0xFF21CBF3.toInt()),
                isSelected = true,
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ColorPickerButtonUnselectedPreview() {
    MaterialTheme {
        Surface {
            ColorPickerButton(
                colorGroup = ColorGroup(0xFF4CAF50.toInt(), 0xFF8BC34A.toInt()),
                isSelected = false,
                onClick = { }
            )
        }
    }
}
