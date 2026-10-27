// ChangeDetection: one cobalt field runs under the whole gallery, lit by soft glows laid out in panorama
// coordinates so the light continues across every seam. Lifted UI gets the logo's viewfinder corners.
import {panoramaGradient, panoramaX} from './studio/shapes.js';

const field = ['#2443e6', '#1c39d6', '#2a49ee', '#1a35cc', '#2343e4', '#1e3cda'];
// Glows: x in card widths across the gallery, y as a fraction of the card height, radius in card widths.
const glows = [
  {x: .1, y: .2, r: .9, color: '#6f95ff', alpha: .55},
  {x: 1.05, y: .78, r: 1, color: '#46d7ff', alpha: .28},
  {x: 1.9, y: .15, r: .85, color: '#8aa6ff', alpha: .5},
  {x: 2.95, y: .7, r: 1.05, color: '#5b7cff', alpha: .45},
  {x: 3.85, y: .22, r: .9, color: '#46d7ff', alpha: .26},
  {x: 4.9, y: .74, r: 1, color: '#7c98ff', alpha: .5},
  {x: 5.9, y: .2, r: .9, color: '#5b7cff', alpha: .45}
];

export default {
  art(ctx) {
    const {W, H, slides} = ctx, unit = Math.max(W, H * .62);
    const defs = glows.map((g, i) => `<radialGradient id="glow${i}"><stop offset="0" stop-color="${g.color}" stop-opacity="${g.alpha}"/><stop offset="1" stop-color="${g.color}" stop-opacity="0"/></radialGradient>`).join('');
    return `<defs>${panoramaGradient(ctx, 'field', field.slice(0, Math.max(2, slides.length)))}${defs}</defs>` +
      `<rect width="${W}" height="${H}" fill="url(#field)"/>` +
      glows.map((g, i) => `<circle cx="${panoramaX(ctx, g.x)}" cy="${g.y * H}" r="${g.r * unit}" fill="url(#glow${i})"/>`).join('');
  },

  // Copy sits centred in the band above the device, however many lines it takes or how far it was fitted.
  refit({kind, slide, main, device}) {
    const header = main.querySelector('header');
    // The landscape feed keeps its copy in a left column, centred by CSS.
    if (kind === 'tablet10' && slide.layout === 'feed') return;
    // The band ends at the device, or at an alert floating above it.
    const floor = Math.min(device?.top ?? Infinity, ...[...main.querySelectorAll('.callout.alert, .callout.feed')].map(a => a.offsetTop - 24));
    if (header && Number.isFinite(floor)) header.style.top = `${Math.max(48, (floor - header.offsetHeight) / 2)}px`;
  },

  decorate(ctx) {
    const {main, H} = ctx;
    this.refit(ctx);
    // Alerts float as they do on a phone; the corners are for the change itself.
    for (const callout of main.querySelectorAll('.callout:not(.alert):not(.back)')) {
      const gap = 18, frame = document.createElement('div');
      frame.className = 'viewfinder';
      frame.innerHTML = '<i></i><i></i><i></i><i></i>';
      Object.assign(frame.style, {
        left: `${callout.offsetLeft - gap}px`, top: `${callout.offsetTop - gap}px`,
        width: `${callout.offsetWidth + gap * 2}px`,
        // A callout resting on the bottom edge keeps its lower corners inside the canvas.
        height: `${Math.min(callout.offsetHeight + gap * 2, H - 2 - (callout.offsetTop - gap))}px`
      });
      frame.dataset.checkBounds = '';
      main.append(frame);
    }
  }
};
