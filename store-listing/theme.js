// ChangeDetection: one dark canvas with a dot grid and a single version timeline through the whole
// gallery; each card adds its commit on the timeline above its device.
const timelineY = kind => kind === 'tablet10' ? 112 : kind === 'tablet7' ? 590 : 552;

export default {
  art({W, H, kind, index}) {
    const step = 36, offset = -((index * W) % step), y = timelineY(kind);
    return `<defs><pattern id="dots" width="${step}" height="${step}" patternUnits="userSpaceOnUse" x="${offset}" y="0">
      <circle cx="${step / 2}" cy="${step / 2}" r="1.6" fill="#ffffff14"/></pattern></defs>
      <rect width="${W}" height="${H}" fill="url(#dots)"/>
      <line x1="0" x2="${W}" y1="${y}" y2="${y}" stroke="#ffffff30" stroke-width="3"/>`;
  },
  decorate(ctx) {
    const {kind, index, slide, main, art, device} = ctx;
    const y = timelineY(kind);
    this.refit(ctx);
    const x = device.left + device.width / 2;
    art.insertAdjacentHTML('beforeend', `<circle cx="${x}" cy="${y}" r="15" fill="var(--ink)" stroke="#ffd84d" stroke-width="6"/>`);
    const commit = document.createElement('div');
    commit.className = 'commit';
    commit.dataset.checkBounds = '';
    commit.innerHTML = `<b>v${index + 1}</b>&nbsp;&nbsp;${slide.commit}`;
    main.append(commit);
    commit.style.left = `${x + 34}px`;
    commit.style.top = `${y - commit.offsetHeight - 10}px`;
  },
  // Portrait copy sits centred in the band above the timeline, whatever its line count or fitted size.
  refit({kind, main}) {
    if (kind === 'tablet10') return;
    const header = main.querySelector('header');
    if (header) header.style.top = `${Math.max(72, (timelineY(kind) - 40 - header.offsetHeight) / 2)}px`;
  }
};
