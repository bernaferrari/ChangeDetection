package com.bernaferrari.changedetection.ui.theme

import android.os.Build

actual fun supportsDynamicColors(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
