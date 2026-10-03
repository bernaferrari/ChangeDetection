# Follow-up implementation and M3 Expressive review — 2026-09-22

**Verdict: approve these bounded corrections; blocked for whole-product sign-off.** The working app has improved, but the complete original reliability and visual goal is not proven. This review does not claim every changed line or every platform state has been exercised.

## Scope and evidence

Rechecked the existing implementation/completion audits, the current monitoring kernel and state publication, Desktop scheduling, Android service stop/notification paths, capture/save guards, and shared watchlist, history, selection, Turbo, typography and navigation code. Compared the visual decisions with the supplied v4 refinement CSS. Reviewed newly rendered Desktop history images at normal and 200% text. Previous test evidence remains distinguished from new execution. No emulator was used.

## Confirmed flaws corrected in this pass

1. **P1: active Turbo could lose its Stop action.** `TurboScreen` conditioned the footer on nonempty loaded pages, independently of session activity. An active session must remain stoppable during loading or an empty list. The footer now stays visible whenever running. A Desktop regression transitions from loading to empty and invokes Stop in both states.
2. **P2: Material container/content roles were mismatched.** `SiteIdentity` painted primary/secondary containers with onSurfaceVariant; selected Turbo row metadata did the same on primaryContainer. Turbo option text also used primary on primaryContainer and onSecondaryContainer on a neutral surface. These now use the corresponding onPrimaryContainer, onSecondaryContainer or onSurface roles. This improves the semantic contract for dynamic/custom themes; it is not a claim of an exhaustive measured contrast matrix.
3. **P2: history obscured its essential timestamp.** A long address appeared before the time in a one-line ellipsized label. Time now comes first. Titles can use two lines and rows grow with content. Removed the redundant vertical color stripe beside the existing timeline dot, recovering text width and reducing decorative weight. A 320dp/200% regression checks the time's visible text layout and both open and menu/delete callbacks.

## Remaining actionable findings

### P2 — navigation motion does not mirror in RTL — fixed

`AppContent.kt:162–176` supplies positive full-width offsets for both forward entry and backward exit, without consulting layout direction. The back icon can mirror while the transition continues to originate from the physical right. Derive the offset sign from LocalLayoutDirection and include that direction in the remembered transition key. Validate actual push/pop in both directions with animation enabled and disabled; existing image-comparison RTL tests do not cover navigation.

### P2 — selection edits recreate the embedded browser — fixed; Desktop native verified

`TextRegionPicker.kt:102–146` increments pageRevision for mode/scope/undo/ignore actions and keys the renderer on it. This discards and recreates the native browser even for clicking the already-selected Watch/Ignore mode. Scroll restoration protects position, but it does not preserve native keyboard focus or eliminate redraw. First guard no-op selections. Then introduce a narrow browser update operation for trusted selection/highlight state while retaining the navigation restrictions and sanitizer. Verify keyboard focus and viewport remain stable after edits. This is a functional polish gap, not a reason to weaken the HTML security boundary.

### P2 — source-selection controls have weak narrow-screen discoverability — fixed

The four scope actions share an unmarked horizontally scrolling row (`TextRegionPicker.kt:113`). At large type, Add area and Undo may be offscreen despite being available. Use an adaptive wrap or a clearly labeled overflow for secondary actions while maintaining a useful preview and pinned confirmation. Test 280dp/200% and keyboard traversal; do not merely add more vertical controls and shrink the preview to nothing.

### P2 — Turbo interval controls need localized accessible names — fixed

`TurboHero.kt:70,78,137` builds hardcoded abbreviated labels (15s, 1m, 1 h). The radio semantics expose those abbreviated strings instead of a localized full duration. Keep compact visual labels, but provide localized full accessible names and selection announcements. Verify all four shipped locales rather than changing only English.

### P3 — typography and shape still lack one consistent component policy

