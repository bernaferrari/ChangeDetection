package com.bernaferrari.changedetection.ui.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.ColorGroup
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

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
    allowReselection: Boolean = false,
    onClick: (Boolean) -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    // Shape morphing animation - circle to rounded square
    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cornerRadius"
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

    // Border animation progress - mimics the original View's progress animation
    val borderProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "borderProgress"
    )

    // Calculate border properties with smooth interpolation
    val borderWidth = (2.dp * borderProgress).coerceAtLeast(0.dp)
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "borderColor"
    )

    // Rotation animation for selected state
    val rotation by animateFloatAsState(
        targetValue = if (isSelected) 0f else 45f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotation"
    )

    val density = LocalDensity.current
    val sizePx = remember(density) { // Remember to avoid re-calculation unless density changes
        with(density) { 36.dp.toPx() }
    }

    // Counter-rotate gradient to keep it visually stable.
    // Base gradient is from (0,0) to (sizePx, sizePx) (top-left to bottom-right).
    // We rotate both points by -rotation degrees around the origin.
    val angleRadians = Math.toRadians(-rotation.toDouble())
    val cosA = cos(angleRadians).toFloat()
    val sinA = sin(angleRadians).toFloat()

    // Rotate start point (0, 0) - this stays at origin
    val startX = 0f * cosA - 0f * sinA
    val startY = 0f * sinA + 0f * cosA

    // Rotate end point (sizePx, sizePx)
    val endX = sizePx * cosA - sizePx * sinA
    val endY = sizePx * sinA + sizePx * cosA

    val gradientStart = Offset(startX, startY)
    val gradientEnd = Offset(endX, endY)

    Box(
        modifier = modifier
            .size(36.dp)
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
                    ),
                    start = gradientStart,
                    end = gradientEnd
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(borderWidth)
            .clip(RoundedCornerShape((cornerRadius - borderWidth).coerceAtLeast(0.dp)))
            .background(
                color = borderColor,
                shape = RoundedCornerShape((cornerRadius - borderWidth).coerceAtLeast(0.dp))
            )
            .padding(2.dp)
            .clip(RoundedCornerShape((cornerRadius - borderWidth - 2.dp).coerceAtLeast(0.dp)))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(colorGroup.first),
                        Color(colorGroup.second)
                    ),
                    start = gradientStart,
                    end = gradientEnd
                ),
                shape = RoundedCornerShape((cornerRadius - borderWidth - 2.dp).coerceAtLeast(0.dp))
            )
            .clickable {
                if (!isSelected || allowReselection) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
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
