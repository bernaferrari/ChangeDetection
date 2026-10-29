/**
 * The icon factory: one drawing, every icon an app ships.
 *   PLAYWRIGHT_MODULE=… node store-listing/studio/icon.cjs [store-listing/brand/icon.json]
 *
 * An app draws its icon once, as SVG layers on Android's 108 x 108 adaptive canvas (launchers show the
 * middle 72, from 18 to 90; keep the mark inside the 66-wide safe circle centred on 54, 54):
 *   store-listing/brand/background.svg   the full-bleed ground
 *   store-listing/brand/foreground.svg   the mark, on transparent
 *   store-listing/brand/monochrome.svg   the mark in one colour, for Android's themed icons (optional)
 * and lists where the icons go in store-listing/brand/icon.json, with paths from the repository root:
 *   {
 *     "android": {"res": "app/src/main/res", "legacy": true},
 *     "files": [
 *       {"path": "store-listing/brand/icon.png", "size": 512, "shape": "square"},
 *       {"path": "web/favicon.svg", "shape": "circle"},
 *       {"path": "web/favicon.ico", "shape": "rounded"},
 *       {"path": "desktop/AppIcon.icns", "shape": "rounded"}
 *     ]
 *   }
 * "opaque": true writes a PNG with no transparency (Apple's 1024px App Store icon must be opaque).
 * A target's "scale" grows or shrinks the mark on its ground (an Apple icon shows the whole square, so
 * its mark is usually larger than Android's safe zone allows). Shapes: "square" (full bleed; Play and app stores mask their own), "rounded" (a 22% corner, like a
 * launcher), "circle", and "mark" (the foreground alone, on transparent); {"layer": "name"} draws one
 * extra layer, brand/name.svg, alone. A path ending in .svg is written as vector, .ico holds 16-256px
 * PNG entries, .icns is built with macOS's iconutil, and any other path is a PNG of the given size.
 * A path ending in .icon is an Apple Icon Composer icon: {"path": "ios/AppIcon.icon", "apple":
 * {"Background": "background", "Mark": "foreground"}} writes each layer as Assets/<Name>.png at 1024px
 * (glass layers stay separate, so iOS can light them), keeps an existing icon.json (its glass, dark and
 * tinted settings are tuned in Icon Composer) or writes a plain one, and checks it with Xcode's ictool.
 *
 * An existing Android vector drawable can become a layer as is: `node icon.cjs --import drawable.xml >
 * brand/foreground.svg` (paths, groups, clips, alphas and gradients).
 *
 * Layers may embed bitmap art: an <image href="art.png"> beside the SVG is inlined when it is read.
 * An app with several icons (launcher shortcuts, say) lists them as {"icons": [{"layers": "todo",
 * "android": {...}, "files": [...]}, ...]}, each drawn in its own brand/<layers>/ folder; a layer missing
 * there comes from brand/ itself, so a variant (a build flavour's tinted ground) redraws only what changes; "monochrome": false on an
 * icon leaves out the themed layer.
 *
 * Android gets adaptive icons from bitmap layers at every density (foreground, background and, when
 * monochrome.svg exists, a themed-icon layer), plus ic_launcher and ic_launcher_round PNGs for older
 * launchers when "legacy" is true. "name" (default ic_launcher) and "type" ("mipmap", the default, or
 * "drawable") match what the manifest refers to. Every run also writes store-listing/brand/icon-preview.png: the icon
 * in every shape, at launcher sizes on light and dark, and themed, to check it at a glance.
 */
const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const {execFileSync} = require('node:child_process');
const {chromium} = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

