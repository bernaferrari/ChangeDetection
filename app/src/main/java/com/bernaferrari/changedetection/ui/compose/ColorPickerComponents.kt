package com.bernaferrari.changedetection.ui.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.ColorGroup
import kotlinx.coroutines.launch

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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(gradientList) { index, colorGroup ->
            ColorPickerButton(
                colorGroup = colorGroup,
                isSelected = currentColors == colorGroup,
                isFirst = index == 0,
                animationDelay = index * 80,
                onClick = { _ ->
                    onColorSelected(colorGroup)
                }
            )
        }
    }
}

@Composable
fun ColorPickerButton(
    modifier: Modifier = Modifier,
    colorGroup: ColorGroup,
    isSelected: Boolean,
    isFirst: Boolean = false,
    allowReselection: Boolean = false,
    animationDelay: Int = 0,
    onClick: (Boolean) -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Shape morphing animation - circle to rounded square
    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cornerRadius"
    )

    // Enhanced scale animation with bounce
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.9f
            isSelected -> 1f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    // Rotation animation for selected state
    val rotation by animateFloatAsState(
        targetValue = if (isSelected) 90f else 45f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotation"
    )

    // Inner border width animation
    val innerBorderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "innerBorder"
    )

    // Entrance animation with better staggering
    val entranceScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "entrance"
    )

    // Elevation animation
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 6.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "elevation"
    )

    // Border color with subtle glow effect
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color.White.copy(alpha = 0.9f) else Color.Transparent,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "borderColor"
    )

    Box(
        modifier = modifier
            .size(36.dp)
            .scale(scale * entranceScale)
            .rotate(rotation)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(cornerRadius),
                clip = false
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(colorGroup.first),
                        Color(colorGroup.second)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(innerBorderWidth)
            .clip(RoundedCornerShape(cornerRadius - innerBorderWidth))
            .background(
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius - innerBorderWidth)
            )
            .padding(if (isSelected) 2.dp else 0.dp)
            .clip(RoundedCornerShape(cornerRadius - innerBorderWidth - if (isSelected) 1.dp else 0.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(colorGroup.first),
                        Color(colorGroup.second)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius - innerBorderWidth - if (isSelected) 1.dp else 0.dp)
            )
            .clickable {
                if (!isSelected || allowReselection) {
                    isPressed = true
                    onClick(!isSelected)
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(150)
                        isPressed = false
                    }
                }
            }
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
