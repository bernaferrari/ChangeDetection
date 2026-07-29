# Plan 002: Publish only verified Change Detection imagery

> **Executor instructions**: Follow this plan step by step. Preserve all pre-existing user edits. Do not use AI-generated, stock, or another product's UI to represent a Change Detection feature. When done, update this plan's status in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat cff7931..HEAD -- web/public/assets web/components/hero.tsx web/components/showcase.tsx web/app/layout.tsx web/README.md`

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: LOW — limited to static marketing presentation, but inaccurate screenshots damage trust.
- **Depends on**: `plans/001-reproducible-android-verification.md`
- **Category**: docs, direction
- **Planned at**: commit `cff7931`, 2026-07-28

## Why this matters

The main marketing experience presently represents Change Detection with files that depict a different product, “Default Opener.” The hero, showcase cards, and social metadata all use assets from `web/public/assets/`. This is the fastest visible improvement available: truthful screenshots make a simpler interface feel intentional, while incorrect screenshots make even good engineering look untrustworthy.

## Current state

- `web/components/hero.tsx:95-112` renders `/assets/main-screen.png` as the phone mockup and calls it “Change Detection - Main Interface showing tracked websites.”
- `web/components/showcase.tsx:101-156` renders `/assets/main-screen.png` as “Dashboard View” and `/assets/header.png` as “Version History.”
- `web/app/layout.tsx:29-43` uses `/assets/banner.png` for Open Graph and Twitter metadata.
- The checked-in files `web/public/assets/main-screen.png` and `web/public/assets/header.png` depict an unrelated Default Opener interface. Their dimensions are 2056×1468 and 1280×640 respectively, unsuitable as truthful phone captures.
- The web app is a Next.js static site; direct verification succeeds with `node_modules/.bin/eslint .` and `node_modules/.bin/next build` from `web/`.

## Commands you will need

| Purpose | Command | Expected on success |
|---|---|---|
| Android build | `./gradlew :app:assembleDebug` | `BUILD SUCCESSFUL` |
| Inspect assets | `file web/public/assets/*` | each replacement has expected format/dimensions |
| Web lint | `cd web && node_modules/.bin/eslint .` | exit 0 |
| Web production build | `cd web && node_modules/.bin/next build` | exit 0 |

## Scope

**In scope**:

- `web/public/assets/main-screen.png`
- `web/public/assets/history-screen.png` (create)
- `web/public/assets/comparison-screen.png` (create if used)
- `web/public/assets/banner.png`
- `web/components/hero.tsx`
- `web/components/showcase.tsx`
- `web/app/layout.tsx`
- `web/README.md` (only to document the screenshot update procedure)

**Out of scope**:

- App UI behavior and screenshot content generation code.
- Claims about iOS availability: the current product description says iOS is coming soon and must remain truthful.
- Generic marketing-site redesign unrelated to truthful imagery.

## Git workflow

- Use a focused branch such as `codex/002-verified-marketing-imagery` after preserving existing dirty work.
- Match the existing polished, direct wording; do not add an asset pipeline dependency merely to resize three images.
- Do not push or publish without explicit instruction.

## Steps

### Step 1: Capture a reproducible, privacy-safe visual story

After Plan 001's Android debug build succeeds, launch Change Detection in an emulator or test device. Add synthetic public test URLs or local fixtures only; do not capture personal browsing data, real saved content, notification text, or credentials. Capture at least:

1. A populated monitored-sites list showing title, domain/favicon, changed/unchanged/checking status, and the add action.
2. A version-history or comparison state showing the app's actual diff capability.
3. A 1200×630 social card assembled solely from the captured Change Detection UI, the official logo, product name, and a short truthful statement such as “Private, on-device website monitoring.”

Use the same light or dark theme, device frame treatment, crop discipline, and content scale across all captures. Do not put a phone screenshot into a wide card by severe cropping.

**Verify**: inspect every capture at 100% and confirm it shows Change Detection, no unrelated product name, no personal data, and readable text at intended display size.

### Step 2: Replace or remove misleading assets atomically

Replace `main-screen.png` with a portrait capture appropriate for Hero's `aspect-[9/19.5]` container. Add purpose-named `history-screen.png` and/or `comparison-screen.png` instead of reusing one asset for unrelated claims. Replace `banner.png` with the verified social card.

If an authentic capture cannot be produced in this plan, remove the phone mockup and screenshot cards from `hero.tsx` and `showcase.tsx` rather than retaining false imagery. A clean text-and-diff presentation is better than deceptive visual evidence.

**Verify**: `file web/public/assets/main-screen.png web/public/assets/history-screen.png web/public/assets/banner.png` → each is a real image with a purpose-appropriate orientation and no accidental Default Opener asset remains referenced.

### Step 3: Bind names, alt text, and metadata to the actual captures

Update `Hero` and `Showcase` so each `Image` source, heading, and alt text describe the exact image: monitored-sites dashboard, snapshot history, or visual comparison. Preserve `next/image` width/height values consistent with actual aspect ratios; update the layout to avoid distorted or aggressively cropped captures.

Keep `layout.tsx` Open Graph/Twitter paths pointing at the genuine 1200×630 card. Its text must accurately match current capability: local snapshots and public-page monitoring are supported; authenticated pages and saved login cookies are not.

**Verify**: `rg -n 'Default Opener|main-screen.png|history-screen.png|comparison-screen.png|banner.png' web` → all references and alt text match the assets' contents; `Default Opener` has zero matches.

### Step 4: Document asset refresh and visually inspect the result

In `web/README.md`, add a short maintainer section covering: use synthetic data, required target dimensions, filenames, and the no-unrelated-product rule. Run the web production build. If a browser-verification tool is available, start the local production site and visually inspect `/` at mobile and desktop widths; otherwise use the rendered Next output and manually open it.

**Verify**: `cd web && node_modules/.bin/eslint . && node_modules/.bin/next build` → exit 0. The rendered hero, showcase, and social-card asset paths resolve without a 404.

## Test plan

- Automated: ESLint and Next production build.
- Manual visual regression: check 375px mobile width and 1440px desktop width; hero image must remain within its phone frame, screenshot cards must not clip labels, and all alt text must correspond to visible content.
- Content regression: search for the unrelated product name and old asset reuse.

## Done criteria

- [ ] No web page or metadata represents Change Detection with another product's screenshot.
- [ ] Hero, showcase, and Open Graph card use purpose-specific truthful imagery or intentionally omit imagery until it exists.
- [ ] All screenshots use synthetic, non-sensitive data.
- [ ] `web/README.md` documents the refresh process.
- [ ] ESLint and the Next production build succeed.

## STOP conditions

- Stop if the Android app cannot be built after Plan 001; do not fabricate screenshots as a workaround.
- Stop if no safe synthetic data source exists; remove misleading visuals and request a device/emulator capture decision.
- Stop if a screenshot would reveal a URL, history, notification, or other data the operator has not authorized for publication.

## Maintenance notes

- Any UI redesign that materially changes the monitored-sites or comparison surface must update these screenshots before release.
- Reviewers should compare images to the current app, not merely confirm image dimensions or successful web compilation.
