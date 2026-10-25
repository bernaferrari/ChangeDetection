package com.bernaferrari.changedetection.browser
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.sync.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
class ExternalStylesheetReadinessTest {
 @Test fun pendingStylesheetCannotBecomeAnUnstyledSnapshot()=runBlocking {
  Styles().use {s->s.delay=20000;val e=runCatching{capture(s.url)}.exceptionOrNull();assertTrue("Pending CSS must fail instead of saving fallback styling: $e",e is CaptureNotReadyException || e is TimeoutCancellationException);s.delay=0;val a=capture(s.url);val b=capture(s.url);assertArrayEquals(a,b)}
 }
 private suspend fun capture(url:String):ByteArray=withContext(Dispatchers.Main.immediate){val b=AndroidScreenshotBrowserFactory(ApplicationProvider.getApplicationContext<Context>()).create();try{(b.capture(Site("CSS",url,0L,"css",ColorGroup(0,0),true,false).copy(captureMode=CaptureMode.SCREENSHOT),0) as ScreenshotCaptureOutcome.Success).bytes}finally{b.close()}}
 private class Styles:AutoCloseable {
  val socket=java.net.ServerSocket(0);val pool=java.util.concurrent.Executors.newCachedThreadPool();@Volatile var delay=0L;val url="http://127.0.0.1:${socket.localPort}/"
  init{pool.submit{while(!socket.isClosed){val c=runCatching{socket.accept()}.getOrNull()?:break;pool.submit{c.use{val r=c.getInputStream().bufferedReader();val line=r.readLine()?:return@use;while(!r.readLine().isNullOrEmpty()){};val css=line.contains(" /style.css ");if(css)Thread.sleep(delay);val bytes=(if(css)"body{background:blue;color:white;font-size:28px}" else "<meta name='viewport' content='width=device-width,initial-scale=1'><link rel='stylesheet' href='/style.css'><body><div style='width:280px;height:240px'>Stable CSS fixture</div></body>").toByteArray();try{c.getOutputStream().apply{write("HTTP/1.1 200 OK\r\nContent-Type: ${if(css)"text/css" else "text/html"}\r\nCache-Control: no-store\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray());write(bytes);flush()}}catch(_:java.io.IOException){}}}}}}
  override fun close(){socket.close();pool.shutdownNow()}
 }
}
