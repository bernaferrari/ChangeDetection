package com.bernaferrari.changedetection.error

import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import kotlinx.io.IOException
import kotlinx.datetime.Clock

/**
 * Centralized error handling system for the Change Detection app
 * Provides user-friendly error messages and handles different error types appropriately
 */
class ErrorHandler {

    private val _errorEvents = MutableSharedFlow<ErrorEvent>()
    val errorEvents: SharedFlow<ErrorEvent> = _errorEvents.asSharedFlow()

    /**
     * Handle errors and convert them to user-friendly messages
     */
    fun handleError(
        error: Throwable,
        context: ErrorContext = ErrorContext.GENERAL,
        userAction: String? = null
    ): ErrorResult {

        Napier.e("Error in ${context.name}: ${error.message}")

        val errorResult = when (error) {
            is ClientRequestException -> handleNetworkError(error.response.status.value, errorContext = context)
            is ServerResponseException -> handleNetworkError(error.response.status.value, errorContext = context)
            is NetworkError -> handleNetworkError(error.code, context)
            is ValidationError -> handleValidationError(error, context)
            is DatabaseError -> handleDatabaseError(error, context)
            is SyncError -> handleSyncError(error, context)
            is AuthenticationError -> handleAuthenticationError(error, context)
            is IOException -> handleNetworkConnectivity(error, context) // Includes UnknownHostException
            is ConnectTimeoutException, is SocketTimeoutException -> handleTimeout(error, context)
            // SecurityException and OutOfMemoryError are JVM-only, handled by generic catch
            else -> handleGenericError(error, context)
        }

        // Log the error event for monitoring
        val errorEvent = ErrorEvent(
            type = errorResult.type,
            message = errorResult.userMessage,
            context = context,
            userAction = userAction,
            timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds(),
            stackTrace = error.stackTraceToString()
        )

        // Emit error event for UI handling
        try {
            _errorEvents.tryEmit(errorEvent)
        } catch (e: Exception) {
            Napier.e("Failed to emit error event")
        }

        return errorResult
    }

    private fun handleNetworkError(code: Int, errorContext: ErrorContext): ErrorResult {
        val message = when (code) {
            404 -> "Page not found"
            403 -> "Access forbidden"
            500, 502, 503 -> "Server error"
            else -> "Network error ($code)"
        }

        return ErrorResult(
            type = ErrorType.NETWORK,
            userMessage = message,
            isRetryable = code in listOf(408, 429, 500, 502, 503, 504),
            suggestedActions = if (code == 404) {
                listOf("Check URL", "Verify page exists")
            } else {
                listOf("Try again later")
            }
        )
    }

    private fun handleValidationError(error: ValidationError, errorContext: ErrorContext): ErrorResult {
        val message = when (error.field) {
            "url" -> "Invalid URL"
            "email" -> "Invalid email"
            "css_selector" -> "Invalid CSS selector"
            else -> "Validation error: ${error.field}"
        }

        return ErrorResult(
            type = ErrorType.VALIDATION,
            userMessage = message,
            isRetryable = false,
            suggestedActions = listOf("Check input", "See examples")
        )
    }

    private fun handleDatabaseError(error: DatabaseError, errorContext: ErrorContext): ErrorResult {
        return ErrorResult(
            type = ErrorType.DATABASE,
            userMessage = "Database error",
            isRetryable = true,
            suggestedActions = listOf("Restart app", "Clear cache")
        )
    }

    private fun handleSyncError(error: SyncError, errorContext: ErrorContext): ErrorResult {
        val message = when (error.reason) {
            SyncError.Reason.RATE_LIMITED -> "Rate limited"
            SyncError.Reason.QUOTA_EXCEEDED -> "Quota exceeded"
            SyncError.Reason.CONTENT_TOO_LARGE -> "Content too large"
            SyncError.Reason.INVALID_CONTENT -> "Invalid content"
            else -> "Sync error"
        }

        return ErrorResult(
            type = ErrorType.SYNC,
            userMessage = message,
            isRetryable = error.reason != SyncError.Reason.QUOTA_EXCEEDED,
            suggestedActions = when (error.reason) {
                SyncError.Reason.RATE_LIMITED -> listOf("Wait and retry")
                SyncError.Reason.CONTENT_TOO_LARGE -> listOf("Use element selector", "Add content filters")
                else -> listOf("Check settings")
            }
        )
    }

