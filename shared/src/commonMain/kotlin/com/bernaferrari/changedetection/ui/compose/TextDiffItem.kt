package com.bernaferrari.changedetection.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
// import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun TextDiffItem(
    title: String,
    index: Int,
    modifier: Modifier = Modifier,
) {
    // Parse the diff format: +TEXTADDED this text was added TEXTADDED
    val tokens = title.split("TEXTADDED|TEXTREMOVED".toRegex())
    val firstChar = title.firstOrNull() ?: ' '

    val (backgroundColor, highlightColor, numberBackgroundColor, textAlign) = when (firstChar) {
        '+' -> {
            val bgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            val numBg = MaterialTheme.colorScheme.primaryContainer
            Quadruple(bgColor, highlightColor, numBg, TextAlign.End)
        }

        '-' -> {
            val bgColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            val highlightColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
            val numBg = MaterialTheme.colorScheme.errorContainer
            Quadruple(bgColor, highlightColor, numBg, TextAlign.Start)
        }

        else -> {
            Quadruple(Color.Transparent, Color.Transparent, Color.Transparent, TextAlign.Center)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor),
        verticalAlignment = Alignment.Top,
    ) {
        // Line number
        Box(
            modifier = Modifier
                .width(48.dp)
                .background(numberBackgroundColor)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = com.bernaferrari.changedetection.util.localizedInteger(index),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = textAlign,
                fontFamily = FontFamily.Monospace,
            )
        }

        // Diff content
        Text(
            text = buildAnnotatedString {
                var currentIndex = 0

                tokens.forEachIndexed { tokenIndex, token ->
                    if (tokenIndex % 2 != 0 && highlightColor != Color.Transparent) {
                        // Highlighted text (added/removed)
                        withStyle(SpanStyle(background = highlightColor)) {
                            append(token)
                        }
                    } else {
                        // Normal text
                        append(token)
                    }
                    currentIndex += token.length
                }
            },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

/*
@Preview(showBackground = true)
@Composable
private fun TextDiffItemAdditionPreview() {
    MaterialTheme {
        Surface {
            TextDiffItem(
                title = "+TEXTADDEDThis is new textTEXTADDED that was added",
                index = 42
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TextDiffItemDeletionPreview() {
    MaterialTheme {
        Surface {
            TextDiffItem(
                title = "-TEXTREMOVEDThis text was removedTEXTREMOVED from here",
                index = 15
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TextDiffItemNormalPreview() {
    MaterialTheme {
        Surface {
            TextDiffItem(
                title = " This is unchanged text content",
                index = 1
            )
        }
    }
}
*/
