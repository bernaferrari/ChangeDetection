# Current implementation and M3 Expressive review

Verdict: approve with changes for the reviewed implementation; whole-product sign-off remains blocked by the unresolved acceptance gates. The app is substantially more coherent, but neither a 9/10 rating nor complete reliability is established.

## Scope and evidence

Reviewed the current working-tree implementation and earlier review/progress records, focusing on watchlist, Turbo UI/kernel/Android service, settings recovery, snapshot deletion/maintenance, navigation motion and platform capabilities. Compared existing Compose captures with the supplied v4 preview and HTML. Read the live Desktop Settings semantics through the connected Hot Reload bridge. This is a broad risk-based review, not a claim that every changed line or every interaction was independently exercised again.

Ran `:composeApp:desktopTest :composeApp:testAndroidHostTest spotlessCheck`: successful, with the test tasks up-to-date. Current reports contain 150 Desktop and 274 Android host tests, with zero failures/errors/skips. `git diff --check` passes. Log: `build/current-review-validation.log`. No emulator used. Existing visual captures were inspected, not freshly generated in this review. No production source changes were made.

## Findings to fix

### P2 — Selected watchlist tiles still mix incompatible color roles — corrected

`composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/main/SiteRow.kt:113–115,153,176–224`

Selection changes the container to primaryContainer, but title, supporting text, menu and status keep onSurface/onSurfaceVariant/primary/error. Those roles are intended for different backgrounds; custom and dynamic schemes need not preserve their contrast on primaryContainer. The earlier correction to SiteIdentity and TurboSiteRow did not cover this row.

Use onPrimaryContainer for ordinary selected-row content. Preserve error/unread meaning through explicit icons/text or a small separately paired status container. Verify selected normal/error/unread states in light, dark and generated schemes. This is a confirmed token-contract issue, not a claim that a measured contrast threshold fails in every current palette.

### P2 — A storage summary failure disables unrelated settings — corrected

`composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/settings/SettingsViewModel.kt:77–102`

Storage-size observation and preferences share one combine/catch. If the database summary fails while DataStore remains healthy, the entire settings state becomes unloaded/failed. Appearance and scheduling disappear or become unavailable even though their underlying preferences are readable.

Give storage its own loading/error/retry state and preserve the last valid preference state. A storage error should affect Saved history, not the entire Settings screen. Test a failure after preferences have loaded, then recovery without resetting a schedule draft.

### P2 — Android republishes an unchanged foreground notification after every check — corrected

`composeApp/src/androidMain/kotlin/com/bernaferrari/changedetection/QuickMonitorService.kt:95–103`

The observer collects the complete TurboState. Every result changes counters/lastCheckTime and causes NotificationManager.notify, although the notification displays only page count and cadence. With many pages this creates repeated unnecessary notification construction and system calls throughout a long session.

Observe a distinct projection of active/end state and fields actually rendered in the notification. Keep terminal Stop handling immediate. Verify counter-only emissions do not republish, while page-count/cadence changes and termination do.

### P2 — Deleting a page can miss the file from a concurrent capture

**Corrected in follow-through:** file identities are now read by `SitesDao.deleteSiteWithSnapshots` inside the same Room write transaction as cascading deletion, and returned for file cleanup. Generated Desktop DAO code confirms the transaction wrapper. The source no longer reads complete Snap records for this operation. A real SQLite/Desktop-file contention test verifies a concurrent committed capture is removed, another page remains untouched, late inserts are rejected by the foreign key, and repeated deletion returns no IDs. This still materializes a list of IDs; bounded durable cleanup and power-loss recovery are separate concerns.

`composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/repo/source/local/SnapsLocalDataSource.kt:43–49`

The snapshot-ID read and page deletion are separate database operations. A capture can commit between them: cascading deletion removes its row, but its file is absent from the previously collected IDs. NonCancellable protects against navigation cancellation; it does not make these two operations atomic. The orphan sweep mitigates this only later, after its 24-hour age threshold and a subsequent maintenance opportunity.

Collect cleanup identities and delete the page within one write transaction, or use a transactional cleanup queue for bounded large-archive deletion. Add a deterministic concurrent-capture regression. Separately, this path still materializes every snapshot record; the bounded manual-prune fix does not cover it.

### P2 — The browser demo exposes a Turbo action that does nothing — corrected

`composeApp/src/wasmJsMain/kotlin/com/bernaferrari/changedetection/di/PlatformDiModule.wasmJs.kt:63–68`; `composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/AppContent.kt:260–270`

