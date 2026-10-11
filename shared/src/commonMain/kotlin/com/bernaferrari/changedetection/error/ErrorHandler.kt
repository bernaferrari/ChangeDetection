/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.error

import io.github.aakira.napier.Napier
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.io.IOException
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi

/**
 * Centralized error handling system for the Change Detection app.
 *
 * Provides:
 * - User-friendly error messages
 * - Recovery actions
 * - Severity classification
 * - Error event streaming for UI
 */
@OptIn(ExperimentalUuidApi::class)
class ErrorHandler {
    private val _errorEvents = MutableSharedFlow<ErrorEvent>(extraBufferCapacity = 16)
    val errorEvents: SharedFlow<ErrorEvent> = _errorEvents.asSharedFlow()

    /**
     * Handle errors and convert them to user-friendly messages.
     *
     * @param error The throwable to handle
     * @param context Where the error occurred
     * @param userAction What action triggered the error (for logging)
     * @param siteId Optional site ID for context-specific errors
     * @return ErrorResult with user-friendly message and recovery options
     */
    fun handleError(
        error: Throwable,
        context: ErrorContext = ErrorContext.GENERAL,
        userAction: String? = null,
        siteId: String? = null,
    ): ErrorResult {
        val classified = classifyError(error, context)
        val errorResult = classified.copy(presentation = errorPresentation(classified, error))

        // Log with appropriate level based on severity
        logError(error, errorResult, context, userAction)

        // Emit event for UI consumption
        emitErrorEvent(errorResult, context, userAction, siteId, error)

        return errorResult
    }

    private fun classifyError(
        error: Throwable,
        context: ErrorContext,
    ): ErrorResult = when (error) {
        // Ktor HTTP errors
        is ClientRequestException -> handleClientError(error)

        is ServerResponseException -> handleServerError(error)

        // Custom app errors
        is NetworkError -> handleNetworkError(error.code)

        is ValidationError -> handleValidationError(error)

        is DatabaseError -> handleDatabaseError(error)

        is SnapshotStorageError -> ErrorResult(
            type = ErrorType.DATABASE,
            userMessage = if (error.lowSpace) "Not enough storage" else "Could not save this check",
            description = if (error.lowSpace) "Free up device storage, then check the page again. Existing history is preserved." else "The snapshot could not be written. Existing history is preserved.",
            severity = ErrorSeverity.ERROR,
            isRetryable = !error.lowSpace,
            suggestedActions = if (error.lowSpace) listOf("Free up device storage", "Check the page again") else listOf("Check the page again"),
            recoveryAction = if (error.lowSpace) RecoveryAction.NONE else RecoveryAction.RETRY,
        )

        is SyncError -> handleSyncError(error)

        is AuthenticationError -> handleAuthenticationError(error)

        is ContentError -> handleContentError(error)

        is HttpRequestTimeoutException -> handleTimeoutError()

        is ConnectTimeoutException -> handleTimeoutError()

        is SocketTimeoutException -> handleTimeoutError()

        // Network connectivity
        is IOException -> handleConnectivityError(error)

        // Fallback
        else -> handleGenericError(error)
    }

