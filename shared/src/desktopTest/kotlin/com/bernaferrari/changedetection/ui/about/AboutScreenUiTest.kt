package com.bernaferrari.changedetection.ui.about

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class AboutScreenUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun socialIconsKeepAccessibleNamesAndDestinationsAndPrivacyShieldSitsAboveText() {
        val opened = mutableListOf<String>()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.8f)) {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    Box(Modifier.size(320.dp, 760.dp)) { AboutScreen({}, { opened += it }, {}, {}, {}) }
                }
            }
        }
        compose.onNodeWithTag("about-list").performScrollToKey("elsewhere")
        val links = listOf("GitHub" to "https://github.com/bernaferrari", "X" to "https://x.com/bernaferrari", "Reddit" to "https://www.reddit.com/user/bernaferrari", "LinkedIn" to "https://www.linkedin.com/in/bernaferrari")
        for ((name, url) in links) {
            compose.onNodeWithContentDescription(name).assertIsDisplayed().performClick()
            compose.runOnIdle { assertEquals(url, opened.last()) }
            compose.onNodeWithText(name).assertDoesNotExist()
        }
        val privacy = runBlocking { getString(Res.string.settings_privacy_line) }
        compose.onNodeWithText(privacy).performScrollTo().assertIsDisplayed()
        assertTrue(compose.onNodeWithTag("privacy-shield").fetchSemanticsNode().boundsInRoot.bottom < compose.onNodeWithText(privacy).fetchSemanticsNode().boundsInRoot.top)
        val output = File("../build/i18n/screens/about-simple.png").apply { parentFile.mkdirs() }
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", output)
    }
}
