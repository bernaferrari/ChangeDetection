package com.bernaferrari.changedetection.browser

import androidx.test.platform.app.InstrumentationRegistry
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

/** Disposable test key only. Served but never installed/trusted on the emulator. */
internal fun untrustedTlsPage(): TestPageServer {
    val password = "tls-fixture".toCharArray()
    val keys = KeyStore.getInstance("PKCS12").apply {
        InstrumentationRegistry.getInstrumentation().context.assets.open("tls-fixture.p12").use { load(it, password) }
    }
    val managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply { init(keys, password) }
    val tls = SSLContext.getInstance("TLS").apply { init(managers.keyManagers, null, null) }
    return TestPageServer(200, "<html><body>Untrusted TLS fixture</body></html>", socket = tls.serverSocketFactory.createServerSocket(0), scheme = "https")
}
