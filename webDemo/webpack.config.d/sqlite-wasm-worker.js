// Room's Kotlin/Wasm worker URL is constructed with `new URL(...)` inside a raw JS
// interop string, so webpack cannot discover it automatically. Bundle the local NPM
// worker as a second entry and preserve the URL expected by WebWorkerSQLiteDriver.
config.entry = config.entry || {};
config.entry['sqlite-wasm-worker'] = {
  import: require.resolve('sqlite-wasm-worker/worker.js'),
  filename: 'sqlite-wasm-worker/worker.js',
};
