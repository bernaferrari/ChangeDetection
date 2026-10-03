/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

import com.bernaferrari.changedetection.repo.Site
import io.ktor.http.Url

/** Human-readable page identity that preserves meaningful path and query text. */
fun String.displayPageAddress(): String = runCatching {
    val parsed = Url(this)
    val host = parsed.host.removePrefix("www.").ifBlank { this }
    val path = parsed.encodedPath.takeUnless { it.isBlank() || it == "/" }.orEmpty()
    val query = parsed.encodedQuery.takeIf { it.isNotBlank() }?.let { "?$it" }.orEmpty()
    "$host$path$query"
}.getOrElse {
    removePrefix("https://").removePrefix("http://").removePrefix("www.").ifBlank { this }
}

fun Site.displayPageTitle(): String = title?.takeIf { it.isNotBlank() } ?: url.displayPageAddress()
