# Change Detection web site

The marketing site is a small Next.js application for the public product overview and documentation.

## Local checks

```bash
pnpm install --frozen-lockfile
pnpm lint
pnpm build
```

`next build` must be able to run without fetching a font at build time. The site uses a resilient system/Inter font stack for that reason.

## Product previews

The homepage uses `components/product-preview.tsx` for illustrative monitoring, comparison, and history surfaces. Those previews are intentionally synthetic—not captured user data and not screenshots from another product.

When adding a real Change Detection screenshot:

1. Use only synthetic public test content and remove any personal data, credentials, notifications, or saved browsing history.
2. Capture the current product UI; do not stretch, recolor, or otherwise misrepresent it.
3. Update the component and its accessible text together, then rerun lint and the production build.

Do not add stale screenshots, generated stand-ins that look like real user data, or another product’s assets to marketing or social metadata.
