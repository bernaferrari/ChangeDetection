/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

/** Display formatting uses native locale rules; persisted dates and numbers remain locale-independent. */
internal expect fun localizedTimestamp(timestamp: Long, includeSeconds: Boolean): String
internal expect fun localizedTime(timestamp: Long, includeSeconds: Boolean): String
internal expect fun localizedNumber(value: Double, maxFractionDigits: Int = 1, minFractionDigits: Int = 0): String

internal expect fun localizedPercent(value: Double, maxFractionDigits: Int = 1, minFractionDigits: Int = 0): String
