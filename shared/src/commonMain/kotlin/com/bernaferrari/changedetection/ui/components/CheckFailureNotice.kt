package com.bernaferrari.changedetection.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.error.CheckFailure
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.localization.stringResource

@Composable
fun checkFailureTitle(code: String?): String = stringResource(
    when (CheckFailure.fromCode(code)) {
        CheckFailure.CONNECTION -> Res.string.check_failure_connection
        CheckFailure.TIMEOUT -> Res.string.check_failure_timeout
        CheckFailure.LOGIN -> Res.string.check_failure_login
        CheckFailure.ACCESS_DENIED -> Res.string.check_failure_access_denied
        CheckFailure.NOT_FOUND -> Res.string.check_failure_not_found
        CheckFailure.RATE_LIMITED -> Res.string.check_failure_rate_limited
        CheckFailure.SERVER -> Res.string.check_failure_server
        CheckFailure.STORAGE_FULL -> Res.string.check_failure_storage_full
        CheckFailure.SAVE_FAILED -> Res.string.check_failure_save_failed
        CheckFailure.CONTENT_TOO_LARGE -> Res.string.check_failure_content_too_large
        CheckFailure.CONTENT -> Res.string.check_failure_content
        CheckFailure.CAPTURE_UNAVAILABLE -> Res.string.check_failure_capture_unavailable
        CheckFailure.REGION -> Res.string.check_failure_region
        CheckFailure.UNKNOWN -> Res.string.check_failure_unknown
    },
)

@Composable
fun CheckFailureNotice(code: String?, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    val detail = stringResource(
        when (CheckFailure.fromCode(code)) {
            CheckFailure.CONNECTION -> Res.string.check_failure_connection_detail
            CheckFailure.TIMEOUT -> Res.string.check_failure_timeout_detail
            CheckFailure.LOGIN -> Res.string.check_failure_login_detail
            CheckFailure.ACCESS_DENIED -> Res.string.check_failure_access_denied_detail
            CheckFailure.NOT_FOUND -> Res.string.check_failure_not_found_detail
            CheckFailure.RATE_LIMITED -> Res.string.check_failure_rate_limited_detail
            CheckFailure.SERVER -> Res.string.check_failure_server_detail
            CheckFailure.STORAGE_FULL -> Res.string.check_failure_storage_full_detail
            CheckFailure.SAVE_FAILED -> Res.string.check_failure_save_failed_detail
            CheckFailure.CONTENT_TOO_LARGE -> Res.string.check_failure_content_too_large_detail
            CheckFailure.CONTENT -> Res.string.check_failure_content_detail
            CheckFailure.CAPTURE_UNAVAILABLE -> Res.string.check_failure_capture_unavailable_detail
            CheckFailure.REGION -> Res.string.check_failure_region_detail
            CheckFailure.UNKNOWN -> Res.string.check_failure_unknown_detail
        },
    )
    Surface(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(checkFailureTitle(code), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
        }
    }
}
