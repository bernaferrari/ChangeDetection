#!/usr/bin/env node
/**
 * Fictional third-party apps for capture emulators: tiny code-free APKs with a name, a drawn adaptive
 * icon and (optionally) the INTERNET permission, so firewalls, launchers and app pickers list them
 * through the real PackageManager with no fixture code in the app under test.
 *
 *   node store-listing/studio/fixture-apps.cjs store-listing/fixture-apps.json [--install emulator-5578] [--uninstall emulator-5578]
 *
 * Spec: {"apps": [{"package": "app.fernway.maps", "label": "Fernway Maps", "colors": ["#3fb27f", "#1e8a5c"],
 *         "glyph": "map", "tint": "#ffffff", "internet": true, "category": "maps"}]}
 * glyph: a Lucide icon name (resolved from lucide-react in the studio master's node_modules or LUCIDE_DIR),
 * {"svg": "<path d=\"...\"/>"} drawn on a 24-unit stroke grid, or {"text": "Aa"}.
 * category: an android:appCategory value (game, audio, video, image, social, news, maps, productivity).
 * Needs the Android SDK (ANDROID_HOME or ~/Library/Android/sdk) and Playwright (PLAYWRIGHT_MODULE) for the icons.
 * Builds into <spec dir>/.fixture-apps/ (keep it git-ignored) and signs with the local debug keystore.
 */
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const {execFileSync} = require('node:child_process');

const args = process.argv.slice(2);
const specFile = path.resolve(args.find(a => !a.startsWith('--') && !/^emulator-/.test(a)) || '');
const flag = name => { const i = args.indexOf(`--${name}`); return i >= 0 ? args[i + 1] : null; };
if (!fs.existsSync(specFile)) {
  console.error('Usage: node fixture-apps.cjs fixture-apps.json [--install <emulator-serial>] [--uninstall <emulator-serial>]');
  process.exit(1);
}
const spec = JSON.parse(fs.readFileSync(specFile, 'utf8'));
const out = path.join(path.dirname(specFile), '.fixture-apps');
const serial = flag('install') || flag('uninstall');
if (serial && !serial.startsWith('emulator-')) throw new Error('Install fixture apps on a dedicated emulator only.');

const sdk = process.env.ANDROID_HOME || process.env.ANDROID_SDK_ROOT || path.join(os.homedir(), 'Library/Android/sdk');
const newest = dir => fs.readdirSync(dir).filter(v => /^\d/.test(v) && !/rc|preview/i.test(v))
  .sort((a, b) => a.localeCompare(b, undefined, {numeric: true})).pop();
const tools = path.join(sdk, 'build-tools', newest(path.join(sdk, 'build-tools')));
const platform = fs.readdirSync(path.join(sdk, 'platforms')).filter(p => fs.existsSync(path.join(sdk, 'platforms', p, 'android.jar')))
  .sort((a, b) => a.localeCompare(b, undefined, {numeric: true})).pop();
const androidJar = path.join(sdk, 'platforms', platform, 'android.jar');
const run = (tool, ...rest) => execFileSync(path.join(tools, tool), rest, {stdio: ['ignore', 'pipe', 'inherit']});
const adb = (...rest) => execFileSync('adb', ['-s', serial, ...rest], {stdio: ['ignore', 'pipe', 'pipe']}).toString();

if (flag('uninstall')) {
  for (const app of spec.apps) {
    try { adb('uninstall', app.package); console.log(`removed   ${app.package}`); } catch { /* not installed */ }
  }
  process.exit(0);
}

function lucideDir() {
  const candidates = [process.env.LUCIDE_DIR,
    path.join(__dirname, 'node_modules/lucide-react/dist/esm/icons'),
    path.join(__dirname, '../node_modules/lucide-react/dist/esm/icons'),
    path.join(os.homedir(), 'Documents/screenshot-studio/node_modules/lucide-react/dist/esm/icons')];
  return candidates.find(dir => dir && fs.existsSync(dir));
}

function glyphMarkup(glyph) {
  if (glyph && typeof glyph === 'object' && glyph.svg) return glyph.svg;
  if (typeof glyph !== 'string') return '';
  const dir = lucideDir();
  if (!dir) throw new Error('Lucide icons not found: set LUCIDE_DIR to lucide-react/dist/esm/icons');
  const file = path.join(dir, `${glyph}.mjs`);
  if (!fs.existsSync(file)) throw new Error(`No Lucide icon named ${glyph}`);
  let source = fs.readFileSync(file, 'utf8');
  const alias = source.match(/from '\.\/([\w-]+)\.mjs'/);
  if (!source.includes('__iconData') && alias) source = fs.readFileSync(path.join(dir, `${alias[1]}.mjs`), 'utf8');
  const start = source.indexOf('{', source.indexOf('const __iconData'));
  const literal = source.slice(start, source.indexOf('__iconData.node', start)).trim().replace(/;$/, '');
  // A plain object literal from the installed lucide-react package.
  // eslint-disable-next-line no-new-func
  const elements = Function(`return (${literal})`)().node;
  return elements.map(([tag, attrs]) => `<${tag} ${Object.entries(attrs).filter(([k]) => k !== 'key')
    .map(([k, v]) => `${k}="${v}"`).join(' ')}/>`).join('');
}