    private fun handleClientError(error: ClientRequestException): ErrorResult {
        val code = error.response.status.value
        return when (code) {
            400 ->
                ErrorResult(
                    type = ErrorType.VALIDATION,
                    userMessage = "Invalid request",
                    severity = ErrorSeverity.WARNING,
                    isRetryable = false,
                    recoveryAction = RecoveryAction.CHECK_URL,
                )

            401 ->
                ErrorResult(
                    type = ErrorType.AUTHENTICATION,
                    userMessage = "Login required",
                    severity = ErrorSeverity.WARNING,
                    isRetryable = true,
                    recoveryAction = RecoveryAction.LOGIN_REQUIRED,
                )

            403 ->
                ErrorResult(
                    type = ErrorType.NETWORK,
                    userMessage = "Access denied",
                    description = "This site blocks automated requests",
                    severity = ErrorSeverity.WARNING,
                    isRetryable = false,
                    suggestedActions = listOf("Try a different URL", "The site may require login"),
                    recoveryAction = RecoveryAction.CHECK_URL,
                )

            404 ->
                ErrorResult(
                    type = ErrorType.NETWORK,
                    userMessage = "Page not found",
                    severity = ErrorSeverity.WARNING,
                    isRetryable = false,
                    suggestedActions = listOf("Check if the URL is correct", "The page may have been moved"),
                    recoveryAction = RecoveryAction.CHECK_URL,
                )

            429 ->
                ErrorResult(
                    type = ErrorType.RATE_LIMITED,
                    userMessage = "Too many requests",
                    description = "Please wait before trying again",
                    severity = ErrorSeverity.WARNING,
                    isRetryable = true,
                    recoveryAction = RecoveryAction.WAIT_AND_RETRY,
                )

            else ->
                ErrorResult(
                    type = ErrorType.NETWORK,
                    userMessage = "Request failed ($code)",
                    severity = ErrorSeverity.WARNING,
                    isRetryable = code >= 500,
                )
        }
    }

    private fun handleServerError(error: ServerResponseException): ErrorResult {
        val code = error.response.status.value
        return ErrorResult(
            type = ErrorType.NETWORK,
            userMessage =
            when (code) {
                500 -> "Server error"
                502 -> "Bad gateway"
                503 -> "Service unavailable"
                504 -> "Gateway timeout"
                else -> "Server error ($code)"
            },
            description = "The website's server is having issues",
            severity = ErrorSeverity.WARNING,
            isRetryable = true,
            recoveryAction = RecoveryAction.RETRY_LATER,
        )
    }

    private fun handleNetworkError(code: Int): ErrorResult = when (code) {
        401 ->
            ErrorResult(
                type = ErrorType.AUTHENTICATION,
                userMessage = "Login required",
                severity = ErrorSeverity.WARNING,
                isRetryable = true,
                recoveryAction = RecoveryAction.LOGIN_REQUIRED,
            )

        403 ->
            ErrorResult(
                type = ErrorType.NETWORK,
                userMessage = "Access denied",
                description = "This site blocks automated requests",
                severity = ErrorSeverity.WARNING,
                isRetryable = false,
                suggestedActions = listOf("Try a different URL", "The site may require login"),
                recoveryAction = RecoveryAction.CHECK_URL,
            )

        404 ->
            ErrorResult(
                type = ErrorType.NETWORK,
                userMessage = "Page not found",
                severity = ErrorSeverity.WARNING,
                isRetryable = false,
                suggestedActions = listOf("Check if the URL is correct", "The page may have been moved"),
                recoveryAction = RecoveryAction.CHECK_URL,
            )

        429 ->
            ErrorResult(
                type = ErrorType.RATE_LIMITED,
                userMessage = "Too many requests",
                description = "Please wait before trying again",
                severity = ErrorSeverity.WARNING,
                isRetryable = true,
                recoveryAction = RecoveryAction.WAIT_AND_RETRY,
            )

        in 500..599 ->
            ErrorResult(
                type = ErrorType.NETWORK,
                userMessage = "Server error ($code)",
                description = "The website's server is having issues",
                severity = ErrorSeverity.WARNING,
                isRetryable = true,
                recoveryAction = RecoveryAction.RETRY_LATER,
            )

        else ->
            ErrorResult(
                type = ErrorType.NETWORK,
                userMessage = "Network error ($code)",
                severity = ErrorSeverity.WARNING,
                isRetryable = true,
            )
    }