/** Android VectorDrawable XML as an SVG on the 108 canvas: `node icon.cjs --import drawable.xml > layer.svg`. */
function importVector(file) {
  const text = fs.readFileSync(file, 'utf8').replace(/<!--[\s\S]*?-->/g, '');
  const attrs = tag => Object.fromEntries([...tag.matchAll(/([\w:]+)="([^"]*)"/g)].map(m => [m[1].replace(/^android:/, ''), m[2]]));
  const colour = (value, alpha = 1) => {
    if (!value || value === '@android:color/transparent') return ['none', 1];
    let hex = value.replace('#', '');
    if (hex.length === 3) hex = hex.split('').map(c => c + c).join('');
    if (hex.length === 4) hex = hex.split('').map(c => c + c).join('');
    let a = 1;
    if (hex.length === 8) { a = parseInt(hex.slice(0, 2), 16) / 255; hex = hex.slice(2); }
    return [`#${hex}`, +(a * alpha).toFixed(3)];
  };
  const root = attrs(text.match(/<vector\b[^>]*>/)[0]);
  const vw = parseFloat(root.viewportWidth), vh = parseFloat(root.viewportHeight);
  let out = '', ids = 0;
  const defs = [];
  const tokens = text.match(/<\/?(group|path|clip-path|aapt:attr|gradient|item)\b[^>]*>/g) || [];
  for (let i = 0; i < tokens.length; i++) {
    const token = tokens[i], a = attrs(token);
    if (token.startsWith('</group')) { out += '</g>'; continue; }
    if (token.startsWith('<group')) {
      const px = +(a.pivotX || 0), py = +(a.pivotY || 0);
      const t = [`translate(${+(a.translateX || 0) + px} ${+(a.translateY || 0) + py})`, `rotate(${a.rotation || 0})`, `scale(${a.scaleX || 1} ${a.scaleY || 1})`, `translate(${-px} ${-py})`];
      out += `<g transform="${t.join(' ')}">`;
      if (token.endsWith('/>')) out += '</g>';
    } else if (token.startsWith('<clip-path')) {
      const id = `clip${ids++}`;
      defs.push(`<clipPath id="${id}"><path d="${a.pathData}"/></clipPath>`);
      out += `<g clip-path="url(#${id})">`;
      // A clip applies to the rest of its group; close it when the group closes.
      let depth = 0;
      for (let j = i + 1; j < tokens.length; j++) {
        if (tokens[j].startsWith('<group') && !tokens[j].endsWith('/>')) depth++;
        if (tokens[j].startsWith('</group')) { if (depth === 0) { tokens.splice(j, 0, '</g>'.replace('</g>', '</groupclip>')); break; } depth--; }
      }
    } else if (token === '</groupclip>') {
      out += '</g>';
    } else if (token.startsWith('<path')) {
      let fill = colour(a.fillColor, +(a.fillAlpha ?? 1)), stroke = colour(a.strokeColor, +(a.strokeAlpha ?? 1));
      // A gradient fill or stroke follows as <aapt:attr name="android:fillColor"><gradient …><item …/></gradient>.
      if (!token.endsWith('/>')) {
        for (let j = i + 1; j < tokens.length && !tokens[j].startsWith('</path'); j++) {
          const attr = tokens[j].match(/name="android:(fillColor|strokeColor)"/);
          if (!attr) continue;
          const g = attrs(tokens[j + 1]), id = `grad${ids++}`, stops = [];
          for (let k = j + 2; k < tokens.length && tokens[k].startsWith('<item'); k++) {
            const item = attrs(tokens[k]), [c, o] = colour(item.color);
            stops.push(`<stop offset="${item.offset}" stop-color="${c}" stop-opacity="${o}"/>`);
          }
          defs.push(g.type === 'radial'
            ? `<radialGradient id="${id}" gradientUnits="userSpaceOnUse" cx="${g.centerX}" cy="${g.centerY}" r="${g.gradientRadius}">${stops.join('')}</radialGradient>`
            : `<linearGradient id="${id}" gradientUnits="userSpaceOnUse" x1="${g.startX}" y1="${g.startY}" x2="${g.endX}" y2="${g.endY}">${stops.join('')}</linearGradient>`);
          if (attr[1] === 'fillColor') fill = [`url(#${id})`, 1]; else stroke = [`url(#${id})`, 1];
        }
      }
      const parts = [`d="${a.pathData}"`, `fill="${fill[0]}"`];
      if (fill[1] !== 1) parts.push(`fill-opacity="${fill[1]}"`);
      if (a.fillType === 'evenOdd') parts.push('fill-rule="evenodd"');
      if (stroke[0] !== 'none' && +(a.strokeWidth ?? 0) > 0) {
        parts.push(`stroke="${stroke[0]}"`, `stroke-width="${a.strokeWidth}"`);
        if (stroke[1] !== 1) parts.push(`stroke-opacity="${stroke[1]}"`);
        if (a.strokeLineCap) parts.push(`stroke-linecap="${a.strokeLineCap}"`);
        if (a.strokeLineJoin) parts.push(`stroke-linejoin="${a.strokeLineJoin}"`);
        if (a.strokeMiterLimit) parts.push(`stroke-miterlimit="${a.strokeMiterLimit}"`);
      }
      out += `<path ${parts.join(' ')}/>`;
    }
  }
  const scale = `scale(${108 / vw} ${108 / vh})`;
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">\n  <!-- Imported from ${path.basename(file)}. -->\n  <defs>${defs.join('')}</defs>\n  <g transform="${scale}">${out}</g>\n</svg>\n`;
}
if (process.argv[2] === '--import') {
  process.stdout.write(importVector(path.resolve(process.argv[3])));
  process.exit(0);
}

const specFile = path.resolve(process.argv[2] || path.join(__dirname, '..', 'brand', 'icon.json'));
const brand = path.dirname(specFile);
const root = path.resolve(brand, '..', '..');
const spec = JSON.parse(fs.readFileSync(specFile, 'utf8'));
const rel = file => path.relative(root, file);
const MIME = {'.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.webp': 'image/webp', '.svg': 'image/svg+xml'};

/** A layer's inner markup, ids prefixed so layers can share one document, bitmap art inlined. */
function readLayer(dir, name, required = true) {
  // A variant folder (an alpha build's icon, say) redraws only what changes and inherits the rest.
  let file = path.join(dir, `${name}.svg`);
  if (!fs.existsSync(file) && dir !== brand) file = path.join(brand, `${name}.svg`);
  dir = path.dirname(file);
  if (!fs.existsSync(file)) {
    if (required) throw new Error(`Missing ${rel(file)}`);
    return null;
  }
  const text = fs.readFileSync(file, 'utf8');
  const open = text.match(/<svg\b[^>]*>/);
  if (!open || !/viewBox="0 0 108 108"/.test(open[0])) throw new Error(`${rel(file)} must be an <svg viewBox="0 0 108 108">`);
  const prefix = name.replace(/[^a-z0-9]/gi, '');
  return text.slice(open.index + open[0].length, text.lastIndexOf('</svg>'))
    .replace(/\bid="([^"]+)"/g, `id="${prefix}-$1"`)
    .replace(/url\(#([^)]+)\)/g, `url(#${prefix}-$1)`)
    .replace(/(xlink:)?href="#([^"]+)"/g, `href="#${prefix}-$2"`)
    .replace(/(xlink:)?href="(?!#|data:|https?:)([^"]+)"/g, (_, __, art) => {
      const source = path.join(dir, art);
      return `href="data:${MIME[path.extname(art).toLowerCase()] || 'application/octet-stream'};base64,${fs.readFileSync(source).toString('base64')}"`;
    });
}

