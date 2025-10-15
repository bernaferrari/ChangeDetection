package com.bernaferrari.changedetection.error

import android.content.Context
import com.bernaferrari.changedetection.R
import com.orhanobut.logger.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error handling system for the Change Detection app
 * Provides user-friendly error messages and handles different error types appropriately
 */
@Singleton
class ErrorHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
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
        
        Logger.e("Error in ${context.name}: ${error.message}", error)
        
        val errorResult = when (error) {
            is NetworkError -> handleNetworkError(error, context)
            is ValidationError -> handleValidationError(error, context)
            is DatabaseError -> handleDatabaseError(error, context)
            is SyncError -> handleSyncError(error, context)
            is AuthenticationError -> handleAuthenticationError(error, context)
            is UnknownHostException -> handleNetworkConnectivity(error, context)
            is SocketTimeoutException -> handleTimeout(error, context)
            is SecurityException -> handlePermissions(error, context)
            is OutOfMemoryError -> handleMemoryError(error, context)
            else -> handleGenericError(error, context)
        }
        
        // Log the error event for monitoring
        val errorEvent = ErrorEvent(
            type = errorResult.type,
            message = errorResult.userMessage,
            context = context,
            userAction = userAction,
            timestamp = System.currentTimeMillis(),
            stackTrace = error.stackTraceToString()
        )
        
        // Emit error event for UI handling
        try {
            _errorEvents.tryEmit(errorEvent)
        } catch (e: Exception) {
            Logger.e("Failed to emit error event", e)
        }
        
        return errorResult
    }
    
    private fun handleNetworkError(error: NetworkError, errorContext: ErrorContext): ErrorResult {
        val message = when (error.code) {
            404 -> context.getString(R.string.error_page_not_found)
            403 -> context.getString(R.string.error_access_forbidden)
            500, 502, 503 -> context.getString(R.string.error_server_error)
            else -> context.getString(R.string.error_network_general, error.code)
        }
        
        return ErrorResult(
            type = ErrorType.NETWORK,
            userMessage = message,
            isRetryable = error.code in listOf(408, 429, 500, 502, 503, 504),
            suggestedActions = if (error.code == 404) {
                listOf(
                    context.getString(R.string.action_check_url),
                    context.getString(R.string.action_verify_page_exists)
                )
            } else {
                listOf(context.getString(R.string.action_try_again_later))
            }
        )
    }
    
    private fun handleValidationError(error: ValidationError, errorContext: ErrorContext): ErrorResult {
        val message = when (error.field) {
            "url" -> context.getString(R.string.error_invalid_url)
            "email" -> context.getString(R.string.error_invalid_email)
            "css_selector" -> context.getString(R.string.error_invalid_css_selector)
            else -> context.getString(R.string.error_validation_general, error.field)
        }
        
        return ErrorResult(
            type = ErrorType.VALIDATION,
            userMessage = message,
            isRetryable = false,
            suggestedActions = listOf(
                context.getString(R.string.action_check_input),
                context.getString(R.string.action_see_examples)
            )
        )
    }
    
    private fun handleDatabaseError(error: DatabaseError, errorContext: ErrorContext): ErrorResult {
        return ErrorResult(
            type = ErrorType.DATABASE,
            userMessage = context.getString(R.string.error_database_general),
            isRetryable = true,
            suggestedActions = listOf(
                context.getString(R.string.action_restart_app),
                context.getString(R.string.action_clear_cache)
            )
        )
    }
    
    private fun handleSyncError(error: SyncError, errorContext: ErrorContext): ErrorResult {
        val message = when (error.reason) {
            SyncError.Reason.RATE_LIMITED -> context.getString(R.string.error_rate_limited)
            SyncError.Reason.QUOTA_EXCEEDED -> context.getString(R.string.error_quota_exceeded)
            SyncError.Reason.CONTENT_TOO_LARGE -> context.getString(R.string.error_content_too_large)
            SyncError.Reason.INVALID_CONTENT -> context.getString(R.string.error_invalid_content)
            else -> context.getString(R.string.error_sync_general)
        }
        
        return ErrorResult(
            type = ErrorType.SYNC,
            userMessage = message,
            isRetryable = error.reason != SyncError.Reason.QUOTA_EXCEEDED,
            suggestedActions = when (error.reason) {
                SyncError.Reason.RATE_LIMITED -> listOf(
                    context.getString(R.string.action_wait_and_retry)
                )
                SyncError.Reason.CONTENT_TOO_LARGE -> listOf(
                    context.getString(R.string.action_use_element_selector),
                    context.getString(R.string.action_add_content_filters)
                )
                else -> listOf(context.getString(R.string.action_check_settings))
            }
        )
    }
    
    private fun handleAuthenticationError(error: AuthenticationError, errorContext: ErrorContext): ErrorResult {
        return ErrorResult(
            type = ErrorType.AUTHENTICATION,
            userMessage = context.getString(R.string.error_authentication_failed),
            isRetryable = true,
            suggestedActions = listOf(
                context.getString(R.string.action_check_credentials),
                context.getString(R.string.action_refresh_cookies)
            )
        )
    }
    
    private fun handleNetworkConnectivity(error: UnknownHostException, errorContext: ErrorContext): ErrorResult {
        return ErrorResult(
            type = ErrorType.CONNECTIVITY,
            userMessage = context.getString(R.string.error_no_internet),
            isRetryable = true,
            suggestedActions = listOf(
                context.getString(R.string.action_check_internet),
                context.getString(R.string.action_check_wifi)
            )
        )
    }
    
    private fun handleTimeout(error: SocketTimeoutException, errorContext: ErrorContext): ErrorResult {
        return ErrorResult(
            type = ErrorType.TIMEOUT,
            userMessage = context.getString(R.string.error_request_timeout),
            isRetryable = true,
            suggestedActions = listOf(
                context.getString(R.string.action_check_connection_speed),
                context.getString(R.string.action_try_again)
            )
        )
    }
    
    private fun handlePermissions(error: SecurityException, errorContext: ErrorContext): ErrorResult {
        return ErrorResult(
            type = ErrorType.PERMISSIONS,
            userMessage = context.getString(R.string.error_permissions_denied),
            isRetryable = false,
            suggestedActions = listOf(
                context.getString(R.string.action_check_permissions),
                context.getString(R.string.action_grant_permissions)
            )
        )
    }
    
    private fun handleMemoryError(error: OutOfMemoryError, errorContext: ErrorContext): ErrorResult {
        return ErrorResult(
            type = ErrorType.MEMORY,
            userMessage = context.getString(R.string.error_out_of_memory),
            isRetryable = true,
            suggestedActions = listOf(
                context.getString(R.string.action_clear_cache),
                context.getString(R.string.action_restart_app),
                context.getString(R.string.action_reduce_monitoring_sites)
            )
        )
    }
    
    private fun handleGenericError(error: Throwable, errorContext: ErrorContext): ErrorResult {
        return ErrorResult(
            type = ErrorType.UNKNOWN,
            userMessage = context.getString(R.string.error_generic),
            isRetryable = true,
            suggestedActions = listOf(
                context.getString(R.string.action_try_again),
                context.getString(R.string.action_restart_app)
            ),
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