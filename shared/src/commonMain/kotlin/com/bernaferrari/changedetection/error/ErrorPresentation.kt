package com.bernaferrari.changedetection.error

import com.bernaferrari.changedetection.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource

/** Resolve UI text when rendered, so changing language also updates an already-visible error. Diagnostics stay in logs. */
data class ErrorPresentation(val title: StringResource, val body: StringResource? = null)

internal fun errorPresentation(result: ErrorResult, error: Throwable?): ErrorPresentation {
    if (result.type == ErrorType.VALIDATION) return ErrorPresentation(Res.string.error_invalid_input, Res.string.error_invalid_input_detail)
    if (result.type == ErrorType.INFO) {
        return ErrorPresentation(
            if (error is SyncError && error.reason == SyncError.Reason.SITE_DISABLED) Res.string.detail_status_paused else Res.string.comparison_no_changes_title,
        )
    }
    if (error is DatabaseError) {
        when (error.operation) {
            DatabaseError.Operation.READ -> return ErrorPresentation(Res.string.comparison_load_failed, Res.string.comparison_load_failed_detail)
            DatabaseError.Operation.DELETE -> return ErrorPresentation(Res.string.delete_site_failed)
            else -> Unit
        }
    }
    val pair = when (CheckFailure.from(result, error)) {
        CheckFailure.BACKGROUND_RESTRICTED -> Res.string.check_failure_background_restricted to Res.string.check_failure_background_restricted_detail
        CheckFailure.TLS -> Res.string.check_failure_tls to Res.string.check_failure_tls_detail
        CheckFailure.CONNECTION -> Res.string.check_failure_connection to Res.string.check_failure_connection_detail
        CheckFailure.TIMEOUT -> Res.string.check_failure_timeout to Res.string.check_failure_timeout_detail
        CheckFailure.LOGIN -> Res.string.check_failure_login to Res.string.advice_login
        CheckFailure.ACCESS_DENIED -> Res.string.check_failure_access_denied to Res.string.check_failure_access_denied_detail
        CheckFailure.NOT_FOUND -> Res.string.check_failure_not_found to Res.string.check_failure_not_found_detail
        CheckFailure.RATE_LIMITED -> Res.string.check_failure_rate_limited to Res.string.check_failure_rate_limited_detail
        CheckFailure.SERVER -> Res.string.check_failure_server to Res.string.check_failure_server_detail
        CheckFailure.STORAGE_FULL -> Res.string.check_failure_storage_full to Res.string.check_failure_storage_full_detail
        CheckFailure.SAVED_CONTENT_UNAVAILABLE -> Res.string.check_failure_saved_content_unavailable to Res.string.check_failure_saved_content_unavailable_detail
        CheckFailure.SAVE_FAILED -> Res.string.check_failure_save_failed to Res.string.check_failure_save_failed_detail
        CheckFailure.CONTENT_TOO_LARGE -> Res.string.check_failure_content_too_large to Res.string.check_failure_content_too_large_detail
        CheckFailure.CONTENT -> Res.string.check_failure_content to Res.string.check_failure_content_detail
        CheckFailure.CAPTURE_INCOMPLETE -> Res.string.check_failure_capture_incomplete to Res.string.check_failure_capture_incomplete_detail
        CheckFailure.CAPTURE_UNAVAILABLE -> Res.string.check_failure_capture_unavailable to Res.string.check_failure_capture_unavailable_detail
        CheckFailure.MULTIPLE_VISUAL_REGIONS -> Res.string.check_failure_region to Res.string.preview_single_visual_region
        CheckFailure.REGION -> Res.string.check_failure_region to Res.string.check_failure_region_detail
        CheckFailure.UNKNOWN -> Res.string.check_failure_unknown to Res.string.check_failure_unknown_detail
    }
    return ErrorPresentation(pair.first, pair.second)
}
