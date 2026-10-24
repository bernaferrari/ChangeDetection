package com.bernaferrari.changedetection.browser

import java.io.File
import java.net.ServerSocket
import java.util.concurrent.Executors

internal class FontFixtureServer : AutoCloseable {
    private val socket = ServerSocket(0)
    private val threads = Executors.newCachedThreadPool()

    @Volatile var delayMs = 0L

    @Volatile var customHtml: String? = null
    val url = "http://127.0.0.1:${socket.localPort}/"
    init {
        threads.submit {
            while (!socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                threads.submit {
                    client.use {
                        val reader = client.getInputStream().bufferedReader()
                        val request = reader.readLine() ?: return@submit
                        while (!reader.readLine().isNullOrEmpty()) { }
                        val font = request.contains(" /font.ttf ")
                        if (font) Thread.sleep(delayMs)
                        val bytes = if (font) File("/system/fonts/Roboto-Regular.ttf").readBytes() else (customHtml ?: """<meta name="viewport" content="width=device-width,initial-scale=1"><style>@font-face{font-family:CaptureFixture;src:url('/font.ttf');font-display:swap}body{font:24px CaptureFixture,monospace;background:white;color:black}h1{width:300px}</style><h1>Stable responsive inventory heading</h1><p>Stable price and availability</p>""").toByteArray()
                        val type = if (font) "font/ttf" else "text/html; charset=utf-8"
                        client.getOutputStream().apply {
                            write("HTTP/1.1 200 OK\r\nContent-Type: $type\r\nContent-Length: ${bytes.size}\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n".toByteArray())
                            write(bytes)
                            flush()
                        }
                    }
                }
            }
        }
    }
    override fun close() {
        socket.close()
        threads.shutdownNow()
    }
}
