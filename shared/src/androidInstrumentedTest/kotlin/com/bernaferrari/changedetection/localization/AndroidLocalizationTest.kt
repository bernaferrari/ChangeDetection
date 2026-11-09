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
        val titles = mapOf(
            "en" to "⚡ Turbo active",
            "es" to "⚡ Turbo activo",
            "it" to "⚡ Turbo attivo",
            "pt" to "⚡ Turbo ativo",
            "de" to "⚡ Turbo aktiv",
            "hi" to "⚡ टर्बो चालू",
            "bn" to "⚡ টার্বো চালু",
            "zh-Hans" to "⚡ Turbo 已开启",
            "fr" to "⚡ Turbo actif",
            "id" to "⚡ Turbo aktif",
            "ar" to "⚡ Turbo قيد التشغيل",
            "ru" to "⚡ Turbo включён",
        )
        for ((language, title) in titles) {
            val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
            val resources = context.createConfigurationContext(configuration).resources
            assertEquals(language, title, resources.getString(R.string.quick_monitor_notification_title))
            for (count in listOf(0, 1, 2, 5, 100)) {
                val text = resources.getQuantityString(R.plurals.quick_monitor_notification_subtitle, count, count, 15)
                assertTrue(text.isNotBlank())
                assertFalse(text.contains("%1$"))
                assertFalse(text.contains("%2$"))
            }
        }
    }
}
