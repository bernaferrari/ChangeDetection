/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.worker

import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import org.w3c.dom.Worker

actual fun createSQLiteWasmWorker() = WebWorkerSQLiteDriver(jsWorker())

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsWorker(): Worker = js("""new Worker(new URL("sqlite-wasm-worker/worker.js", import.meta.url))""")
