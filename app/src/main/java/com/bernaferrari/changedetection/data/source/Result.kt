@file:Suppress("unused")

package com.bernaferrari.changedetection.data.source

sealed class Result<out T : Any> {

    class Success<out T : Any>(val data: T) : Result<T>()

    class Error : Result<Nothing>()
}
