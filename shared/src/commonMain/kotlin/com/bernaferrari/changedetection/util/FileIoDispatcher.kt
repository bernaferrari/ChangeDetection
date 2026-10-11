/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.util

import kotlinx.coroutines.CoroutineDispatcher

/** Blocking file access uses an IO pool on native targets; web storage uses its event loop. */
internal expect val fileIoDispatcher: CoroutineDispatcher
