/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.staticCompositionLocalOf

/** Wallpaper-derived scheme is also available while a fixed palette is selected, for its preview. */
internal val LocalWallpaperColorScheme = staticCompositionLocalOf<ColorScheme?> { null }

expect fun supportsDynamicColors(): Boolean
