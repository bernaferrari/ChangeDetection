package com.bernaferrari.changedetection.browser

import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

internal class TestPageServer(private val status: Int, @Volatile var html: String, private val cacheControl: String = "no-store", private val resourceDelayMs: Long = 0) : AutoCloseable {
    private val socket = ServerSocket(0)
    private val executor = Executors.newCachedThreadPool()
    val url = "http://127.0.0.1:${socket.localPort}/"
    init {
        executor.submit {
            while (!socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                executor.submit { respond(client) }
            }
        }
    }
    private fun respond(client: Socket) {
        client.use {
            val input = client.getInputStream().bufferedReader()
            val request = input.readLine() ?: return
            while (true) {
                val line = input.readLine() ?: return
                if (line.isEmpty()) break
            }
            if (resourceDelayMs > 0 && request.contains(" /slow-image ")) Thread.sleep(resourceDelayMs)
            val bytes = html.toByteArray()
            val headers = "HTTP/1.1 $status ${if (status == 200) "OK" else "Unavailable"}\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nCache-Control: $cacheControl\r\nConnection: close\r\n\r\n"
            client.getOutputStream().apply {
                write(headers.toByteArray())
                write(bytes)
                flush()
            }
        }
    }
    override fun close() {
        socket.close()
        executor.shutdownNow()
    }
}
