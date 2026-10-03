# Product completion audit — 2026-09-19

The full objective remains unproven. This matrix preserves the original review's scope and identifies evidence still needed; it is not a release sign-off or a numerical quality score. The source review is `2026-09-07-full-product-review.md`. Historical execution details and artifact paths are in `implementation-progress.md`.

“Implemented” below means the named correction exists in the current source. It does not turn narrower tests into proof of all lifecycle, accessibility or release requirements. Each evidence item must be revisited if its implementation changes.

## Original numbered findings

| ID | Current correction / evidence location | Remaining proof or implementation |
|---|---|---|
| 01 | App manifest declares dataSync on WorkManager's SystemForegroundService. AndroidPageCaptureTest and recorded foreground device journeys exist. | Final current-build foreground-worker journey in the consolidated release evidence. |
| 02 | Shared MonitoringSchedule and first-monitor setup; AddEditViewModel tests and recorded fresh-install scheduling journey. | Reconcile fresh-install journey artifacts with final build. |
| 03 | SitesDao uses Upsert and preserves current observation metadata on edits; real Room tests. | Include current Room execution in final gate results. |
| 04 | Legacy unknown-hash pruning correction and Room regression tests are recorded. | Reinspect query and specific migration assertions during final data audit. |
| 05 | TurboSession owns awaited runner work, with cadence, timeout, concurrency and selection tests. | Confirm every current entry point still uses that owner; actual device endurance remains separate. |
| 06 | Service finish/Stop funnels to session.stop; old worker-tag cleanup remains compatibility code. | Audit app, notification and widget stop paths together and record current device stop evidence. |
| 07 | Credential files removed from working tree; external-path signing configuration; credential-remediation.md. | External revocation/rotation and coordinated published-history cleanup are NOT done. No release credentials may be reused. |
| 08 | No sibling checkout in settings.gradle.kts; lint fails on errors. New dedicated Android CI job now executes instrumentation and retains reports. | Hosted run and required-check configuration unverified. Current-source clean verification now passes; see the September22 clean-source evidence below. |
| 09 | Session state is controller-owned; restoration/race tests in TurboSessionTest. | Reinspect persisted-state recovery and current process-death evidence. |
| 10 | WorkerHelper persists session transitions then updates all widgets. | Real widget refresh after app/notification/expiry transitions still needs consolidated proof. |
| 11 | Shared MonitoringSchedule default consumed by UI and scheduler; schedule host tests. | Final default/first-run matrix. |
| 12 | ContentHasher no longer caches document bodies. Large-text/diff tests exercise bounded rendering. | Selected-text capture now passes Android stress for repeated >6 MB pages and 2,000 nested elements; unnecessary serialization growth was fixed. Real WebView stress now covers repeated tall changing pages, footer preservation, pixel/height caps and oversized-page recovery. Image-heavy DOM/heap exhaustion remains unverified. |
| 13 | SnapshotRetention preserves the newest pair and enforces retention; engine orphan cleanup; storage tests. | Reinspect exact byte/age policy and user-visible policy against changing HTML/image stress. |
| 14 | Empty-response policy and regression coverage recorded in SyncEngine/ContentHasher tests. | Reinspect exact failure classification and final empty-baseline/change behavior. |
| 15 | SitesDao transactional reset and history-preservation tests. | Consolidated Room failure/cancellation gate. |
| 16 | Observation writes preserve read-state; comparison requires explicit acknowledgement of the loaded latest identity and supports guarded Undo. | Current Room plus notification/late-capture gate. |
| 17 | Equal-size replacement regression fixed by changedByteCount. | Legacy byte filters now have an explicit explanation and save-only removal; renaming/cancelling preserves them. TEXT/SOURCE rules bypass obsolete thresholds. The legacy byte metric is not presented as visual tolerance; HTML region/ignore controls are now implemented with shared preview/capture extraction; opt-in visual tolerance is now implemented; see the per-monitor filtering evidence below. |
| 18 | Notification identity contains site/capture; comparison selects requested capture and refuses silent missing-history fallback. | Consolidate actual notification tap and older-alert-after-newer-capture evidence. |
| 19 | Hard session duration and onTimeout→SystemLimit implemented; virtual deadline/cancellation tests. | Actual service timeout/three-hour endurance limitations must stay explicit. |
| 20 | Durable last success/failure, waiting-baseline state and schedule eligibility shown. Watchlist read failure now retains last complete result and reports failure. | Watchlist status/latest/count now come from one SQLite statement, verified during 40 transactional updates and history/status invalidation. Audit remaining health labels; avoid claiming exact background cadence. |
| 21 | Site options expose pause/resume and notification toggles; MainScreen wires them. | Current device toggling while Turbo is active. |
| 22 | Draft schedule with units/presets/domain limits and serialized WorkManager UPDATE; recorded 15min/7d journey. | Final settings/save/active-work regression evidence. |
| 23 | Turbo rows show failure and open results while running; explicit Edit selects pages; TurboUiTest. | Current device suite and real failure-state screenshot. |
| 24 | Correct historical pair selection and formatted previous-change labels; comparison read-state/device tests. | Final chronology source/labels audit. |
| 25 | One lazy history list replaces the sheet→Older dialog flow; selected pair is the initial scroll anchor, PageFacts shares the scroll area, and Close is pinned. | 1,001-version device test proves selected/oldest/latest reachability with bounded composition at 2× text/RTL. Installed-app sheet screenshot reviewed; broader accessibility matrix remains. |
| 26 | HistoryList keeps loaded rows and explains append failures with a Retry action. | Real Pager device test fails the first append, verifies loaded entries remain usable, retries successfully without refreshing the initial page, and reaches the final entry. Current implementation is verified for append recovery. |
| 27 | MainScreen wires Open page; comparison navigation work recorded. | Verify both current menu and detail action routes. |
| 28 | SettingsScreen's first content item is monitoring; setup inherits schedule. | Final add/detail/settings visual journey. |