    private fun handleValidationError(error: ValidationError): ErrorResult {
        val (message, description) =
            when (error.field) {
                "url" -> "Invalid URL" to "Please enter a valid web address"
                "title" -> "Invalid title" to "Title cannot be empty"
                "selector" -> "Invalid selector" to "CSS selector is not valid"
                "threshold" -> "Invalid threshold" to "Threshold must be a positive number"
                else -> "Invalid input" to error.message
            }

        return ErrorResult(
            type = ErrorType.VALIDATION,
            userMessage = message,
            description = description,
            severity = ErrorSeverity.INFO,
            isRetryable = false,
            recoveryAction = RecoveryAction.FIX_INPUT,
        )
    }

    private fun handleDatabaseError(error: DatabaseError): ErrorResult {
        val (message, description) =
            when (error.operation) {
                DatabaseError.Operation.READ -> "Storage read error" to "Failed to read data"
                DatabaseError.Operation.WRITE -> "Storage save error" to "Failed to save data"
                DatabaseError.Operation.DELETE -> "Storage delete error" to "Failed to delete data"
                else -> "Storage error" to "A database error occurred"
            }

        return ErrorResult(
            type = ErrorType.DATABASE,
            userMessage = message,
            description = description,
            severity = ErrorSeverity.ERROR,
            isRetryable = true,
            suggestedActions = listOf("Restart the app", "Free up storage space"),
            recoveryAction = RecoveryAction.RESTART_APP,
        )
    }

    private fun handleSyncError(error: SyncError): ErrorResult = when (error.reason) {
        SyncError.Reason.RATE_LIMITED ->
            ErrorResult(
                type = ErrorType.RATE_LIMITED,
                userMessage = "Rate limited",
                description = "Too many requests to this site",
                severity = ErrorSeverity.WARNING,
                isRetryable = true,
                recoveryAction = RecoveryAction.WAIT_AND_RETRY,
            )

        SyncError.Reason.CONTENT_TOO_LARGE ->
            ErrorResult(
                type = ErrorType.CONTENT,
                userMessage = "Content too large",
                description = "The page is too big to process",
                severity = ErrorSeverity.WARNING,
                isRetryable = false,
                suggestedActions = listOf("Use element selection", "Choose a smaller page section"),
                recoveryAction = RecoveryAction.SELECT_ELEMENT,
            )

        SyncError.Reason.NO_CHANGES ->
            ErrorResult(
                type = ErrorType.INFO,
                userMessage = "No changes detected",
                severity = ErrorSeverity.INFO,
                isRetryable = false,
            )

        SyncError.Reason.SITE_DISABLED ->
            ErrorResult(
                type = ErrorType.INFO,
                userMessage = "Site monitoring disabled",
                severity = ErrorSeverity.INFO,
                isRetryable = false,
            )

        else ->
            ErrorResult(
                type = ErrorType.SYNC,
                userMessage = "Sync failed",
                severity = ErrorSeverity.WARNING,
                isRetryable = true,
            )
    }

    private fun handleAuthenticationError(error: AuthenticationError): ErrorResult = ErrorResult(
        type = ErrorType.AUTHENTICATION,
        userMessage = "Authentication required",
        description = "This site requires login",
        severity = ErrorSeverity.WARNING,
        isRetryable = false,
        suggestedActions = listOf("Open in browser to log in", "Use a public page instead"),
        recoveryAction = RecoveryAction.LOGIN_REQUIRED,
    )

    private fun handleContentError(error: ContentError): ErrorResult = ErrorResult(
        type = ErrorType.CONTENT,
        userMessage =
        when (error.reason) {
            ContentError.Reason.EMPTY -> "Empty content"
            ContentError.Reason.UNSUPPORTED -> "Unsupported content type"
            ContentError.Reason.CAPTURE_UNAVAILABLE -> "Screenshot monitoring is unavailable on this platform"
            ContentError.Reason.ENCODING -> "Encoding error"
            else -> "Content processing failed"
        },
        severity = ErrorSeverity.WARNING,
        isRetryable = error.reason !in setOf(ContentError.Reason.UNSUPPORTED, ContentError.Reason.CAPTURE_UNAVAILABLE),
    )

