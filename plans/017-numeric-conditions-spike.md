# Plan 017: Spike: can the app read "the number" on a page well enough to alert on a condition?

> **Executor instructions**
> This is a **spike**: a small, time-boxed investigation that ends with tested code and a written answer, not with a feature. You have not seen the audit or the conversation that produced this plan; everything you need is in this file.
> 1. Run the drift check: `git diff --stat a9f8f8ba..HEAD -- shared/src/commonMain/kotlin/com/bernaferrari/changedetection/preview/ChangePreview.kt shared/src/commonMain/kotlin/com/bernaferrari/changedetection/detection/`. Changes there do not block this plan; it adds new files only.
> 2. Follow the steps in order and run each **Verify** command.
> 3. Touch only the files listed under "In scope". **Do not** change the database, the `Site` class, any screen, or any string file.
> 4. Stop at the time box (one working day). Write down what you found even if the answer is "no".
> 5. When finished, set this plan's row in `plans/README.md` to `DONE` and add one line there with the answer.

## Status

| Field | Value |
| --- | --- |
| Priority | Direction (explore before committing) |
| Effort | S (one day, time-boxed) |
| Risk | None to users: new, unused code and a document |
| Depends on | Nothing |
| Category | Research spike |
| Planned at | commit `a9f8f8ba`, 2026-11-09 |

## Why this matters

Today the app answers "did this page change?". The most common thing people want from a page watcher is narrower: "tell me when the price drops below 500", "tell me when it is back in stock", "tell me when the number goes up". The app already has most of what that needs:

- A page can be narrowed to one part with a selector (`Site.textIncludeSelector`, `Site.textExcludeSelector`; `detection/TextRegion.kt`).
- The change reader already separates "only the numbers moved" from real edits (`TextDigest.numberUpdates` in `preview/ChangePreview.kt:241-251`) and already knows a price from a counter (`currency` regex, `ChangePreview.kt:232`).

What nobody knows yet is whether a number can be read out of real page text reliably enough to compare it with a threshold: prices are written `1,234.56`, `1.234,56`, `1 234,56`, `R$ 1.234,56`, `1'234.50`. If reading fails one time in ten, a "below 500" alert is worse than no alert. This spike answers that with code and tests, **before** anyone designs a screen or changes the database.

## Current state

`shared/src/commonMain/kotlin/com/bernaferrari/changedetection/preview/ChangePreview.kt:217,232`:

```kotlin
private val digits = Regex("\\d+([.,:]\\d+)*")
...
private val currency = Regex("[$€£¥₹₩₽]|R\\$|\\b(USD|EUR|GBP|BRL|JPY|CHF|CAD|AUD)\\b")
```

These only recognise and mask numbers; nothing turns text into a value. `rg -n "toDouble|toBigDecimal" shared/src/commonMain/kotlin/com/bernaferrari/changedetection/preview shared/src/commonMain/kotlin/com/bernaferrari/changedetection/detection` prints nothing.

Pure-logic tests that run on both the Android host JVM and the desktop JVM live in `shared/src/jvmSharedTest/kotlin/com/bernaferrari/changedetection/`; the exemplar is `preview/ChangePreviewTest.kt` (JUnit 4, `org.junit.Assert.*`, test names in camelCase sentences).

## Commands you will need

| Purpose | Command | Expected |
| --- | --- | --- |
| The spike's tests | `./gradlew :shared:testAndroidHostTest --tests '*AmountsTest'` | `BUILD SUCCESSFUL` |
| Format | `./gradlew :shared:spotlessApply` then `./gradlew :shared:spotlessCheck` | `BUILD SUCCESSFUL` |
| Full gate | `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` | `BUILD SUCCESSFUL` |
| Other platforms compile | `./gradlew :webDemo:wasmJsBrowserDistribution` | `BUILD SUCCESSFUL` |

## Before you start: the state of the build

Checked on 2026-11-09 at commit `a9f8f8ba`. Read this before the steps; it overrides any line below that expects otherwise.

1. **Plan 000 must be done first.** Before you change anything, run `./gradlew :shared:spotlessCheck`. If it fails, Plan 000 (`plans/000-make-the-gates-green.md`) has not landed: STOP and report that. Do not run `spotlessApply` to get past it; on an unprepared tree it rewrites about 36 unrelated files. Once Plan 000 has landed, the test and format commands in this plan behave as described, and `spotlessApply` only touches what you changed.
2. **Two commands fail for reasons no plan fixes.** `./gradlew :shared:compileKotlinIosSimulatorArm64` stops with five errors, all in `shared/src/iosMain/kotlin/com/bernaferrari/changedetection/browser/IosPageCapture.kt` (the first is `Unresolved reference 'kmpwright'`; a library was built with a newer Kotlin than this project uses). `./gradlew :webDemo:wasmJsBrowserDistribution` compiles the Kotlin and then stops at `:kotlinWasmStoreYarnLock` with `Lock file was changed`. Wherever this plan expects one of them to print `BUILD SUCCESSFUL`, the bar is instead: for iOS, **those same five errors and no others**; for the web demo, run `./gradlew :webDemo:compileKotlinWasmJs` and expect `BUILD SUCCESSFUL`. Never run `kotlinWasmUpgradeYarnLock`. If either command has since been repaired and passes, that is fine.
3. **Line numbers may have moved.** Two changes landed after this plan was written. Plan 000 re-formats about 36 files (layout only). The "failure experience" change (`plans/failure-experience-changes.md`) added code to `SyncEngine.kt`, `SitesDao.kt`, `Site.kt`, `CheckFailure.kt`, `DiagnosticTrace.kt`, `PageDetailScreen.kt`, `MainViewModel.kt`, `LegacyMigration.kt`, `DatabaseFactory.kt`, `AppContent.kt`, `AndroidChangeNotifier.kt`, the four `PlatformDiModule` files and the string files. Find code by the text this plan quotes, not by line number. A quoted block that differs only in line breaks or indentation is the same code. The drift check at the top of this plan will list those files as changed; that alone is not a reason to stop. Stop only if the quoted code is gone or does something different.

