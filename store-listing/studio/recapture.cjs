/**
 * Rebuild the capture app, capture on the emulator, and export, as studio.json's "capture" block says:
 *   node store-listing/studio/recapture.cjs [locale…] [--profile phone] [--no-build] [--no-capture] [--no-export] [--dry-run]
 * Without locales every locale in studio.json is exported, and captured where a run asks for it.
 *
 *   "capture": {
 *     "build": "./gradlew :app:assembleStoreCapture",               shell command, run from the repository
 *     "install": [{"apk": "app/build/…/app-storeCapture.apk", "package": "com.example.storecapture"}],
 *     "runs": [{"script": "store-listing/capture-emulator.py",      (the default)
 *               "args": ["--theme", "dark"],                         ${NAME} reads an environment variable
 *               "profiles": ["phone", "tablet7"],                    default: every capture profile; false: no --profile
 *               "locales": "default"}],                              "all" (default), "default" or a list
 *     "retries": 2,                                                  per capture run
 *     "export": "cd store-listing && node render.mjs --locale {locale}",  instead of studio/export.cjs, per locale
 *     "after": ["python3 store-listing/prepare.py"]                  shell commands once everything is exported
 *   }
 *
 * Each run is `python3 <script> --serial <emulator> --locale <locale> [--profile <profile>] <args>`, through
 * `uv run --with pillow` when uv is installed. The default locale goes first, since it records the tap map
 * other locales replay. Captures use the one capture emulator (CAPTURE_SERIAL, default emulator-5578).
 */
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const {spawnSync} = require('node:child_process');

const listing = path.resolve(__dirname, '..');
const repo = path.resolve(listing, '..');
const config = JSON.parse(fs.readFileSync(path.join(listing, 'studio.json')));
const profiles = JSON.parse(fs.readFileSync(path.join(listing, 'profiles.json')));
const capture = config.capture;
if (!capture) throw new Error('studio.json has no "capture" block; see the top of studio/recapture.cjs');

const argv = process.argv.slice(2);
const flag = name => argv.includes(`--${name}`);
const option = name => { const i = argv.indexOf(`--${name}`); return i >= 0 ? argv[i + 1] : null; };
const values = new Set(['profile', 'serial'].map(name => option(name)).filter(Boolean));
const requested = argv.filter(word => !word.startsWith('--') && !values.has(word));
const serial = option('serial') || process.env.CAPTURE_SERIAL || 'emulator-5578';
if (!serial.startsWith('emulator-')) throw new Error('Use the capture emulator, never a physical phone.');
const dryRun = flag('dry-run');
const defaultLocale = config.defaultLocale || 'en-US';
const locales = requested.length ? requested : config.locales || [defaultLocale];
const unknown = locales.filter(locale => !(config.locales || [defaultLocale]).includes(locale));
if (unknown.length) throw new Error(`Not in studio.json locales: ${unknown.join(', ')}`);
locales.sort((a, b) => (b === defaultLocale) - (a === defaultLocale));
const captureProfiles = Object.keys(profiles).filter(name => profiles[name].captureDensity);
const onlyProfile = option('profile');
if (onlyProfile && !profiles[onlyProfile]?.captureWidth) throw new Error(`No capture profile ${onlyProfile} in profiles.json`);

const has = command => spawnSync('sh', ['-c', `command -v ${command}`], {stdio: 'ignore'}).status === 0;
const python = has('uv') ? ['uv', 'run', '-q', '--with', 'pillow', 'python3'] : ['python3'];
const playwright = path.join(os.homedir(), '.cache/weakspot-shots/node_modules/playwright');
const env = {...process.env, CAPTURE_SERIAL: serial, ANDROID_SERIAL: serial};
if (!env.PLAYWRIGHT_MODULE && fs.existsSync(playwright)) env.PLAYWRIGHT_MODULE = playwright;
const expand = word => String(word).replace(/\$\{(\w+)\}/g, (_, name) => {
  if (env[name] === undefined) throw new Error(`Set ${name} for this capture (studio.json capture args use it)`);
  return env[name];
});

function run(command, args = [], {shell = false, check = true} = {}) {
  const line = shell ? command : [command, ...args].join(' ');
  console.log(`\n$ ${line}`);
  if (dryRun) return true;
  const result = shell ? spawnSync('sh', ['-c', command], {cwd: repo, env, stdio: 'inherit'})
    : spawnSync(command, args, {cwd: repo, env, stdio: 'inherit'});
  if (result.status !== 0 && check) throw new Error(`Failed (${result.status ?? result.signal}): ${line}`);
  return result.status === 0;
}

const studioPy = path.relative(repo, path.join(__dirname, 'capture.py'));
if (!flag('no-build') && capture.build) run(capture.build, [], {shell: true});
if (!flag('no-capture')) {
  for (const {apk, package: id} of capture.install || []) run(python[0], [...python.slice(1), studioPy, 'install', apk, '--package', id, '--serial', serial]);
  for (const locale of locales) {
    for (const spec of capture.runs || [{}]) {
      const wanted = spec.locales || 'all';
      if (wanted === 'default' ? locale !== defaultLocale : Array.isArray(wanted) && !wanted.includes(locale)) continue;
      const runProfiles = spec.profiles === false ? [null] : (spec.profiles || captureProfiles).filter(name => !onlyProfile || name === onlyProfile);
      for (const profile of runProfiles) {
        const args = [...python.slice(1), spec.script || 'store-listing/capture-emulator.py', '--serial', serial, '--locale', locale,
          ...(profile ? ['--profile', profile] : []), ...(spec.args || []).map(expand)];
        const attempts = (capture.retries ?? 2) + 1;
        for (let attempt = 1; ; attempt++) {
          if (run(python[0], args, {check: attempt === attempts})) break;
          console.error(`Capture failed; reset the emulator and try again (${attempt + 1}/${attempts})`);
          run(python[0], [...python.slice(1), studioPy, 'reset', '--serial', serial]);
        }
      }
    }
  }
}
if (!flag('no-export')) {
  for (const locale of locales) {
    if (capture.export) run(capture.export.replaceAll('{locale}', locale), [], {shell: true});
    else run('node', [path.relative(repo, path.join(__dirname, 'export.cjs')), '--locale', locale]);
  }
  for (const command of capture.after || []) run(command, [], {shell: true});
}
console.log(`\nRecaptured ${locales.join(', ')}${dryRun ? ' (dry run)' : ''}.`);
