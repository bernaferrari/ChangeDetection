/**
 * Render every card to a store-ready image and check the composition. Requires Playwright
 * (set PLAYWRIGHT_MODULE to an existing installation if it is not resolvable from here).
 *   node store-listing/studio/export.cjs [--locale de-DE] [--kind phone] [--strict] [--fastlane]
 * Without --locale every locale in studio.json is rendered. --strict fails when a non-default locale
 * still borrows default-locale words or captures, or a callout could not follow its UI element.
 * It also warns (and --strict fails) when a gallery folder holds images no card writes any more, when a
 * continuous background (theme `continuous`) breaks at a seam, and when <locale>/listing.json is over Play's
 * limits. --fastlane writes fastlane/metadata/android/<locale>/ text and screenshots (studio.json "fastlane").
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
const fastlane = process.argv.includes('--fastlane');
const config = JSON.parse(fs.readFileSync(path.join(listing, 'studio.json')));
const profiles = JSON.parse(fs.readFileSync(path.join(listing, 'profiles.json')));
const galleries = JSON.parse(fs.readFileSync(path.join(listing, 'gallery.json')));
const defaultLocale = config.defaultLocale || 'en-US';
// Problems a person would otherwise catch by eye: warnings, errors under --strict.
const problems = [];
const problem = message => { problems.push(message); console.warn(`warning: ${message}`); };
const images = /\.(jpe?g|png|webp)$/i;

// Play's listing limits, in characters.
const LIMITS = [['title', 30], ['shortDescription', 80], ['fullDescription', 4000]];
const listingFor = locale => {
  const file = path.join(listing, locale, 'listing.json');
  if (!fs.existsSync(file)) return null;
  const json = JSON.parse(fs.readFileSync(file));
  return {...json, title: json.title ?? json.appName};
};

// fastlane: "fastlane": {"dir": "fastlane/metadata/android", "screenshots": {"phone": "images/phoneScreenshots"},
// "screenshotLocales": ["en-US"] (or "all"), "locales": {"ru-RU": "ru"}} (all optional; these are the defaults but
// the locale map). Text comes from <locale>/listing.json, screenshots are the gallery's cards as 1.png, 2.png, ….
const lane = {dir: 'fastlane/metadata/android', screenshots: {phone: 'images/phoneScreenshots'}, screenshotLocales: [defaultLocale], locales: {}, ...config.fastlane};
const laneDir = locale => path.join(repo, lane.dir, lane.locales[locale] || locale);
const laneShots = (locale, kind) => fastlane && lane.screenshots[kind] && (lane.screenshotLocales === 'all' || lane.screenshotLocales.includes(locale))
  ? path.join(laneDir(locale), lane.screenshots[kind]) : null;

// The two pixel columns at each edge of a card, with the foreground hidden: the background alone, as RGB rows
// ([outer, inner] on the left, [inner, outer] on the right).
const edgeColumns = async (page, width, height) => {
  await page.evaluate(() => { for (const el of document.querySelectorAll('.device, header, .callout, .device-label, .identity')) el.style.visibility = 'hidden'; });
  const shots = [];
  for (const x of [0, width - 2]) shots.push((await page.screenshot({type: 'png', scale: 'css', clip: {x, y: 0, width: 2, height}})).toString('base64'));
  return page.evaluate(shots => Promise.all(shots.map(async data => {
    const image = new Image();
    image.src = `data:image/png;base64,${data}`;
    await image.decode();
    const {naturalWidth: w, naturalHeight: h} = image;
    const canvas = Object.assign(document.createElement('canvas'), {width: w, height: h});
    canvas.getContext('2d').drawImage(image, 0, 0);
    const px = canvas.getContext('2d').getImageData(0, 0, w, h).data;
    return [0, 1].map(x => { const column = []; for (let y = 0; y < h; y++) for (let c = 0; c < 3; c++) column.push(px[(y * w + x) * 4 + c]); return column; });
  })), shots);
};
// Neighbouring edges match when few rows differ visibly and the average difference stays small. A row may match the
// other side up to 4 rows away (a thin line crossing the seam at a slant) or one column further in (a hairline lying
// on the seam itself).
const seamBreak = ([leftInner, leftOuter], [rightOuter, rightInner]) => {
  let bad = 0, sum = 0;
  const rows = leftOuter.length / 3;
  const pairs = [[leftOuter, rightOuter], [leftOuter, rightInner], [leftInner, rightOuter]];
  for (let r = 0; r < rows; r++) {
    let d = Infinity;
    for (const [a, b] of pairs) for (let s = Math.max(0, r - 4); s <= Math.min(rows - 1, r + 4); s++)
      d = Math.min(d, Math.max(...[0, 1, 2].map(c => Math.abs(a[r * 3 + c] - b[s * 3 + c]))));
    sum += d;
    if (d > 24) bad++;
  }
  return bad / rows > .02 || sum / rows > 6 ? Math.round(bad / rows * 100) : null;
};

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
      const written = new Set();
      const tally = {locale, cards: 0, untranslated: 0, borrowedCaptures: 0, movedCallouts: 0, fixedCallouts: 0, shrunk: 0};
      for (const kind of Object.keys(galleries)) {
        if (arg('kind') && arg('kind') !== kind) continue;
        // README art (a hero, a before/after) is written once, in the default locale.
        if (profiles[kind].defaultLocaleOnly && locale !== defaultLocale) continue;
        const slides = cardsFor(galleries, kind);
        const {width, height} = profiles[kind];
        manifest.galleries[kind] = [];
        const edges = [];
        const shots = laneShots(locale, kind);
        if (shots) {
          fs.mkdirSync(shots, {recursive: true});
          for (const f of fs.readdirSync(shots)) if (images.test(f)) fs.rmSync(path.join(shots, f));
        }
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
          written.add(path.join(dir, file));
          if (shots) fs.writeFileSync(path.join(shots, `${i + 1}.png`), await page.screenshot({type: 'png', scale: 'css'}));
          if (report.continuous && !profiles[kind].defaultLocaleOnly) edges[i] = await edgeColumns(page, width, height);
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
        for (let i = 0; i + 1 < edges.length; i++) {
          const rows = edges[i] && edges[i + 1] && seamBreak(edges[i][1], edges[i + 1][0]);
          if (rows !== null && rows !== undefined) problem(`${locale}/${kind}: the background breaks at the seam between cards ${i + 1} and ${i + 2} (${rows}% of rows differ)`);
        }
        // Contact sheet: the set edge to edge, so seams can be checked at a glance.
        if (slides.length < 2 || profiles[kind].defaultLocaleOnly || (config.contactSheets || {})[kind] === false) continue;
        const h = width > height ? 600 : 960, w = Math.round(h * width / height);
        const sheet = await browser.newPage({viewport: {width: w * slides.length, height: h}});
        await sheet.setContent(`<body style="margin:0;display:flex">${manifest.galleries[kind].map(m => `<img src="${base}/${locale}/${m.file}?t=${Date.now()}" style="height:${h}px;width:${w}px">`).join('')}</body>`);
        await sheet.waitForLoadState('networkidle');
        const sheetName = (config.contactSheets || {})[kind] || `contact-${kind}.jpg`;
        written.add(path.join(dir, sheetName));
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
        written.add(path.join(dir, strip.file));
        await sheet.screenshot({path: path.join(dir, strip.file), type: 'jpeg', quality: config.jpegQuality || 92});
        await sheet.close();
        console.log(`${locale}/${strip.file}  (strip of ${strip.kind} ${strip.cards.map(i => i + 1).join(', ')})`);
      }
      // Stale outputs: images in a folder of cards that no card, contact sheet or strip writes any more.
      if (!arg('kind')) {
        const cards = Object.values(manifest.galleries).flat().map(m => path.join(dir, m.file));
        for (const folder of new Set(cards.map(f => path.dirname(f)))) {
          for (const f of fs.readdirSync(folder)) {
            if (images.test(f) && !written.has(path.join(folder, f))) problem(`${path.relative(listing, path.join(folder, f))} is no longer in the gallery; delete it`);
          }
        }
      }
      const words = listingFor(locale);
      for (const [field, max] of words ? LIMITS : []) {
        const length = typeof words[field] === 'string' ? [...words[field]].length : 0;
        if (length > max) problem(`${locale}/listing.json ${field} has ${length} characters; Play allows ${max}`);
      }
      if (fastlane && words) {
        fs.mkdirSync(laneDir(locale), {recursive: true});
        for (const [field, name] of [['title', 'title'], ['shortDescription', 'short_description'], ['fullDescription', 'full_description']])
          if (typeof words[field] === 'string') fs.writeFileSync(path.join(laneDir(locale), `${name}.txt`), `${words[field].trimEnd()}\n`);
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
  if (problems.length) console.log(`\n${problems.length} warning(s):\n${problems.map(p => `  ${p}`).join('\n')}`);
  if (failures) throw new Error(`--strict: ${failures} locale(s) still borrow ${defaultLocale} words, captures or callout positions`);
  if (strict && problems.length) throw new Error(`--strict: ${problems.length} warning(s) above`);
})().catch(error => { console.error(error.message || error); process.exitCode = 1; });
