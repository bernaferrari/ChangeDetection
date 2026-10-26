# ChangeDetection · Play screenshot studio

Real app screens, captured on Android with the real status bar and gesture pill, composed in HTML into Play-ready artwork for phone, 7-inch tablet, and 10-inch tablet.

## The look

The set is a track-changes document. Each headline is a diff: the old phrase struck through in red, the new one under the app's own yellow marker (`~old~` and `[new]` in `<locale>/copy.json`). One dark ink canvas with a fine dot grid runs under every card, and a single version timeline crosses the whole gallery with one commit per card (`v2  price −$250`), so the cards read as one strip in the Play carousel. Type is the app's own: Work Sans for words, IBM Plex Mono for the machine's voice. Phone cards may lift one real region of the capture out of the screen (`callouts`, in capture pixels).

The artwork runs on [Screenshot Studio](studio/), the shared template used by all of these apps (master copy in `~/Documents/screenshot-studio`; never edit `studio/` here, sync it from there). Headline markup, callouts, the editor and the exporter come from the studio; this app provides:

| Change | Source |
| --- | --- |
| Order, captures, callouts | `gallery.json` |
| Headlines, body copy, commits | `<locale>/copy.json` |
| Canvas and capture sizes, device frames | `profiles.json` |
| Type, colors, device placement per device | `theme.css` |
| Dot grid, timeline and commits | `theme.js` |
| Sample pages and the screens captured | `shared/src/androidInstrumentedTest/.../store/StoreCaptureTest.kt` |

## Capture

Sample pages use reserved `.example` addresses and local content; nothing touches the network. On a dedicated **Resizable (Experimental)** emulator:

```sh
./gradlew :shared:assembleAndroidDeviceTest
python3 store-listing/capture-emulator.py --serial emulator-5566
```

For each device profile and theme the script sets display size and density, Android night mode, gesture navigation and System UI demo mode (9:41, full battery and Wi-Fi), runs `StoreCaptureTest`, and pulls full-display PNGs into `en-US/raw/<profile>/<theme>/`. `--profile` and `--theme` limit a run. Display overrides and demo mode are reset afterwards. `--apk /path/to/shared-androidTest.apk` selects an isolated build output when another build uses the checkout. If the emulator refuses the install for low storage, lower its threshold with `adb shell settings put global sys_storage_threshold_percentage 1`.

## Preview and export

```sh
node store-listing/studio/server.cjs 8790
```

Open `http://127.0.0.1:8790/store-listing/studio/` to review each device gallery live, joined edge to edge or spaced like Play. Then export:

```sh
PLAYWRIGHT_MODULE=/path/to/node_modules/playwright node store-listing/studio/export.cjs
```

The exporter serves the repository itself and writes `en-US/<kind>/NN-feature.jpg`, a joined `contact-<kind>.jpg` per device, and `en-US/assets.json`. It fails if fonts don't load, copy, callouts or commits leave the canvas, a headline overflows, copy overlaps the app, a capture has the wrong aspect ratio, or any request fails. Device frames deliberately run off the bottom edge.

## Localized screenshots (Studio v4.0)

`gallery.json` is shared layout; `<locale>/copy.json` contains only words. Add or refresh a language with `node store-listing/studio/locales.cjs --init de-DE` from the repository root (existing words are preserved). `node store-listing/studio/locales.cjs` reports copy, capture and layout coverage. Other supported languages are scaffolded as drafts with empty fields.

Capture English first, then pass `--locale de-DE` to the capture script. It sets only the isolated capture app's language, verifies Android's language setting, writes images and `*.nodes.json` UI trees under `<locale>/raw/`, and resets the app language afterwards. Use only the dedicated `StudioCapture` emulator (`emulator-5566`); no physical phone or another session's emulator. Sample names and other fictional content remain English.

Open the editor with `node store-listing/studio/server.cjs 8790`. Use **Cards**, **Languages**, and **Copy**; Copy saves directly to the selected language's file (Cmd/Ctrl+Enter also saves). Pick callouts on English captures; matching localized layout nodes move them. Headline copy shrinks to fit down to 72%, with script-specific font fallbacks.

Render one completed language with `node store-listing/studio/export.cjs --locale de-DE --strict` (set `PLAYWRIGHT_MODULE` if needed). `--kind phone` limits the gallery. Missing words or images borrow English and are flagged. A callout that cannot follow its UI element is flagged too; `--strict` rejects those three conditions. Draft locales should fail strict checks until translated and captured. README images continue to use English assets. Nothing is uploaded or published by this workflow.

### Multiple projects

The server also opens a shared workspace at `http://127.0.0.1:8790/studio/`. Use **Add project** to connect another app or screenshot folder, or **Find projects** to discover listings in a parent folder. The sidebar switches between projects; each overview shows its languages, captures, layout maps and exports. Plain folders open as image libraries. Registrations are shared in `~/.screenshot-studio/projects.json`; removing a project leaves its files intact. Unsaved copy survives project switches and reloads in the same browser tab. Save before ending the browser session. Complete asset counts describe file presence; run strict export and review artwork before release.

Project and view navigation now updates in place, preserving the sidebar and recently loaded previews. Browser Back/Forward works between views, and pending saves stay scoped to the app where they started.

The editor includes searchable language coverage, copy-completion filters and expandable previews. Copy supports search, missing-field filtering, card navigation and field status; English reference editing uses one column. On narrow screens, Preview opens the saved artwork in a dialog. The gallery includes Previous/Next scroll controls.

Studio v4 uses React and official shadcn/ui components throughout, including the expandable Sidebar and mobile Sheet drawer. The sidebar footer offers Light, Dark and System appearance; Cmd/Ctrl+K searches projects. Interface source lives in the master repository’s `ui/src/`; run `npm ci` and `npm run build` there before syncing. This app receives the compiled local bundle and still starts with the same Node server command.
