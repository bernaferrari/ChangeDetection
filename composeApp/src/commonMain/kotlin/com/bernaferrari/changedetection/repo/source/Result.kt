/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:Suppress("unused")

package com.bernaferrari.changedetection.repo.source

sealed class Result<out T : Any> {
    class Success<out T : Any>(
        val data: T,
    ) : Result<T>()

    class Error : Result<Nothing>()
}

sealed class WebResult<out T : Any> {
    class Success<out T : Any>(
        val data: T,
    ) : WebResult<T>()

    class Error(
        val description: String,
    ) : WebResult<Nothing>()
}
