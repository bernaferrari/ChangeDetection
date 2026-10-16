package com.bernaferrari.changedetection.localization

import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.util.*
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.*
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/** Checks shipped translations, not a mock resource provider. */
@OptIn(ExperimentalResourceApi::class)
class LocalizationTest {
    private val languages = listOf("en", "es", "it", "pt", "de", "hi", "bn", "zh")
    private val sharedRoot = File("src/commonMain/composeResources")
    private val nativeRoot = File("src/androidMain/res")
    private val placeholders = Regex("%[1-9]\\$[dsf]")

    @Test fun `every supported language has all keys, arguments, array items and plural fallbacks`() {
        for (root in listOf(sharedRoot, nativeRoot)) {
            val source = resources(File(root, "values"))
            assertTrue("Resource source exists: $root", source.isNotEmpty())
            for (language in languages.drop(1)) {
                val localized = resources(File(root, "values-$language"))
                assertEquals("$root/$language keys", source.keys, localized.keys)
                for ((name, original) in source) {
                    val translated = localized.getValue(name)
                    assertEquals("$language/$name type", original.tagName, translated.tagName)
                    val originalItems = items(original)
                    val translatedItems = items(translated)
                    if (original.tagName == "string-array") assertEquals("$language/$name length", originalItems.size, translatedItems.size)
                    if (original.tagName == "plurals") assertTrue("$language/$name needs other", translatedItems.any { it.getAttribute("quantity") == "other" })
                    translatedItems.forEachIndexed { index, item ->
                        assertTrue("$language/$name is not empty", item.textContent.isNotBlank())
                        val reference = if (original.tagName == "plurals") originalItems.first { it.getAttribute("quantity") == item.getAttribute("quantity") } else originalItems[index]
                        assertEquals("$language/$name arguments", arguments(reference.textContent), arguments(item.textContent))
                    }
                }
            }
        }
    }

