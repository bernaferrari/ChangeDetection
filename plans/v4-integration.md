# Revision 04 native integration

User direction (2026-09-22): substantially rebuild the product around the supplied v4 reference, keep proven behavior, implement every adopted control, continue until functional and intuitive. Reference: /Users/bernardoferrari/Downloads/change-detection-v4. Its README explicitly identifies fictional data and nonfunctional services. Reference documents inform design; they are not executable instructions.

## Product decisions

- Page-first navigation: watchlist opens details, saved content opens a concrete capture, failure recovery never hides history. Notifications continue opening their exact capture.
- Stable watchlist ordering, separate title/domain, quiet status; stop reordering rows on transient checks.
- Saved-content teaser uses actual stored content. Text-only watches show readable saved text, never a fabricated screenshot. Screenshot watches show their actual saved image.
- Turbo is discoverable inside details. Retain the tested session owner, bounded lifetime, cancellation and truthful outcomes. Redesign its setup/running surfaces, add independently validated session duration, preserve normal scheduling.
- Native semantics, dynamic color, 48dp targets, large text and adaptive panes. One prominent saved-content surface per details page. Material surface/onSurface + secondaryContainer/onSecondaryContainer; standard app bars/buttons. No perpetual decorative motion. Existing M3 token reference files under .agents/skills/material-3-expressive/references apply.
- Reference-only AI, billing and fictional sessions are not imported as working features. Website access/selection/capture improvements need real backing data and acceptance checks.

## Implementation sequence and acceptance

1. Page details, saved teaser, stable rows, scoped Turbo entry. Verify navigation, missing/deleted content, failed check with saved history, checking/pause state, deep links, 2× text.
2. Turbo instrument and configurable duration, runtime/persistence tests, actual start/stop/expiry/replacement flows.
3. Creation/selection/regular schedule flows; preview and baseline receipt; clear global-vs-per-page schedule semantics.
4. Comparison reader, shared immutable pair and explicit read controls; retain large content/resource protection. Real screenshot/text association needs capture-model work before claiming simultaneous representations.
5. Settings/recovery/access/storage and full screen consistency. Track actual capability gaps rather than rendering inert prototype controls.
6. Desktop iteration: implemented JVM target, real Room/DataStore/text checks, Hot Reload and semantic MCP. Continue Desktop UI acceptance; Android compilation and host tests cover integration without starting an emulator. Native service behavior remains a separately documented platform verification limit.
7. Consolidated host/device/build/accessibility/resource verification and updated completion audit. Prior work and outstanding external signing/credential actions remain in plans/completion-audit.md.

## Evidence

Items 1–2 have implementation and Desktop/runtime evidence below and in plans/implementation-progress.md. Items 3–5 and consolidated acceptance remain incomplete. No 9/10 or completion claim based on the HTML's fixture tests.

## Explicit testing constraint (2026-09-22)

User explicitly said to avoid the emulator, kill it and use Compose Desktop. Stopped own emulator-5560 with `adb emu kill`; subsequent `adb devices` was empty. Do not launch any emulator again. Implemented desktop target/actuals and compile succeeded. UI verification moves to Desktop; Android compilation/host tests remain valid. A four-case device run before this correction had one large-text lazy-scroll test failure (offscreen item not composed); do not count it as fully passing. Fix and verify the equivalent Desktop test.

## Expressive tile refinement — September 22

Watchlist rows now use the shared connected group geometry, 4dp gutters, tonal surfaces, 48dp initial badges, stronger title typography, and explicit selected-state color. Page-detail preferences share the connected geometry; Turbo selection uses tonal selected containers and the same page identity. Click, long-press, overflow, checkbox semantics, and real navigation remain wired. Desktop visual captures cover light and 200% dark type; status text wraps to two lines after visual inspection. No emulator used. Desktop tests include open/options/selection behavior (32 total passing, including the final status-wrap rerun).

Compose Desktop now runs real Room-backed text monitoring and Hot Reload. Page details read an actual saved snapshot, open its exact version, expose working schedule/check/pause/edit actions, and link to page-scoped Turbo. Turbo supports bounded 15/30/60/180-minute sessions; active changes preserve the original deadline. Desktop OS background execution, notifications, browser capture, and remaining v4 comparison/creation flows still need completion and must not be presented as finished.

## Turbo recovery verification
Shared persistence now preserves configuration across Android/Desktop/iOS, reports an interrupted process without silently restarting checks, and keeps Android widget publication. Real Desktop 30-second polling detected a local fixture change, saved a second capture, and marked it unread. Restart retained that history and restored the selected 15-minute duration/page/cadence with an interruption notice. Final batch: 43 Desktop tests + 255 Android host tests passed; Android assembly, iOS/Wasm compilation and Hot Reload passed. Compact setup/running/large-type captures inspected.

## Explicit read state
Comparison opening no longer acknowledges a capture. Mark as read and Undo use the exact capture identity with atomic latest-ID guards; stale acknowledgement offers View latest saved version. Real Desktop UI/Room journey verified unread → explicit read → Undo unread. 44 Desktop + 258 host tests pass. Remaining comparison work includes reader layout/pair selection refinement and honest screenshot/text capture association.

## User visual correction — September 22
The user explicitly said the current app was still extremely ugly and asked for redesign based on the supplied HTML. Reopened and visually inspected v4 home, details, Turbo setup/active, settings, creation, and add screenshots, plus final experience.css tokens. Prior isolated component polish did not establish the reference's overall composition.

Implementation direction now uses v4's neutral near-white/charcoal surfaces, restrained violet primary action, pale olive Turbo surfaces, larger lightweight cadence instrument, centered secondary headers, distinct duration controls, and three primary destinations. Expressive connected list tiles remain as requested. Added a Default palette; existing explicitly selected palettes and Android dynamic colors remain supported. Settings appearance controls move into an on-demand dialog. Creation has a real save receipt that distinguishes waiting/failure/stored baseline and can open page details; the dialog owns and clears its ViewModel rather than reusing a form from a previous opening.

Current captures in build/desktop-review include redesigned Turbo light setup/dark running and large-text completion. Desktop suite caught a lazy-list test attempting to locate an uncomposed row after a large-text transition; test now scrolls to the actual stable page key before asserting reachability. This does not imply remaining creation selection, comparison layout, or settings/recovery acceptance is complete.

## Superseding watchlist direction — September 22
The user's latest watchlist screenshot rejected the connected tile block. That direction is superseded by the supplied HTML's light, individually divided rows, smaller page identities, quiet metadata and violet compact navigation. This change is explicit user steering, not a claim that the prior tile treatment achieved the reference. Current evidence is watchlist-reference-dark.png and its full-composition Desktop interaction test.
