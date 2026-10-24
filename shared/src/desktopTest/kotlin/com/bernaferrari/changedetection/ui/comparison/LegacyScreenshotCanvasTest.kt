package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
class LegacyScreenshotCanvasTest {
    @get:Rule val compose = createComposeRule()
    private fun transparent(): ByteArray = ByteArrayOutputStream().use { output ->
        ImageIO.write(BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB), "png", output)
        output.toByteArray()
    }

    @Test fun savedBrowserScreenshotUsesWhiteCanvasEvenInDarkTheme() {
        val bytes = transparent()
        val snap = Snap(snapId = "legacy", siteId = "page", timestamp = 0L, contentType = "image/png", contentCharset = "", contentSize = bytes.size, contentHash = "image-v3:fixture")
        compose.setContent {
            ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                SingleSnapshotState(snap, null, bytes, false, false, {}, modifier = Modifier.size(240.dp, 180.dp).testTag("canvas"))
            }
        }
        compose.waitForIdle()
        assertEquals(0xffffffff.toInt(), compose.onNodeWithTag("canvas").captureToImage().toPixelMap()[4, 4].toArgb())
    }

    @Test fun rawSourceImageKeepsItsOriginalViewerBackdrop() {
        val bytes = transparent()
        val snap = Snap(snapId = "raw", siteId = "page", timestamp = 0L, contentType = "image/png", contentCharset = "", contentSize = bytes.size, contentHash = "image-v2:fixture")
        var expected = 0
        compose.setContent {
            ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                expected = androidx.compose.material3.MaterialTheme.colorScheme.surfaceBright.toArgb()
                SingleSnapshotState(snap, null, bytes, false, false, {}, modifier = Modifier.size(240.dp, 180.dp).testTag("canvas"))
            }
        }
        compose.waitForIdle()
        assertEquals(expected, compose.onNodeWithTag("canvas").captureToImage().toPixelMap()[4, 4].toArgb())
    }

    @Test fun legacyBrowserComparisonUsesWhiteCanvas() {
        val bytes = transparent()
        compose.setContent {
            ChangeDetectionTheme(darkTheme = true, dynamicColor = false) {
                ImageComparisonContent(bytes, bytes, null, ComparisonMode.ImageSwipe, Modifier.size(240.dp, 180.dp).testTag("canvas"), opaqueWhiteBackdrop = true)
            }
        }
        compose.waitForIdle()
        assertEquals(0xffffffff.toInt(), compose.onNodeWithTag("canvas").captureToImage().toPixelMap()[4, 70].toArgb())
    }
}