    private fun handleAuthenticationError(error: AuthenticationError, errorContext: ErrorContext): ErrorResult {
        return ErrorResult(
            type = ErrorType.AUTHENTICATION,
            userMessage = "Authentication failed",
            isRetryable = true,
            suggestedActions = listOf("Check credentials", "Refresh cookies")
        )
    }

    private fun handleNetworkConnectivity(error: Throwable, errorContext: ErrorContext): ErrorResult {
        return ErrorResult(
            type = ErrorType.CONNECTIVITY,
            userMessage = "No internet connection",
            isRetryable = true,
            suggestedActions = listOf("Check internet", "Check WiFi")
        )
    }

    private fun handleTimeout(error: Throwable, errorContext: ErrorContext): ErrorResult {
        return ErrorResult(
            type = ErrorType.TIMEOUT,
            userMessage = "Request timeout",
            isRetryable = true,
            suggestedActions = listOf("Check connection speed", "Try again")
        )
    }

    // handlePermissions removed: SecurityException is JVM-only

    // handleMemoryError removed: OutOfMemoryError is JVM-only

    private fun handleGenericError(error: Throwable, errorContext: ErrorContext): ErrorResult {
        return ErrorResult(
            type = ErrorType.UNKNOWN,
            userMessage = "Something went wrong",
            isRetryable = true,
            suggestedActions = listOf("Try again", "Restart app"),
            technicalDetails = error.message
        )
    }
}

// Error types and data classes
enum class ErrorType {
    NETWORK,
    VALIDATION,
    DATABASE,
    SYNC,
    AUTHENTICATION,
    CONNECTIVITY,
    TIMEOUT,
    PERMISSIONS,
    MEMORY,
    UNKNOWN
}

enum class ErrorContext {
    GENERAL,
    SYNC_WORKER,
    TURBO_SYNC,
    ELEMENT_SELECTOR,
    AUTHENTICATION,
    DATABASE_OPERATION,
    CONTENT_PROCESSING,
    USER_INPUT
}

data class ErrorResult(
    val type: ErrorType,
    val userMessage: String,
    val isRetryable: Boolean,
    val suggestedActions: List<String> = emptyList(),
    val technicalDetails: String? = null
)

data class ErrorEvent(
    val type: ErrorType,
    val message: String,
    val context: ErrorContext,
    val userAction: String?,
    val timestamp: Long,
    val stackTrace: String
)

// Custom exception classes
class NetworkError(val code: Int, message: String) : Exception(message)

class ValidationError(val field: String, message: String) : Exception(message)

class DatabaseError(message: String, cause: Throwable? = null) : Exception(message, cause)

class SyncError(val reason: Reason, message: String) : Exception(message) {
    enum class Reason {
        RATE_LIMITED,
        QUOTA_EXCEEDED,
        CONTENT_TOO_LARGE,
        INVALID_CONTENT,
        NETWORK_ERROR,
        UNKNOWN
    }
}

class AuthenticationError(message: String, cause: Throwable? = null) : Exception(message, cause)

// Extension functions for easier error handling
fun Throwable.toErrorResult(errorHandler: ErrorHandler, context: ErrorContext = ErrorContext.GENERAL): ErrorResult {
    return errorHandler.handleError(this, context)
}

fun <T> Result<T>.handleError(
    errorHandler: ErrorHandler,
    context: ErrorContext = ErrorContext.GENERAL
): Result<T> {
    return this.onFailure { error ->
        errorHandler.handleError(error, context)
    }
}