    private fun handleConnectivityError(error: IOException): ErrorResult {
        val errorName = error::class.simpleName ?: "IOException"

        return when {
            errorName.contains("UnknownHost", ignoreCase = true) ->
                ErrorResult(
                    type = ErrorType.CONNECTIVITY,
                    userMessage = "Website not found",
                    description = "Could not find the website",
                    severity = ErrorSeverity.WARNING,
                    isRetryable = true,
                    suggestedActions = listOf("Check the URL", "Check your internet connection"),
                    recoveryAction = RecoveryAction.CHECK_CONNECTION,
                )

            errorName.contains("SSL", ignoreCase = true) ->
                ErrorResult(
                    type = ErrorType.CONNECTIVITY,
                    userMessage = "SSL error",
                    description = "Secure connection failed",
                    severity = ErrorSeverity.WARNING,
                    isRetryable = false,
                    suggestedActions = listOf("The site may have certificate issues"),
                    recoveryAction = RecoveryAction.CHECK_URL,
                )

            else ->
                ErrorResult(
                    type = ErrorType.CONNECTIVITY,
                    userMessage = "No internet connection",
                    severity = ErrorSeverity.WARNING,
                    isRetryable = true,
                    suggestedActions = listOf("Check your connection", "Try again"),
                    recoveryAction = RecoveryAction.CHECK_CONNECTION,
                )
        }
    }

    private fun handleTimeoutError(): ErrorResult = ErrorResult(
        type = ErrorType.TIMEOUT,
        userMessage = "Request timed out",
        description = "The website took too long to respond",
        severity = ErrorSeverity.WARNING,
        isRetryable = true,
        suggestedActions = listOf("Check your connection speed", "Try again"),
        recoveryAction = RecoveryAction.RETRY,
    )

    private fun handleGenericError(error: Throwable): ErrorResult = ErrorResult(
        type = ErrorType.UNKNOWN,
        userMessage = "Something went wrong",
        description = error.message?.take(100),
        severity = ErrorSeverity.ERROR,
        isRetryable = true,
        suggestedActions = listOf("Try again", "Restart the app if problem persists"),
        recoveryAction = RecoveryAction.RETRY,
    )

    private fun logError(
        error: Throwable,
        result: ErrorResult,
        context: ErrorContext,
        userAction: String?,
    ) {
        val tag = "ErrorHandler"
        val message =
            buildString {
                append("[${context.name}] ")
                append(result.userMessage)
                if (userAction != null) append(" (action: $userAction)")
                append(" - ${error::class.simpleName}: ${error.message}")
            }

        when (result.severity) {
            ErrorSeverity.INFO -> Napier.i(message, tag = tag)
            ErrorSeverity.WARNING -> Napier.w(message, tag = tag)
            ErrorSeverity.ERROR -> Napier.e(message, error, tag = tag)
            ErrorSeverity.CRITICAL -> Napier.e("CRITICAL: $message", error, tag = tag)
        }
    }

    private fun emitErrorEvent(
        result: ErrorResult,
        context: ErrorContext,
        userAction: String?,
        siteId: String?,
        error: Throwable,
    ) {
        val event =
            ErrorEvent(
                id =
                kotlin.uuid.Uuid
                    .random()
                    .toString(),
                type = result.type,
                severity = result.severity,
                message = result.userMessage,
                presentation = result.presentation,
                description = result.description,
                context = context,
                userAction = userAction,
                siteId = siteId,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                isRetryable = result.isRetryable,
                recoveryAction = result.recoveryAction,
                suggestedActions = result.suggestedActions,
            )

        _errorEvents.tryEmit(event)
    }
}

// Error classification
enum class ErrorType {
    NETWORK,
    VALIDATION,
    DATABASE,
    SYNC,
    AUTHENTICATION,
    CONNECTIVITY,
    TIMEOUT,
    PERMISSIONS,
    CONTENT,
    RATE_LIMITED,
    INFO,
    UNKNOWN,
}

