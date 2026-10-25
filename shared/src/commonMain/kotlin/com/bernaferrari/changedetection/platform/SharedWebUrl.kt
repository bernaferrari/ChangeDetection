package com.bernaferrari.changedetection.platform

/** Shared prose is untrusted input; bound scanning before splitting it into tokens. */
fun extractSharedWebUrl(text: String): String? = text.take(16_384).lineSequence()
    .flatMap { it.splitToSequence(' ', '\t') }
    .map { it.trim().trimEnd('.', ',', ')', ']') }
    .firstOrNull { it.length <= 8_192 && (it.startsWith("http://") || it.startsWith("https://")) }
