/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.extensions

import androidx.compose.runtime.Composable
import com.bernaferrari.changedetection.util.getRelativeTimeLabel

@Composable
fun Long.convertTimestampToDate(): String = getRelativeTimeLabel(this)
