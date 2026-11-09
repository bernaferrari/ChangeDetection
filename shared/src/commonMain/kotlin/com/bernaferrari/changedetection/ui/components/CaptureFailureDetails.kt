package com.bernaferrari.changedetection.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.bernaferrari.changedetection.browser.CaptureExtent
import com.bernaferrari.changedetection.browser.CaptureResourceFailure
import com.bernaferrari.changedetection.browser.CaptureResourceKind
import com.bernaferrari.changedetection.localization.stringResource
import com.bernaferrari.changedetection.shared.generated.resources.*

@Composable
internal fun CaptureFailureDetails(resources: List<CaptureResourceFailure>, extent: CaptureExtent?, attached: Boolean? = null) {
    if (resources.isEmpty()) {
        Text(stringResource(Res.string.capture_resource_details_unavailable), style = MaterialTheme.typography.bodyMedium)
    }
    for (failure in resources) {
        val kind = stringResource(
            when (failure.kind) {
                CaptureResourceKind.IMAGE -> Res.string.capture_resource_image
                CaptureResourceKind.FONT -> Res.string.capture_resource_font
                CaptureResourceKind.STYLESHEET -> Res.string.capture_resource_stylesheet
                CaptureResourceKind.FRAME -> Res.string.capture_resource_frame
                CaptureResourceKind.OTHER -> Res.string.capture_resource_other
            },
        )
        Text(
            when {
                failure.certificateRejected -> stringResource(Res.string.capture_resource_certificate, kind)
                failure.httpStatus != null -> stringResource(Res.string.capture_resource_http, kind, failure.httpStatus)
                else -> stringResource(Res.string.capture_resource_load, kind)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    Text(
        when (extent) {
            null -> stringResource(Res.string.capture_size_unavailable)
            CaptureExtent.Empty -> stringResource(Res.string.capture_size_zero)
            else -> stringResource(Res.string.capture_size_pixels, extent.width, extent.height)
        },
        style = MaterialTheme.typography.bodyMedium,
    )
    if (attached != null) {
        Text(
            stringResource(if (attached) Res.string.capture_browser_attached else Res.string.capture_browser_detached),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