The v4 reference is quieter because its secondary rows are compact, type weights restrained, and outlines sparse. Current typography remains predominantly bold for headings while individual screens override size, weight and corners. Turbo's 80sp numeral is a defensible single hero moment; duplicating that treatment elsewhere would not improve Expressive quality. Prefer shared screen-title, identity and supporting-text roles, semantic container pairs, and Material shape tokens where components repeat. Keep the lighter watchlist; do not turn every item into a large pill to imply M3 Expressive compliance.

## Architecture assessment

Keep the shared TurboSession owner, monotonic live deadline, guarded capture commit, page-level locking, separated retention policy and typed preview failures. These are meaningful improvements over UI-owned timers and optimistic status. The new Stop test addresses a presentation seam around that owner rather than requiring another timer refactor.

The next valuable architectural seam is the embedded browser's trusted selection-update interface. Today immutable draft state is well separated, but the composable bridges it to native state through destructive recreation. Avoid a broad rewrite of the already-tested monitoring/storage kernel for aesthetic reasons.

## Unproven requirements, distinct from reproduced defects

- Real Android screen-off/Doze/OEM/process-death and notification endurance on the final build; Desktop and host tests cannot establish these.
- Complete keyboard/screen-reader/RTL/landscape/zero-motion journey coverage and measured dynamic-theme contrast.
- Full visual acceptance against the supplied HTML; source selection intentionally uses sanitized content, not live-page fidelity.
- Very-large-archive and image-heavy resource pressure; external credential remediation and hosted CI protections remain recorded separately in the completion audit.

## New validation

`build/m3-review-validation.log`: 115 Desktop tests and 266 Android host tests, zero failures/errors/skips. Spotless, Android debug assembly, iOS simulator-target compilation and Wasm compilation passed. `git diff --check` passed. Compose Desktop Hot Reload returned success (MCP request 193).

Visual evidence: `build/desktop-review/history-row-large-dark.png` and `history-refresh-recovery.png`, both inspected. The large-text row keeps the full timestamp and menu target visible. This is bounded visual evidence, not whole-app acceptance.


## Turbo accessibility follow-through

Full option names now resolve in English, Portuguese, Spanish and Italian. Compact child text is excluded from semantics; the parent retains its radio role, selection and action. Four Desktop locale tests exercise the actual resources and selection. Help no longer promises an unconditional three-hour session or unsupported desktop notifications. `build/turbo-options-final.log`:119 Desktop/266 Android host tests pass, plus formatting and Android/iOS/Wasm compilation. Physical screen-reader speech remains outside this host evidence.


## Navigation motion follow-through

Production metadata now reads layout direction and mirrors push/pop offsets. Four real NavDisplay Desktop tests verify mid-animation position in LTR/RTL, changing direction while composed, and immediate settled navigation at zero motion scale. Full suite:123 Desktop/266 Android host tests pass; Android assembly and iOS/Wasm compilation pass (`build/navigation-motion-final.log`).


## Selection toolbar follow-through

Actual text measurement now selects inline buttons or a labeled Tools dropdown. No horizontal guessing or extra rows consume the preview. The 280dp/200% Desktop test verifies all four menu entries and successful Undo/confirmation; captures inspected. Repeated clicks on the current Watch/Ignore mode no longer recreate the renderer, verified by mount counts. Browser recreation for actual edits still needs the trusted update seam described above. Full validation:124 Desktop/266 Android host tests and platform compilation pass (`build/selection-toolbar-final.log`).


## Native selection update follow-through

Removed edit-driven renderer keys and kept browser HTML stable across draft changes. RegionHighlightUpdate carries numeric identities and emits a fixed attribute-only update; source text/selectors do not enter executable code. Platform adapters apply updates after load. The real Chromium smoke test verifies changed/cleared highlights with one load, unchanged body identity, focus and 600px scroll position. Compose mount tests cover modes, Add area, Undo and ignored picks. All124 Desktop/266 host tests and platform compilations pass (`build/region-live-final.log`); native smoke passes (`build/region-live-native.log`). Android/iOS hardware focus verification remains separate.


## P1 navigation-owned drafts — fixed after live journey

