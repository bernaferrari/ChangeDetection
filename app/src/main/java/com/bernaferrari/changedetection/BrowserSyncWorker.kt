package com.bernaferrari.changedetection

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.orhanobut.logger.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

@HiltWorker
class BrowserSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val sitesRepository: SitesRepository,
    private val snapsRepository: SnapsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Logger.d("🌐 BrowserSyncWorker starting")

        return try {
            val siteId = inputData.getString("id") ?: return Result.failure()
            val site = sitesRepository.getSiteById(siteId) ?: return Result.failure()

            if (!site.browserMode) {
                Logger.e("Site $siteId is not in browser mode")
                return Result.failure()
            }

            when (val webContent = fetchWithBrowser(site)) {
                is BrowserFetchResult.Success -> {
                    val snap = com.bernaferrari.changedetection.repo.Snap(
                        siteId = siteId,
                        timestamp = System.currentTimeMillis(),
                        contentType = "text/html",
                        contentSize = webContent.content.toByteArray().size,
                        contentCharset = "UTF-8"
                    )

                    snapsRepository.saveSnap(snap, webContent.content.toByteArray(), 0)

                    // Update cookies if they changed
                    if (webContent.cookies != site.cookieData) {
                        sitesRepository.updateSite(
                            site.copy(
                                cookieData = webContent.cookies,
                                isSuccessful = true,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    } else {
                        sitesRepository.updateSite(
                            site.copy(
                                isSuccessful = true,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }

                    Logger.d("✅ Browser sync successful for site: $siteId")
                    Result.success()
                }

                is BrowserFetchResult.Error -> {
                    sitesRepository.updateSite(site.copy(isSuccessful = false))
                    Logger.e("❌ Browser sync failed: ${webContent.message}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Logger.e("❌ BrowserSyncWorker error: ${e.message}")
            Result.retry()
        }
    }

    private suspend fun fetchWithBrowser(site: com.bernaferrari.changedetection.repo.Site): BrowserFetchResult =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                try {
                    val webView = WebView(applicationContext).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.userAgentString =
                            site.userAgent.ifEmpty { settings.userAgentString }
                    }

                    // Restore cookies
                    restoreCookies(site.cookieData, site.url)

                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)

                            // Wait a bit for dynamic content to load
                            view?.postDelayed({
                                try {
                                    view.evaluateJavascript(
                                        if (site.cssSelector.isNotEmpty()) {
                                            """
                                        (function() {
                                            var element = document.querySelector('${site.cssSelector}');
                                            return element ? element.outerHTML : document.documentElement.outerHTML;
                                        })();
                                        """.trimIndent()
                                        } else {
                                            "document.documentElement.outerHTML"
                                        }
                                    ) { html ->
                                        val cleanHtml =
                                            html?.removeSurrounding("\"")?.replace("\\\"", "\"")
                                                ?.replace("\\n", "\n") ?: ""
                                        val cookies = saveCookies(site.url)

                                        if (continuation.isActive) {
                                            continuation.resume(
                                                BrowserFetchResult.Success(
                                                    cleanHtml,
                                                    cookies
                                                )
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    if (continuation.isActive) {
                                        continuation.resume(BrowserFetchResult.Error("Failed to extract content: ${e.message}"))
                                    }
                                }
                            }, 2000) // Wait 2 seconds for dynamic content
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            if (continuation.isActive) {
                                continuation.resume(BrowserFetchResult.Error("WebView error: $description"))
                            }
                        }
                    }

                    webView.loadUrl(site.url)

                    // Timeout after 30 seconds
                    continuation.invokeOnCancellation {
                        webView.destroy()
                    }

                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resume(BrowserFetchResult.Error("Browser setup failed: ${e.message}"))
                    }
                }
            }
        }

    private fun restoreCookies(cookieData: String, url: String) {
        if (cookieData.isEmpty()) return

        try {
            val cookieManager = CookieManager.getInstance()
            val cookiesArray = JSONArray(cookieData)

            for (i in 0 until cookiesArray.length()) {
                val cookieObj = cookiesArray.getJSONObject(i)
                val cookieString = "${cookieObj.getString("name")}=${cookieObj.getString("value")}"
                cookieManager.setCookie(url, cookieString)
            }

            cookieManager.flush()
        } catch (e: Exception) {
            Logger.e("Failed to restore cookies: ${e.message}")
        }
    }

    private fun saveCookies(url: String): String {
        return try {
            val cookieManager = CookieManager.getInstance()
            val cookieString = cookieManager.getCookie(url)

            if (cookieString != null) {
                val cookiesArray = JSONArray()
                val cookies = cookieString.split(";")

                cookies.forEach { cookie ->
                    val parts = cookie.trim().split("=", limit = 2)
                    if (parts.size == 2) {
                        val cookieObj = JSONObject().apply {
                            put("name", parts[0])
                            put("value", parts[1])
                        }
                        cookiesArray.put(cookieObj)
                    }
                }

                cookiesArray.toString()
            } else {
                ""
            }
        } catch (e: Exception) {
            Logger.e("Failed to save cookies: ${e.message}")
            ""
        }
    }
}

sealed class BrowserFetchResult {
    data class Success(val content: String, val cookies: String) : BrowserFetchResult()
    data class Error(val message: String) : BrowserFetchResult()
}
