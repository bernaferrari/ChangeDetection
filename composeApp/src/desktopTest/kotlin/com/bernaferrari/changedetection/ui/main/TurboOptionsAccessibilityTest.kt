/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.ui.main.components.TurboHero
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import java.util.Locale
import org.junit.Rule
import org.junit.Test

class TurboOptionsAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun englishOptions() = verify("en", "Every 30 seconds", "Every 1 minute", "Session: 1 hour")

    @Test fun portugueseOptions() = verify("pt", "A cada 30 segundos", "A cada 1 minuto", "Sessão: 1 hora")

    @Test fun spanishOptions() = verify("es", "Cada 30 segundos", "Cada 1 minuto", "Sesión: 1 hora")

    @Test fun italianOptions() = verify("it", "Ogni 30 secondi", "Ogni 1 minuto", "Sessione: 1 ora")

    private fun verify(language: String, initial: String, minute: String, hour: String) {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag(language))
            var interval by mutableIntStateOf(30)
            var duration by mutableIntStateOf(180)
            compose.setContent {
                ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                    Box(Modifier.size(320.dp, 650.dp).verticalScroll(rememberScrollState())) {
                        TurboHero(QuickMonitorState(), interval, { interval = it }, duration, { duration = it })
                    }
                }
            }
            compose.onNodeWithContentDescription(initial).assertIsSelected()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            compose.onNodeWithContentDescription(minute).performScrollTo().performClick().assertIsSelected()
            compose.onNodeWithContentDescription(hour).performScrollTo().performClick().assertIsSelected()
            // Compact labels are visual only; they must not be announced in addition to full names.
            compose.onNodeWithText("30s").assertDoesNotExist()
            compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)).assertCountEquals(8)
        } finally {
            Locale.setDefault(previous)
        }
    }
}
