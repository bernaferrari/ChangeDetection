package com.bernaferrari.changedetection.ui.kit

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.bernaferrari.changedetection.ui.theme.marks

/** Every case-insensitive occurrence of [query] in [text], as ranges. Blank queries match nothing. */
internal fun matchRanges(text: String, query: String): List<IntRange> {
    val needle = query.trim()
    if (needle.isEmpty()) return emptyList()
    return buildList {
        var from = 0
        while (true) {
            val at = text.indexOf(needle, from, ignoreCase = true)
            if (at < 0) break
            add(at until at + needle.length)
            from = at + needle.length
        }
    }
}

/** True when a search [query] finds something in [text]. */
internal fun matchesQuery(text: String, query: String): Boolean = matchRanges(text, query).isNotEmpty()

/**
 * [text] with every match of a search [query] marked the way the app marks new words in a change, so
 * results show why they matched. Without a query the text is returned as is.
 */
@Composable
fun highlightMatches(text: String, query: String): AnnotatedString {
    val marks = MaterialTheme.marks
    return remember(text, query, marks) {
        val ranges = matchRanges(text, query)
        if (ranges.isEmpty()) {
            AnnotatedString(text)
        } else {
            buildAnnotatedString {
                var cursor = 0
                ranges.forEach { range ->
                    append(text.substring(cursor, range.first))
                    withStyle(SpanStyle(background = marks.highlight, color = marks.onHighlight)) { append(text.substring(range)) }
                    cursor = range.last + 1
                }
                append(text.substring(cursor))
            }
        }
    }
}
