package com.bernaferrari.changedetection.extensions

import androidx.compose.runtime.Composable
import com.bernaferrari.changedetection.util.getRelativeTimeLabel

@Composable
fun Long.convertTimestampToDate(): String = getRelativeTimeLabel(this)
