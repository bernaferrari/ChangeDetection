// ChangeDetection: one light field under the whole set, from the app icon's blue into the yellow the app marks
// changes with, and back, so the first four cards (all most people see) hold the whole arc and every seam continues.

// One deep cobalt ink for every word, with the app's yellow highlighter under the key ones.
const V = {light: .6, ink: '#0d1b6b', em: '#0d1b6b', marker: '#ffe98a', body: '#0d1b6bc7'};
const FIELD = ['#c9d6ff', '#d9e2ff', '#e8ecff', '#f6f1dc', '#fff0b8', '#ffe79a'];
function field(W, H, index, count) {
  const there = [...FIELD, ...FIELD.slice(0, -1).reverse()];
  const stops = there.map((c, i) => `<stop offset="${(i / (there.length - 1)).toFixed(3)}" stop-color="${c}"/>`).join('');
  return `<defs><linearGradient id="field" gradientUnits="userSpaceOnUse" x1="${-index * W}" y1="0" x2="${(count - index) * W}" y2="0">${stops}</linearGradient>
    <linearGradient id="light" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#ffffff" stop-opacity="${V.light}"/><stop offset=".45" stop-color="#ffffff" stop-opacity="0"/></linearGradient></defs>
    <rect width="${W}" height="${H}" fill="url(#field)"/><rect width="${W}" height="${H}" fill="url(#light)"/>`;
}

// README art: the app icon's blue, lit softly from the top and a touch deeper at the foot, behind the app in its Blue palette.
const README = {field: '#2f6cf2', marker: '#ffe58f', em: '#1f4fd8'};
function readmeField(W, H) {
  return `<defs><radialGradient id="top" cx=".5" cy="-.15" r=".9"><stop offset="0" stop-color="#ffffff" stop-opacity=".22"/><stop offset="1" stop-color="#ffffff" stop-opacity="0"/></radialGradient>
    <linearGradient id="foot" x1="0" y1="0" x2="0" y2="1"><stop offset=".55" stop-color="#001a66" stop-opacity="0"/><stop offset="1" stop-color="#001a66" stop-opacity=".18"/></linearGradient></defs>
    <rect width="${W}" height="${H}" fill="${README.field}"/><rect width="${W}" height="${H}" fill="url(#top)"/><rect width="${W}" height="${H}" fill="url(#foot)"/>`;
}

// A highlighter stroke: flat yellow over the lower part of the words, with slightly uneven ends like a real pen.
const marker = colour => `url("data:image/svg+xml,${encodeURIComponent(`<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100' preserveAspectRatio='none'><path d='M1.5 40 L98 36 L99.5 60 L98.6 91 L1 94 L0 70 Z' fill='${colour}'/></svg>`)}")`;

// The same pen, but over the full height of the words.
const stroke = colour => `url("data:image/svg+xml,${encodeURIComponent(`<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100' preserveAspectRatio='none'><path d='M1.2 6 L98.6 2 L100 48 L98.8 97 L0.6 99 L0 52 Z' fill='${colour}'/></svg>`)}")`;

export default {
  art({W, H, index, slide, slides}) {
    if (W === 2400 && H === 1260) return readmeField(W, H);
    return field(W, H, index, slides.length);
  },

  compose({main, slide, W, H}) {
    const readme = W === 2400 && H === 1260;
    // On the README's blue, the stroke covers the whole word and the word is knocked out in blue.
    main.style.setProperty('--marker', readme ? stroke(README.marker) : marker(slide.marker || V.marker));
    main.style.setProperty('--ink', readme ? '#ffffff' : V.ink);
    main.style.setProperty('--em-ink', readme ? README.em : V.em);
    main.style.setProperty('--body', readme ? '#ffffffc7' : V.body);
    // The README's first image is the app's name card: icon and name, large, over the real app; nothing else.
    if (slide.identity) {
      main.querySelector('header')?.remove();
      const id = document.createElement('div');
      id.className = 'identity';
      id.dataset.checkBounds = '';
      id.innerHTML = '<img src="../../website/public/logo.png" alt="">ChangeDetection';
      main.append(id);
    }
  },

  /** Bezel and corners scale with the device width. */
  layout({main}) {
    for (const node of main.querySelectorAll('.device')) {
      const image = node.querySelector('img'), width = node.clientWidth, landscape = image.naturalWidth > image.naturalHeight;
      node.style.setProperty('--bezel', `${Math.round(width * (landscape ? .011 : .02))}px`);
      node.style.setProperty('--radius', `${Math.round(width * (landscape ? .035 : .09))}px`);
    }
  }
};
