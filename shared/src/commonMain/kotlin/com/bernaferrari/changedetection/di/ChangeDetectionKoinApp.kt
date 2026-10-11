/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module

/** The generated, cross-platform composition root for Change Detection. */
@KoinApplication(modules = [ChangeDetectionModule::class])
object ChangeDetectionKoinApp

@Module(includes = [CoreDiModule::class, PlatformDiModule::class])
@Configuration
@ComponentScan("com.bernaferrari.changedetection")
class ChangeDetectionModule
