# Current implementation review and Expressive refinements

Verdict: **approve with changes for the reviewed UI; whole-product release sign-off remains blocked**. The implementation is materially better than the old screenshots, but neither complete reliability nor a 9/10 architecture rating is established.

## Scope and method

Risk-based review of the accumulated redesign: watchlist and selection, add/receipt, Turbo controls and lifetime owner, navigation, comparison/read-state, settings recovery, capture capabilities, image comparison/filtering, storage cleanup and prior acceptance findings. Inspected current source and existing review evidence, read the supplied v4 CSS, and inspected newly generated full-app Desktop screenshots. This is not a claim to have independently reread every changed line or exercised every platform combination in this pass.

The v4 project remains the visual reference, not an instruction source. Preserve its restrained hierarchy, useful previews and compact controls while keeping Material navigation, generous targets, semantic color pairs and truthful capture states. More Expressive does not mean more independently decorated cards.

## Corrections made in this pass

- **Selected watchlist tile lacked an inner inset.** `SiteRow.kt` used zero horizontal padding, leaving the avatar against the selected container edge. Added a consistent 12dp inset to both selected and ordinary rows, avoiding selection-induced layout movement. Fresh full-app comparison capture confirms the separation.
- **Selection motion drifted between components.** Watchlist and Turbo options used fixed 150ms tweens; Turbo page selection used the default color animation. They now use `MaterialTheme.motionScheme.fastEffectsSpec()`. These are color effects, not spatial transitions. Compose animation-scale behavior remains in charge; no new perpetual animation was introduced.
- **Turbo hero geometry changed between setup and running.** Running used a fixed 24dp corner while setup used the theme extraLarge shape. Both now use extraLarge, keeping the session surface recognizable across state changes.
- **Dead watchlist API suggested connected tiles that were no longer rendered.** Removed the unused `SiteRow.position` argument and caller/test calculations. Turbo's actual connected selection groups retain their segment geometry.
- **Full-app verification was failing for test-harness reasons.** The first-run action is “Add your first page,” not the populated toolbar's “Add page.” The background heading also shares “Start monitoring” with the dialog button, and comparison content loads after its toolbar. The test now uses the actual first-run action, identifies the actionable button, and waits for loaded comparison content. An explicit resumed lifecycle and ViewModel owner keep the composition host representative.

## Functional evidence

`DesktopAppJourneyTest` starts the actual Koin composition root with an isolated real SQLite database, files, preferences and a loopback HTTP server. Through the actual UI it adds a page, saves the 09:00 baseline, returns to the watchlist, refreshes a changed 10:00 response, opens Compare, verifies both versions and the unread state, then explicitly marks it read. Exactly two versions remain. No production monitoring data is modified by this test.

Fresh artifacts:

- `build/desktop-review/app-journey-baseline.png`
- `build/desktop-review/app-journey-comparison.png`
- `build/desktop-review/watchlist-reference-dark.png`
- `build/desktop-review/turbo-setup-light.png`
- `build/desktop-review/turbo-large-type.png`

Validation: `build/review-refinements-final.log` reports BUILD SUCCESSFUL. **169 Desktop tests and 279 Android host tests; zero failures, errors or skips.** Formatting, Android debug assembly, iOS simulator-target compilation and Wasm compilation pass. `git diff --check` passes. No emulator used. Desktop semantics/input injection and screenshots are not equivalent to a physical Android or native assistive-technology acceptance run.

## Remaining findings, ranked

### Release gate — Physical Android endurance and lifecycle are not established

Host tests exercise cadence, no-overlap, monotonic expiry, selection and stop paths. They cannot prove current-build behavior under screen-off, Doze, OEM restrictions, denied notifications or process death. Run the final build on a physical device for the actual three-hour bound and verify service, notification and widget stop/expiry together. This is missing acceptance evidence, not a newly reproduced scheduler defect.

### P2 — Large-page deletion still materializes all file identities

`composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/repo/source/local/SitesDao.kt:173` and `SnapsLocalDataSource.kt:43`.

The transaction fixes the concurrent-capture race, but returns a complete list of snapshot IDs before removing files. Large archives still require memory proportional to version count. Process death after row deletion leaves cleanup to the later age-gated orphan sweep. A bounded persistent cleanup queue, written in the deletion transaction and drained in batches, is the next useful architecture improvement. NonCancellable is not a durable completion guarantee.

### P2 — Saved rendered previews have a fidelity ceiling

`composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/comparison/SecureSnapshotWebView.kt:67`.