const CLIP = {
  square: '<rect x="18" y="18" width="72" height="72"/>',
  rounded: '<rect x="18" y="18" width="72" height="72" rx="16"/>',
  circle: '<circle cx="54" cy="54" r="36"/>',
};
/** A single layer on the full 108 canvas, as Android's adaptive icon expects. */
const canvas = inner => `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">${inner}</svg>`;
/** Markup drawn alone on the launcher's visible 72 x 72. */
const visible = inner => `<svg xmlns="http://www.w3.org/2000/svg" viewBox="18 18 72 72">${inner}</svg>\n`;

function write(file, data) {
  fs.mkdirSync(path.dirname(file), {recursive: true});
  fs.writeFileSync(file, data);
  console.log(rel(file));
}

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  async function raster(svg, size, opaque = false) {
    await page.setViewportSize({width: size, height: size});
    await page.setContent(`<style>html,body{margin:0;background:transparent}</style><img src="data:image/svg+xml;base64,${Buffer.from(svg).toString('base64')}" width="${size}" height="${size}" style="display:block">`);
    await page.waitForFunction(() => document.images[0].complete && document.images[0].naturalWidth > 0);
    return page.screenshot({omitBackground: !opaque, clip: {x: 0, y: 0, width: size, height: size}});
  }

  async function appleIcon(bundle, layers, dir) {
    for (const [name, layerName] of Object.entries(layers)) {
      // The background fills the square; every other layer floats on transparent for the glass.
      write(path.join(bundle, 'Assets', `${name}.png`), await raster(visible(readLayer(dir, layerName)), 1024));
    }
    const json = path.join(bundle, 'icon.json');
    if (!fs.existsSync(json)) {
      const names = Object.keys(layers).filter(name => layers[name] !== 'background');
      write(json, JSON.stringify({
        groups: [{layers: [...names.map(name => ({'image-name': `${name}.png`, name})),
          ...(Object.values(layers).includes('background') ? [{'image-name': 'Background.png', name: 'Background', glass: false}] : [])],
          shadow: {kind: 'neutral', opacity: 0.3}, translucency: {enabled: true, value: 0.15}}],
        'supported-platforms': {squares: 'shared'},
      }, null, 2) + '\n');
    }
    if (process.platform === 'darwin' && fs.existsSync('/usr/bin/xcrun')) {
      const out = fs.mkdtempSync(path.join(os.tmpdir(), 'apple-icon-'));
      try {
        execFileSync('/usr/bin/xcrun', ['ictool', bundle, '--compile', out, '--platform', 'iphoneos', '--minimum-deployment-target', '26.0'], {stdio: 'pipe'});
      } catch (error) {
        throw new Error(`Xcode rejected ${rel(bundle)}: ${error.stderr || error.message}`);
      }
    }
  }

  const jobs = spec.icons || [spec];
  for (const job of jobs) {
    const dir = path.join(brand, job.layers || '.');
    const background = readLayer(dir, 'background');
    const foreground = readLayer(dir, 'foreground');
    const monochrome = job.monochrome === false ? null : readLayer(dir, 'monochrome', false);
    /** The icon as one SVG document, showing the launcher's visible 72 x 72. */
    const icon = (shape, scale = 1) => {
      const mark = scale === 1 ? foreground : `<g transform="translate(54 54) scale(${scale}) translate(-54 -54)">${foreground}</g>`;
      if (shape === 'mark') return visible(mark);
      if (!CLIP[shape]) throw new Error(`Unknown shape "${shape}"; use square, rounded, circle or mark`);
      return visible(`<defs><clipPath id="icon-shape">${CLIP[shape]}</clipPath></defs><g clip-path="url(#icon-shape)">${background}${mark}</g>`);
    };
    if (job.android) {
      const res = path.join(root, job.android.res);
      const name = job.android.name || 'ic_launcher';
      const type = job.android.type || 'mipmap';
      const densities = {mdpi: 1, hdpi: 1.5, xhdpi: 2, xxhdpi: 3, xxxhdpi: 4};
      for (const [density, factor] of Object.entries(densities)) {
        const folder = path.join(res, `${type}-${density}`);
        write(path.join(folder, `${name}_foreground.png`), await raster(canvas(foreground), 108 * factor));
        write(path.join(folder, `${name}_background.png`), await raster(canvas(background), 108 * factor));
        if (monochrome) write(path.join(folder, `${name}_monochrome.png`), await raster(canvas(monochrome), 108 * factor));
        if (job.android.legacy) {
          write(path.join(folder, `${name}.png`), await raster(icon('rounded'), 48 * factor));
          if (job.android.round !== false) write(path.join(folder, `${name}_round.png`), await raster(icon('circle'), 48 * factor));
        }
      }
      const layers = [['background', 'background'], ['foreground', 'foreground'], ...(monochrome ? [['monochrome', 'monochrome']] : [])];
      const adaptive = [
        '<?xml version="1.0" encoding="utf-8"?>',
        '<!-- Generated by store-listing/studio/icon.cjs from store-listing/brand. -->',
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">',
        ...layers.map(([tag, suffix]) => `    <${tag} android:drawable="@${type}/${name}_${suffix}" />`),
        '</adaptive-icon>', '',
      ].join('\n');
      write(path.join(res, `${type}-anydpi-v26/${name}.xml`), adaptive);
      if (job.android.round !== false) write(path.join(res, `${type}-anydpi-v26/${name}_round.xml`), adaptive);
    }

    for (const target of job.files || []) {
      const file = path.join(root, target.path);
      const shape = target.shape || 'rounded';
      const draw = () => icon(shape, target.scale || 1);
      if (file.endsWith('.icon')) {
        await appleIcon(file, target.apple || {Background: 'background', Mark: 'foreground'}, dir);
      } else if (target.layer) {
        if (!target.size) throw new Error(`${target.path} needs a "size"`);
        write(file, await raster(visible(readLayer(dir, target.layer)), target.size));
      } else if (file.endsWith('.svg')) {
        write(file, draw());
      } else if (file.endsWith('.ico')) {
        // PNG-compressed entries keep every Windows size crisp.
        const sizes = [16, 24, 32, 48, 64, 128, 256];
        const images = [];
        for (const size of sizes) images.push(await raster(draw(), size));
        const header = Buffer.alloc(6 + sizes.length * 16);
        header.writeUInt16LE(0, 0); header.writeUInt16LE(1, 2); header.writeUInt16LE(sizes.length, 4);
        let offset = header.length;
        sizes.forEach((size, i) => {
          const at = 6 + i * 16;
          header.writeUInt8(size === 256 ? 0 : size, at); header.writeUInt8(size === 256 ? 0 : size, at + 1);
          header.writeUInt16LE(1, at + 4); header.writeUInt16LE(32, at + 6);
          header.writeUInt32LE(images[i].length, at + 8); header.writeUInt32LE(offset, at + 12);
          offset += images[i].length;
        });
        write(file, Buffer.concat([header, ...images]));
      } else if (file.endsWith('.icns')) {
        if (process.platform !== 'darwin') { console.warn(`Skipped ${target.path}: iconutil needs macOS`); continue; }
        const set = fs.mkdtempSync(path.join(os.tmpdir(), 'icon-')) + '/AppIcon.iconset';
        fs.mkdirSync(set);
        for (const size of [16, 32, 128, 256, 512]) {
          fs.writeFileSync(path.join(set, `icon_${size}x${size}.png`), await raster(draw(), size));
          fs.writeFileSync(path.join(set, `icon_${size}x${size}@2x.png`), await raster(draw(), size * 2));
        }
        fs.mkdirSync(path.dirname(file), {recursive: true});
        execFileSync('/usr/bin/iconutil', ['-c', 'icns', '-o', file, set]);
        console.log(rel(file));
      } else {
        if (!target.size) throw new Error(`${target.path} needs a "size"`);
        write(file, await raster(draw(), target.size, target.opaque));
      }
    }

    // The preview: every shape large, launcher sizes on light and dark, and the themed icon.
    const img = (svg, size) => `<img width="${size}" height="${size}" src="data:image/svg+xml;base64,${Buffer.from(svg).toString('base64')}">`;
    const themed = monochrome
      ? `<svg xmlns="http://www.w3.org/2000/svg" viewBox="18 18 72 72"><circle cx="54" cy="54" r="36" fill="#3b4a3a"/><g style="filter:brightness(0) invert(.9) sepia(.3) hue-rotate(60deg)">${monochrome}</g></svg>`
      : null;
    const row = (bg, sizes) => `<div style="background:${bg};border-radius:18px;padding:18px;display:flex;gap:18px;align-items:end">${sizes.map(([s, shape]) => img(icon(shape), s)).join('')}${themed ? img(themed, sizes[0][0]) : ''}</div>`;
    await page.setViewportSize({width: 1200, height: 600});
    await page.setContent(`<body style="margin:0;padding:24px;background:#fff;font:14px sans-serif;display:flex;flex-direction:column;gap:18px">
      <div style="display:flex;gap:24px;align-items:end">${img(icon('square'), 220)}${img(icon('rounded'), 180)}${img(icon('circle'), 180)}${img(icon('mark'), 180)}</div>
      ${row('#ece6e2', [[72, 'rounded'], [48, 'circle'], [32, 'rounded'], [16, 'rounded']])}
      ${row('#1d1b20', [[72, 'rounded'], [48, 'circle'], [32, 'rounded'], [16, 'rounded']])}
    </body>`);
    await page.waitForTimeout(200);
    write(path.join(dir, 'icon-preview.png'), await page.screenshot({fullPage: true}));
  }
  await browser.close();
})().catch(error => { console.error(error.message); process.exit(1); });
