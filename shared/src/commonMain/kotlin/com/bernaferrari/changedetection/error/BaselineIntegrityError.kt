package com.bernaferrari.changedetection.error

/** Fixed, privacy-safe marker for an unavailable stored body, distinct from a database query failure. */
class BaselineIntegrityError(cause: Throwable? = null) : Exception(
    "The saved baseline is missing, damaged or too large to read. Saved history is kept. Check available device storage and send a report if the problem continues.",
    cause,
)
