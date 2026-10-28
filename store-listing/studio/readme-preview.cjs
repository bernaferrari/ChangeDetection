/**
 * Render the repository README the way GitHub shows it and save a full-page screenshot.
 *   node store-listing/studio/readme-preview.cjs [--out /tmp/readme.png] [--width 1012]
 * Uses GitHub's Markdown API through the gh CLI (so HTML and tables render exactly as on github.com) and
 * GitHub's light stylesheet. Relative image paths load from the working tree, so unpushed images show too.
 */
const fs = require('node:fs');
const path = require('node:path');
const {execFileSync} = require('node:child_process');
const {chromium} = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const repo = path.resolve(__dirname, '..', '..');
const arg = (name, fallback) => { const i = process.argv.indexOf(`--${name}`); return i > 0 ? process.argv[i + 1] : fallback; };
const out = path.resolve(arg('out', path.join(require('node:os').tmpdir(), 'readme-preview.png')));
const width = Number(arg('width', 1012));

const body = execFileSync('gh', ['api', '-X', 'POST', '/markdown', '-f', `text=${fs.readFileSync(path.join(repo, 'README.md'), 'utf8')}`, '-f', 'mode=gfm'], {encoding: 'utf8'});
const html = `<!doctype html><meta charset="utf-8"><base href="file://${repo}/">
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/github-markdown-css@5/github-markdown-light.css">
<style>body{margin:0;background:#fff}.markdown-body{box-sizing:border-box;max-width:${width}px;margin:0 auto;padding:32px}</style>
<article class="markdown-body">${body}</article>`;
const file = path.join(require('node:os').tmpdir(), 'readme-preview.html');
fs.writeFileSync(file, html);

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({viewport: {width: width + 64, height: 900}, deviceScaleFactor: 1});
  await page.goto(`file://${file}`);
  await page.waitForLoadState('networkidle');
  const broken = await page.evaluate(() => [...document.images].filter(i => !i.naturalWidth && !/^https?:/.test(i.getAttribute('src'))).map(i => i.getAttribute('src')));
  await page.screenshot({path: out, fullPage: true});
  await browser.close();
  console.log(out);
  if (broken.length) { console.error(`Missing images:\n${broken.join('\n')}`); process.exitCode = 1; }
})();
