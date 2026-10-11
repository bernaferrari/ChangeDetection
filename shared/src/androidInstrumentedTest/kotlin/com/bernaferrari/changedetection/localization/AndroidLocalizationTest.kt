/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.localization

import android.content.res.Configuration
import androidx.test.platform.app.InstrumentationRegistry
import com.bernaferrari.changedetection.library.R
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class AndroidLocalizationTest {
    @Test fun localizedNotificationsAndWidgetsResolveForEverySupportedLanguage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val languages = listOf("en", "es", "it", "pt", "de", "hi", "bn", "zh-Hans")
        val expected = listOf("⚡ Turbo active", "⚡ Turbo activo", "⚡ Turbo attivo", "⚡ Turbo ativo", "⚡ Turbo aktiv", "⚡ टर्बो चालू", "⚡ টার্বো চালু", "⚡ Turbo 已开启")
        for ((index, language) in languages.withIndex()) {
            val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
            val resources = context.createConfigurationContext(configuration).resources
            assertEquals(language, expected[index], resources.getString(R.string.quick_monitor_notification_title))
            for (count in listOf(0, 1, 2, 100)) {
                val text = resources.getQuantityString(R.plurals.quick_monitor_notification_subtitle, count, count, 15)
                assertTrue(text.isNotBlank())
                assertFalse(text.contains("%1$"))
                assertFalse(text.contains("%2$"))
            }
        }
    }
}