/** Adaptive icon layers, 432 px (108 dp at xxxhdpi): a gradient field and a glyph inside the 66 dp safe zone. */
function layers(app) {
  const [top, bottom = top] = app.colors;
  const tint = app.tint || '#ffffff';
  const background = `<svg xmlns="http://www.w3.org/2000/svg" width="432" height="432" viewBox="0 0 432 432">
    <defs><linearGradient id="g" x1="0" y1="0" x2="0.35" y2="1"><stop offset="0" stop-color="${top}"/><stop offset="1" stop-color="${bottom}"/></linearGradient>
    <radialGradient id="h" cx=".3" cy=".22" r=".8"><stop offset="0" stop-color="#fff" stop-opacity=".22"/><stop offset=".6" stop-color="#fff" stop-opacity="0"/></radialGradient></defs>
    <rect width="432" height="432" fill="url(#g)"/><rect width="432" height="432" fill="url(#h)"/></svg>`;
  let mark;
  if (app.glyph && typeof app.glyph === 'object' && app.glyph.text) {
    const size = app.glyph.text.length > 1 ? 104 : 140;
    mark = `<text x="216" y="216" dy=".35em" text-anchor="middle" font-family="Google Sans, Roboto, Helvetica, Arial, sans-serif"
      font-weight="700" font-size="${size}" fill="${tint}">${app.glyph.text}</text>`;
  } else {
    // 24-unit Lucide grid scaled to 168 px, centred.
    mark = `<g transform="translate(132 132) scale(7)" fill="none" stroke="${tint}" stroke-width="${app.strokeWidth || 2.1}"
      stroke-linecap="round" stroke-linejoin="round">${glyphMarkup(app.glyph)}</g>`;
  }
  const foreground = `<svg xmlns="http://www.w3.org/2000/svg" width="432" height="432" viewBox="0 0 432 432">${mark}</svg>`;
  return {background, foreground};
}

async function renderIcons(apps) {
  const {chromium} = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
  const browser = await chromium.launch();
  const page = await browser.newPage({viewport: {width: 432, height: 432}});
  for (const app of apps) {
    const dir = path.join(out, app.package, 'res', 'drawable-nodpi');
    fs.mkdirSync(dir, {recursive: true});
    for (const [name, svg] of Object.entries(layers(app))) {
      await page.setContent(`<style>html,body{margin:0;background:transparent}</style>${svg}`);
      await page.locator('svg').screenshot({path: path.join(dir, `ic_${name}.png`), omitBackground: true});
    }
  }
  await browser.close();
}

function build(app) {
  const root = path.join(out, app.package);
  fs.mkdirSync(path.join(root, 'res', 'mipmap-anydpi-v26'), {recursive: true});
  fs.writeFileSync(path.join(root, 'res', 'mipmap-anydpi-v26', 'ic_launcher.xml'),
    `<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
  <background android:drawable="@drawable/ic_background"/>
  <foreground android:drawable="@drawable/ic_foreground"/>
</adaptive-icon>
`);
  const label = app.label.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
  fs.writeFileSync(path.join(root, 'AndroidManifest.xml'),
    `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="${app.package}">
  ${app.internet === false ? '' : '<uses-permission android:name="android.permission.INTERNET"/>'}
  <application android:label="${label}" android:icon="@mipmap/ic_launcher" android:hasCode="false"${app.category ? ` android:appCategory="${app.category}"` : ''}/>
</manifest>
`);
  const compiled = path.join(root, 'compiled.zip');
  run('aapt2', 'compile', '--dir', path.join(root, 'res'), '-o', compiled);
  const unsigned = path.join(root, 'unsigned.apk'), aligned = path.join(root, 'aligned.apk');
  run('aapt2', 'link', '-o', unsigned, '-I', androidJar, '--manifest', path.join(root, 'AndroidManifest.xml'),
    '--min-sdk-version', '26', '--target-sdk-version', '34', '--version-code', '1', '--version-name', '1.0', compiled);
  run('zipalign', '-p', '-f', '4', unsigned, aligned);
  const apk = path.join(out, `${app.package}.apk`);
  // The SDK's public debug keystore; these APKs only ever run on capture emulators.
  run('apksigner', 'sign', '--ks', path.join(os.homedir(), '.android', 'debug.keystore'), '--ks-pass', 'pass:android',
    '--ks-key-alias', 'androiddebugkey', '--key-pass', 'pass:android', '--out', apk, aligned);
  return apk;
}

(async () => {
  const fingerprint = JSON.stringify(spec.apps);
  const stamp = path.join(out, 'spec.json');
  const fresh = fs.existsSync(stamp) && fs.readFileSync(stamp, 'utf8') === fingerprint &&
    spec.apps.every(app => fs.existsSync(path.join(out, `${app.package}.apk`)));
  if (!fresh) {
    fs.rmSync(out, {recursive: true, force: true});
    fs.mkdirSync(out, {recursive: true});
    await renderIcons(spec.apps);
    for (const app of spec.apps) console.log(`built     ${path.relative(process.cwd(), build(app))}`);
    fs.writeFileSync(stamp, fingerprint);
  }
  if (flag('install')) {
    for (const app of spec.apps) {
      adb('install', '-r', '-t', path.join(out, `${app.package}.apk`));
      console.log(`installed ${app.package}`);
    }
  }
})().catch(error => { console.error(error); process.exit(1); });
