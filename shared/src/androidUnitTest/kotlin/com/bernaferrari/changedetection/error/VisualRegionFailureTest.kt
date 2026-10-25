package com.bernaferrari.changedetection.error

import com.bernaferrari.changedetection.detection.*
import com.bernaferrari.changedetection.shared.generated.resources.*
import org.junit.Assert.*
import org.junit.Test

class VisualRegionFailureTest {
    @Test fun `multiple visual matches get actionable copy`() {
        val error = TextRegionException(TextRegionFailure.REGION_MISSING, issue = TextRegionIssue.MULTIPLE_MATCHES)
        val result = ErrorHandler().handleError(error)
        assertEquals(CheckFailure.MULTIPLE_VISUAL_REGIONS, CheckFailure.from(result, error))
        assertEquals(Res.string.preview_single_visual_region, result.presentation?.body)
    }

    @Test fun `genuinely missing selector retains missing-region category`() {
        val error = TextRegionException(TextRegionFailure.REGION_MISSING, issue = TextRegionIssue.NO_MATCH)
        val result = ErrorHandler().handleError(error)
        assertEquals(CheckFailure.REGION, CheckFailure.from(result, error))
        assertEquals(Res.string.check_failure_region_detail, result.presentation?.body)
    }
}
