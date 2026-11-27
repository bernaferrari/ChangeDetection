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
            "en" to "⚡ Quick Monitor active",
            "es" to "⚡ Monitorización rápida activa",
            "it" to "⚡ Monitoraggio rapido attivo",
            "pt" to "⚡ Monitoramento rápido ativo",
            "de" to "⚡ Schnellüberwachung aktiv",
            "hi" to "⚡ तेज़ निगरानी चालू",
            "bn" to "⚡ দ্রুত নজরদারি চালু",
            "zh-Hans" to "⚡ 快速监控已开启",
            "fr" to "⚡ Surveillance rapide active",
            "id" to "⚡ Pemantauan cepat aktif",
            "ar" to "⚡ المراقبة السريعة قيد التشغيل",
            "ru" to "⚡ Быстрая проверка включена",
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
