/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

internal data class TextChangeSection(val context: String?, val before: List<DiffRowModel>, val after: List<DiffRowModel>)

/** Keep separate edits separate; never pair removed lines across unchanged source sections. */
internal fun textChangeSections(rows: List<DiffRowModel>, headingLines: Set<String> = emptySet()): List<TextChangeSection> = buildList {
    var context: String? = null
    var sectionContext: String? = null
    var before = mutableListOf<DiffRowModel>()
    var after = mutableListOf<DiffRowModel>()
    fun flush() {
        if (before.isNotEmpty() || after.isNotEmpty()) {
            add(TextChangeSection(sectionContext, before, after))
            before = mutableListOf()
            after = mutableListOf()
        }
    }
    for (row in rows) {
        if (row.type == DiffLineType.UNCHANGED) {
            flush()
            if (row.content.isNotBlank() && (headingLines.isEmpty() || row.content in headingLines)) context = row.content.take(160)
        } else {
            if (before.isEmpty() && after.isEmpty()) sectionContext = context
            if (row.type == DiffLineType.REMOVED) before.add(row) else after.add(row)
        }
    }
    flush()
}
