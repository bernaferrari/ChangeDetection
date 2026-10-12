package com.bernaferrari.changedetection.error

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ErrorHandler.
 *
 * Tests cover:
 * - Error classification
 * - User-friendly message generation
 * - Recovery action suggestions
 * - Severity classification
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ErrorHandlerTest {

    private lateinit var errorHandler: ErrorHandler

    @Before
    fun setup() {
        errorHandler = ErrorHandler()
    }

    // ========== Network Error Tests ==========

    @Test
    fun `handleError classifies 404 as NETWORK error`() {
        // Arrange
        val error = NetworkError(404, "Not Found")

        // Act
        val result = errorHandler.handleError(error, ErrorContext.SYNC_WORKER)

        // Assert
        assertEquals(ErrorType.NETWORK, result.type)
        assertTrue(result.userMessage.contains("not found", ignoreCase = true))
        assertEquals(RecoveryAction.CHECK_URL, result.recoveryAction)
        assertFalse(result.isRetryable)
    }

    @Test
    fun `handleError classifies 403 as NETWORK with CHECK_URL recovery`() {
        // Arrange
        val error = NetworkError(403, "Forbidden")

        // Act
        val result = errorHandler.handleError(error, ErrorContext.SYNC_WORKER)

        // Assert
        assertEquals(ErrorType.NETWORK, result.type)
        assertEquals(RecoveryAction.CHECK_URL, result.recoveryAction)
        assertFalse(result.isRetryable)
    }

    @Test
    fun `handleError classifies 429 as RATE_LIMITED`() {
        // Arrange
        val error = NetworkError(429, "Too Many Requests")

        // Act
        val result = errorHandler.handleError(error, ErrorContext.SYNC_WORKER)

        // Assert
        assertEquals(ErrorType.RATE_LIMITED, result.type)
        assertTrue(result.isRetryable)
        assertEquals(RecoveryAction.WAIT_AND_RETRY, result.recoveryAction)
    }

    @Test
    fun `handleError classifies 500 as retryable server error`() {
        // Arrange
        val error = NetworkError(500, "Internal Server Error")

        // Act
        val result = errorHandler.handleError(error, ErrorContext.SYNC_WORKER)

        // Assert
        assertEquals(ErrorType.NETWORK, result.type)
        assertTrue(result.isRetryable)
        assertEquals(RecoveryAction.RETRY_LATER, result.recoveryAction)
    }

    // ========== Validation Error Tests ==========

    @Test
    fun `handleError classifies URL validation error correctly`() {
        // Arrange
        val error = ValidationError("url", "Invalid URL format")

        // Act
        val result = errorHandler.handleError(error, ErrorContext.ADD_SITE)

        // Assert
        assertEquals(ErrorType.VALIDATION, result.type)
        assertTrue(result.userMessage.contains("URL", ignoreCase = true))
        assertEquals(RecoveryAction.FIX_INPUT, result.recoveryAction)
        assertFalse(result.isRetryable)
    }

    @Test
    fun `handleError classifies threshold validation error correctly`() {
        // Arrange
        val error = ValidationError("threshold", "Threshold must be positive")

        // Act
        val result = errorHandler.handleError(error, ErrorContext.EDIT_SITE)

        // Assert
        assertEquals(ErrorType.VALIDATION, result.type)
        assertTrue(result.userMessage.contains("threshold", ignoreCase = true))
        assertEquals(ErrorSeverity.INFO, result.severity)
    }

    // ========== Database Error Tests ==========

    @Test
    fun `handleError classifies database read error`() {
        // Arrange
        val error = DatabaseError(DatabaseError.Operation.READ, "Failed to read", RuntimeException("DB error"))

        // Act
        val result = errorHandler.handleError(error, ErrorContext.DATABASE_OPERATION)

        // Assert
        assertEquals(ErrorType.DATABASE, result.type)
        assertTrue(result.userMessage.contains("read", ignoreCase = true))
        assertEquals(ErrorSeverity.ERROR, result.severity)
        assertTrue(result.isRetryable)
    }

    @Test
    fun `handleError classifies database write error`() {
        // Arrange
        val error = DatabaseError(DatabaseError.Operation.WRITE, "Failed to write")

        // Act
        val result = errorHandler.handleError(error, ErrorContext.DATABASE_OPERATION)

        // Assert
        assertEquals(ErrorType.DATABASE, result.type)
        assertTrue(result.userMessage.contains("save", ignoreCase = true))
        assertTrue(result.suggestedActions.any { it.contains("Restart", ignoreCase = true) })
    }

    // ========== Sync Error Tests ==========

    @Test
    fun `handleError classifies rate limited sync error`() {
        // Arrange
        val error = SyncError(SyncError.Reason.RATE_LIMITED, "Too many requests")

        // Act
        val result = errorHandler.handleError(error, ErrorContext.SYNC_WORKER)

        // Assert
        assertEquals(ErrorType.RATE_LIMITED, result.type)
        assertEquals(RecoveryAction.WAIT_AND_RETRY, result.recoveryAction)
        assertTrue(result.isRetryable)
    }

    @Test
    fun `handleError classifies content too large error`() {
        // Arrange
        val error = SyncError(SyncError.Reason.CONTENT_TOO_LARGE, "Content exceeds limit")

        // Act
        val result = errorHandler.handleError(error, ErrorContext.SYNC_WORKER)

        // Assert
        assertEquals(ErrorType.CONTENT, result.type)
        assertEquals(RecoveryAction.SELECT_ELEMENT, result.recoveryAction)
        assertFalse(result.isRetryable)
    }

    @Test
    fun `handleError classifies no changes as INFO`() {
        // Arrange
        val error = SyncError(SyncError.Reason.NO_CHANGES, "No changes detected")

        // Act
        val result = errorHandler.handleError(error, ErrorContext.SYNC_WORKER)

        // Assert
        assertEquals(ErrorType.INFO, result.type)
        assertEquals(ErrorSeverity.INFO, result.severity)
    }

    // ========== Authentication Error Tests ==========

    @Test
    fun `handleError classifies authentication error correctly`() {
        // Arrange
        val error = AuthenticationError("Login required")

        // Act
        val result = errorHandler.handleError(error, ErrorContext.AUTHENTICATION)

        // Assert
        assertEquals(ErrorType.AUTHENTICATION, result.type)
        assertEquals(RecoveryAction.LOGIN_REQUIRED, result.recoveryAction)
        assertFalse(result.isRetryable)
        assertTrue(result.suggestedActions.any { it.contains("browser", ignoreCase = true) })
    }

    // ========== Content Error Tests ==========

    @Test
    fun `handleError classifies empty content error`() {
        // Arrange
        val error = ContentError(ContentError.Reason.EMPTY, "No content")

        // Act
        val result = errorHandler.handleError(error, ErrorContext.CONTENT_PROCESSING)

        // Assert
        assertEquals(ErrorType.CONTENT, result.type)
        assertTrue(result.userMessage.contains("Empty", ignoreCase = true))
    }

    @Test
    fun `handleError classifies unsupported content type error`() {
        // Arrange
        val error = ContentError(ContentError.Reason.UNSUPPORTED, "Unsupported type")

        // Act
        val result = errorHandler.handleError(error, ErrorContext.CONTENT_PROCESSING)

        // Assert
        assertEquals(ErrorType.CONTENT, result.type)
        assertFalse(result.isRetryable)
    }

    // ========== Generic Error Tests ==========

    @Test
    fun `handleError provides fallback for unknown exceptions`() {
        // Arrange
        val error = RuntimeException("Unknown error")

        // Act
        val result = errorHandler.handleError(error, ErrorContext.GENERAL)

        // Assert
        assertEquals(ErrorType.UNKNOWN, result.type)
        assertTrue(result.isRetryable)
        assertEquals(ErrorSeverity.ERROR, result.severity)
    }

    // ========== Error Event Streaming Tests ==========

    @Test
    fun `errorEvents emits events on error`() = runTest {
        // Arrange
        val error = NetworkError(500, "Server error")

        // Subscribe to events
        val event = backgroundScope.async { errorHandler.errorEvents.first() }
        runCurrent()

        // Act
        errorHandler.handleError(error, ErrorContext.SYNC_WORKER)

        // Allow event to propagate
        runCurrent()

        // Assert
        assertEquals(ErrorType.NETWORK, event.await().type)
    }

    // ========== Severity Tests ==========

    @Test
    fun `network errors have WARNING severity`() {
        // Arrange
        val error = NetworkError(404, "Not found")

        // Act
        val result = errorHandler.handleError(error, ErrorContext.SYNC_WORKER)

        // Assert
        assertEquals(ErrorSeverity.WARNING, result.severity)
    }

    @Test
    fun `database errors have ERROR severity`() {
        // Arrange
        val error = DatabaseError(DatabaseError.Operation.READ, "Read failed")

        // Act
        val result = errorHandler.handleError(error, ErrorContext.DATABASE_OPERATION)

        // Assert
        assertEquals(ErrorSeverity.ERROR, result.severity)
    }

    // ========== Context Tests ==========

    @Test
    fun `error context is preserved in events`() = runTest {
        // Arrange
        val error = NetworkError(404, "Not found")
        val event = backgroundScope.async { errorHandler.errorEvents.first() }
        runCurrent()

        // Act
        errorHandler.handleError(error, ErrorContext.ADD_SITE)
        runCurrent()

        // Assert
        assertEquals(ErrorContext.ADD_SITE, event.await().context)
    }

    // ========== Helper Tests ==========

    @Test
    fun `shouldShowToUser returns true for WARNING and above`() {
        // Arrange
        val warningResult = ErrorResult(ErrorType.NETWORK, "Warning", severity = ErrorSeverity.WARNING)
        val infoResult = ErrorResult(ErrorType.INFO, "Info", severity = ErrorSeverity.INFO)

        // Assert
        assertTrue(warningResult.shouldShowToUser)
        // INFO type should still show if it's explicitly INFO type
        assertTrue(infoResult.shouldShowToUser)
    }

    @Test
    fun `shouldLog returns true for WARNING and above`() {
        // Arrange
        val warningResult = ErrorResult(ErrorType.NETWORK, "Warning", severity = ErrorSeverity.WARNING)
        val errorResult = ErrorResult(ErrorType.DATABASE, "Error", severity = ErrorSeverity.ERROR)

        // Assert
        assertTrue(warningResult.shouldLog)
        assertTrue(errorResult.shouldLog)
    }
}
