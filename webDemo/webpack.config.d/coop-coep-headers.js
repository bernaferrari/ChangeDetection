// Ensures OPFS / SharedArrayBuffer works for Room 3 WebWorkerSQLiteDriver during
// :webDemo:wasmJsBrowserDevelopmentRun (and any webpack-dev-server based preview).
;(function (config) {
  const isolationHeaders = {
    'Cross-Origin-Opener-Policy': 'same-origin',
    'Cross-Origin-Embedder-Policy': 'require-corp',
  };

  config.devServer = config.devServer || {};
  config.devServer.headers = Object.assign({}, config.devServer.headers || {}, isolationHeaders);

  const previousSetup = config.devServer.setupMiddlewares;
  config.devServer.setupMiddlewares = function (middlewares, devServer) {
    if (!devServer) {
      throw new Error('webpack-dev-server is not defined');
    }
    devServer.app.use(function (req, res, next) {
      Object.keys(isolationHeaders).forEach(function (key) {
        res.setHeader(key, isolationHeaders[key]);
      });
      next();
    });
    if (typeof previousSetup === 'function') {
      return previousSetup(middlewares, devServer);
    }
    return middlewares;
  };
})(config);
