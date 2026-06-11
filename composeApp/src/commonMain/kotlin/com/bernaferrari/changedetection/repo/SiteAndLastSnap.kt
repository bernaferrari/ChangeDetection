/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo

data class SiteAndLastSnap(
    val site: Site,
    val snap: Snap?,
    val isSyncing: Boolean = false,
)
