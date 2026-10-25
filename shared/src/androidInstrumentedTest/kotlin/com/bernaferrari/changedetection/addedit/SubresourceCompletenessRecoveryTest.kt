package com.bernaferrari.changedetection.addedit

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.bernaferrari.changedetection.browser.TestPageServer
import com.bernaferrari.changedetection.browser.untrustedTlsPage
import com.bernaferrari.changedetection.detection.TextRegion
import com.bernaferrari.changedetection.detection.TextRegionException
import com.bernaferrari.changedetection.error.*
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.storage.SnapshotFileCleanup
import com.bernaferrari.changedetection.sync.*
import com.bernaferrari.changedetection.util.AndroidFileStorage
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import io.ktor.client.HttpClient
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class SubresourceCompletenessRecoveryTest {
 @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
 @Test fun invalidVisibleResourceMustNotCreateAChange() = untrustedTlsPage().use { tls -> ImageServer().use { image ->
  val html="<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head><body><p>Unchanged page inventory</p><img src='${image.url}' width='280' height='240'></body></html>"
  TestPageServer(200,html).use { server ->
   compose.setContent { Text("Review fixture") }
   val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
   val task=scope.async { withTimeout(90000) {
    val context=ApplicationProvider.getApplicationContext<Context>();val name="review-${java.util.UUID.randomUUID()}.db"
    val db=getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(context,context.getDatabasePath(name).absolutePath),BundledSQLiteDriver());val files=AndroidFileStorage(context);val client=HttpClient();val alerts=java.util.concurrent.atomic.AtomicInteger()
    val engine=SyncEngine(client,db,db.siteDao(),db.snapsDao(),files,ErrorHandler(),ChangeNotifier { _,_->alerts.incrementAndGet();Unit })
    val site=Site("Review",server.url,0L,"review",ColorGroup(0,0),true,false).copy(captureMode=CaptureMode.SCREENSHOT,isNotificationEnabled=true,visualChangeThresholdBps=100)
    val prefs=preferencesOf(SettingsViewModel.BROWSER_DELAY_KEY to 1000L);val store=object:DataStore<Preferences>{override val data=flowOf(prefs);override suspend fun updateData(transform:suspend(Preferences)->Preferences)=transform(prefs)}
    val repo=SitesRepository(SitesLocalDataSource(AppExecutors(),db.siteDao(),SnapshotFileCleanup(db.pendingFileDeletionDao(),db.snapsDao(),files)))
    val runner=AndroidCheckRunner(repo,engine,AndroidScreenshotSyncer(engine,store,SyncProgressTracker(),browserFactory=AndroidScreenshotBrowserFactory(context)),store);val previewer=AndroidCapturePreviewer(context,engine,store)
    try {
     db.siteDao().insertSite(site);val preview=previewer.capture(server.url,CaptureMode.SCREENSHOT);assertTrue(previewer.keepAsFirstVersion(site,preview));val baseline=db.snapsDao().getLastSnapForSiteId(site.id)!!;val bytes=files.readBytes(baseline.snapId)
     val same=runner.check(site.id);assertTrue(same is SyncResult.Success&&!same.hadChanges)
     image.redirect=tls.url
     val result=runner.check(site.id);val versions=db.snapsDao().getAllSnapsForSiteId(site.id).size
     println("REVIEW_SUBRESOURCE result=$result versions=$versions alerts=${alerts.get()} baselineIntact=${bytes.contentEquals(files.readBytes(baseline.snapId))}")
     assertTrue("Invalid visible resource must fail rather than alert: result=$result versions=$versions alerts=${alerts.get()}",result is SyncResult.Failed)
     assertEquals(1,versions);assertEquals(0,alerts.get())
     image.redirect=null
     val recovery=runner.check(site.id);assertTrue(recovery is SyncResult.Success&&!recovery.hadChanges);assertEquals(1,db.snapsDao().getAllSnapsForSiteId(site.id).size);assertEquals(0,alerts.get())
     server.html=html.replace("<body>","<body style='background:yellow'>").replace("Unchanged page inventory","Genuine updated inventory")
     assertTrue((runner.check(site.id) as SyncResult.Success).hadChanges);assertFalse((runner.check(site.id) as SyncResult.Success).hadChanges);assertEquals(1,alerts.get())
     server.html=html;assertTrue((runner.check(site.id) as SyncResult.Success).hadChanges);assertEquals(2,alerts.get());assertEquals(3,db.snapsDao().getAllSnapsForSiteId(site.id).size);assertArrayEquals(bytes,files.readBytes(baseline.snapId))
    } finally {db.snapsDao().getAllSnapsForSiteId(site.id).forEach {files.deleteFile(it.snapId)};client.close();db.close();context.deleteDatabase(name)}
   }}
   try {compose.waitUntil(100000){task.isCompleted};runBlocking{task.await()}}finally{scope.cancel()}
  }
 }}
 private class ImageServer:AutoCloseable {
  private val socket=java.net.ServerSocket(0);private val pool=java.util.concurrent.Executors.newCachedThreadPool();@Volatile var redirect:String?=null;val url="http://127.0.0.1:${socket.localPort}/image"
  private val bytes=android.graphics.Bitmap.createBitmap(280,240,android.graphics.Bitmap.Config.ARGB_8888).let { b->b.eraseColor(android.graphics.Color.BLUE);java.io.ByteArrayOutputStream().use {o->b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,o);b.recycle();o.toByteArray()} }
  init {pool.submit {while(!socket.isClosed){val c=runCatching{socket.accept()}.getOrNull()?:break;pool.submit {c.use {val reader=c.getInputStream().bufferedReader();reader.readLine();while(!reader.readLine().isNullOrEmpty()){};val target=redirect;val body=if(target==null)bytes else byteArrayOf();val h="HTTP/1.1 ${if(target==null)"200 OK" else "302 Found"}\r\nContent-Type: image/png\r\nCache-Control: no-store\r\nContent-Length: ${body.size}\r\n${target?.let{"Location: $it\r\n"}.orEmpty()}Connection: close\r\n\r\n";c.getOutputStream().apply{write(h.toByteArray());write(body);flush()}}}}} }
  override fun close(){socket.close();pool.shutdownNow()}
 }
}