The shared route offers Start Turbo, but DemoMonitoringManager.startQuickMonitor is a no-op. Regular scheduling already communicates the unavailable platform capability; Turbo does not. Scope this finding to the Wasm demo, not Android or Desktop monitoring.

Expose Turbo capability to the route, and show an explanatory unavailable state in preview builds. Avoid a clickable action that silently leaves the screen unchanged.

## M3 Expressive refinements

### P2 — Turbo setup gives its least informative value too much space — corrected

`composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/main/components/TurboHero.kt:60–89`

The 80sp cadence numeral, 88sp line height and repeated 24dp gaps dominate setup before page selection. In the inspected 400×768 capture, the second selected page is already cut off by the footer. The supplied reference reserves the large numeral for an active countdown, which carries changing information.

Keep the running countdown as the single expressive hero. Make setup cadence and duration a compact control group, putting the selected page/context higher. Retain the pinned Start/Stop action, 48dp targets, full accessible duration names and adaptive wrapping. Do not shrink touch targets to copy the HTML density.

### P3 — Consolidate component policy before adding more decoration

The lighter watchlist and quiet detail/settings rows are improvements worth keeping. Shape and type decisions are still scattered: SiteRow hardcodes 12dp, TurboHero hardcodes 24dp, segmentShape hardcodes 24/8dp, and Type.kt makes most headline/display roles bold or semibold. SiteRow still accepts a segment position it no longer uses. This makes later changes easy to apply inconsistently.

Define shared identity, screen-title, supporting-text and group-shape roles. Use MaterialTheme shapes for repeated components and explicit exceptions for the single hero. Remove unused segment-position plumbing from flat rows. Do not make every row a filled capsule: connected selection groups and quiet navigational lists serve different purposes.

### Motion

Keep the verified mirrored navigation transitions and zero-motion behavior. Avoid adding perpetual pulses, entrance staggers or bouncing list rows. Prefer Material motionScheme effects specs for custom color transitions instead of accumulating unrelated fixed tweens/default springs; this is a consistency refinement, not a demonstrated reduced-motion failure. Inspect rapid repeated selection and interruption, not just a single animation.

### Component context and token references

- Target: Android compact phones, validated locally through shared Compose Desktop; medium/expanded and 200% text remain explicit acceptance cases.
- Intensity: foundational for lists/settings, stronger expression in the active Turbo countdown and primary action.
- Components: list rows, selection groups, buttons, app bars and progress indication.
- Local references: `.agents/skills/material-3-expressive/references/m3-color-foundation-tokens.md`, `m3-shape-foundation-tokens.md`, `m3-button-groups-specs-tokens.md`, `m3-motion-foundation-tokens.md`, `m3-typography-type-scale-tokens.md`.
- Preserve semantic color pairs, 48dp targets, explicit selected semantics, standard navigation and system motion scale. Measure theme contrast rather than inferring it from token names alone.

## Keep, and do not overclaim

Keep the shared TurboSession lifetime owner, monotonic deadline, per-page capture guards, entry-owned drafts, explicit comparison/read state, recoverable preference writes, literal history search and deterministic paging. These improvements do not justify another broad architectural rewrite for aesthetic reasons.

Still unverified: final-build Android screen-off/Doze/OEM/process-death endurance and notification delivery; full keyboard/screen-reader/RTL/landscape/dynamic-contrast journeys; image-heavy resource pressure; whole-product visual acceptance against the HTML; external credential remediation and hosted CI protections. Green host reports do not establish those outcomes.


## Compact Turbo setup follow-through

Removed the oversized setup numeral, grouped cadence label/choices and reduced internal gaps while retaining 48dp options, localized accessible names and the pinned action. Normal capture now shows three selected pages;180% capture retains wrapping and scroll access. The active countdown remains prominent. Extended setup regression verifies page visibility above Start before scrolling; full151 Desktop/274 host tests and Android/iOS/Wasm builds pass (`build/turbo-compact-final.log`). Hot Reload268 succeeds. Broader visual acceptance remains open.


## Android notification follow-through

The service now observes a distinct notification-specific state: page count, cadence and active/end state. It builds notifications from that captured state; initial promotion uses the requested configuration. Real TurboSession virtual-time host coverage verifies quiet repeated checks and immediate configuration/Stop/expiry emissions. Full151 Desktop/275 host tests and platform builds pass (`build/turbo-notification-final.log`). Actual OS delivery and battery endurance still require hardware evidence.


