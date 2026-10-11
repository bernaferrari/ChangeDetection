/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

/** A route owns its ViewModels until it is removed from the back stack. */
@Composable
internal fun <T : Any> rememberAppEntryDecorators() = listOf(
    rememberSaveableStateHolderNavEntryDecorator<T>(),
    rememberViewModelStoreNavEntryDecorator<T>(),
)
