package com.bernaferrari.changedetection.repo

internal expect val supportsVisualMonitoring: Boolean

/** Whether Text checks execute browser DOM rather than fetch source HTML. */
internal expect val supportsRenderedTextMonitoring: Boolean