## Required gates and design scope

1. **Reproducible verification:** refresh clean-source build, run exact CI commands, verify hosted workflow and required status externally. Local tests alone do not prove hosted CI.
2. **Fresh-install product journey:** add → baseline → normal schedule → changed capture → alert → correct comparison. Use synthetic data and a current build.
3. **History safety:** rename/Keep history, unknown hashes, transactional Start fresh failure, read-state races, deletion Undo and retry. Current targeted tests exist; consolidate exact cases/results.
4. **Turbo execution:** virtual 30s/slow/retry/multi-site/selection/deadline tests plus all entry-point Stop checks. Do not replace this with UI-only tests.
5. **Android lifecycle:** foreground screenshot, screen off, Doze/constraints, process death, offline recovery and notification denial. Existing local journeys must be mapped to final source; no physical-OEM guarantee or exact offline scheduling claim.
6. **Resources:** large changing HTML/images, bounded body/hash/diff/bitmap allocations, storage retention and exhaustion recovery. Near-10MiB text rendering and Android selected-text preparation of repeated >6 MB pages/2,000 nested elements are verified. Real WebView tests also cover repeated tall changing pages within the 4M-pixel/10,000-height caps and oversized-page rejection/recovery. Image-heavy DOM/heap pressure and storage-exhaustion recovery remain broader checks.
7. **UX/accessibility:** compact/medium/expanded, 2x font, light/dark/dynamic color, measured contrast, TalkBack, landscape, RTL and zero-animation behavior. Turbo light/dark/1.8x/zero-motion, long history at 2×/RTL, and Changes summary no-overflow/4.5:1 dynamic light-dark contrast at 280dp/2× are verified subsets.

All six original visual findings remain in scope: competing Turbo signals; decorative data-like ribbon; incidental comparison surfaces; list bottom clearance; translated/large labels and semantic contrast; wide/short Turbo reachability. Current Turbo frequency-first refinement and watchlist/comparison polish address several, but a complete layout/accessibility matrix is still outstanding.

Product direction also remains in scope: per-monitor capture choice and preview exist; new Readable text defaults and explicit Any source change behavior are now implemented and tested. Legacy positive thresholds are now explained and removable. HTML region/ignore controls are implemented and tested, including missing-match failures and schema migration; opt-in visual-tolerance controls are now implemented (see the per-monitor filtering evidence below). Monitoring health must reflect actual observations. On-device opportunistic scheduling remains the product contract; exact execution while a phone is offline/off requires a different service and must not be promised.

