/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */

package com.bernaferrari.changedetection.di

import com.bernaferrari.changedetection.domain.repository.SitesRepository
import com.bernaferrari.changedetection.domain.repository.SnapsRepository
import com.bernaferrari.changedetection.ui.viewmodel.MainViewModel

/**
 * Factory for creating ViewModels with their dependencies.
 *
 * This factory approach is platform-independent and can be used with any DI framework
 * or directly in platform-specific code.
 *
 * Usage:
 * ```kotlin
 * // In platform-specific code (Android, iOS, Desktop)
 * val snapsRepository: SnapsRepository = // get from platform DI
 * val sitesRepository: SitesRepository = // get from platform DI
 *
 * val mainViewModel = viewModelFactory.createMainViewModel(
 *     snapsRepository,
 *     sitesRepository
 * )
 * ```
 */
object ViewModelFactory {
    /**
     * Create a MainViewModel instance
     *
     * @param snapsRepository The snapshots repository
     * @param sitesRepository The sites repository
     * @return A configured MainViewModel instance
     */
    fun createMainViewModel(
        snapsRepository: SnapsRepository,
        sitesRepository: SitesRepository
    ): MainViewModel {
        return MainViewModel(
            snapsRepository = snapsRepository,
            sitesRepository = sitesRepository
        )
    }
}

/**
 * Extension function for easier ViewModel creation
 *
 * Usage:
 * ```kotlin
 * val viewModel = createMainViewModel(snapsRepo, sitesRepo)
 * ```
 */
fun createMainViewModel(
    snapsRepository: SnapsRepository,
    sitesRepository: SitesRepository
): MainViewModel = ViewModelFactory.createMainViewModel(snapsRepository, sitesRepository)
