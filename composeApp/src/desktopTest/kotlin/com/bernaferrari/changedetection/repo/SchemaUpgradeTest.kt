/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo

import androidx.room3.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.getRoomDatabase
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Every schema ever shipped (v3 onwards) upgrades to the current one with its pages and history intact.
 * Room validates the final tables and indexes on open, so a migration that drifts from the entities fails here.
 */
class SchemaUpgradeTest {
    private val schemas = File("schemas/com.bernaferrari.changedetection.repo.source.local.ChangeDatabase")

    @Test fun everyShippedSchemaUpgradesAndKeepsPagesAndHistory() = runBlocking {
        val versions = schemas.listFiles().orEmpty().mapNotNull { it.nameWithoutExtension.toIntOrNull() }.sorted()
        val current = versions.last()
        versions.dropLast(1).forEach { version -> upgradeFrom(version, current) }
    }

    @Test fun historyQueriesUseTheirIndexWithoutSorting() = runBlocking {
        val directory = Files.createTempDirectory("schema-plan").toFile()
        val path = directory.resolve("test.db").path
        val driver = BundledSQLiteDriver()
        try {
            getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = path), driver).apply {
                siteDao().getSitesList()
                close()
            }
            driver.open(path).use { connection ->
                val plan = connection.queryAll("EXPLAIN QUERY PLAN SELECT * FROM snaps WHERE siteId = 'page' ORDER BY timestamp DESC, snapId DESC LIMIT 1")
                assertTrue(plan, "index_snaps_siteId_timestamp_snapId" in plan)
                assertFalse(plan, "TEMP B-TREE" in plan)
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    private suspend fun upgradeFrom(version: Int, current: Int) {
        val directory = Files.createTempDirectory("schema-$version").toFile()
        val path = directory.resolve("test.db").path
        val driver = BundledSQLiteDriver()
        try {
            val schema = Json.parseToJsonElement(schemas.resolve("$version.json").readText()).jsonObject.getValue("database").jsonObject
            driver.open(path).use { connection ->
                schema.getValue("entities").jsonArray.forEach { node ->
                    val entity = node.jsonObject
                    val table = entity.getValue("tableName").jsonPrimitive.content
                    connection.exec(entity.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                    entity["indices"]?.jsonArray?.forEach { connection.exec(it.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table)) }
                }
                schema.getValue("setupQueries").jsonArray.forEach { connection.exec(it.jsonPrimitive.content) }
                connection.insertRow(schema, "sites", mapOf("siteId" to "'page'", "url" to "'https://example.test'", "title" to "'Legacy'", "timestamp" to "123", "colors" to "'0,0'", "constraints" to "'false,false,false,false'", "notes" to "''"))
                connection.insertRow(schema, "snaps", mapOf("snapId" to "'saved'", "siteId" to "'page'", "timestamp" to "123", "contentType" to "'image/png'", "contentCharset" to "''", "contentSize" to "42"))
                connection.exec("PRAGMA user_version = $version")
            }
            val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = path), driver)
            try {
                val site = requireNotNull(db.siteDao().getSiteById("page")) { "v$version lost its page" }
                assertEquals("v$version", "Legacy", site.title)
                assertEquals("v$version", "", site.iconUrl)
                assertEquals("v$version", "saved", db.snapsDao().getLastSnapForSiteId("page")?.snapId)
                db.siteDao().updateIcon("page", "https://example.test/icon.png")
                assertEquals("v$version", "https://example.test/icon.png", db.siteDao().getSiteById("page")?.iconUrl)
            } finally {
                db.close()
            }
            driver.open(path).use { assertEquals("v$version", current.toString(), it.queryAll("PRAGMA user_version").trim()) }
        } finally {
            directory.deleteRecursively()
        }
    }

    /** Inserts a row using the given values, filling any other required column with a neutral value. */
    private fun SQLiteConnection.insertRow(schema: JsonObject, table: String, values: Map<String, String>) {
        val entity = schema.getValue("entities").jsonArray.map { it.jsonObject }.first { it.getValue("tableName").jsonPrimitive.content == table }
        val columns = entity.getValue("fields").jsonArray.map { it.jsonObject }.mapNotNull { field ->
            val name = field.getValue("columnName").jsonPrimitive.content
            val value = values[name] ?: if (field["notNull"]?.jsonPrimitive?.boolean == true && field["defaultValue"] == null) {
                if (field.getValue("affinity").jsonPrimitive.content == "TEXT") "''" else "0"
            } else {
                null
            }
            value?.let { name to it }
        }
        exec("INSERT INTO $table (${columns.joinToString { it.first }}) VALUES (${columns.joinToString { it.second }})")
    }

    private fun SQLiteConnection.exec(sql: String) = prepare(sql).use { it.step() }

    private fun SQLiteConnection.queryAll(sql: String): String = prepare(sql).use { statement ->
        buildString {
            while (statement.step()) {
                for (i in 0 until statement.getColumnCount()) append(statement.getText(i)).append(' ')
                append('\n')
            }
        }
    }
}
