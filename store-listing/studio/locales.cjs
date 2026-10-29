/**
 * Languages at a glance, and adding new ones.
 *   node store-listing/studio/locales.cjs                 coverage: words translated, captures present, layout sidecars
 *   node store-listing/studio/locales.cjs --init de-DE    add a locale: empty copy fields to fill in the editor
 * Empty strings count as untranslated: the renderer falls back to the default locale and reports them.
 */
const fs = require('node:fs');
const path = require('node:path');
const listing = path.resolve(__dirname, '..');
const read = (file, fallback) => fs.existsSync(file) ? JSON.parse(fs.readFileSync(file)) : fallback;
const configFile = path.join(listing, 'studio.json');
const config = read(configFile, {});
const defaultLocale = config.defaultLocale || 'en-US';
const galleries = read(path.join(listing, 'gallery.json'), {});
const defaultCopy = read(path.join(listing, defaultLocale, 'copy.json'), {});

(async () => {
  const {cardsFor, devicesFor, nodesFile} = await import('./gallery.js');
  const init = process.argv.indexOf('--init');
  if (init > 0) {
    const locale = process.argv[init + 1];
    if (!/^[a-z]{2,3}(-[A-Za-z0-9]{2,8})*$/.test(locale || '')) throw new Error('Usage: --init <locale>, e.g. de-DE');
    const file = path.join(listing, locale, 'copy.json');
    const copy = read(file, {});
    for (const [key, fields] of Object.entries(defaultCopy)) {
      copy[key] ||= {};
      for (const field of Object.keys(fields)) if (!(field in copy[key])) copy[key][field] = '';
    }
    fs.mkdirSync(path.dirname(file), {recursive: true});
    fs.writeFileSync(file, JSON.stringify(copy, null, 2) + '\n');
    config.locales = [...new Set([...(config.locales || [defaultLocale]), locale])];
    fs.writeFileSync(configFile, JSON.stringify(config, null, 2) + '\n');
    console.log(`Added ${locale}: fill ${path.relative(process.cwd(), file)} in the editor, and capture into ${locale}/raw/.`);
    return;
  }
  const profiles = read(path.join(listing, 'profiles.json'), {});
  const rawsFor = locale => new Set(Object.keys(galleries)
    .filter(kind => locale === defaultLocale || !profiles[kind]?.defaultLocaleOnly)
    .flatMap(kind => cardsFor(galleries, kind).flatMap(card => [
      ...devicesFor(card).map(d => d.raw), ...(card.callouts || []).map(c => c.raw).filter(Boolean)
    ])));
  const raws = rawsFor(defaultLocale);
  const fields = Object.values(defaultCopy).reduce((n, f) => n + Object.keys(f).length, 0);
  console.log(`${raws.size} captures and ${fields} copy fields per locale (default ${defaultLocale})\n`);
  console.log('locale      words        captures     layout sidecars');
  for (const locale of config.locales || [defaultLocale]) {
    const raws = rawsFor(locale);
    const copy = read(path.join(listing, locale, 'copy.json'), {});
    let done = 0;
    for (const [key, f] of Object.entries(defaultCopy)) for (const field of Object.keys(f)) if (copy[key]?.[field]?.trim()) done++;
    const present = [...raws].filter(r => fs.existsSync(path.join(listing, locale, 'raw', r))).length;
    const sidecars = [...raws].filter(r => fs.existsSync(path.join(listing, locale, 'raw', nodesFile(r)))).length;
    console.log(`${locale.padEnd(10)}  ${`${done}/${fields}`.padEnd(11)}  ${`${present}/${raws.size}`.padEnd(11)}  ${sidecars}/${raws.size}`);
  }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