## Settings storage failure isolation follow-through

Storage observation now owns its error/retry state. Readable preferences remain loaded and subscribed during failure/retry. Saved history exposes localized Retry both on Settings and inside its dialog; schedule drafts retain their interval and enabled Save. Host fault injection, Desktop interactions and inspected recovery capture pass with152 Desktop/276 host tests plus Android/iOS/Wasm builds (`build/settings-isolation-final.log`); Hot Reload269 succeeds.


## Selected watchlist follow-through

Selected rows now pair primaryContainer with onPrimaryContainer across text/status/menu content, retaining explicit error/unread labels and icons. Row shape uses the theme medium role. Light selected-error/unread and200% dark selected-paused captures were inspected; interaction tests retain selection/open/options behavior. Full152 Desktop/276 host tests and platform builds pass (`build/selected-tiles-final.log`), Hot Reload270 succeeds. This is not an exhaustive measured dynamic-contrast claim.


## Browser preview capability follow-through

Turbo availability is explicit and false on Wasm. The shared screen replaces nonfunctional setup/Start controls with localized guidance; active Stop remains reachable. A Desktop capability-injection regression and inspected unavailable-state render pass alongside153 Desktop/276 host tests and platform builds (`build/turbo-capability-final.log`). Hot Reload271 succeeds. Live deployed-Wasm behavior and whole-product acceptance remain separate from this shared UI evidence.


## Latest review: remaining flaws and Expressive refinements

Verdict: **approve with changes for this review, not whole-product approval**. This follow-up re-read the current watchlist row, options sheet, Turbo controls/countdown, typography, cleanup APIs, and keyboard-test coverage. Re-inspected existing light tile, Turbo setup, receipt and detail captures against the supplied v4 preview. These are saved test renders, not a newly operated native app. The historical findings above that say corrected remain closed.

Validation: `:composeApp:desktopTest :composeApp:testAndroidHostTest spotlessCheck` succeeds (`build/latest-review-validation.log`); test tasks were up-to-date. Existing reports contain **157 Desktop and 276 Android host tests**, zero failures/errors/skips. `git diff --check` succeeds. No emulator used, no production code changed in this review.

### Should fix — Turbo selection has two competing visual conventions

`composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/main/components/TurboHero.kt:153–200`

Cadence selection uses a pale neutral fill without an outline or check; duration uses purple fill plus outline. The inspected light capture makes the chosen 30s option substantially less obvious than the chosen 3h duration. Radio semantics exist, so this is a sighted-user affordance problem, not missing selected accessibility state. Use a consistent explicit selected marker and paired container/content roles for both groups, adapting their backgrounds to context. Keep 48dp targets and wrapping. M3 Expressive benefits from recognizable selection and deliberate shape contrast, not additional card decoration.

### Should fix — Page identity changes between the list and its options sheet

`composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/main/SiteOptionsBottomSheet.kt:93–97,170–194`; `ui/main/SiteRow.kt:241–269`

The list assigns a theme-paired identity color using title hash, whereas the sheet uses the persisted site color at 16% alpha with onSurface. Opening options changes the identity treatment of the same page and duplicates avatar construction. Reuse a single identity component and color policy. The sheet's custom composite also needs measured contrast; a failure is not established merely by reading its tokens.

### Should fix — Typography and shape policy still drift

`composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/theme/Type.kt:19–75`; `ui/main/components/TurboHero.kt:103,183`; `ui/main/SiteRow.kt:89`

Most display/headline roles globally choose bold or semibold; several components then override weights locally. This weakens control over the quiet hierarchy of the HTML reference. Define normal screen-title and emphasized hero roles deliberately instead of treating Expressive as globally heavier text. Setup uses theme extraLarge while active Turbo still uses a fixed 24dp shape, and options use fixed 12dp. Consolidate intentional component roles; do not blindly replace every radius with one value. Remove SiteRow's unused segment position and its call-site plumbing.

### Should fix — Remaining cleanup APIs do not share the newer deletion guarantees

`composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/repo/source/local/SnapsLocalDataSource.kt:50–54,87–94`

