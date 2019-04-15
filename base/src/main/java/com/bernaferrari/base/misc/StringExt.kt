package com.bernaferrari.base.misc

import android.net.Uri
import java.text.Normalizer

/** An extension wrapper to [Uri.parse]. */
fun String.toUri() = Uri.parse(this)!!

/**
 * Convert (most) special characters into standard ones, useful for search.
 * éíúã becomes eiua
 * it doesn't remove all special characters from all languages, but already helps a lot.
 */
fun String.normalizeString() =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .toLowerCase()
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")