Live Desktop reopening Edit exposed an unsaved draft retained from an earlier visit. AppContent lacked ViewModelStoreNavEntryDecorator; a root-scoped parameterized ViewModel could therefore be reused for another navigation entry. The shared decorator helper now owns models per route and clears them on pop. A real NavDisplay regression verifies same-route preservation beneath preview, cross-route isolation, clearing and fresh reopen. Post-dependency-restart live edit → change Main content → back without Save → reopen returned Whole page.126 Desktop/266 host tests and platform builds pass (`build/entry-scope-final.log`).


## Comparison state follow-through

Returning to a retained comparison route no longer resets its historical selection. Retry now refreshes by visible/requested snapshot identity, including initial database failures, and reports a removed version as unavailable. Explicit View latest remains separate. Four regression cases pass with the full126 Desktop/270 host suite and platform compilation (`build/comparison-entry-final.log`).


## Recovery copy and reachability follow-through

Comparison load failures no longer store/render raw exception messages; typed Page/Content states drive localized guidance. Shared recovery states now scroll in constrained heights and cap reading width, keeping Retry/secondary actions reachable at200% text. Two Desktop layout/callback tests and inspected captures accompany128 Desktop/270 host passing tests plus platform compilation (`build/comparison-recovery-copy.log`).


## Actual missing-file recovery verification

Two new integration tests use real Desktop files, SQLite, sources/repositories and the comparison ViewModel. Missing baseline/pair content fails visibly, keeps rows/unread status, and recovers the same identity after file restoration and Retry. Content-level error layouts now scroll; Retry works at280×180dp/200% text.131 Desktop/270 host tests and platform builds pass (`build/missing-snapshot-recovery.log`). Storage exhaustion and hardware behavior remain separate gates.


## Desktop storage-write recovery follow-through

Desktop writes now apply the32MiB reserve, flush data before atomic publication and report typed storage failures. Real-engine/SQLite/files tests with injected space readings verify rejection preserves history and recovery emits exactly one saved change/notification. A real failed atomic replacement preserves existing data and cleans temporary files.133 Desktop/270 host tests and platform builds pass (`build/storage-recovery-final.log`). Physical disk exhaustion/power loss remains outside this evidence.


## Final detail-row and RTL review follow-through

**Verdict: approve the bounded fixes; whole-product sign-off remains open.** Reconciled the current implementation and completion audits, inspected the detail, settings, watchlist and Turbo captures, and reran the shared regression/platform gates. This is not a claim that every changed line or every device state was independently reviewed in this pass.

### Corrected

- Detail preferences used bulky 88dp connected containers beneath an already prominent preview and Turbo card. They now use content-sized rows with a72dp minimum, bodyLarge labels, neutral surfaces and thin outlineVariant dividers. Turbo retains the one colored emphasis in this section. Existing check, edit, schedule and pause callbacks are unchanged. Normal/light and200% dark captures were inspected; scroll position in the latter intentionally shows the lower actions.
- Detail and settings navigation chevrons were physically right-pointing despite mirrored navigation transitions. Both now use the same AutoMirrored KeyboardArrowRight icon already used in Turbo rows. This corrects the icon contract; it does not establish a complete RTL interaction matrix.

### Remaining design refinements, in priority order

1. **P2: action versus navigation affordances — fixed in follow-through below.** PageDetailScreen's private DetailRow still applies a trailing navigation chevron to immediate Check now and Pause/Resume commands. The labels make these operable, but a chevron suggests a destination. Separate the trailing affordance for destination rows from commands; retain full-row touch targets and explicit state feedback.
2. **P3: component consistency.** SettingsActionRow still uses segmented surfaceContainerLow panels while detail and watchlist use lighter rows. Grouping may differ by purpose, but title weights, leading-icon treatment and density should follow one documented component policy. Type.kt also makes headlineLarge Bold while adjacent headline roles are SemiBold. Avoid another blanket corner/weight change; compare complete screens against the supplied HTML first.
3. **P3: selection visual fidelity.** The native selection surface is sanitized and reformatted. Its interaction improvements are verified, but retaining safe source styling/layout more faithfully needs a deliberate design and security decision. It should not be described as a pixel-faithful page preview.

### Verification and limits

