/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

/**
 * Decodes snapshot bytes with the charset recorded at capture time.
 *
 * Platforms fall back to UTF-8 when the supplied label is unknown or the payload is malformed.
 */
expect fun decodeByteArray(
    bytes: ByteArray,
    charsetName: String,
): String