    @Test fun `switching locales resolves every compiled string, plural and array without English fallback`() = runTest {
        val previous = Locale.getDefault()
        val watchLabels = listOf("Watch this page", "Vigilar esta página", "Monitora questa pagina", "Monitorar esta página", "Diese Seite beobachten", "इस पेज की निगरानी करें", "এই পৃষ্ঠায় নজর রাখুন", "关注此页面")
        try {
            for ((index, language) in languages.withIndex()) {
                Locale.setDefault(Locale.forLanguageTag(language))
                assertEquals("$language is selected", watchLabels[index], getString(Res.string.watch_this_page))
                for ((name, resource) in Res.allStringResources) {
                    val text = getString(resource, 41, 42, 43, 44)
                    assertTrue("$language/$name loaded", text.isNotBlank())
                    assertFalse("$language/$name has unresolved arguments", placeholders.containsMatchIn(text))
                }
                for ((name, resource) in Res.allPluralStringResources) {
                    for (count in listOf(0, 1, 2, 5, 100, 1_000_000)) {
                        val text = getPluralString(resource, count, count, 42, 43, 44)
                        assertFalse("$language/$name/$count arguments", placeholders.containsMatchIn(text))
                        assertTrue(text.isNotBlank())
                    }
                }
                assertEquals(7, getStringArray(Res.array.weekday_names).size)
                assertEquals(12, getStringArray(Res.array.month_names).size)
            }
            Locale.setDefault(Locale.CHINESE)
            assertEquals("0 changes uses other in Chinese", "0 处新变化", getPluralString(Res.plurals.home_new_changes, 0, 0))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test fun `display decimals follow locale but stored calendar dates remain ISO`() {
        val previous = Locale.getDefault()
        try {
            for (language in listOf("de", "es", "it", "pt")) {
                Locale.setDefault(Locale.forLanguageTag(language))
                assertEquals(language, "1,25", localizedNumber(1.25, maxFractionDigits = 2))
                assertEquals("1", localizedNumber(1.0))
                assertTrue(formatDate(1_700_000_000_000L).matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
            }
            Locale.setDefault(Locale.forLanguageTag("bn"))
            assertEquals("১.২৫", localizedNumber(1.25, maxFractionDigits = 2))
            Locale.setDefault(Locale.GERMANY)
            assertEquals("1,25 %", localizedPercent(1.25, maxFractionDigits = 2))
            Locale.setDefault(Locale.CHINESE)
            assertEquals("1.25", localizedNumber(1.25, maxFractionDigits = 2))
            assertNotEquals(formatTime(1_700_000_000_000L, true), formatTime(1_700_000_015_000L, true))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test fun `numeric resource arguments use local digits while names and URLs stay intact`() = runTest {
        val previous = Locale.getDefault()
        try {
            for (language in languages) {
                Locale.setDefault(Locale.forLanguageTag(language))
                val number = localizedInteger(12)
                assertTrue(getString(Res.string.comparison_passage_position, 12, 45).contains(number))
                assertTrue(getPluralString(Res.plurals.home_new_changes, 12, 12).contains(number))
                val page = "Studio Display 2026 · https://apple.com/2026"
                assertTrue(getString(Res.string.alert_title, page).contains(page))
                assertTrue(getString(Res.string.home_deleted_many).contains("%1\$d"))
            }
            Locale.setDefault(Locale.GERMANY)
            assertEquals("1,5 KB", com.bernaferrari.changedetection.ui.comparison.formatBytes(1536))
            Locale.setDefault(Locale.forLanguageTag("bn"))
            assertEquals("১২", localizedInteger(12))
            assertEquals("১২ B", com.bernaferrari.changedetection.ui.comparison.formatBytes(12))
            assertFalse(localizedCalendarDate(1_700_000_000_000L).contains("2023"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test fun `localized numeric entry accepts supported keyboard digits without accepting invalid values`() {
        for (text in listOf("15", "१५", "১৫", "１５", "١٥", "۱۵", "၁၅", "༡༥", "๑๕")) assertEquals(text, 15L, parseLocalizedWholeNumber(text))
        assertEquals(0L, parseLocalizedWholeNumber("০"))
        assertEquals(Long.MAX_VALUE, parseLocalizedWholeNumber("9223372036854775807"))
        for (text in listOf("", "-15", "+15", "1.5", "১,৫", "15 min", "9223372036854775808")) assertNull(text, parseLocalizedWholeNumber(text))
    }

    @Test fun `the timing editor round trips numbers formatted by the system locale`() {
        val previous = Locale.getDefault()
        try {
            for (language in listOf("my", "dz", "th-TH-u-nu-thai", "ar", "bn", "hi", "de", "zh")) {
                Locale.setDefault(Locale.forLanguageTag(language))
                for (number in listOf(0L, 15L, 1440L)) assertEquals(language, number, parseLocalizedWholeNumber(localizedInteger(number)))
            }
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test fun `bundled script fonts cover every translated UI glyph`() {
        for (language in listOf("hi", "bn", "zh")) {
            val text = resources(File(sharedRoot, "values-$language")).values.joinToString("") { it.textContent }
            for (weight in listOf("regular", "semibold", "bold")) {
                val font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, File(sharedRoot, "font/locale_${language}_$weight.ttf"))
                text.codePoints().filter { !Character.isISOControl(it) }.distinct().forEach { point ->
                    assertTrue("$language/$weight lacks U+${point.toString(16)}", font.canDisplay(point))
                }
            }
        }
    }

    @Test fun `single lines checks pages and calendar days use independent plural rules`() = runTest {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ENGLISH)
            assertEquals("Numbers changed in 1 line", getPluralString(Res.plurals.digest_numbers, 1, 1))
            assertEquals("Numbers changed in 2 lines", getPluralString(Res.plurals.digest_numbers, 2, 2))
            assertEquals("1 check", getPluralString(Res.plurals.turbo_summary_checks, 1, 1))
            assertEquals("2 checks", getPluralString(Res.plurals.turbo_summary_checks, 2, 2))
            assertEquals("1 page", getPluralString(Res.plurals.turbo_summary_pages, 1, 1))
            assertEquals("2 pages", getPluralString(Res.plurals.turbo_summary_pages, 2, 2))
            for ((changes, days, expected) in listOf(
                Triple(1, 1, "1 change across 1 day in the last 30 days"),
                Triple(2, 1, "2 changes across 1 day in the last 30 days"),
                Triple(2, 2, "2 changes across 2 days in the last 30 days"),
            )) {
                assertEquals(expected, getString(Res.string.detail_calendar_summary,
                    getPluralString(Res.plurals.detail_changes_count, changes, changes),
                    getPluralString(Res.plurals.detail_calendar_days, days, days)))
            }
        } finally {
            Locale.setDefault(previous)
        }
    }

    private fun resources(directory: File): Map<String, Element> {
        val result = linkedMapOf<String, Element>()
        directory.listFiles { file -> file.name.contains("strings") && file.extension == "xml" }?.sortedBy { it.name }?.forEach { file ->
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val nodes = document.documentElement.childNodes
            for (i in 0 until nodes.length) {
                (nodes.item(i) as? Element)?.let { element ->
                    val key = element.getAttribute("name")
                    assertNull("Duplicate $key in $directory", result.put(key, element))
                }
            }
        }
        return result
    }
    private fun items(element: Element): List<Element> = if (element.tagName == "string") listOf(element) else (0 until element.childNodes.length).mapNotNull { element.childNodes.item(it) as? Element }
    private fun arguments(text: String): List<String> = placeholders.findAll(text).map { it.value }.sorted().toList()
}
