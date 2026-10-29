// ChangeDetection: the app in its Blue palette on the app icon's blue, Play cards and README alike. White type, and
// the key words get the app's yellow highlighter over their full height, knocked out in blue.
const V = {field: '#2f6cf2', ink: '#ffffff', body: '#ffffffc7', marker: '#ffe58f', em: '#1f4fd8'};

// Lit softly from the top and a touch deeper at the foot. Every card gets the same light, so seams always meet.
function field(W, H) {
  return `<defs><radialGradient id="top" cx=".5" cy="-.15" r=".9"><stop offset="0" stop-color="#ffffff" stop-opacity=".22"/><stop offset="1" stop-color="#ffffff" stop-opacity="0"/></radialGradient>
    <linearGradient id="foot" x1="0" y1="0" x2="0" y2="1"><stop offset=".55" stop-color="#001a66" stop-opacity="0"/><stop offset="1" stop-color="#001a66" stop-opacity=".18"/></linearGradient></defs>
    <rect width="${W}" height="${H}" fill="${V.field}"/><rect width="${W}" height="${H}" fill="url(#top)"/><rect width="${W}" height="${H}" fill="url(#foot)"/>`;
}

// A highlighter stroke over the full height of the words, with slightly uneven ends like a real pen.
const stroke = colour => `url("data:image/svg+xml,${encodeURIComponent(`<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100' preserveAspectRatio='none'><path d='M1.2 6 L98.6 2 L100 48 L98.8 97 L0.6 99 L0 52 Z' fill='${colour}'/></svg>`)}")`;

export default {
  // Every card shares the same field, so the strip joins at every seam.
  continuous: true,

  art({W, H}) {
    return field(W, H);
  },

  compose({main, slide}) {
    main.style.setProperty('--marker', stroke(slide.marker || V.marker));
    main.style.setProperty('--ink', V.ink);
    main.style.setProperty('--em-ink', V.em);
    main.style.setProperty('--body', V.body);
  }
};
