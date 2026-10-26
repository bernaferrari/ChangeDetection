/** Local project registration and coverage. It never modifies a connected project's files. */
const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const {randomUUID} = require('node:crypto');
const SKIP = new Set(['node_modules', 'build', 'dist', 'target', 'vendor', 'Pods', 'coverage', 'generated', 'out', 'venv']);
const MARKERS = ['screenshots', 'docs/screenshots', 'store-listing', 'fastlane/metadata'];
const IMAGE = /\.(png|jpe?g|webp)$/i;
const localePattern = /^[a-z]{2,3}(-[A-Za-z0-9]{2,8})*$/;
const read = (file, fallback) => {
  try { return JSON.parse(fs.readFileSync(file, 'utf8')); }
  catch (error) { if (error.code === 'ENOENT' && fallback !== undefined) return fallback; throw error; }
};
const exists = file => { try { return fs.statSync(file).isFile(); } catch { return false; } };
const inside = (root, file) => { const relative = path.relative(root, file); return relative === '' || (!relative.startsWith(`..${path.sep}`) && relative !== '..' && !path.isAbsolute(relative)); };
const statusError = (message, status = 400) => Object.assign(new Error(message), {status});
function folder(input) {
  if (typeof input !== 'string' || !input.trim()) throw statusError('Choose a project folder.');
  let expanded = input.trim().replace(/^~(?=$|[\\/])/, os.homedir());
  let root;
  try { root = fs.realpathSync(path.resolve(expanded)); if (!fs.statSync(root).isDirectory()) throw Error(); }
  catch { throw statusError('That folder could not be opened. Check the path and try again.'); }
  if (path.basename(root) === 'store-listing' && exists(path.join(root, 'studio.json'))) root = path.dirname(root);
  return root;
}
function imageFiles(root, limit = 2000) {
  const starts = MARKERS.map(marker => path.join(root, marker)).filter(file => { try { return fs.statSync(file).isDirectory(); } catch { return false; } });
  if (!starts.length) starts.push(root);
  const seen = new Set(), files = [], queue = starts.map(dir => ({dir, depth: 0}));
  let visited = 0;
  while (queue.length && files.length < limit && visited++ < 1500) {
    const {dir, depth} = queue.shift();
    let entries; try { entries = fs.readdirSync(dir, {withFileTypes: true}); } catch { continue; }
    for (const entry of entries) {
      if (entry.name.startsWith('.') || SKIP.has(entry.name) || entry.isSymbolicLink()) continue;
      const file = path.join(dir, entry.name);
      if (entry.isDirectory() && depth < 5) queue.push({dir: file, depth: depth + 1});
      else if (entry.isFile() && IMAGE.test(entry.name) && !seen.has(file)) {
        seen.add(file); const stat = fs.statSync(file);
        files.push({file: path.relative(root, file).split(path.sep).join('/'), bytes: stat.size, modified: stat.mtimeMs});
        if (files.length >= limit) break;
      }
    }
  }
  files.sort((a, b) => b.modified - a.modified || a.file.localeCompare(b.file));
  return {files, truncated: files.length >= limit || queue.length > 0};
}
const assetURL = (id, relative) => `/p/${id}/${relative.split(path.sep).map(encodeURIComponent).join('/')}`;