`build/detail-review-final.log`:133 Desktop and270 Android host tests, zero failures/errors/skips; Android debug assembly, iOS simulator-target and Wasm compilation pass. `build/detail-review-format.log`: Spotless passes. `git diff --check` passes. Desktop Hot Reload227 succeeds. No emulator was started or used.

Captures inspected: detail-preferences-light.png, detail-preferences-large-dark.png, tiles-light.png, turbo-setup-light.png and settings-groups-light.png under build/desktop-review. These include focused component states rather than an exhaustive whole-product screenshot matrix.

Unverified release requirements remain distinct from defects: physical Android Doze/process-death/notification endurance; complete screen-reader, keyboard, RTL, landscape and dynamic-contrast coverage; image-heavy resource pressure; external credential revocation and hosted CI protections. Green host tests do not close these gates.


## Detail command affordances follow-through

Check now and Pause/Resume no longer receive a navigation chevron; Watching and Regular checks retain the mirrored destination indicator. The row remains one large clickable surface. Checking uses neutral supporting color while disabled instead of looking like an available primary action. Existing localized labels remain unchanged.

Two Desktop regressions verify Checking cannot invoke another check while Pause remains enabled, and paused pages still offer manual Check and Resume. Existing normal and200% dark action tests/captures were rerun and inspected; removing unused trailing space reduces wrapping at large type.

`build/detail-actions-final.log`:135 Desktop/270 Android host tests pass; Android assembly, iOS/Wasm compilation and Spotless pass. Diff check passes. Desktop Hot Reload228 succeeds. No emulator. Component consistency, preview fidelity and the wider acceptance gates remain open.


## Settings consistency follow-through

SettingsGroup now follows the supplied v4 outlined-group treatment, with inset separators and quiet unboxed icons. Repeated labels use bodyLarge regular weight. Measured title/value widths select an inline or stacked layout; explanatory text is no longer capped at two lines. At constrained width/font scale, icons move into a header to preserve readable whole words. Destination chevrons are suppressed for noninteractive rows.

The new280dp/200% regression checks long text for overflow, ellipsis, word splitting and action delivery. Existing standalone large-text tests caught a cadence-width regression, fixed by assigning stacked text the available width. Normal/light and large/dark renders were inspected after correction. Full136 Desktop/270 host tests pass plus formatting, Android assembly and iOS/Wasm compilation (build/settings-refinement-final.log); Desktop Hot Reload231 succeeds. Broader heading policy, preview fidelity and full hardware/accessibility acceptance remain open.


## P1 platform scheduling promises — fixed after live inspection

Desktop's schedule sheet incorrectly claimed background execution and offered Wi-Fi/charging/battery constraints its scheduler ignores. Platform capabilities now drive the title, explanation, constraint visibility and unavailable-preview enablement. Android retains its constraint controls; iOS does not expose constraints its manager currently discards. Localized copy covers all four locales. Live Desktop verified the corrected sheet and7d draft cancellation/reopen preserving1h.136 Desktop/270 host tests and platform builds pass (`build/schedule-capabilities-final.log`), formatting/diff checks pass. Physical background execution remains an independent acceptance gate.


## Schedule interval accessibility follow-through

Unit controls now wrap using measured label widths instead of squeezing three equal columns. Presets retain compact visual text but announce full localized cadence names and selection. New280dp/200% layout/callback and invalid-input recovery tests pass; capture inspected.138 Desktop/270 host tests and platform/format checks pass (build/schedule-interval-final.log), Hot Reload248 succeeds. Broader translated/device accessibility remains open.


## P1 initial schedule draft race — fixed

A sheet could copy default settings before persisted preferences arrived and keep that default draft. It now waits for loaded preferences, disables Save meanwhile, initializes once and preserves subsequent user edits. Preference availability no longer waits for storage-size emission. Real-sheet and ViewModel delayed-load regressions pass with139 Desktop/271 host tests and platform/format checks (build/schedule-loading-final.log). Terminal preference/storage flow failures remain a separate recovery concern.


## Settings observation recovery follow-through

