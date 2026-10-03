/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.detection

import com.fleeksoft.ksoup.Ksoup

/** Only native-generated candidate indices enter executable browser code. */
internal data class RegionHighlightUpdate(val watched: List<Int>, val ignored: List<Int>, val selected: Int?, val bodyWatched: Boolean) {
    init {
        require((watched + ignored + listOfNotNull(selected)).all { it >= 0 })
    }

    fun script(): String = """
        (() => {
          const watched = new Set([${watched.joinToString(",")}]);
          const ignored = new Set([${ignored.joinToString(",")}]);
          const selected = ${selected ?: -1};
          document.body.toggleAttribute('data-cd-watched', $bodyWatched);
          document.querySelectorAll('[data-cd-region]').forEach(node => {
            const index = Number(node.getAttribute('data-cd-region'));
            node.toggleAttribute('data-cd-watched', watched.has(index));
            node.toggleAttribute('data-cd-ignored', ignored.has(index));
            node.toggleAttribute('data-cd-selected', index === selected);
            node.setAttribute('aria-pressed', watched.has(index) || index === selected ? 'true' : 'false');
          });
        })();
    """.trimIndent()
}

internal fun regionHighlightUpdate(document: SelectableTextDocument, selected: String?): RegionHighlightUpdate {
    val parsed = Ksoup.parse(requireNotNull(document.selectionHtml))
    fun indices(attribute: String) = parsed.select("[data-cd-region][$attribute]").mapNotNull {
        it.attr("data-cd-region").toIntOrNull()?.takeIf { index -> index in document.regions.indices }
    }
    return RegionHighlightUpdate(indices("data-cd-watched"), indices("data-cd-ignored"), document.regions.indexOfFirst { it.selector == selected }.takeIf { it >= 0 }, parsed.body().hasAttr("data-cd-watched"))
}