## Immediate order

Finish validating the new CI execution gate and rendered region-editing journey. Follow with the resource and accessibility matrices, consolidate lifecycle/history evidence, and rerun final clean-source checks. External credential and repository-protection work must remain explicitly unverified until independently confirmed.

### CI execution evidence

The dedicated native job uses the upstream [Android Emulator Runner configuration](https://github.com/ReactiveCircus/android-emulator-runner#configurations). Local `ANDROID_SERIAL=emulator-5560 ./gradlew :composeApp:connectedAndroidDeviceTest` executed50 tests successfully and produced the declared report directories. `actionlint` validates the workflow. These results strengthen gate1 but do not prove a hosted workflow run or required-check configuration.

The complete native build step also passed locally, including209 host tests, formatting, lint (0 errors/52 warnings), debug assembly and optimized unsigned release assembly. See `build/ci-android-verification-online.log`. This is current-worktree evidence, not a fresh checkout or hosted-run result.


### Desktop schedule integration evidence — 2026-09-22

DesktopScheduleJourneyTest now exercises actual loopback HTTP, SQLite, snapshot files and DesktopMonitoringManager at both15minute/7day coroutine-time boundaries. Baseline has no change notice; changed captures provide notice IDs, unread state and correct before/after bytes; disabling scheduling prevents later requests. Full154 Desktop/276 host tests pass (`build/desktop-schedule-journey-final.log`). This strengthens Desktop scheduling/data integration evidence, not physical elapsed-time endurance, preference persistence or a completed UI journey. Native automation twice reports the Mac locked; an unlock request is pending. Do not treat MCP's successful click response with unchanged Settings semantics as navigational success.


### Scheduled server-failure recovery evidence — 2026-09-22

The real Desktop schedule journey now includes HTTP503: failure metadata changes without losing the last-success timestamp, unread status, saved rows/files or future scheduling. Changed HTTP200 recovery produces exactly one new comparison/notice; the next unchanged scheduled response produces no duplicate.154 Desktop/276 host tests pass (`build/desktop-schedule-recovery-final.log`). This is local HTTP/SQLite/files with virtual schedule waits and a recorded notifier; physical offline/Doze/OEM/OS-notification endurance and the locked live UI journey remain unverified.


### Desktop image resource bounds — 2026-09-22

Replaced full Desktop screenshot decoding with metadata-first bounded decoding, source subsampling for supported large formats and safe rejection otherwise. Display4MP/4096, comparison1MP/2048; original size is preserved and sampled statistics labeled. Real6000×1600PNG tests cover output bounds, small-image pixels, dimension mismatch and malformed input.156 Desktop/276 host tests and platform builds pass (`build/desktop-image-final.log`). This closes the identified per-image unbounded decode path, not the broader peak-memory/GPU/cancellation/codec/physical-device stress acceptance gate.


### Desktop image concurrency follow-through — 2026-09-22

Both image consumers now use a shared two-worker dispatcher; comparison cooperatively checks cancellation between decode/buffer/analysis stages. A blocking-worker test verifies capacity and skipped canceled queued work.157 Desktop/276 host tests and platform builds pass (`build/desktop-image-queue-final.log`), Hot Reload276 succeeds. An in-progress native decode may finish before cancellation takes effect; process/GPU peak memory and hardware endurance remain separate acceptance requirements.


### Clean-source local gate — 2026-09-22

A fresh716-file snapshot of current tracked/untracked nonignored source passed all206executed tasks with --no-build-cache:157Desktop/276host tests, Spotless, Android test-APK/debug/release assembly, lintDebug (0errors/53warnings), iOS and Wasm compilation. No generated directories/local.properties were copied; installed toolchains/dependency caches were reused. Before/after source hashes matched. Reports/APKs retained in build/clean-source-evidence, SHA256manifest in build/clean-source-review-manifest.json and log in build/clean-source-review.log. Temporary source/build copy was removed after evidence retention.

This strengthens gate1's local reproducibility and current-build compile/test evidence. It does not establish hosted CI/branch protection, device-test execution or a clean committed Git checkout. The Mac remains locked, so the pending live UI journey is still unverified. No emulator was started.


### Desktop CI job added — 2026-09-22

Compose Desktop verification now has a dedicated macOS job, running Desktop tests/iOS compilation/formatting and retaining test reports plus rendered PNGs on success/failure. SDK setup is pinned to upstream v4 commit be39fa834029ff78f1a44aa3bb0819b8fc2bd8fd. Actionlint and the exact local Gradle command pass (build/desktop-ci-actionlint.log, build/desktop-ci-local.log). The local Desktop task reused the preceding clean verification outputs. Workflow changes remain uncommitted/unpublished; hosted execution and branch protection remain unverified. No emulator is added to the Desktop job or executed locally.


## Preserve self-contained page styling in previews

Found that secureSnapshotDocument discarded every embedded stylesheet and all original body attributes, turning even self-contained source pages into generic text. It now retains up to32 embedded CSS blocks within a combined256000-character budget, conservatively omitting network-dependent, escaped, non-CSS or oversized blocks. Original media conditions, sanitized body attributes and root language/direction/class/id/style survive. Deny-by-default CSP, executable/navigable element and attribute removal, remote-resource restrictions and script-disabled snapshot WebViews remain. This benefits both saved HTML viewing and the rendered region picker; it does not change stored content or comparison hashes. Short preview bodies now fill the viewport instead of showing an unrelated white lower panel.

Two new host regressions verify class-based typography/layout and body/root identity, print-only media isolation, and rejection of remote/escaped/oversized styles without dropping content. Actual Desktop Chromium integration verifies computed background, padding, font, border and viewport coverage across five browser sessions, alongside script removal, selection bridge, retained/ignored highlights, live updates without reload, and focus/scroll restoration. Fresh native-browser-smoke.png and native-region-selection.png were inspected. Logs: build/snapshot-styles-test.log, build/snapshot-styles-browser.log, build/snapshot-styles-final.log.

Final160 Desktop/278 Android host tests pass, zero failures/errors/skips; formatting, Android debug assembly, iOS/Wasm compilation and Hot Reload279 pass. No emulator used. This improves fidelity for self-contained CSS only: external styles/assets remain unavailable, rejected mixed-resource stylesheets are omitted as a whole, and existing text-only captures cannot regain discarded HTML. Full source-page fidelity and visual-tolerance controls remain open product requirements. Native CUA inventory was checked again and reports the Mac locked; full live app interaction remains unavailable, distinct from the successful isolated embedded-browser integration.


## Preserve saved capture modes across platform-limited editing

Found and fixed a silent configuration change: AddEditViewModel.loadSite replaced an existing Screenshot or App default mode with HTML on platforms without screenshot capture. Renaming could then persist that replacement. Loading now preserves the saved mode. The picker retains an unsupported current choice, disables selecting it as a new option, and explains in EN/PT/ES/IT that it stays unchanged until the user chooses another mode. The saved-mode dialog capture was inspected (`build/desktop-review/capture-mode-preserved.png`).

Also prevented the shared HTTP check entry point from silently capturing HTML for an explicit Screenshot monitor on a platform without visual monitoring. It now reports stable CAPTURE_UNAVAILABLE with localized actionable guidance and a non-retryable content error; it keeps history and last-success metadata. Android screenshot execution is unchanged. App default resolution is not asserted to be screenshot capture on Desktop.

A real Desktop SQLite/files regression verifies preserved Screenshot/App default draft and persisted mode during rename, retained image metadata/bytes, zero HTTP requests and no notification for unavailable screenshot checks, preserved lastSuccessfulCheck, and explicit supported-mode selection remaining a draft until Save. A UI regression verifies the current unavailable mode and guidance remain visible, with a working explicit Readable text choice. Initial test fixture failure was a missing temporary snapshots directory; fixed the fixture, not production storage behavior.

Final162 Desktop/278 Android host tests pass with zero failures/errors/skips; formatting, Android debug assembly, iOS/Wasm compilation pass (`build/saved-capture-mode-final.log`). No emulator used. This fixes an immediate data/behavior integrity issue discovered while auditing capture controls; visual-tolerance functionality remains unfinished and must not be claimed complete.


## Consistent image comparison and functional Desktop highlights

Found different comparison rules: Desktop used independent RGB-channel cutoffs and ignored alpha; Android's RGBA path used combined squared distance including transparency. Introduced reusable detection.PixelDifference and applied the Android convention to both adapters. Invalid tolerance values now fail consistently at the shared rule. Shared display-color generation keeps red changed pixels and the muted baseline consistent too.

Desktop now encodes the bounded difference image as PNG instead of returning only statistics and silently falling back to swipe under Pixel diff. The extra image/output buffers remain limited by the existing1MP comparison decode cap and two-worker dispatcher; temporary BufferedImages are flushed. This is bounded-buffer implementation evidence, not a peak-heap/GPU benchmark.

New native-PNG tests cover single-channel noise, larger combined differences, transparency, zero/default/maximum tolerance and exact parity with the RGBA result and rendered output pixels. Host coverage includes invalid tolerances. A Desktop UI test computes a real difference and verifies the Changed pixels image, absence of the swipe Before control, and visible red highlights; inspected build/desktop-review/desktop-pixel-highlights.png. Changed the inaccurate pixel-identical message to localized 'No differences detected in this preview', since sampling/tolerance cannot prove full-resolution identity.

Full164 Desktop/279 Android host tests pass, zero failures/errors/skips; formatting, Android debug assembly, iOS/Wasm compilation pass (`build/pixel-parity-final.log`). No emulator used. These changes affect comparison results and presentation; they do not yet wire a persisted per-monitor visual-change threshold into SyncEngine. That product requirement and device/live-app acceptance remain open.


## Reusable image analysis for monitoring

Moved non-composable image processing out of ui.comparison into the shared image package: SnapshotImageDiff/PlatformImageDiff, RGBA comparison, Android sampling and Desktop decoding. Screen/view-model consumers and tests now import that layer. This avoids making the synchronization engine depend on screen implementation when visual filtering is wired.

Added includeDiffImage (default true) to the comparison contract. Android's RGBA path and Desktop skip the output-pixel allocation/PNG encoding when false while keeping exactly the same statistics. Native PNG and host RGBA tests verify result parity and absent output images. Existing screen calls preserve red highlights. Wasm's existing statistics adapter now also uses the shared PixelDifference rule; only its compilation, not live browser behavior, was verified.

Full164 Desktop/279 Android host tests pass, zero failures/errors/skips; spotlessCheck, Android debug assembly, iOS/Wasm compilation and Hot Reload282 pass (`build/image-module-final.log`). No emulator used. One initial command used nonexistent compileKotlinAndroid; replaced it with the established assembleDebug validation and completed successfully.

Visual filtering remains unfinished. Next implementation must persist an opt-in per-monitor minimum changed-area setting with a backward-compatible migration/default off; surface it in screenshot setup with honest sampling/tolerance wording; compare incoming images to the last saved baseline via statistics-only analysis; always retain first/dimension-changing captures; surface decode failures instead of treating them as unchanged; include policy in in-flight commit guards; keep ignored small changes from advancing the baseline so they accumulate; prove migration, save/cancel/reopen, threshold boundary and actual capture/notification behavior. Do not claim the monitoring policy has changed from this module extraction alone.


## Per-monitor visual filtering wired end to end

Implemented optional visualChangeThresholdBps on Site, Room schema8 and7→8 migration default0 (off). Add/edit draft loads, preserves and saves it; the shared screenshot/App default editor exposes Off/0.1%/1%/5% choices in EN/PT/ES/IT with explicit reduced-resolution/color-tolerance and accumulation wording. Cancel/reopen preserves the stored value, not an unsaved choice. Enabling the visual filter supersedes/clears a legacy byte filter on Save; legacy filtering remains visible when relevant. Text/source captures do not apply this screenshot filter. Invalid out-of-range stored values disable visual suppression rather than suppressing all changes.

SyncEngine uses statistics-only image analysis before saving/notification. First captures and dimension changes are retained; threshold comparison is inclusive and uses integer sampled-pixel counts. Below-threshold captures update check health without replacing the last saved baseline, allowing cumulative changes. Decode/storage failures return a failed observation while retaining history and last-success state. Visual policy is included in pre-processing and commit guards against edits during an in-flight capture.

Evidence: real PNG/SQLite/files/notifier journey verifies99/100 pixel boundary at1%, cumulative100→150→200 changes, resizing, malformed-image failure, retained file and last-success data, stale-policy rejection, and disabling the filter. Actual version7 schema migration preserves screenshot mode, legacy byte setting and a saved version; saved filter survives reopening the DB. Editor regressions cover draft/cancel/save/reopen. Normal and180% text choice/cancel/off interactions pass; fresh visual-filter-normal.png and visual-filter-large.png inspected. These dialog captures use the Desktop test window, not a physical compact Android device.

Final168 Desktop/279 Android host tests pass, zero failures/errors/skips; formatting, Android debug assembly, iOS/Wasm compilation pass (`build/visual-filter-final.log`). The first test run found an ambiguous UI selector (field and selected dialog row shared text); scoped the selected-row assertion and reran successfully. The initial new-test constructor and formatting errors were corrected before final validation.

Live Desktop was rebuilt with Hot Reload285 and restarted through the connected tool286. First restart had still loaded old compiled schema7, so it was not accepted as migration proof. After rebuild/restart, direct read-only inspection proves schema8, all3 pages and4 snapshots retained, all prior filters0. Live semantics show All3/New1/Errors0 and the three pages. Native pointer/keyboard access is still blocked by the locked Mac. No emulator used. This closes the previously unfinished persistence/UI/shared-engine visual-filter implementation; actual Android capture/device-endurance and whole-product acceptance remain open.

## Durable file cleanup — September22

Schema9 now persists cleanup intent transactionally via a snapshot-deletion trigger. Page cascades and history resets no longer materialize an archive-sized file-ID list. Indexed keyset reads cap each application batch at500 identities; file failures stay pending without starving later work, and a process-start retry loop operates even when scheduled monitoring is off. Tests exercise1203-version restart recovery, failures/missing files, rollback, cancellation between file removal and acknowledgement, and migration8→9.172 Desktop/279 Android host tests plus Android app/device-test APK assembly and iOS/Wasm compilation pass. This supersedes earlier statements that current committed deletion cleanup relies solely on NonCancellable or delayed orphan sweeping; the latter remains for old/uncommitted leftovers. It does not prove physical device endurance, disk-corruption recovery or a bounded SQLite cascade execution time.


## 2026-09-22 — Cohesive visual redesign after user rejection

The user explicitly rejected the mixed visual system across Watchlist, Changes and Settings. Earlier positive visual assessments are not acceptance. Reworked the shared neutral/purple color roles, typography weights and shape scale across all four platform themes. Watchlist now uses independent tonal cards, filter chips and a labeled Add page extended FAB. Changes uses the same cards and site identity, a quiet count summary and full-width titles. Settings uses tonal groups and icon containers with clearer Theme and color / Storage labels in all four bundled languages. Creation, receipt and Turbo inherit the shared foundation; their individual layouts are not all considered redesigned or accepted.

Validation: 173 Desktop and 279 Android host tests pass, zero failures/errors/skips; formatting, Android debug assembly, device-test APK compilation, iOS simulator and Wasm compilation pass (`build/cohesive-redesign-final.log`). The actual Koin-root HTTP/SQLite/files journey now also opens the populated-watchlist FAB and closes the new form without adding another page. That test exposed a missing merged accessible name on the FAB; the explicit localized name fixes it. Light/dark watchlist, 200% history title, settings, Turbo and receipt captures inspected. No emulator.

Desktop managed restart308 loaded the final resources. Fresh live screenshots320/322/324 show the watchlist, quiet Changes summary and renamed Settings rows. Current screenshots: `build/desktop-review/redesign-live-watchlist.png`, `redesign-live-changes.png`, `redesign-live-settings.png`. Live UI error326 is false. The Mac is now unlocked: AppleScript brought the owned Java window forward after CUA could not address the unbundled java app. Opening Add page was visually confirmed; native Escape followed by settled semantics319 returned to the existing three-page watchlist. This is limited keyboard-dismissal proof, not a complete keyboard or screen-reader audit. Immediate screenshots during transitions can contain the prior frame; older occluded screenshots are not visual evidence.

Remaining product work includes more informative saved-version presentation, friendly localized dates, complete flow-level visual acceptance and physical-device/endurance coverage. This pass improves consistency; it does not establish a 9/10 app or completion of the overall goal.


## 2026-09-22 — User-directed connected expressive cards

The user explicitly requested Material3 Expressive segmented cards throughout grouped interfaces and more refinement. This supersedes the earlier standalone-card direction. Watchlist rows now derive shared first/middle/last shapes from filtered ordering, with4dp gaps. History derives segments within each calendar-day group using paging peek (no extra load-triggering lookups). Settings actions use individually shaped segments with4dp gaps instead of dividers; page preferences use the same arrangement. Turbo already uses connected groups. Standalone preview/content cards remain standalone where no list group exists. Geometry is centralized in ExpressiveGroup's24dp exterior/8dp interior shape mapping and a shared boundary helper.

Comparison changes from the same turn: moved explicit Mark as read into the Scaffold bottom bar, retained existing guarded acknowledgement/Undo behavior, shortened URL display and softened mode/content surfaces. Before/after cards use24dp outer corners.18 focused comparison/journey checks initially passed after resolving a build-vs-HotReload output race; managed restart360 refreshed resource caches. Do not run HotReload compilation concurrently with Gradle validation against the same outputs.

Final178 Desktop/280 Android host tests pass, no failures/errors/skips; Android debug assembly and iOS/Wasm compilation pass (`build/expressive-segments-final.log`). Formatting applied and diff check passes. Light/dark watchlist, settings, page preferences and updated comparison fixture captures inspected. HotReload361 succeeded; live screenshots363/365 show connected Watchlist and Settings, UI error366 false. Screenshots: `build/desktop-review/segmented-watchlist-live.png`, `segmented-settings-live.png`, `app-journey-comparison.png`. No emulator. Further refinement and overall goal acceptance remain open; no award-winning/9-of-10 claim.


## 2026-09-22 — Comparison and history follow-through

Earlier remaining-work notes about date headings and repeated introductory history excerpts are superseded by implemented localized headings and bounded Before/After passage previews (see implementation-progress). The comparison was subsequently rebuilt after direct user rejection: compact version rail, shared background, After-first connected passages, expandable originals, real HTML section headings, compact acknowledgement, and now pinned multi-passage navigation/picker. Immutable pair selection and explicit read/Undo remain required and covered by journeys. Latest complete-suite evidence is189 Desktop/289 host before the final small refinements; later focused gates are documented separately. General visual acceptance, broader end-to-end product review and external/device/endurance limitations remain open. None of these narrower checks establishes overall completion.


## 2026-09-22 — Refreshed regression scope

The current-worktree gate passed201 Desktop/291 Android host tests, formatting, Android debug/device-test APK assembly, lintDebug (0 errors/52 warnings), iOS/Wasm compilation (`build/product-regression-current.log`). This supersedes the older full-suite counts for the then-current navigation/comparison/settings/detail implementation. A subsequent history-decision dialog extraction passes4 focused Desktop tests plus Android/iOS/Wasm builds (`build/address-history-dialog.log`); a post-extraction full gate is not claimed. Device-test APK compilation is not execution. No emulator was used. Physical lifecycle/endurance, native assistive technology, full visual acceptance and external release limitations remain unproven.


### Palette contrast evidence

The default and generated selectable palette roles now pass504 nominal text/background pairs at4.5:1 in light/dark, after correcting three reproduced default-light failures.41 focused UI/contrast tests plus platform builds pass (`build/palette-contrast-final.log`). This narrows the color gate; real Android wallpaper-derived colors, disabled/interactive overlays, every custom rendered combination and assistive-technology acceptance remain separate.
