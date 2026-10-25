package com.bernaferrari.changedetection.error

/** A platform execution refusal is distinct from a page/network failure. */
class BackgroundExecutionUnavailable(cause: Throwable? = null) : Exception("Android did not allow the background check to start", cause)
