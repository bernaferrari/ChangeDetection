/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.theme

import android.os.Build

actual fun supportsDynamicColors(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
