// Small helpers themes share: colors, panorama coordinates, and Material 3 Expressive shapes.

export const rgb = color => color.match(/[0-9a-f]{2}/gi).map(channel => parseInt(channel, 16));
export const mix = (color, target, t) => `rgb(${rgb(color).map((v, i) => Math.round(v + (target[i] - v) * t)).join(',')})`;
export const isDark = color => {
  const [r, g, b] = rgb(color);
  return (r * 299 + g * 587 + b * 114) / 1000 < 128;
};

/** x position of a panorama coordinate (in card widths across the whole gallery) on this card. */
export const panoramaX = ({index, W}, x) => (x - index) * W;

/** A horizontal gradient spanning the whole gallery; each card shows its own window of it. */
export const panoramaGradient = ({index, W, slides}, id, colors) => {
  const stops = colors.map((c, i) => `<stop offset="${i / (colors.length - 1)}" stop-color="${c}"/>`).join('');
  return `<linearGradient id="${id}" gradientUnits="userSpaceOnUse" x1="${-index * W}" x2="${(slides.length - index) * W}" y1="0" y2="0">${stops}</linearGradient>`;
};

const polar = (fn, cx, cy, radius, rotation = 0) => {
  const points = [];
  for (let i = 0; i < 1080; i++) {
    const t = i / 1080 * Math.PI * 2;
    const r = radius * fn(t - rotation);
    points.push(`${(cx + r * Math.cos(t)).toFixed(1)},${(cy + r * Math.sin(t)).toFixed(1)}`);
  }
  return `<path d="M${points.join('L')}Z"/>`;
};

export const shapes = {
  // Four round leaves around a filled core.
  clover: (cx, cy, r, rot = 0) => [0, 1, 2, 3].map(i => {
    const a = rot + i * Math.PI / 2;
    return `<circle cx="${cx + Math.cos(a) * r * .5}" cy="${cy + Math.sin(a) * r * .5}" r="${r * .5}"/>`;
  }).join('') + `<circle cx="${cx}" cy="${cy}" r="${r * .6}"/>`,
  cookie: (cx, cy, r, rot = 0) => polar(t => .93 + .07 * Math.cos(9 * t), cx, cy, r, rot),
  flower: (cx, cy, r, rot = 0) => polar(t => .78 + .22 * Math.abs(Math.cos(4 * t)) ** .5, cx, cy, r, rot),
  sunny: (cx, cy, r, rot = 0) => polar(t => .92 + .08 * Math.cos(12 * t), cx, cy, r, rot),
  circle: (cx, cy, r) => `<circle cx="${cx}" cy="${cy}" r="${r}"/>`,
  pill: (cx, cy, r, rot = 0) => `<rect x="${cx - r * 1.25}" y="${cy - r * .5}" width="${r * 2.5}" height="${r}" rx="${r * .5}" transform="rotate(${rot * 180 / Math.PI} ${cx} ${cy})"/>`
};
