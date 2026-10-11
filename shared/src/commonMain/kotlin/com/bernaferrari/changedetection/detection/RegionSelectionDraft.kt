/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.detection

/** A bounded, reversible draft. Only explicit confirmation turns this into a capture rule. */
internal data class RegionSelectionDraft(
    val selected: String? = null,
    val areas: List<String> = emptyList(),
    val ignored: List<String> = emptyList(),
    val activeIgnored: String? = null,
    private val initialRule: TextRegion? = null,
    private val preserveInitialRule: Boolean = true,
    private val history: List<RegionSelectionSnapshot> = emptyList(),
    private val preferredChildren: Map<String, String> = emptyMap(),
) {
    val canUndo: Boolean get() = history.isNotEmpty()
    val watched: List<String> get() = (areas + listOfNotNull(selected)).distinct()
    private val currentRule: TextRegion get() = initialRule?.takeIf { preserveInitialRule } ?: TextRegion(watched.joinToString(", "), ignored.joinToString(", "))
    val exceedsRuleLimit: Boolean get() = currentRule.include.length > 512 || currentRule.exclude.length > 512
    val rule: TextRegion? get() = currentRule.takeIf { watched.isNotEmpty() && !exceedsRuleLimit }

    companion object {
        fun fromRule(rule: TextRegion): RegionSelectionDraft {
            if (rule.isEmpty) return RegionSelectionDraft()
            return RegionSelectionDraft(
                areas = if (rule.include.isBlank()) listOf("body") else selectorGroups(rule.include),
                ignored = selectorGroups(rule.exclude),
                initialRule = rule,
            )
        }
    }

    private fun checkpoint() = (history + RegionSelectionSnapshot(selected, areas, ignored, activeIgnored, preserveInitialRule)).takeLast(100)

    fun restoreIgnored(selector: String): RegionSelectionDraft = if (selector !in ignored) this else copy(ignored = ignored - selector, activeIgnored = activeIgnored.takeUnless { it == selector }, preserveInitialRule = false, history = checkpoint())

    fun toggleIgnored(selector: String, document: SelectableTextDocument): RegionSelectionDraft {
        if (document.regions.none { it.selector == selector }) return this
        return if (selector in ignored) restoreIgnored(selector) else copy(ignored = ignored + selector, activeIgnored = selector, preserveInitialRule = false, history = checkpoint())
    }

    fun addArea(): RegionSelectionDraft = if (selected == null || exceedsRuleLimit) {
        this
    } else {
        copy(selected = null, areas = watched, preserveInitialRule = false, history = checkpoint())
    }

    fun removeArea(selector: String): RegionSelectionDraft = if (selector !in areas) {
        this
    } else {
        copy(selected = selected.takeUnless { it == selector }, areas = areas - selector, preserveInitialRule = false, history = checkpoint())
    }

    fun toggle(selector: String, document: SelectableTextDocument): RegionSelectionDraft = when {
        selector in areas -> removeArea(selector)
        selector == selected -> copy(selected = null, preserveInitialRule = false, history = checkpoint())
        else -> if (document.regions.any { it.selector == selector }) addArea().choose(selector, document) else this
    }

    fun choose(selector: String, document: SelectableTextDocument): RegionSelectionDraft {
        if (selector == selected || document.regions.none { it.selector == selector }) return this
        return copy(selected = selector, areas = areas - selector, preserveInitialRule = false, history = checkpoint())
    }

    private fun active(ignore: Boolean): String? = if (ignore) activeIgnored else selected
    private fun preferenceKey(selector: String?, ignore: Boolean) = if (ignore) "ignore:$selector" else selector

    fun largerTarget(document: SelectableTextDocument, ignore: Boolean = false): String? = document.regions.find { it.selector == active(ignore) }?.parentSelector?.takeUnless { ignore && it in ignored }

    fun smallerTarget(document: SelectableTextDocument, ignore: Boolean = false): String? {
        val current = active(ignore)
        val children = document.regions.filter { it.parentSelector == current && current != null }
        return (children.find { it.selector == preferredChildren[preferenceKey(current, ignore)] }?.selector ?: children.firstOrNull()?.selector)?.takeUnless { ignore && it in ignored }
    }

    private fun changeScope(target: String, document: SelectableTextDocument, ignore: Boolean): RegionSelectionDraft = if (ignore) {
        copy(ignored = ignored.map { if (it == activeIgnored) target else it }.distinct(), activeIgnored = target, preserveInitialRule = false, history = checkpoint())
    } else {
        choose(target, document)
    }

    fun larger(document: SelectableTextDocument, ignore: Boolean = false): RegionSelectionDraft {
        val target = largerTarget(document, ignore) ?: return this
        return changeScope(target, document, ignore).copy(preferredChildren = preferredChildren + (requireNotNull(preferenceKey(target, ignore)) to requireNotNull(active(ignore))))
    }

    fun smaller(document: SelectableTextDocument, ignore: Boolean = false): RegionSelectionDraft = smallerTarget(document, ignore)?.let { changeScope(it, document, ignore) } ?: this

    fun undo(): RegionSelectionDraft = if (history.isEmpty()) this else copy(selected = history.last().selected, areas = history.last().areas, ignored = history.last().ignored, activeIgnored = history.last().activeIgnored, preserveInitialRule = history.last().preserveInitialRule, history = history.dropLast(1))
}

internal data class RegionSelectionSnapshot(val selected: String?, val areas: List<String>, val ignored: List<String>, val activeIgnored: String?, val preserveInitialRule: Boolean)

/** Split only top-level selector groups; commas in attributes, pseudos and escapes stay intact. */
internal fun selectorGroups(selector: String): List<String> {
    val groups = mutableListOf<String>()
    var start = 0
    var depth = 0
    var quote: Char? = null
    var escaped = false
    selector.forEachIndexed { index, character ->
        when {
            escaped -> escaped = false

            character == '\\' -> escaped = true

            quote != null -> if (character == quote) quote = null

            character == '\'' || character == '"' -> quote = character

            character == '(' || character == '[' -> depth++

            character == ')' || character == ']' -> depth--

            character == ',' && depth == 0 -> {
                groups += selector.substring(start, index).trim()
                start = index + 1
            }
        }
    }
    groups += selector.substring(start).trim()
    return groups.filter { it.isNotBlank() }.distinct()
}
