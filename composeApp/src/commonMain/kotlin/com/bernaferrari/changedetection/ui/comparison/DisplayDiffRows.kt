/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

/** Bound each text-layout operation, including single-line minified documents.
 * Statistics and exported patches use the original rows; these are display fragments only.
 */
internal fun displayDiffRows(rows: List<DiffRowModel>, maxCharacters: Int = 2_048): List<DiffRowModel> {
    require(maxCharacters >= 2)
    return buildList {
        rows.forEach { row ->
            if (row.content.length <= maxCharacters && row.content.count { it == '\n' } <= 64) {
                add(row)
            } else {
                var start = 0
                while (start < row.content.length) {
                    var end = minOf(start + maxCharacters, row.content.length)
                    var breaks = 0
                    for (index in start until end) {
                        if (row.content[index] == '\n' && ++breaks == 64) {
                            end = index + 1
                            break
                        }
                    }
                    if (end < row.content.length && row.content[end - 1].isHighSurrogate() && row.content[end].isLowSurrogate()) end--
                    add(row.copy(content = row.content.substring(start, end), isContinuation = start > 0))
                    start = end
                }
            }
        }
    }
}
