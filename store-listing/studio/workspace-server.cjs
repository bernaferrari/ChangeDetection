/** Workspace HTTP routes. Project files stay under their registered repository root. */
const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const {execFile} = require('node:child_process');
const {promisify} = require('node:util');
const {statusError} = require('./workspace-store.cjs');
const execute = promisify(execFile);
const send = (response, value, status = 200) => {
  response.writeHead(status, {'Content-Type': 'application/json', 'Cache-Control': 'no-store'});
  response.end(JSON.stringify(value));
};
const body = request => new Promise((resolve, reject) => {
  let data = '', size = 0;
  request.on('data', chunk => { size += chunk.length; if (size > 256 * 1024) reject(statusError('This request is too large.', 413)); else data += chunk; });
  request.on('end', () => { try { resolve(data ? JSON.parse(data) : {}); } catch { reject(statusError('Send valid JSON.')); } });
  request.on('error', reject);
});
async function pickFolder() {
  if (process.platform !== 'darwin') throw statusError('Use the folder path field on this computer.', 501);
  try {
    const {stdout} = await execute('osascript', ['-e', 'POSIX path of (choose folder with prompt "Choose a Screenshot Studio project folder")'], {timeout: 120000});
    return stdout.trim();
  } catch (error) {
    if (String(error.stderr).includes('-128')) return null;
    throw statusError('The folder chooser could not open. Enter the folder path instead.');
  }
}
function workspaceRoutes({store, uiDirectory, activeProjectId = null, selectFolder = pickFolder}) {
  return async (request, response, pathname, serveFile) => {
    if (pathname.startsWith('/api/')) {
      try {
        const host = request.headers.host || '';
        if (!/^(127\.0\.0\.1|localhost)(:\d+)?$/.test(host)) throw statusError('Use the local Studio address.', 403);
        if (request.method !== 'GET' && request.headers.origin && request.headers.origin !== `http://${host}`) throw statusError('This request must come from the local studio.', 403);
        if (pathname === '/api/workspace' && request.method === 'GET') {
          const documents = path.join(os.homedir(), 'Documents');
          return send(response, {projects: await store.list(), activeProjectId,
            defaultSearchRoot: fs.existsSync(documents) ? documents : os.homedir(),
            capabilities: {folderPicker: process.platform === 'darwin', revealFolder: ['darwin', 'win32', 'linux'].includes(process.platform)}}), true;
        }
        if (pathname === '/api/projects' && request.method === 'POST') {
          const input = await body(request); const projects = await store.add([{root: input.root, name: input.name}]);
          return send(response, {projects: await store.list(), added: projects.map(project => project.id)}, 201), true;
        }
        if (pathname === '/api/projects/import' && request.method === 'POST') {
          const input = await body(request); const projects = await store.add(input.roots);
          return send(response, {projects: await store.list(), added: projects.map(project => project.id)}, 201), true;
        }
        if (pathname === '/api/projects/discover' && request.method === 'POST') return send(response, await store.discover((await body(request)).root)), true;
        if (pathname === '/api/workspace/pick-folder' && request.method === 'POST') return send(response, {path: await selectFolder()}), true;
        const match = pathname.match(/^\/api\/projects\/([a-zA-Z0-9-]+)(?:\/(files|reveal))?$/);
        if (match) {
          const [, id, action] = match, project = store.get(id);
          if (!project) throw statusError('This project is no longer connected.', 404);
          if (action === 'files' && request.method === 'GET') {
            const library = store.library(id);
            return send(response, {...library, files: library.files.map(image => ({...image, url: `/p/${id}/${image.file.split('/').map(encodeURIComponent).join('/')}`}))}), true;
          }
          if (action === 'reveal' && request.method === 'POST') {
            const command = process.platform === 'darwin' ? 'open' : process.platform === 'win32' ? 'explorer.exe' : 'xdg-open';
            await execute(command, [project.root], {timeout: 10000});
            return send(response, {opened: true}), true;
          }
          if (!action && request.method === 'PATCH') {
            await store.update(id, await body(request)); return send(response, {projects: await store.list()}), true;
          }
          if (!action && request.method === 'DELETE') {
            await store.remove(id); return send(response, {projects: await store.list()}), true;
          }
        }
        send(response, {error: 'This workspace action is not available.'}, 404); return true;
      } catch (error) { send(response, {error: error.message}, error.status || 500); return true; }
    }
    if (pathname === '/') { response.writeHead(302, {Location: '/studio/'}).end(); return true; }
    if (pathname === '/studio' || pathname.startsWith('/studio/')) {
      const relative = pathname.slice('/studio'.length) || '/';
      await serveFile(uiDirectory, relative, {editable: false}); return true;
    }
    const mounted = pathname.match(/^\/p\/([a-zA-Z0-9-]+)(\/.*)?$/);
    if (mounted) {
      const project = store.get(mounted[1]);
      if (!project) { response.writeHead(404).end('Project is not connected'); return true; }
      const relative = mounted[2] || '/';
      if (relative === '/' || relative === '/store-listing/studio') {
        response.writeHead(302, {Location: `/p/${project.id}/store-listing/studio/`}).end(); return true;
      }
      // Use this server's shared UI and renderer while resolving app data and fonts in its own repo.
      if (relative.startsWith('/store-listing/studio/')) await serveFile(uiDirectory, relative.slice('/store-listing/studio'.length), {editable: false});
      else await serveFile(project.root, relative, {editable: true, listingRoot: path.join(project.root, 'store-listing')});
      return true;
    }
    return false;
  };
}
module.exports = {workspaceRoutes};
