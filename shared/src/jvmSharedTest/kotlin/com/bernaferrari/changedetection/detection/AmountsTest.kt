package com.bernaferrari.changedetection.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountsTest {
    private fun value(text: String) = amounts(text).single().value

    @Test fun plainNumbersAreRead() {
        assertEquals(42.0, value("42"), 0.0)
        assertEquals(19.99, value("19.99"), 0.0)
        assertEquals(12.5, value("12,5"), 0.0)
        assertEquals(0.999, value("0,999"), 0.0)
    }

    @Test fun groupingAndDecimalMarksAreToldApart() {
        assertEquals(1234.56, value("1,234.56"), 0.0)
        assertEquals(1234.56, value("1.234,56"), 0.0)
        assertEquals(1234.56, value("1 234,56"), 0.0)
        assertEquals(1234.56, value("1\u00A0234,56"), 0.0)
        assertEquals(1234.5, value("1'234.50"), 0.0)
        assertEquals(1234567.0, value("1.234.567"), 0.0)
        assertEquals(1234567.0, value("1,234,567"), 0.0)
        assertEquals(100000.0, value("1,00,000"), 0.0)
    }

    @Test fun aLoneGroupOfThreeDigitsIsReadAsThousands() {
        // "1,234" and "1.234" cannot be told apart without knowing the page's language; whole thousands are far more common in prices.
        assertEquals(1234.0, value("1,234"), 0.0)
        assertEquals(1234.0, value("1.234"), 0.0)
    }

    @Test fun currencyIsRecognisedBeforeOrAfterTheNumber() {
        assertEquals(Amount(1234.56, "R$", "R$ 1.234,56"), amounts("Por R$ 1.234,56 à vista").single())
        assertEquals(Amount(499.0, "€", "499 €"), amounts("Preis: 499 € inkl. MwSt.").single())
        assertEquals(Amount(19.99, "$", "$19.99"), amounts("Now $19.99").single())
        assertEquals(Amount(1299.0, "USD", "USD 1,299"), amounts("USD 1,299").single())
    }

    @Test fun everyNumberInAPassageIsFoundInOrder() {
        assertEquals(listOf(76.0, 12.0, 3.0), amounts("76 points · 12 comments · 3 hours ago").map { it.value })
        assertEquals(listOf(5.0, 10.0), amounts("5 of 10").map { it.value })
    }

    @Test fun pricesAreSeparatedFromOtherNumbers() {
        val found = amounts("Model 3000 · was $899.00 · now $749.00 · 214 reviews")
        assertEquals(listOf(899.0, 749.0), found.filter { it.currency != null }.map { it.value })
    }

    @Test fun textWithoutNumbersHasNoAmounts() {
        assertTrue(amounts("In stock").isEmpty())
        assertTrue(amounts("").isEmpty())
    }

    @Test fun malformedTokensAreNotGuessedAt() {
        assertNull(parseAmount("1,2,3"))
        assertNull(parseAmount(""))
        assertNull(parseAmount("."))
    }

    @Test fun aUsStartingPrice() {
        assertEquals(899.0, amounts("Buy from \$899").last { it.currency != null }.value, 0.0)
    }

    @Test fun aUsPriceWithCents() {
        assertEquals(899.0, amounts("From \$899.00").last { it.currency != null }.value, 0.0)
    }

    @Test fun aUkStartingPrice() {
        assertEquals(1199.0, amounts("From £1,199").last { it.currency != null }.value, 0.0)
    }

    @Test fun aGermanStartingPrice() {
        assertEquals(1449.0, amounts("Ab 1.449 €").last { it.currency != null }.value, 0.0)
    }

    @Test fun aFrenchShelfPrice() {
        assertEquals(49.99, amounts("Prix 49,99€").last { it.currency != null }.value, 0.0)
    }

    @Test fun aBrazilianStartingPrice() {
        assertEquals(11999.0, amounts("A partir de R\$ 11.999").last { it.currency != null }.value, 0.0)
    }

    @Test fun aBrazilianPriceAfterInstalments() {
        assertEquals(10499.0, amounts("R\$ 874,92/mês ou R\$ 10.499").last { it.currency != null }.value, 0.0)
    }

    @Test fun aSwissPriceWithNoCents() {
        assertEquals(1199.0, amounts("Ab CHF 1199.–").last { it.currency != null }.value, 0.0)
    }

    @Test fun anIndianPriceWithRupees() {
        assertEquals(164900.0, amounts("₹164900.00").last { it.currency != null }.value, 0.0)
    }

    @Test fun aJapanesePriceWithYen() {
        assertEquals(8999.0, amounts("¥8,999価格 ¥ 8999").last { it.currency != null }.value, 0.0)
    }

    @Test fun aSpanishTradeInRange() {
        assertEquals(1020.0, amounts("entre 150 € y 1.020 €").last { it.currency != null }.value, 0.0)
    }

    @Test fun anItalianTradeInRange() {
        assertEquals(1020.0, amounts("da € 150 a € 1.020").last { it.currency != null }.value, 0.0)
    }
}
