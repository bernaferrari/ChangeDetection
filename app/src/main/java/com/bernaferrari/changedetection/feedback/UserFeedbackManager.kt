package com.bernaferrari.changedetection.feedback

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.bernaferrari.changedetection.error.ErrorResult
import com.bernaferrari.changedetection.error.ErrorType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user feedback including snackbars, toasts, and progress indicators
 * Provides Apple-style user feedback with consistent design and behavior
 */
@Singleton
class UserFeedbackManager @Inject constructor(
    private val context: Context
) {
    
    private val _feedbackEvents = MutableSharedFlow<FeedbackEvent>()
    val feedbackEvents: SharedFlow<FeedbackEvent> = _feedbackEvents.asSharedFlow()
    
    private val _progressEvents = MutableSharedFlow<ProgressEvent>()
    val progressEvents: SharedFlow<ProgressEvent> = _progressEvents.asSharedFlow()
    
    /**
     * Show success message to user
     */
    suspend fun showSuccess(
        message: String,
        actionLabel: String? = null,
        action: (() -> Unit)? = null,
        snackbarHostState: SnackbarHostState
    ) {
        val event = FeedbackEvent.Success(
            message = message,
            actionLabel = actionLabel,
            action = action
        )
        
        _feedbackEvents.emit(event)
        
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Short
        )
        
        if (result == SnackbarResult.ActionPerformed && action != null) {
            action()
        }
    }
    
    /**
     * Show error message to user with appropriate actions
     */
    suspend fun showError(
        errorResult: ErrorResult,
        snackbarHostState: SnackbarHostState,
        onRetry: (() -> Unit)? = null,
        onSettings: (() -> Unit)? = null
    ) {
        val actionLabel = when {
            errorResult.isRetryable && onRetry != null -> "Retry"
            errorResult.type == ErrorType.VALIDATION && onSettings != null -> "Settings"
            errorResult.suggestedActions.isNotEmpty() -> errorResult.suggestedActions.first()
            else -> null
        }
        
        val action = when {
            errorResult.isRetryable && onRetry != null -> onRetry
            errorResult.type == ErrorType.VALIDATION && onSettings != null -> onSettings
            else -> null
        }
        
        val event = FeedbackEvent.Error(
            message = errorResult.userMessage,
            errorType = errorResult.type,
            isRetryable = errorResult.isRetryable,
            suggestedActions = errorResult.suggestedActions,
            actionLabel = actionLabel,
            action = action
        )
        
        _feedbackEvents.emit(event)
        
        val result = snackbarHostState.showSnackbar(
            message = errorResult.userMessage,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Long
        )
        
        if (result == SnackbarResult.ActionPerformed && action != null) {
            action()
        }
    }
    
    /**
     * Show informational message to user
     */
    suspend fun showInfo(
        message: String,
        snackbarHostState: SnackbarHostState,
        actionLabel: String? = null,
        action: (() -> Unit)? = null
    ) {
        val event = FeedbackEvent.Info(
            message = message,
            actionLabel = actionLabel,
            action = action
        )
        
        _feedbackEvents.emit(event)
        
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Short
        )
        
        if (result == SnackbarResult.ActionPerformed && action != null) {
            action()
        }
    }
    
    /**
     * Show warning message to user
     */
    suspend fun showWarning(
        message: String,
        snackbarHostState: SnackbarHostState,
        actionLabel: String? = "OK",
        action: (() -> Unit)? = null
    ) {
        val event = FeedbackEvent.Warning(
            message = message,
            actionLabel = actionLabel,
            action = action
        )
        
        _feedbackEvents.emit(event)
        
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Long
        )
        
        if (result == SnackbarResult.ActionPerformed && action != null) {
            action()
        }
    }
    
    /**
     * Show progress indicator
     */
    suspend fun showProgress(
        message: String,
        isIndeterminate: Boolean = true,
        progress: Float = 0f
    ) {
        val event = ProgressEvent.Show(
            message = message,
            isIndeterminate = isIndeterminate,
            progress = progress
        )
        _progressEvents.emit(event)
    }
    
    /**
     * Update progress
     */
    suspend fun updateProgress(
        progress: Float,
        message: String? = null
    ) {
        val event = ProgressEvent.Update(
            progress = progress,
            message = message
        )
        _progressEvents.emit(event)
    }
    
    /**
     * Hide progress indicator
     */
    suspend fun hideProgress() {
        _progressEvents.emit(ProgressEvent.Hide)
    }
    
    /**
     * Show confirmation dialog
     */
    suspend fun showConfirmation(
        title: String,
        message: String,
        confirmText: String = "Confirm",
        cancelText: String = "Cancel"
    ) {
        val event = FeedbackEvent.Confirmation(
            title = title,
            message = message,
            confirmText = confirmText,
            cancelText = cancelText
        )
        _feedbackEvents.emit(event)
    }
}

// Feedback event types
sealed class FeedbackEvent {
    data class Success(
        val message: String,
        val actionLabel: String? = null,
        val action: (() -> Unit)? = null
    ) : FeedbackEvent()
    
    data class Error(
        val message: String,
        val errorType: ErrorType,
        val isRetryable: Boolean,
        val suggestedActions: List<String>,
        val actionLabel: String? = null,
        val action: (() -> Unit)? = null
    ) : FeedbackEvent()
    
    data class Info(
        val message: String,
        val actionLabel: String? = null,
        val action: (() -> Unit)? = null
    ) : FeedbackEvent()
    
    data class Warning(
        val message: String,
        val actionLabel: String? = null,
        val action: (() -> Unit)? = null
    ) : FeedbackEvent()
    
    data class Confirmation(
        val title: String,
        val message: String,
        val confirmText: String,
        val cancelText: String
    ) : FeedbackEvent()
}

// Progress event types
sealed class ProgressEvent {
    data class Show(
        val message: String,
        val isIndeterminate: Boolean = true,
        val progress: Float = 0f
    ) : ProgressEvent()
    
    data class Update(
        val progress: Float,
        val message: String? = null
    ) : ProgressEvent()
    
    object Hide : ProgressEvent()
}

// Extension functions for easier usage
fun CoroutineScope.showUserSuccess(
    feedbackManager: UserFeedbackManager,
    snackbarHostState: SnackbarHostState,
    message: String,
    actionLabel: String? = null,
    action: (() -> Unit)? = null
) {
    launch {
        feedbackManager.showSuccess(message, actionLabel, action, snackbarHostState)
    }
}

fun CoroutineScope.showUserError(
    feedbackManager: UserFeedbackManager,
    snackbarHostState: SnackbarHostState,
    errorResult: ErrorResult,
    onRetry: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null
) {
    launch {
        feedbackManager.showError(errorResult, snackbarHostState, onRetry, onSettings)
    }
}

fun CoroutineScope.showUserProgress(
    feedbackManager: UserFeedbackManager,
    message: String,
    isIndeterminate: Boolean = true,
    progress: Float = 0f
) {
    launch {
        feedbackManager.showProgress(message, isIndeterminate, progress)
    }
}

fun CoroutineScope.hideUserProgress(
    feedbackManager: UserFeedbackManager
) {
    launch {
        feedbackManager.hideProgress()
    }
}