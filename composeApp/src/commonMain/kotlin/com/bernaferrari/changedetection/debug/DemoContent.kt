/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.debug

import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.util.FileStorage

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE

/** Realistic text pages with saved history, for screenshots and manual design review. Never used in release flows. */
suspend fun seedDemoContent(db: ChangeDatabase, files: FileStorage, now: Long) {
    val sites = db.siteDao()
    val snaps = db.snapsDao()
    if (sites.getSitesList().isNotEmpty()) return
    suspend fun page(id: String, title: String, url: String, configure: (Site) -> Site = { it }) {
        val base = Site(title, url, now - 4 * MINUTE, "", id, 0 to 0)
            .copy(captureMode = com.bernaferrari.changedetection.repo.CaptureMode.TEXT, isSuccessful = true, lastSuccessfulCheck = now - 4 * MINUTE, isRead = true)
        sites.insertSite(configure(base))
    }
    suspend fun version(siteId: String, at: Long, html: String) {
        val id = "$siteId-$at"
        val bytes = html.encodeToByteArray()
        files.writeBytes(id, bytes)
        snaps.insertSnap(Snap(id, siteId, at, "text/html", "UTF-8", bytes.size))
    }

    page("fellowship", "Fellowship guide", "https://opencall.example/fellowship") { it.copy(isRead = false) }
    version("fellowship", now - 2 * 24 * HOUR, fellowship("31 October", "€18,000"))
    version("fellowship", now - 2 * HOUR, fellowship("14 November", "€20,000"))

    page("apartments", "Apartment listings", "https://homes.example/rent?city=lisbon") { it.copy(isRead = false) }
    version("apartments", now - 5 * HOUR, listings(0))
    version("apartments", now - 11 * MINUTE, listings(7))

    page("price", "Studio Display", "https://store.example/display") { it.copy(isRead = false) }
    version("price", now - 3 * 24 * HOUR, product("$1,599.00", "Ships in 2–3 weeks"))
    version("price", now - 38 * MINUTE, product("$1,349.00", "In stock · free delivery"))

    page("hn", "Hacker News", "https://news.ycombinator.com") { it.copy(isRead = false) }
    version("hn", now - 3 * HOUR, frontPage(0))
    version("hn", now - 7 * MINUTE, frontPage(1))

    page("exam", "Exam results", "https://campus.example/results") {
        it.copy(isSuccessful = false, lastFailureCode = "SERVER", timestamp = now - 6 * MINUTE, lastSuccessfulCheck = now - 2 * HOUR)
    }
    version("exam", now - 3 * 24 * HOUR, "<html><body><h1>Results</h1><p>Results will be published at 12:00.</p></body></html>")

    page("conference", "Conference program", "https://offscript.example/program")
    version("conference", now - 9 * 24 * HOUR, "<html><body><h1>The program</h1><p>Keynote: to be announced</p></body></html>")
    version("conference", now - 26 * HOUR, "<html><body><h1>The program</h1><p>Keynote: Ana Ruiz</p></body></html>")

    page("release", "Release notes", "https://fieldnotes.example/changelog")
    version("release", now - 12 * 24 * HOUR, "<html><body><h2>3.1</h2><p>Bug fixes.</p></body></html>")
    version("release", now - 3 * 24 * HOUR, "<html><body><h2>3.2</h2><p>Offline mode.</p><h2>3.1</h2><p>Bug fixes.</p></body></html>")

    page("roles", "Design roles", "https://forma.example/jobs")
    version("roles", now - 6 * 24 * HOUR, "<html><body><h1>Open roles</h1><p>No open roles right now.</p></body></html>")

    page("poster", "Event poster", "https://studiogrid.example/poster") { it.copy(isSyncEnabled = false) }
    version("poster", now - 20 * 24 * HOUR, "<html><body><h1>Poster</h1></body></html>")
}

private fun fellowship(deadline: String, amount: String) = """
    <html><body><main>
    <h1>Open Call Fellowship 2027</h1>
    <p>Applications are open to independent researchers, designers and writers working on public-interest projects.</p>
    <h2>Key dates</h2>
    <ul><li>Applications close $deadline, 23:59 CET</li><li>Interviews in January</li></ul>
    <h2>Funding</h2>
    <p>Each fellow receives $amount over six months, plus a travel budget.</p>
    <h2>Eligibility</h2>
    <p>Open to anyone, anywhere. Previous fellows may apply again after two years.</p>
    </main></body></html>
""".trimIndent()

private fun product(price: String, stock: String) = """
    <html><body><main>
    <h1>Studio Display</h1>
    <p>Standard glass · Tilt-adjustable stand</p>
    <h2>Price</h2><p>$price</p>
    <h2>Availability</h2><p>$stock</p>
    </main></body></html>
""".trimIndent()

private fun listings(seed: Int): String {
    val streets = listOf("Rua Augusta", "Avenida da Liberdade", "Rua do Carmo", "Travessa do Fala-Só", "Rua da Rosa", "Calçada do Combro", "Rua de São Bento", "Largo do Rato", "Rua Garrett", "Rua dos Fanqueiros", "Rua Nova do Almada", "Rua da Prata")
    val items = (0 until 12).joinToString("") { i ->
        val n = (i + seed) % streets.size
        "<li><h3>${streets[n]} ${10 + n}</h3><p>T${1 + (n % 3)} · ${900 + n * 75} € / month · ${40 + n * 6} m²</p></li>"
    }
    return "<html><body><h1>Rentals in Lisbon</h1><ul>$items</ul></body></html>"
}

/** A busy front page: counts and times move on every check, a few stories are genuinely new. */
private fun frontPage(round: Int): String {
    val stories = listOf(
        "Show HN: Quake ported to safe Rust, playable in the browser",
        "The unreasonable effectiveness of plain text",
        "Why we moved our database back to a single server",
        "A visual guide to how SQLite stores your data",
        "Ask HN: What are you working on this month?",
        "The quiet death of the RSS reader, and its comeback",
        "Designing calm notifications",
        "How a 1970s typesetting trick fixed our layout bug",
        "Launch HN: A tiny search engine for your own notes",
        "Reverse-engineering the cheapest e-ink display",
        "What the Apollo guidance computer can teach web developers",
        "Show HN: I built a weather app that only uses words",
    )
    val fresh = listOf("Show HN: Change Detection, a calm way to watch any web page", "Europe's new right-to-repair rules take effect today")
    val list = if (round == 0) stories else fresh + stories.dropLast(2)
    val items = list.mapIndexed { i, title ->
        val points = 40 + (list.size - i) * 17 + round * 9
        val comments = 8 + (list.size - i) * 5 + round * 4
        val age = if (round == 1 && i < 2) "12 minutes ago" else "${i + 1 + round * 2} hours ago"
        "<tr><td>${i + 1}.</td><td><a>$title</a></td></tr><tr><td></td><td>$points points by user$i $age | hide | $comments comments</td></tr>"
    }.joinToString("")
    return "<html><head><title>Hacker News</title></head><body><table>$items</table></body></html>"
}
