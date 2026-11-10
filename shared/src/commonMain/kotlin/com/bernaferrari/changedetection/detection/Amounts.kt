package com.bernaferrari.changedetection.detection

/** A number found in page text, with the currency written next to it (if any) and the exact text it came from. */
data class Amount(val value: Double, val currency: String?, val text: String)

private const val CURRENCY = "R\\$|[$€£¥₹₩₽]|\\b(?:USD|EUR|GBP|BRL|JPY|CHF|CAD|AUD)\\b"

/** Space-like group marks only count when exactly three digits follow, so "5 10" stays two numbers. */
private const val NUMBER = "\\d{1,3}(?:[ \u00A0\u202F']\\d{3})+(?:[.,]\\d+)?|\\d+(?:[.,]\\d+)*"

private val amountPattern = Regex("(?:($CURRENCY)[ \u00A0]?)?($NUMBER)(?:[ \u00A0]?($CURRENCY))?")

/** Every readable number in [text], in order. Tokens that cannot be read with confidence are left out. */
fun amounts(text: String): List<Amount> = amountPattern.findAll(text).mapNotNull { match ->
    val value = parseAmount(match.groupValues[2]) ?: return@mapNotNull null
    val currency = match.groupValues[1].ifEmpty { match.groupValues[3] }.ifEmpty { null }
    Amount(value, currency, match.value.trim())
}.toList()

/**
 * Reads one written number without knowing the page's language. The last of two different marks is the decimal
 * mark; a mark that repeats is a grouping mark; a single mark followed by exactly three digits is read as thousands.
 */
fun parseAmount(token: String): Double? {
    val compact = token.filterNot { it == ' ' || it == '\u00A0' || it == '\u202F' || it == '\'' }
    if (compact.isEmpty() || !compact.first().isDigit() || !compact.last().isDigit()) return null
    val dots = compact.count { it == '.' }
    val commas = compact.count { it == ',' }
    val normalized = when {
        dots == 0 && commas == 0 -> compact

        dots > 0 && commas > 0 -> {
            val decimal = if (compact.lastIndexOf('.') > compact.lastIndexOf(',')) '.' else ','
            val grouping = if (decimal == '.') ',' else '.'
            // The decimal mark appears once and after every grouping mark; anything else is not a number we understand.
            if (compact.count { it == decimal } != 1) return null
            compact.replace(grouping.toString(), "").replace(decimal, '.')
        }

        else -> {
            val mark = if (dots > 0) '.' else ','
            val groups = compact.split(mark)
            when {
                groups.size > 2 -> if (groups.drop(1).all { it.length == 3 || it.length == 2 } && groups.last().length == 3) groups.joinToString("") else return null
                groups[1].length == 3 && groups[0] != "0" && groups[0].length <= 3 -> groups.joinToString("")
                else -> groups.joinToString(".")
            }
        }
    }
    return normalized.toDoubleOrNull()
}
