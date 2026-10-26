// Gallery rules shared by the renderer, the editor and the exporter.
//
// Layout lives once in ../gallery.json (devices, captures, callouts, colors, output names).
// Words live per language in ../<locale>/copy.json, keyed by card `feature`, with optional
// "<kind>/<feature>" entries for one device gallery. Missing words fall back to the default
// locale and are reported, so a half-translated language still renders and is easy to spot.

/**
 * A device gallery is either a list of cards, or {"inherits": "phone", "cards": [...]}: each card is
 * merged over the inherited card at the same position, so a tablet only swaps its captures and composition.
 */
export function cardsFor(galleries, kind, seen = new Set()) {
  const gallery = galleries[kind];
  if (!gallery) throw new Error(`Unknown gallery: ${kind}`);
  if (Array.isArray(gallery)) return gallery;
  if (seen.has(kind)) throw new Error(`Gallery ${kind} inherits from itself`);
  seen.add(kind);
  const base = cardsFor(galleries, gallery.inherits, seen);
  if (gallery.cards.length !== base.length) throw new Error(`${kind} must override all ${base.length} cards of ${gallery.inherits}`);
  return gallery.cards.map((card, i) => ({...base[i], ...card}));
}

/** Words for one card: the locale's copy over the default locale's, kind-specific entries over shared ones. */
export function withCopy(card, kind, copy = {}, fallback = {}) {
  // `"sharedCopy": false` keeps a card to its own "<kind>/<feature>" words (unframed desktop screenshots, say).
  const keys = card.sharedCopy === false ? [`${kind}/${card.feature}`] : [card.feature, `${kind}/${card.feature}`];
  const own = Object.assign({}, ...keys.map(k => copy[k] || {}));
  const base = Object.assign({}, ...keys.map(k => fallback[k] || {}));
  const empty = value => typeof value === 'string' && !value.trim();
  const missing = Object.keys(base).filter(field => !(field in own) || empty(own[field]));
  const translated = Object.fromEntries(Object.entries(own).filter(([, value]) => !empty(value)));
  return {card: {...card, ...base, ...translated}, missing};
}

/** Every copy key a gallery file can use, in display order: shared card words first, then per-kind overrides. */
export function copyKeys(galleries, defaultCopy = {}) {
  const features = [];
  for (const kind of Object.keys(galleries)) for (const card of cardsFor(galleries, kind)) if (!features.includes(card.feature)) features.push(card.feature);
  return [...features, ...Object.keys(defaultCopy).filter(k => !features.includes(k))];
}

/** Devices on a card: `devices` when several, otherwise the single `raw` capture; none for art-only cards. */
export const devicesFor = card => card.devices || (card.raw ? [{raw: card.raw}] : []);

export const outputFile = (kind, card, i) => card.file || `${kind}/${String(i + 1).padStart(2, '0')}-${card.feature}.jpg`;

/** The layout sidecar written next to a capture: the app's UI tree with bounds, in capture pixels. */
export const nodesFile = raw => raw.replace(/\.[a-z]+$/i, '.nodes.json');

/** Resolve a pinned element. Prefer a unique resource ID; paths require the recorded class to agree. */
function pinnedNode(box, tree) {
  if (!tree?.nodes || !box.target) return null;
  let node;
  if (box.targetId) {
    const matches = tree.nodes.filter(n => n.id === box.targetId);
    if (matches.length !== 1) return null;
    node = matches[0];
  } else node = tree.nodes.find(n => n.path === box.target);
  if (!node || (box.targetClass && node.cls !== box.targetClass)) return null;
  return node;
}

/**
 * Follow a capture's UI element into another locale. Element-mode selections use its exact bounds;
 * hand-drawn crops keep their offsets from the reference anchor. Unpinned legacy crops find an
 * anchor by overlap. Missing, ambiguous or incompatible elements return null for strict reporting.
 */
export function followCallout(box, reference, localized) {
  if (!reference?.nodes || !localized?.nodes || (box.targetMode === 'element' && !box.target)) return null;
  const cx = box.x + box.w / 2, cy = box.y + box.h / 2;
  const overlap = ([l, t, r, b]) => {
    const w = Math.max(0, Math.min(r, box.x + box.w) - Math.max(l, box.x)), h = Math.max(0, Math.min(b, box.y + box.h) - Math.max(t, box.y));
    return w * h / ((r - l) * (b - t) + box.w * box.h - w * h);
  };
  const candidates = reference.nodes.filter(n => n.b[0] <= cx && cx <= n.b[2] && n.b[1] <= cy && cy <= n.b[3] && n.b[2] > n.b[0] && n.b[3] > n.b[1]);
  const anchor = box.target ? pinnedNode(box, reference) : candidates.sort((a, b) => overlap(b.b) - overlap(a.b))[0];
  if (!anchor || (!box.target && overlap(anchor.b) < .2)) return null;
  const target = box.target ? pinnedNode(box, localized) : localized.nodes.find(n => n.path === anchor.path);
  if (!target || (anchor.cls && target.cls && anchor.cls !== target.cls)) return null;
  const [l, t, r, b] = anchor.b, [L, T, R, B] = target.b;
  const moved = box.targetMode === 'element'
    ? {x: L, y: T, w: R - L, h: B - T, path: target.path}
    : {x: L + box.x - l, y: T + box.y - t, w: R - L + box.w - (r - l), h: B - T + box.h - (b - t), path: target.path};
  if (![moved.x, moved.y, moved.w, moved.h].every(Number.isFinite) || moved.w <= 0 || moved.h <= 0 || moved.x < 0 || moved.y < 0) return null;
  if ((localized.width && moved.x + moved.w > localized.width) || (localized.height && moved.y + moved.h > localized.height)) return null;
  return moved;
}