## Scope

**In scope (all new):**

- `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/detection/Amounts.kt`
- `shared/src/jvmSharedTest/kotlin/com/bernaferrari/changedetection/detection/AmountsTest.kt`
- `docs/research/numeric-conditions.md`
- `plans/README.md` (status and one-line answer)

**Out of scope:** database columns, `Site`, settings, screens, strings, notifications, the sync engine. If the answer is "yes, build it", that is a new plan written from this spike's findings.

## Git workflow

- Branch from `main`: `git switch -c advisor/017-numeric-conditions-spike`
- Commit subject, for example: `Amounts on a page can be read as numbers, with the limits written down`
- Do not push, merge, or open a pull request.

## Steps

### Step 1: Write the expectations first

Create `shared/src/jvmSharedTest/kotlin/com/bernaferrari/changedetection/detection/AmountsTest.kt`:

```kotlin
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
}
```

**Verify:** `./gradlew :shared:testAndroidHostTest --tests '*AmountsTest'` fails to compile (unresolved `amounts`). Expected.

### Step 2: A first implementation

Create `shared/src/commonMain/kotlin/com/bernaferrari/changedetection/detection/Amounts.kt`:

```kotlin
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
```

This is a starting point written without compiling. Make the tests in Step 1 pass by changing **the implementation**, not the expectations, with one exception: if an expectation in Step 1 turns out to be impossible to satisfy together with the others, keep both behaviours visible by recording the conflict in the findings (Step 4) and adjust that single test with a comment saying why.

**Verify:** `./gradlew :shared:testAndroidHostTest --tests '*AmountsTest'` passes.

### Step 3: Try it on real page text

Collect the visible text of **at least twelve real pages** in at least five languages and currency styles (suggested mix: two US shops, one UK, one German, one French, two Brazilian, one Swiss, one Indian, one Japanese, a stock quote page, a "items left in stock" page, a news ranking with points and times). For each, copy the few lines around the price or number a person would want to watch, as a narrow selector would give them.

Add them to `AmountsTest` as one test per page, named for what it is (not for the brand), for example `aGermanShopPriceWithTaxNote`, `aBrazilianPriceWithInstalments`. Each asserts the amount a person would mean. Where the first implementation gets it wrong, improve it if a general rule fixes it; if only a special case would, leave the test in place with `@org.junit.Ignore("reason")` and record it in the findings. Do not copy more page text than those few lines, and no personal data.

**Verify:** `./gradlew :shared:testAndroidHostTest --tests '*AmountsTest'` passes (ignored tests are reported as skipped).

### Step 4: Write the answer down

Create `docs/research/numeric-conditions.md` with these headings and real content under each:

1. **Question.** One sentence.
2. **Answer.** Yes / yes with limits / no, in one paragraph a non-programmer can read.
3. **What was tried.** The number of real samples, the languages and currency styles covered.
4. **What works.** With counts ("11 of 12 samples read correctly").
5. **What does not.** Every ignored test and every ambiguity (for example the lone `1.234`), with the sample text.
6. **Which number is "the" number.** When a watched region holds several numbers (old price, new price, instalments, review count), which rule picks the right one, and how often that rule held on the samples. Candidate rules to evaluate: the first amount with a currency; the last amount with a currency; the only amount with a currency; ask the person to tap the number once and remember its position.
7. **What a feature would need.** Only as a list for a future plan: the per-page fields (condition kind, threshold), where the check would run (after the text is extracted, before the alert decision), what the alert would say, and what happens when the number cannot be read (the suggestion: fall back to a normal change alert and say so, never stay silent).
8. **Recommendation.** Build / build narrower / do not build, with the reason.

**Verify:** `./gradlew :shared:spotlessApply` then `./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest` and `./gradlew :webDemo:wasmJsBrowserDistribution`
Expected: `BUILD SUCCESSFUL`.

## Done criteria

- [ ] `AmountsTest` passes, with at least twelve tests built from real page text.
- [ ] `docs/research/numeric-conditions.md` exists with all eight headings filled in, including a plain yes/no recommendation.
- [ ] `rg -n "amounts\(|parseAmount\(" shared/src/commonMain --glob '!**/detection/Amounts.kt'` prints nothing (the spike is not wired into the app).
- [ ] `git status --short` lists only in-scope files.
- [ ] `plans/README.md` carries the one-line answer.

## STOP conditions

- You are about to add a database column, a setting, a string or a screen. That is the follow-up plan, not this one.
- The time box (one working day) is reached. Write the findings with what you have.
- You cannot reach real pages to collect samples. Do Steps 1 and 2, write the findings with "no real samples collected", and leave the status `IN PROGRESS`.

## Maintenance notes

- `Amounts.kt` is deliberately unused by the app when this plan ends. If the recommendation is "do not build", delete it together with its test in the same change that records the decision; do not leave it as dead code.
- If the recommendation is "build", the follow-up plan should start from heading 7 of the findings and treat "the number could not be read" as a first-class state the person can see.
