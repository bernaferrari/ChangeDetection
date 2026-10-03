/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.getRoomDatabase
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class VisualFilterMigrationTest {
    @Test fun versionSevenUpgradesWithFilteringOffAndRetainsItsHistory() = runBlocking {
        val directory = Files.createTempDirectory("visual-filter-migration").toFile()
        val path = directory.resolve("test.db").path
        val driver = BundledSQLiteDriver()
        try {
            val schema = Json.parseToJsonElement(File("schemas/com.bernaferrari.changedetection.repo.source.local.ChangeDatabase/7.json").readText()).jsonObject.getValue("database").jsonObject
            driver.open(path).use { connection ->
                fun execute(sql: String) {
                    connection.prepare(sql).use { it.step() }
                }
                schema.getValue("entities").jsonArray.forEach { node ->
                    val entity = node.jsonObject
                    val table = entity.getValue("tableName").jsonPrimitive.content
                    execute(entity.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                    entity.getValue("indices").jsonArray.forEach { index -> execute(index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table)) }
                }
                schema.getValue("setupQueries").jsonArray.forEach { execute(it.jsonPrimitive.content) }
                execute("INSERT INTO sites (title,url,timestamp,siteId,isSuccessful,isBrowser,threshold,isRead,isSyncEnabled,isNotificationEnabled,notes,colors,constraints,captureMode) VALUES ('Legacy','https://example.test',123,'page',1,1,512,0,1,1,'','0,0','false,false,false,false','SCREENSHOT')")
                execute("INSERT INTO snaps (snapId,siteId,timestamp,contentType,contentCharset,contentSize,contentHash,processedContentHash) VALUES ('saved','page',123,'image/png','',42,'','')")
                execute("PRAGMA user_version = 7")
            }
            val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = path), driver)
            try {
                val site = requireNotNull(db.siteDao().getSiteById("page"))
                assertEquals(0, site.visualChangeThresholdBps)
                assertEquals(512, site.threshold)
                assertEquals(CaptureMode.SCREENSHOT, site.captureMode)
                assertEquals("saved", db.snapsDao().getLastSnapForSiteId("page")?.snapId)
                db.siteDao().updateSite(site.copy(visualChangeThresholdBps = 100))
            } finally {
                db.close()
            }
            val reopened = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = path), driver)
            try {
                assertEquals(100, reopened.siteDao().getSiteById("page")?.visualChangeThresholdBps)
            } finally {
                reopened.close()
            }
        } finally {
            directory.deleteRecursively()
        }
    }
}
