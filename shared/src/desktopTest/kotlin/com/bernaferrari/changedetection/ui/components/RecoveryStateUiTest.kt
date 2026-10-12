package com.bernaferrari.changedetection.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.jetbrains.compose.resources.stringResource
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.graphics.toAwtImage
import java.io.File
import javax.imageio.ImageIO

class RecoveryStateUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun shortLargeTextKeepsBothRecoveryActionsReachable() = verify(2f, 280, 300)

    @Test fun normalRecoveryFitsWithoutHidingItsAction() = verify(1f, 400, 700)

    private fun verify(fontScale: Float, width: Int, height: Int) {
        var retries = 0
        var backs = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ChangeDetectionTheme(darkTheme = fontScale > 1, dynamicColor = false) {
                    Surface(Modifier.size(width.dp, height.dp)) {
                        SharedEmptyState(
                            icon = Icons.Outlined.ErrorOutline,
                            title = stringResource(Res.string.comparison_load_failed),
                            description = stringResource(Res.string.comparison_load_failed_detail),
                            primaryActionLabel = "Retry",
                            onPrimaryAction = { retries++ },
                            secondaryActionLabel = "Go back",
                            onSecondaryAction = { backs++ },
                        )
                    }
                }
            }
        }
        if (fontScale == 1f) compose.onNodeWithText("Retry").assertIsDisplayed()
        compose.onNodeWithText("Try again to load this page and its saved versions.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Retry").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Go back").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1, retries)
            assertEquals(1, backs)
        }
        val output = File("../build/desktop-review/recovery-${if (fontScale > 1) "large-dark" else "light"}.png")
        output.parentFile.mkdirs()
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", output)
    }
}
