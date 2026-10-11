/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.preview.ChangePreview
import com.bernaferrari.changedetection.preview.ChangePreviewRepository
import com.bernaferrari.changedetection.preview.EditSegment
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.alert_generic
import com.bernaferrari.changedetection.shared.generated.resources.alert_large
import com.bernaferrari.changedetection.shared.generated.resources.alert_removed
import com.bernaferrari.changedetection.shared.generated.resources.alert_title
import com.bernaferrari.changedetection.shared.generated.resources.alert_visual
import com.bernaferrari.changedetection.shared.generated.resources.alert_was
import com.bernaferrari.changedetection.shared.generated.resources.preview_numbers_only
import com.bernaferrari.changedetection.shared.generated.resources.preview_reordered
import com.bernaferrari.changedetection.util.displayPageTitle
import com.bernaferrari.changedetection.localization.getString

/** Title and body for a change alert, quoting the same bounded preview the app shows. */
suspend fun changeAlertText(site: Site, snapId: String, snaps: SnapsRepository?, previews: ChangePreviewRepository?): Pair<String, String> {
    val title = getString(Res.string.alert_title, site.displayPageTitle())
    val preview = try {
        snaps?.getSnapById(snapId)?.let { previews?.forVersion(it) }
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
    return title to (preview?.let { summary(it) } ?: getString(Res.string.alert_generic))
}

private suspend fun summary(preview: ChangePreview): String? = when (preview) {
    is ChangePreview.Words -> {
        val now = preview.segments.filter { it.kind != EditSegment.Kind.Removed }.joinToString("") { it.text }.trim()
        val was = preview.segments.filter { it.kind == EditSegment.Kind.Removed }.joinToString(" ") { it.text }.trim()
        buildString {
            preview.section?.let { append(it).append(": ") }
            if (preview.leadingEllipsis) append("…")
            append(now.ifEmpty { getString(Res.string.alert_removed) })
            if (preview.trailingEllipsis) append("…")
            if (was.isNotEmpty() && was.length <= 60 && now.isNotEmpty()) append(" ").append(getString(Res.string.alert_was, was))
        }
    }

    is ChangePreview.Crop -> getString(Res.string.alert_visual, com.bernaferrari.changedetection.util.localizedPercent((preview.percent * 10).toInt() / 10.0))

    is ChangePreview.Large -> when {
        preview.reordered -> getString(Res.string.preview_reordered)

        preview.samples.isNotEmpty() && (preview.added > 0 || preview.removed > 0) -> preview.samples.joinToString(" · ") { sample ->
            sample.filter { it.kind != EditSegment.Kind.Removed }.joinToString("") { it.text }.ifBlank { sample.joinToString("") { it.text } }
        }.take(220)

        preview.numbersOnly -> getString(Res.string.preview_numbers_only)

        else -> getString(Res.string.alert_large)
    }

    else -> null
}