Terminal observation failures now produce a localized Retry state instead of leaving default/pending settings editable. Retry resubscribes, and existing schedule drafts survive a failure/recovery cycle. SettingsScreen recovery scrolls in constrained heights; the sheet uses its existing scroll region. A preference-failure retry test and extended real-sheet draft test pass with139 Desktop/272 host tests plus platform/format checks (`build/settings-recovery-final.log`). Hot Reload251 succeeds.


## Pending storage summary follow-through

The storage summary now distinguishes unknown from measured zero. A pending query shows Loading rather than an invented0MB. The open policy dialog updates when the real measurement arrives; a Desktop regression covers pending→0→4MB.140 Desktop/272 host tests, platform builds and format checks pass (`build/storage-summary-final.log`); Hot Reload252 succeeds.


## Receipt text and dark-theme refinement

Removed the fixed image-height constraint from text receipts and replaced hardcoded preview colors with Material surface/content roles. Footer status/action now wrap at large type. Existing200% test additionally checks the complete document title; fresh captures inspected.140 Desktop/272 host tests, platform builds and formatting pass (build/receipt-refinement-final.log); Hot Reload253 succeeds.


## Receipt observation states follow-through

Initial loading no longer invents a waiting-check status. Failed refreshes preserve known baseline evidence and its action; failed initial reads keep a route to the saved page. Confirmed deletion suppresses stale destinations and contradictory saved copy even during a later failure. Four Desktop regressions pass with144 Desktop/272 host tests, platform builds and formatting (`build/receipt-state-final-confirmed.log`); Hot Reload255 succeeds.


## Adaptive appearance controls follow-through

Theme choices retain connected Material buttons when measured labels fit and stack at constrained width/font scale. Selected dynamic palette content uses its matching onPrimaryContainer role. Two normal/200% Desktop layout and selection tests pass, captures inspected.146 Desktop/272 host tests and platform/format checks pass (`build/appearance-final.log`); Hot Reload256 succeeds. Device theme persistence/contrast matrices remain separate evidence.


## Appearance persistence verification

A full Settings-section dialog test now verifies open→Dark/Blue→Done→reopen selections. A separate real file-backed preference-store test writes through the ViewModel, closes the store, and verifies the choices after opening a new store.147 Desktop/273 host tests and platform/format checks pass (`build/appearance-persistence-final.log`). The live Hot Reload bridge did not expose appearance dialog controls; that journey remains unverified rather than inferred from these tests.


## Preference-write recovery follow-through

Theme/palette/comparison/browser-delay writes now catch failures, preserve cancellation and offer localized Retry. Feedback is reachable inside Appearance as well as Settings. Serialized writes and generation checks prevent a superseded failed choice from being restored by Retry. Host fault-injection and Desktop dialog tests pass with148 Desktop/274 host tests, platform builds and formatting (`build/preference-write-final.log`); Hot Reload262 succeeds.


## History ordering follow-through

History paging/search and legacy reads now break timestamp ties by snapshot ID, matching latest-version lookup. A real3000-capture/50-page database regression failed before the fix and now verifies complete unique paged identities and consistent searched/unfiltered order.149 Desktop/274 host tests and platform/format checks pass (`build/history-order-final.log`); Hot Reload263 succeeds. This does not close the broader image-heavy or very-large-archive resource gate.


## Literal history search follow-through

History search now treats% and_ as literal title/URL characters rather than SQL wildcards and correctly escapes! itself. Real database regression failed before the fix and passes with punctuation-bearing titles/URLs and no-match input.149 Desktop/274 host tests and platform/format checks pass (`build/history-search-final.log`); Hot Reload264 succeeds.


## Manual cleanup memory bound

Old-version pruning now selects/deletes at most500 IDs per SQLite transaction and cleans each file batch before continuing, preserving the newest chronological pair. A1202-version real database/local-source regression verifies ties, repeated content, unrelated-page isolation and idempotence.150 Desktop/274 host tests and platform/format checks pass (`build/manual-cleanup-final.log`); Hot Reload265 succeeds. Physical storage endurance and other deletion paths remain separate gates.
