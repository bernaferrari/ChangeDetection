/**
 * Local Screenshot Studio server. In the master repository it opens the project overview;
 * in an app repository it also keeps the standalone editor available.
 *   node studio/server.cjs [port] [--add /path/to/app] [--registry /path/to/projects.json]
 */
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const {inside} = require('./workspace-store.cjs');
const types = {'.html': 'text/html', '.js': 'text/javascript', '.mjs': 'text/javascript', '.cjs': 'text/javascript', '.css': 'text/css', '.json': 'application/json',
  '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.webp': 'image/webp', '.svg': 'image/svg+xml', '.woff2': 'font/woff2', '.woff': 'font/woff', '.ttf': 'font/ttf', '.otf': 'font/otf', '.ttc': 'font/collection'};
const listing = path.resolve(__dirname, '..');
function writable(file, listingRoot) {
  const relative = path.relative(listingRoot, file).split(path.sep);
  return (relative.length === 1 && relative[0] === 'gallery.json') ||
    (relative.length === 2 && /^[a-z]{2,3}(-[A-Za-z0-9]{2,8})*$/.test(relative[0]) && relative[1] === 'copy.json');
}
function serve(root, port = 0, {editable = false, workspace = null, activeProjectId = null, selectFolder} = {}) {
  root = path.resolve(root);
  const hub = workspace ? require('./workspace-server.cjs').workspaceRoutes({store: workspace, uiDirectory: __dirname, activeProjectId, selectFolder}) : null;
  const server = http.createServer(async (request, response) => {
    async function serveFile(allowedRoot, url, {editable: canWrite = editable, listingRoot = listing} = {}) {
      allowedRoot = path.resolve(allowedRoot);
      let file = path.resolve(allowedRoot, `.${url}`);
      if (!inside(allowedRoot, file)) { response.writeHead(403).end(); return; }
      try {
        if (fs.existsSync(file) && !inside(fs.realpathSync(allowedRoot), fs.realpathSync(file))) { response.writeHead(403).end(); return; }
        if (request.method === 'PUT') {
          if (!canWrite || !writable(file, listingRoot)) { response.writeHead(403).end('Not writable'); return; }
          const host = request.headers.host || '';
          if (request.headers.origin && request.headers.origin !== `http://${host}`) { response.writeHead(403).end('Use the local studio'); return; }
          // New locale folders may not exist yet, so check their nearest existing parent too.
          let parent = path.dirname(file);
          while (!fs.existsSync(parent)) parent = path.dirname(parent);
          if (!inside(fs.realpathSync(allowedRoot), fs.realpathSync(parent))) { response.writeHead(403).end(); return; }
          let body = '', bytes = 0;
          for await (const chunk of request) { bytes += chunk.length; if (bytes > 1024 * 1024) { response.writeHead(413).end('Copy file is too large'); return; } body += chunk; }
          const value = JSON.parse(body);
          const expected = request.headers['x-studio-base'];
          const current = JSON.stringify(fs.existsSync(file) ? JSON.parse(fs.readFileSync(file, 'utf8')) : {});
          if (expected !== undefined && expected !== require('node:crypto').createHash('sha256').update(current).digest('hex')) {
            response.writeHead(409, {'Content-Type': 'application/json'}).end(JSON.stringify({error: 'The saved copy changed. Reload to resolve your draft.'})); return;
          }
          fs.mkdirSync(path.dirname(file), {recursive: true});
          fs.writeFileSync(file, JSON.stringify(value, null, 2) + '\n');
          response.writeHead(204).end(); return;
        }
        if (!['GET', 'HEAD'].includes(request.method)) { response.writeHead(405).end(); return; }
        if (fs.existsSync(file) && fs.statSync(file).isDirectory()) file = path.join(file, 'index.html');
        if (fs.existsSync(file) && !inside(fs.realpathSync(allowedRoot), fs.realpathSync(file))) { response.writeHead(403).end(); return; }
        const data = await fs.promises.readFile(file);
        response.writeHead(200, {'Content-Type': types[path.extname(file).toLowerCase()] || 'application/octet-stream', 'Cache-Control': 'no-store'});
        response.end(request.method === 'HEAD' ? undefined : data);
      } catch (error) {
        response.writeHead(error.code === 'ENOENT' ? 404 : request.method === 'PUT' ? 400 : 500).end(error.code === 'ENOENT' ? 'Not found' : error.message);
      }
    }
    try {
      if (!/^(127\.0\.0\.1|localhost)(:\d+)?$/.test(request.headers.host || '')) { response.writeHead(403).end('Use the local Studio address'); return; }
      const pathname = decodeURIComponent(new URL(request.url, 'http://local').pathname);
      if (hub && await hub(request, response, pathname, serveFile)) return;
      await serveFile(root, pathname);
    } catch (error) { if (!response.headersSent) response.writeHead(400).end(error.message); }
  });
  return new Promise(resolve => server.listen(port, '127.0.0.1', () => resolve(server)));
}
module.exports = {serve};

if (require.main === module) (async () => {
  const args = process.argv.slice(2), value = flag => { const index = args.indexOf(flag); return index < 0 ? undefined : args[index + 1]; };
  const port = Number(args[0] && !args[0].startsWith('--') ? args[0] : 8790);
  const {createWorkspaceStore} = require('./workspace-store.cjs');
  const workspace = createWorkspaceStore({registryFile: value('--registry')});
  const app = fs.existsSync(path.join(listing, 'studio.json'));
  const root = app ? path.dirname(listing) : listing;
  let activeProjectId = null;
  if (app) activeProjectId = (await workspace.add([root]))[0].id;
  for (let index = 0; index < args.length; index++) if (args[index] === '--add') await workspace.add([args[++index]]);
  await serve(root, port, {editable: true, workspace, activeProjectId});
  console.log(`Screenshot Studio workspace: http://127.0.0.1:${port}/studio/`);
  if (app) console.log(`App editor: http://127.0.0.1:${port}/store-listing/studio/`);
})().catch(error => { console.error(error.message); process.exitCode = 1; });
