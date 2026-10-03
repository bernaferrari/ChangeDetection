/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ExpressiveTilesUiTest {
    @get:Rule val compose = createComposeRule()

    private fun exercise(dark: Boolean, fontScale: Float) {
        var opened = 0
        var options = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ChangeDetectionTheme(darkTheme = dark, dynamicColor = false) {
                    androidx.compose.material3.Surface {
                        Column(Modifier.width(400.dp).padding(20.dp).testTag("tiles"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Conference program", "New releases", "Weekend tickets").forEachIndexed { index, title ->
                                SiteRow(
                                    Site(title, "https://example.test", 0L, "", "page-$index", 0 to 0),
                                    when {
                                        index == 0 -> SiteStatus.Changed("2 min ago")
                                        index == 1 && !dark -> SiteStatus.Error
                                        else -> SiteStatus.Paused
                                    },
                                    isQuickMonitored = index == 0,
                                    hasUnreadChange = index == 1 && !dark,
                                    isSelected = index == 1,
                                    selectionSemanticsEnabled = true,
                                    onClick = { opened++ }, onLongClick = { options++ },
                                    modifier = Modifier.testTag("tile-$index"),
                                )
                            }
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("tile-1").assertIsSelected().performClick()
        compose.onNodeWithTag("tile-0").assertIsNotSelected().performTouchInput { longClick() }
        compose.runOnIdle {
            assertEquals(1, opened)
            assertEquals(1, options)
        }
        val target = File("../build/desktop-review/tiles-${if (dark) "dark-large" else "light"}.png")
        target.parentFile.mkdirs()
        ImageIO.write(compose.onNodeWithTag("tiles").captureToImage().toAwtImage(), "png", target)
    }

    @Test fun lightTilesPreserveOpenAndOptionsActions() = exercise(false, 1f)

    @Test fun largeDarkTilesPreserveSelectionAndActions() = exercise(true, 2f)
}