function createWorkspaceStore({registryFile = process.env.SCREENSHOT_STUDIO_PROJECTS || path.join(os.homedir(), '.screenshot-studio', 'projects.json')} = {}) {
  const registry = () => {
    const value = read(registryFile, {version: 1, projects: []});
    if (!Array.isArray(value.projects)) throw statusError('The workspace project list is invalid.', 500);
    return value;
  };
  const save = value => {
    fs.mkdirSync(path.dirname(registryFile), {recursive: true, mode: 0o700});
    const temporary = `${registryFile}.${randomUUID()}.tmp`;
    try { fs.writeFileSync(temporary, JSON.stringify(value, null, 2) + '\n', {mode: 0o600}); fs.renameSync(temporary, registryFile); }
    finally { if (fs.existsSync(temporary)) fs.unlinkSync(temporary); }
  };
  async function change(update) {
    fs.mkdirSync(path.dirname(registryFile), {recursive: true, mode: 0o700});
    const lock = `${registryFile}.lock`, deadline = Date.now() + 4000;
    let acquired = false;
    while (!acquired) {
      try { const descriptor = fs.openSync(lock, 'wx', 0o600); fs.writeFileSync(descriptor, String(process.pid)); fs.closeSync(descriptor); acquired = true; }
      catch (error) {
        if (error.code !== 'EEXIST') throw error;
        try {
          const owner = Number(fs.readFileSync(lock, 'utf8'));
          try { if (owner) process.kill(owner, 0); }
          catch (failure) { if (failure.code === 'ESRCH') { fs.unlinkSync(lock); continue; } }
          if (Date.now() - fs.statSync(lock).mtimeMs > 30000) { fs.unlinkSync(lock); continue; }
        } catch (failure) { if (failure.code === 'ENOENT') continue; }
        if (Date.now() > deadline) throw statusError('Another studio is updating the project list. Try again.', 409);
        await new Promise(resolve => setTimeout(resolve, 25));
      }
    }
    try { const value = registry(); const result = update(value); save(value); return result; }
    finally { fs.unlinkSync(lock); }
  }
  const get = id => registry().projects.find(project => project.id === id);
  async function add(inputs) {
    if (!Array.isArray(inputs) || inputs.length < 1 || inputs.length > 100) throw statusError('Choose between 1 and 100 project folders.');
    const entries = inputs.map(input => typeof input === 'string' ? {root: folder(input)} : {root: folder(input.root), name: typeof input.name === 'string' ? input.name.trim().slice(0, 100) : ''});
    return change(value => entries.map(entry => {
      const existing = value.projects.find(project => project.root === entry.root);
      if (existing) return existing;
      const project = {id: randomUUID(), root: entry.root, ...(entry.name ? {name: entry.name} : {}), createdAt: new Date().toISOString()};
      value.projects.push(project); return project;
    }));
  }
  async function update(id, input) {
    const root = folder(input.root);
    return change(value => {
      const project = value.projects.find(item => item.id === id);
      if (!project) throw statusError('This project is no longer connected.', 404);
      if (value.projects.some(item => item.id !== id && item.root === root)) throw statusError('That folder is already connected.', 409);
      project.root = root;
      if (typeof input.name === 'string') project.name = input.name.trim().slice(0, 100);
      return project;
    });
  }
  async function remove(id) {
    return change(value => {
      const index = value.projects.findIndex(project => project.id === id);
      if (index < 0) throw statusError('This project is no longer connected.', 404);
      return value.projects.splice(index, 1)[0];
    });
  }
  async function summarize(project) {
    const base = {...project, name: project.name || path.basename(project.root), editorURL: `/p/${project.id}/store-listing/studio/`, overviewURL: `/studio/?project=${project.id}`, previews: []};
    try {
      if (!fs.existsSync(project.root)) return {...base, kind: 'unavailable', problem: 'The project folder is unavailable. Reconnect it or remove it from the overview.'};
      const listing = path.join(project.root, 'store-listing');
      if (!exists(path.join(listing, 'studio.json'))) {
        const library = imageFiles(project.root);
        return {...base, kind: 'library', imageCount: library.files.length, truncated: library.truncated,
          previews: library.files.slice(0, 4).map(image => ({url: assetURL(project.id, image.file), source: 'capture', feature: path.basename(image.file)}))};
      }
      const config = read(path.join(listing, 'studio.json')), profiles = read(path.join(listing, 'profiles.json')), layout = read(path.join(listing, 'gallery.json'));
      const {cardsFor, devicesFor, nodesFile, outputFile} = await import('./gallery.js');
      const reference = config.defaultLocale || 'en-US', languages = config.locales || [reference];
      if (!languages.length || languages.some(locale => !localePattern.test(locale))) throw Error('studio.json has an invalid language list.');
      const copy = read(path.join(listing, reference, 'copy.json'), {});
      const fields = Object.entries(copy).flatMap(([key, values]) => Object.keys(values).map(field => ({key, field})));
      const raws = new Set(), outputs = [], galleries = [];
      for (const kind of Object.keys(layout)) {
        if (!profiles[kind]) throw Error(`profiles.json is missing ${kind}.`);
        const cards = cardsFor(layout, kind);
        galleries.push({kind, label: profiles[kind].label || kind, cards: cards.length, width: profiles[kind].width, height: profiles[kind].height});
        for (const [index, card] of cards.entries()) {
          outputs.push({kind, feature: card.feature, file: outputFile(kind, card, index), width: profiles[kind].width, height: profiles[kind].height});
          devicesFor(card).forEach(device => raws.add(device.raw));
        }
      }
      const locales = [], totals = {fields: 0, translated: 0, captures: 0, expectedCaptures: 0, layouts: 0, exports: 0, expectedExports: 0};
      let lastExport = 0;
      const fileIn = (...parts) => { const file = path.resolve(listing, ...parts); return inside(project.root, file) ? file : null; };
      for (const locale of languages) {
        const own = read(path.join(listing, locale, 'copy.json'), {});
        const translated = fields.filter(({key, field}) => typeof own?.[key]?.[field] === 'string' && own[key][field].trim()).length;
        const captures = [...raws].filter(raw => { const file = fileIn(locale, 'raw', raw); return file && exists(file); }).length;
        const layouts = [...raws].filter(raw => { const file = fileIn(locale, 'raw', nodesFile(raw)); return file && exists(file); }).length;
        let exported = 0;
        for (const output of outputs) {
          const file = fileIn(locale, output.file);
          if (!file || !exists(file)) continue;
          exported++; lastExport = Math.max(lastExport, fs.statSync(file).mtimeMs);
          if (locale === reference && base.previews.length < 4 && output.kind !== 'icon' && output.kind !== 'feature') base.previews.push({...output, source: 'export', url: assetURL(project.id, path.relative(project.root, file))});
        }
        const complete = outputs.length > 0 && translated === fields.length && captures === raws.size && layouts === raws.size && exported === outputs.length;
        const next = translated < fields.length ? 'copy' : captures < raws.size ? 'capture' : layouts < raws.size ? 'layout' : exported < outputs.length ? 'export' : outputs.length ? 'review' : 'cards';
        locales.push({locale, translated, fields: fields.length, captures, expectedCaptures: raws.size, layouts, exports: exported, expectedExports: outputs.length, complete, next});
        totals.fields += fields.length; totals.translated += translated; totals.captures += captures; totals.expectedCaptures += raws.size;
        totals.layouts += layouts; totals.exports += exported; totals.expectedExports += outputs.length;
      }
      if (!base.previews.length) {
        for (const raw of [...raws].slice(0, 4)) {
          const file = fileIn(reference, 'raw', raw);
          if (file && exists(file)) base.previews.push({source: 'capture', feature: path.basename(raw), url: assetURL(project.id, path.relative(project.root, file))});
        }
      }
      const icon = path.join(listing, reference, 'graphics/app-icon.png');
      return {...base, name: project.name || config.app || base.name, kind: 'studio', reference, locales, galleries, totals,
        cardCount: outputs.length, completeLocales: locales.filter(locale => locale.complete).length, lastExport: lastExport || null,
        ...(exists(icon) ? {icon: assetURL(project.id, path.relative(project.root, icon))} : {})};
    } catch (error) { return {...base, kind: 'invalid', problem: `The Studio listing needs attention: ${error.message}`}; }
  }
  async function list() { return Promise.all(registry().projects.map(summarize)); }
  async function discover(input) {
    const root = folder(input), found = [], seen = new Set(), registered = registry().projects;
    const queue = [{dir: root, depth: 0}]; let visited = 0;
    while (queue.length && visited < 2000) {
      const {dir, depth} = queue.shift(); visited++;
      let entries; try { entries = await fs.promises.readdir(dir, {withFileTypes: true}); } catch { continue; }
      const studio = exists(path.join(dir, 'store-listing/studio.json'));
      const markers = MARKERS.some(marker => { try { return fs.statSync(path.join(dir, marker)).isDirectory(); } catch { return false; } });
      if (studio || markers || (depth > 0 && /screenshots?/i.test(path.basename(dir)) && entries.some(entry => entry.isFile() && IMAGE.test(entry.name)))) {
        const canonical = fs.realpathSync(dir);
        if (!seen.has(canonical)) {
          seen.add(canonical); let name = path.basename(canonical);
          if (studio) { try { name = read(path.join(dir, 'store-listing/studio.json')).app || name; } catch {} }
          found.push({root: canonical, name, kind: studio ? 'studio' : 'library', registeredId: registered.find(project => project.root === canonical)?.id || null});
        }
        continue;
      }
      if (depth >= 4) continue;
      for (const entry of entries) if (entry.isDirectory() && !entry.name.startsWith('.') && !SKIP.has(entry.name)) queue.push({dir: path.join(dir, entry.name), depth: depth + 1});
    }
    return {root, projects: found.sort((a, b) => a.name.localeCompare(b.name)), visited, truncated: queue.length > 0};
  }
  return {registryFile, get, list, add, update, remove, discover,
    library(id) { const project = get(id); if (!project) throw statusError('Project not found.', 404); return imageFiles(project.root); }};
}
module.exports = {createWorkspaceStore, inside, assetURL, statusError};