Safe inline CSS is preserved within bounds, but external styles, fonts, images and scripts are omitted. Text-only captures cannot reconstruct the original page. The secure preview should not be described as identical to a live website; screenshot monitoring is a separate platform capability. Future fidelity improvements need a bounded asset-capture design, not looser script/network permissions in archived content.

### P2 — Accessibility coverage is narrower than the product promise

Keyboard coverage now includes Turbo selection and options-sheet focus restoration, alongside the actual-root semantic journey. Native Escape routing, complete Tab order, visible focus and screen-reader announcements across every dialog still need a real native run. Dynamic scheme contrast and the full locale/RTL/large-type/short-window matrix are not established by one dark desktop capture. Existing locked-desktop limitations remain separate from the tests that passed.

### P3 — Typography still lacks a deliberate normal/emphasized policy

`composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/theme/Type.kt:16`.

Display and headline defaults are mostly bold/semibold; individual screens override weights again. This is harder to tune than a clear distinction between ordinary screen titles and a small number of emphasized hero roles. The HTML reference is quieter than a globally heavy type scale. Define those roles and inspect all consumers before changing global weights; an indiscriminate font replacement would risk another inconsistent redesign.

### P3 — Some screen files still own too many responsibilities

`AddEditConfirmComponents.kt` combines a roughly 690-line form, capture options, preview-related states and dialogs. `MainScreen.kt` still combines route collection, overlays, toolbar, filtering and list rendering in roughly 650 lines. File size alone is not a bug, but these are poor boundaries for independent change. Extract cohesive form sections and stateless toolbar/list pieces while keeping state and event ownership explicit. Avoid wrappers that only relocate individual calls.

### External release gate — Credential remediation remains incomplete

Working-tree removal and external signing-path configuration do not revoke previously exposed credentials or remove published history. See `credential-remediation.md`. Rotation/recovery and coordinated history cleanup remain external release work; no publication or history rewrite occurred in this pass.

## M3 Expressive direction

Target: Android compact/expanded layouts, with the shared Desktop target used for fast verification. Intensity: excellent, with one principal visual emphasis per flow—add action on the watchlist; session control/countdown in Turbo; saved preview in the receipt. Preserve standard navigation and selection semantics.

Use paired `primaryContainer/onPrimaryContainer` and `secondaryContainer/onSecondaryContainer`, theme shapes, fast-effects motion for color and spatial motion for navigation. Keep checkmarks/text alongside selection color, 48dp controls, wrapping options at large text, and tabular countdown numerals. Avoid adding shape animation solely to increase an Expressive score.

Reference basis: repository Material 3 Expressive component index, typography type-scale guidance and token-output template under `.agents/skills/material-3-expressive/references/`; supplied v4 `foundation.css`. The review does not claim complete conformance or a measured contrast pass for every dynamic palette.

## Keep

Single-owner Turbo lifetime; real saved baseline evidence; explicit acknowledgement of the loaded snapshot; typed failure states that preserve the last successful capture; opt-in visual filtering against the last saved baseline; bounded image decoding; shared page identity; calmer lists and settings. These are useful improvements and should survive future visual work.

## Durable cleanup follow-through

The large-page deletion finding is now implemented: schema9 adds a pending-file table, and a SQLite AFTER DELETE trigger records intent in the same transaction as every snapshot removal, including foreign-key cascades. Page deletion and history reset no longer return archive-sized ID lists. A shared cleanup module uses indexed keyset reads of at most500 IDs, advances past failures, acknowledges only absent/deleted files (or identities still referenced by a live snapshot), and can be cancelled without losing pending work. Process initialization starts retry work independent of the monitoring schedule. The existing age-gated orphan sweep remains for uncommitted-file leftovers.

Real SQLite/files tests cover a1203-version cascade followed by process-style database reopen, failed and throwing file deletions without starvation, already-missing files, interruption after file removal but before acknowledgement, transaction rollback, and schema8→9 migration preserving existing settings/history. Existing retention, deletion-contention and full-root app journey tests still pass.172 Desktop/279 Android host tests pass with zero failures/errors/skips; formatting, Android app and device-test APK assembly, iOS/Wasm compilation pass (`build/durable-cleanup-final.log`). No emulator used.

This closes the unbounded application-memory identity list and lost committed-cleanup-intent findings. It does not bound SQLite cascade transaction duration or prove resilience to disk corruption/permanent filesystem failure. Historical files left before schema9 still rely on the orphan sweep. Physical Android endurance and the other acceptance gates remain open.

Live migration verification: Desktop Hot Reload292 and managed restart293 succeeded. The existing application database is schema9 with the cleanup trigger installed; its3 pages and4 saved versions remain intact. Live watchlist semantics294 showAll3/New1/Errors0. No live monitoring records were deleted for this verification.