`deleteAllSnaps` and deletion by content type still materialize all records and then issue one ID-list deletion followed by file cleanup in a cancellable context. A cancellation after the database commit can leave files for later orphan sweeping; a large archive also expands the query's parameter list without a bound. The newer page-delete/prune paths should not be used to claim these older APIs are safe. No current UI call to `MainViewModel.removeSnapsByType` was found, so this is a latent repository/API flaw rather than a reproduced user-facing failure. Remove unused APIs or give retained ones bounded, cancellation-safe cleanup with an explicit process-death recovery strategy.

### Acceptance gaps, not confirmed implementation defects

- Desktop key-event tests found cover the comparison slider; there is no equivalent full Tab/Enter/Escape journey through add, options, Turbo and settings. Standard Compose components may already work, but mouse/semantic-click tests cannot prove focus order, restoration or visible keyboard focus. Test these before claiming accessible desktop completion.
- Native app interaction was previously blocked by the locked Mac. Existing screenshots cannot establish the complete current live journey or all light/dark, RTL, large-type and wide-window states.
- Physical Android screen-off/Doze/process-death behavior and the three-hour session bound still need final-build device evidence. Host virtual-time tests are useful but insufficient for that claim.
- The reference's authentic page preview and visual comparison remain important product acceptance checks. Simplified text captures or sanitized previews should not be described as visually equivalent to the source website.

Keep the quieter lists/settings, compact setup, active countdown with tabular digits, semantic color-pair fixes, recoverable settings and shared Turbo lifetime owner. Prefer focused refinements and verification over another indiscriminate redesign.


## Turbo selection and shared page identity follow-through

Cadence and duration now share a visible selected check, primaryContainer/onPrimaryContainer pair and primary outline. Check space stays reserved to avoid label movement. Options use the theme medium shape; existing 48dp minimum targets, radio semantics and adaptive wrapping remain. Re-inspected fresh `turbo-setup-light.png` and `turbo-large-type.png` captures: selection is explicit at normal and180% text, with Start reachable. The options sheet now reuses SiteIdentity, so its avatar follows the same palette and typography as the watchlist instead of a separate persisted-color composite.

Targeted Turbo UI tests and full157 Desktop/276 Android host tests pass, zero failures/errors/skips. Formatting, Android debug assembly, iOS simulator-target and Wasm compilation pass (`build/selection-identity-final.log`). Hot Reload277 succeeds. No emulator used. This closes the latest review's selection-affordance and options-sheet identity findings; it does not establish full keyboard/device-endurance acceptance or whole-product completion.


## Keyboard journey verification

Added two Turbo setup regressions at normal and180% text. After establishing initial focus, navigation/activation uses Tab, Enter and Space only: choose30s, choose15min, traverse to the pinned Start action, and assert the callback receives the three eligible pages and selected settings. Both pass. Added an options-sheet test covering keyboard open, accessibility Dismiss, restoration of launcher focus, reopen, Tab from Refresh to Edit, Enter activation and focus restoration after close.

An initial synthetic Escape assertion failed. Inspection of the installed Compose Dialog implementation shows back dismissal registered with the platform NavigationEventDispatcher, outside the scene key path exercised here. Do not treat that assertion as proof of a native bug or the replacement accessibility action as proof that native Escape works. Actual Window Escape routing and the complete app navigation journey remain acceptance checks.

Full Desktop suite now160 tests, zero failures/errors/skips; spotlessCheck and git diff --check pass (`build/keyboard-journeys-final.log`). Only tests/documentation changed; production remains the previously built source with276 Android host tests passing. No emulator used. This expands actual input coverage but is not a whole-app keyboard/screen-reader sign-off.


## Remove unused unsafe history deletion paths

Traced `deleteAllSnaps`, `deleteSnapsForSiteIdAndContentType` and `MainViewModel.removeSnapsByType` through the source tree: no live UI/application caller exists. Removed these repository/source APIs, the unused view-model method and the now-unused content-type snapshot query, and updated test fakes without changing their behavior. The supported page deletion, individual-version deletion and bounded prune paths remain. The demo/device-test DAO reset helper is separate and retained. This removes the latent cancellation/unbounded-parameter-list flaw rather than maintaining an unused destructive API. Page deletion still collects its file IDs in a transaction; this change does not claim bounded durable cleanup or power-loss completion for that path.

Full160 Desktop/276 Android host tests pass, zero failures/errors/skips. Formatting, Android debug assembly and iOS/Wasm compilation pass (`build/cleanup-api-final.log`). Follow-up comment clarification accurately scopes NonCancellable to navigation cancellation; spotlessCheck passes (`build/cleanup-api-comment-check.log`). No emulator used.
