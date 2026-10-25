package com.bernaferrari.changedetection.error

internal class IosCaptureTlsFailure : Exception("The page could not be loaded with valid TLS")
internal actual fun isPlatformTlsFailure(error: Throwable): Boolean = error is IosCaptureTlsFailure
