/**
 * Render every card to a store-ready image and check the composition. Requires Playwright
 * (set PLAYWRIGHT_MODULE to an existing installation if it is not resolvable from here).
 *   node store-listing/studio/export.cjs [--locale de-DE] [--kind phone] [--strict]
 * Without --locale every locale in studio.json is rendered. --strict fails when a non-default locale
 * still borrows default-locale words or captures, or a callout could not follow its UI element.
 */
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const {serve} = require('./server.cjs');
const {chromium} = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const listing = path.resolve(__dirname, '..');
const repo = path.resolve(listing, '..');
const arg = name => { const i = process.argv.indexOf(`--${name}`); return i > 0 ? process.argv[i + 1] : null; };
const strict = process.argv.includes('--strict');
const config = JSON.parse(fs.readFileSync(path.join(listing, 'studio.json')));
const profiles = JSON.parse(fs.readFileSync(path.join(listing, 'profiles.json')));
const galleries = JSON.parse(fs.readFileSync(path.join(listing, 'gallery.json')));
const defaultLocale = config.defaultLocale || 'en-US';

(async () => {
  const {cardsFor, devicesFor, outputFile} = await import('./gallery.js');
  const server = await serve(repo);
  const base = `http://127.0.0.1:${server.address().port}/${path.relative(repo, listing).split(path.sep).join('/')}`;
  const browser = await chromium.launch();
  const summary = [];
  let failures = 0;
  try {
    for (const locale of arg('locale') ? [arg('locale')] : config.locales || [defaultLocale]) {
      const dir = path.join(listing, locale);
      const manifest = {galleries: {}};
      const tally = {locale, cards: 0, untranslated: 0, borrowedCaptures: 0, movedCallouts: 0, fixedCallouts: 0, shrunk: 0};
      for (const kind of Object.keys(galleries)) {
        if (arg('kind') && arg('kind') !== kind) continue;
        // README art (a hero, a before/after) is written once, in the default locale.
        if (profiles[kind].defaultLocaleOnly && locale !== defaultLocale) continue;
        const slides = cardsFor(galleries, kind);
        const {width, height} = profiles[kind];
        manifest.galleries[kind] = [];
        for (const [i, slide] of slides.entries()) {
          const file = outputFile(kind, slide, i);
          // studio.json "supersample": 2 renders at twice the resolution and downsamples, which keeps rotated and
          // scaled UI as crisp as the captures themselves.
          const supersample = config.supersample || 1;
          const page = await browser.newPage({viewport: {width, height}, deviceScaleFactor: supersample, locale});
          const errors = [];
          page.on('pageerror', e => errors.push(e.message));
          page.on('requestfailed', r => errors.push(`Failed to load ${r.url()}`));
          page.on('response', r => {
            // Optional files (locale copy, a locale's capture, layout sidecars) may be missing; everything else must load.
            if (r.status() >= 400 && !/\/(copy|[^/]+\.nodes)\.json$|\/raw\//.test(new URL(r.url()).pathname)) errors.push(`${r.status()} ${r.url()}`);
          });
          await page.goto(`${base}/studio/preview.html?locale=${locale}&kind=${kind}&slide=${i}`);
          await page.locator('html[data-ready="true"]').waitFor({timeout: 30_000}).catch(() => { throw new Error(`${locale}/${file}: ${errors.join('\n') || 'never became ready'}`); });
          const report = JSON.parse(await page.locator('html').getAttribute('data-report'));
          const label = `${locale}/${file}`;
          const fonts = await page.evaluate(() => [...document.fonts].map(f => f.status));
          assert(!fonts.includes('error') && (!slide.title || fonts.includes('loaded')), `Fonts failed to load: ${label}`);
          assert(report.fit > 0, `Copy does not fit even at ${config.minFit || .72}× size; shorten it: ${label}`);
          const inside = b => b && b.x >= -.5 && b.y >= -.5 && b.x + b.width <= width + .5 && b.y + b.height <= height + .5;
          for (const el of await page.locator('[data-check-bounds]').all()) assert(inside(await el.boundingBox()), `Callout or label leaves the canvas: ${label}`);
          assert.equal(errors.length, 0, errors.join('\n'));
          const png = file.endsWith('.png');
          const quality = config.jpegQuality || 94;
          // scale: 'css' downsamples a supersampled render to the canvas size, keeping opaque PNGs opaque.
          const shot = await page.screenshot({type: png ? 'png' : 'jpeg', scale: 'css', omitBackground: png && !!slide.transparent, ...(png ? {} : {quality})});
          // Stores want transparent-capable art (an app icon) as 32-bit PNG even when every pixel is opaque;
          // re-encoding through a canvas always writes RGBA.
          const bytes = png && slide.transparent ? Buffer.from(await page.evaluate(async data => {
            const image = new Image();
            image.src = `data:image/png;base64,${data}`;
            await image.decode();
            const canvas = Object.assign(document.createElement('canvas'), {width: image.naturalWidth, height: image.naturalHeight});
            canvas.getContext('2d').drawImage(image, 0, 0);
            return canvas.toDataURL('image/png').split(',')[1];
          }, shot.toString('base64')), 'base64') : shot;
          fs.mkdirSync(path.dirname(path.join(dir, file)), {recursive: true});
          fs.writeFileSync(path.join(dir, file), bytes);
          await page.close();

          const notes = [];
          if (locale !== defaultLocale) {
            tally.movedCallouts += report.movedCallouts.length;
            if (report.missingCopy.length) { tally.untranslated++; notes.push(`untranslated: ${report.missingCopy.join(', ')}`); }
            if (report.fallbackCaptures.length) { tally.borrowedCaptures++; notes.push(`${defaultLocale} capture: ${report.fallbackCaptures.join(', ')}`); }
            if (report.fixedCallouts.length) { tally.fixedCallouts++; notes.push('callout kept its default-locale position'); }
          }
          if (report.fit < 1) { tally.shrunk++; notes.push(`copy shrunk to ${Math.round(report.fit * 100)}%`); }
          tally.cards++;
          manifest.galleries[kind].push({file, feature: slide.feature, slide: i, width, height,
            bytes: bytes.length, sources: devicesFor(slide).map(d => `raw/${d.raw}`), ...(notes.length ? {notes} : {})});
          console.log(`${label}${notes.length ? `  (${notes.join('; ')})` : ''}`);
        }
        // Contact sheet: the set edge to edge, so seams can be checked at a glance.
        if (slides.length < 2 || profiles[kind].defaultLocaleOnly || (config.contactSheets || {})[kind] === false) continue;
        const h = width > height ? 600 : 960, w = Math.round(h * width / height);
        const sheet = await browser.newPage({viewport: {width: w * slides.length, height: h}});
        await sheet.setContent(`<body style="margin:0;display:flex">${manifest.galleries[kind].map(m => `<img src="${base}/${locale}/${m.file}?t=${Date.now()}" style="height:${h}px;width:${w}px">`).join('')}</body>`);
        await sheet.waitForLoadState('networkidle');
        const sheetName = (config.contactSheets || {})[kind] || `contact-${kind}.jpg`;
        fs.mkdirSync(path.dirname(path.join(dir, sheetName)), {recursive: true});
        await sheet.screenshot({path: path.join(dir, sheetName), type: 'jpeg', quality: 90});
        await sheet.close();
      }
      // README strips: a run of real store cards joined edge to edge, as the store carousel shows them.
      for (const strip of locale === defaultLocale ? config.readmeStrips || [] : []) {
        if (arg('kind') && arg('kind') !== strip.kind && arg('kind') !== 'strips') continue;
        const {width, height} = profiles[strip.kind];
        const shots = [];
        for (const [n, i] of strip.cards.entries()) {
          const position = strip.cards.length === 1 ? 'only' : n === 0 ? 'first' : n === strip.cards.length - 1 ? 'last' : 'middle';
          const page = await browser.newPage({viewport: {width, height}, deviceScaleFactor: config.supersample || 1, locale});
          await page.goto(`${base}/studio/preview.html?locale=${locale}&kind=${strip.kind}&slide=${i}&strip=${position}`);
          await page.locator('html[data-ready="true"]').waitFor({timeout: 30_000});
          shots.push((await page.screenshot({type: 'png', scale: 'css'})).toString('base64'));
          await page.close();
        }
        const h = strip.height || 1200, w = Math.round(h * width / height);
        const sheet = await browser.newPage({viewport: {width: w * shots.length, height: h}});
        await sheet.setContent(`<body style="margin:0;display:flex">${shots.map(s => `<img src="data:image/png;base64,${s}" style="height:${h}px;width:${w}px">`).join('')}</body>`);
        await sheet.waitForLoadState('load');
        fs.mkdirSync(path.dirname(path.join(dir, strip.file)), {recursive: true});
        await sheet.screenshot({path: path.join(dir, strip.file), type: 'jpeg', quality: config.jpegQuality || 92});
        await sheet.close();
        console.log(`${locale}/${strip.file}  (strip of ${strip.kind} ${strip.cards.map(i => i + 1).join(', ')})`);
      }
      if (config.writeAssets !== false && !arg('kind')) fs.writeFileSync(path.join(dir, 'assets.json'), JSON.stringify(manifest, null, 2) + '\n');
      summary.push(tally);
      if (strict && locale !== defaultLocale && (tally.untranslated || tally.borrowedCaptures || tally.fixedCallouts)) failures++;
    }
  } finally {
    await browser.close();
    server.close();
  }
  console.log('\nlocale      cards  untranslated  borrowed captures  moved callouts  unresolved callouts  shrunk copy');
  for (const t of summary) console.log(`${t.locale.padEnd(10)}  ${String(t.cards).padStart(5)}  ${String(t.untranslated).padStart(12)}  ${String(t.borrowedCaptures).padStart(17)}  ${String(t.movedCallouts).padStart(14)}  ${String(t.fixedCallouts).padStart(19)}  ${String(t.shrunk).padStart(11)}`);
  if (failures) throw new Error(`--strict: ${failures} locale(s) still borrow ${defaultLocale} words, captures or callout positions`);
})().catch(error => { console.error(error.message || error); process.exitCode = 1; });