enum class ErrorSeverity {
    INFO, // Just information, no action needed
    WARNING, // Something went wrong but recoverable
    ERROR, // Significant error, may need user action
    CRITICAL, // App-breaking error
}

enum class ErrorContext {
    GENERAL,
    SYNC_WORKER,
    TURBO_SYNC,
    ELEMENT_SELECTOR,
    AUTHENTICATION,
    DATABASE_OPERATION,
    CONTENT_PROCESSING,
    USER_INPUT,
    ADD_SITE,
    EDIT_SITE,
    DELETE_SITE,
    BROWSER,
}

enum class RecoveryAction {
    RETRY, // Simply retry the operation
    RETRY_LATER, // Wait and retry
    WAIT_AND_RETRY, // Rate limited - wait before retry
    CHECK_CONNECTION, // Check internet connection
    CHECK_URL, // Verify the URL is correct
    FIX_INPUT, // Fix the input data
    SELECT_ELEMENT, // Select a smaller element
    LOGIN_REQUIRED, // Site requires authentication
    RESTART_APP, // Restart the application
    CONTACT_SUPPORT, // Contact support
    NONE, // No recovery possible
}

data class ErrorResult(
    val type: ErrorType,
    val userMessage: String,
    val description: String? = null,
    val severity: ErrorSeverity = ErrorSeverity.WARNING,
    val isRetryable: Boolean = false,
    val suggestedActions: List<String> = emptyList(),
    val recoveryAction: RecoveryAction = RecoveryAction.NONE,
    val presentation: ErrorPresentation? = null,
) {
    val shouldShowToUser: Boolean
        get() = severity != ErrorSeverity.INFO || type == ErrorType.INFO

    val shouldLog: Boolean
        get() = severity >= ErrorSeverity.WARNING
}

data class ErrorEvent(
    val id: String,
    val type: ErrorType,
    val severity: ErrorSeverity,
    val message: String,
    val description: String?,
    val context: ErrorContext,
    val userAction: String?,
    val siteId: String?,
    val timestamp: Long,
    val isRetryable: Boolean,
    val recoveryAction: RecoveryAction,
    val suggestedActions: List<String>,
    val presentation: ErrorPresentation? = null,
)

// Custom exception classes with additional context
class SnapshotStorageError(val lowSpace: Boolean, cause: Throwable? = null) :
    Exception(
        if (lowSpace) "Insufficient space to safely save a snapshot" else "Snapshot file write failed",
        cause,
    )

class NetworkError(
    val code: Int,
    message: String,
) : Exception(message)

class ValidationError(
    val field: String,
    message: String,
) : Exception(message)

class DatabaseError(
    val operation: Operation = Operation.UNKNOWN,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    enum class Operation { READ, WRITE, DELETE, UNKNOWN }
}

class SyncError(
    val reason: Reason,
    message: String = "",
) : Exception(message) {
    enum class Reason {
        RATE_LIMITED,
        CONTENT_TOO_LARGE,
        NO_CHANGES,
        SITE_DISABLED,
        NETWORK_ERROR,
        UNKNOWN,
    }
}

class AuthenticationError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class ContentError(
    val reason: Reason,
    message: String = "",
) : Exception(message) {
    enum class Reason { EMPTY, UNSUPPORTED, CAPTURE_UNAVAILABLE, ENCODING, UNKNOWN }
}

// Extension functions
fun Throwable.toErrorResult(
    errorHandler: ErrorHandler,
    context: ErrorContext = ErrorContext.GENERAL,
): ErrorResult = errorHandler.handleError(this, context)

suspend fun <T> Result<T>.handleError(
    errorHandler: ErrorHandler,
    context: ErrorContext = ErrorContext.GENERAL,
): Result<T> = onFailure { error ->
    errorHandler.handleError(error, context)
}

fun <T> Result<T>.getErrorResult(
    errorHandler: ErrorHandler,
    context: ErrorContext = ErrorContext.GENERAL,
): ErrorResult? = exceptionOrNull()?.let { errorHandler.handleError(it, context) }
