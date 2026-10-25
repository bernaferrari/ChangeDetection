package com.bernaferrari.changedetection.browser
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.sync.*
import kotlinx.coroutines.*
import org.junit.Test
import org.junit.Assert.*
class VisualRegionMultiplicityTest {
 @Test fun aChangeInTheSecondSelectedAreaMustBeCaptured()=runBlocking {
  val html="<meta name='viewport' content='width=device-width,initial-scale=1'><body><div id='a' style='width:160px;height:120px;background:blue'>Stable first area</div><div id='b' style='width:160px;height:120px;background:red'>Second area old content</div></body>"
  TestPageServer(200,html).use {server ->
   suspend fun capture():ByteArray=withContext(Dispatchers.Main.immediate){val browser=AndroidScreenshotBrowserFactory(ApplicationProvider.getApplicationContext<Context>()).create();try{(browser.capture(Site("Review",server.url,0L,"review-multiple",ColorGroup(0,0),true,false).copy(captureMode=CaptureMode.SCREENSHOT,textIncludeSelector="#a, #b"),0) as ScreenshotCaptureOutcome.Success).bytes}finally{browser.close()}}
   val error=runCatching {capture()}.exceptionOrNull();assertTrue(error.toString(),error is com.bernaferrari.changedetection.detection.TextRegionException);assertEquals(com.bernaferrari.changedetection.detection.TextRegionIssue.MULTIPLE_MATCHES,(error as com.bernaferrari.changedetection.detection.TextRegionException).issue)

  }
 }
}
