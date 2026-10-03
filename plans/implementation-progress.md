# Product reliability and refinement implementation

Active goal: implement the full review in `2026-09-07-full-product-review.md`, aiming for excellent architecture, reliability, usability and M3 Expressive craft. Preserve the pre-existing dirty tree. No commits, pushes or releases requested.

Beads CLI is available but this repository has no initialized database. Use this checklist as durable progress; do not block implementation on tracker setup.

## Milestones

- [x] Foreground manifest crash; real device launch and worker verification (see Real Android journey verified).
- [ ] History integrity: preserving upsert, unknown legacy hashes, atomic fresh start, read-state races, chronology/deep links.
- [ ] Normal scheduling: one settings model, 15min–7d, first-monitor setup, truthful health, pause/mute/open page.
- [ ] Turbo: awaited session runner, strict 3h deadline, cadence, structured cancellation, single state owner, recovery, widgets and timeout.
- [ ] Resources/detection: bounded hashing, empty responses, retention/storage budgets, meaningful text/visual monitoring.
- [ ] UX/M3: simplify setup, Turbo outcomes, history navigation, adaptive/accessibility layout and visual walkthrough.
- [ ] Delivery: reproducible CI, required native gates, credential containment and remaining external credential rotation.
- [ ] Final tests: host, Room/device/lifecycle, synthetic monitoring journeys, dark/light/large type, build/lint.

## Baseline

114 host tests pass; device tests compile; debug build passes using local sibling Webwright. Installed app reproduced missing SystemForegroundService type crash. In-memory SQLite reproduced edit and legacy-prune history loss. Do not claim a new 9/10 score without verification.

## Implementation checkpoint — Turbo redesign

- Room preservation, legacy hashes, atomic fresh start and concurrent read-state fixes passed six tests on the Samsung device. Host suite expanded to 132 passing tests, including interval and Turbo kernel behavior.
- Normal policy is enabled by default, shares 15min–7d validation and a 60-minute default. Editor drafts minutes/hours/days with one Save. Remaining: truthful global health, persistence/reconciliation error paths and setup UX.
- Turbo now directly awaits a shared Android check runner with bounded concurrency and per-site exclusion. Session owns selection/cadence/deadline; service owns foreground lifetime and a bounded wake lock. UI reads runtime state. Stop/timeout cancels children. Widget extension removed and service state updates refresh widgets.
- New APK installed on Samsung; process remains alive without the old manifest crash. Phone UI automation was occupied; isolated emulator-5558 started read-only from medium_phone AVD for UI work.
- Removed full HTML cache; stored hashes avoid parsing previous documents. Empty response marks failure; same-size replacements satisfy positive byte thresholds. Added edited-page stale capture check.
- History pair deep-link index and chronological labels corrected; older pairs now lazy in a bounded dialog. Logs append errors have Retry. Home page options have pause/resume, alerts and Open page; comparison Open page wired. Settings puts monitoring first.
- User specifically requested another Turbo redesign. Replaced TurboScreen/TurboHero/TurboSiteRow with a calmer tonal session surface, fixed bottom action, native connected cadence controls and checkboxes, real outcome rows, and explicit Edit selection. Three emulator UI tests pass; screenshot review underway. See turbo-redesign.md.
- Remaining major work: retention/storage, content modes per page, reproducible browser dependency/CI, broad architecture cleanup, full device service/notification/cadence tests, global schedule health, final visual and lint checks. Goal remains active.

### Real Android journey verified

On isolated emulator-5558 (Android 16), installed the actual app APK and added a local HTTP test page via Share. The initial screenshot WorkManager job completed successfully with the corrected foreground-service manifest. Turbo then fetched at 00:41:10, 00:41:40, 00:42:10, 00:42:40, 00:43:10 and subsequent 30-second boundaries while the app was backgrounded. Edited the local HTML after the second check: one changed snapshot was saved at 00:42:15 and a change notification appeared. Later identical checks saved no additional snapshots. Used the notification's Stop monitoring action; the foreground service disappeared and HTTP requests stopped (last request 00:44:40; no subsequent ticks across multiple intervals). Evidence: ignored `build/monitor-probe-server.log`, runtime logcat, and UI hierarchy.

Clean Turbo screenshots reviewed after cold-booting the emulator to remove a System UI ANR inherited from its snapshot. Removed the extra native title bar from the Compose test host. Three UI tests passed again. Captures: `build/turbo-review/turbo-setup-light.png`, `turbo-running-dark.png`, `turbo-large-type.png`. Native controls wrap at 1.8× font scale; fixed action remains visible.

Additional changes underway: global pause/initial-baseline health in Home; Resume action; targeted option updates; transactional form-save preservation of latest check metadata. Re-run Room tests for the latter before calling that milestone complete.

The real journey exposed one more health bug: WorkManager's own class-name tag from a perpetually ENQUEUED periodic request was treated as a loading page, leaving Home on “Listening for changes” forever. Restricted worker status tags to explicit reload page IDs and count only RUNNING work; actual capture progress remains supplied by SyncProgressTracker. Verify on the next APK.

### Standalone capture and storage checkpoint

- Replaced the unpublished Webwright runtime dependency with an app-owned, cancellable WebView capture adapter. Fresh Android builds no longer need the sibling checkout; standalone host tests and debug assembly passed. Capture is capped at four million output pixels and tiles long documents. Device regression exposed a missing scroll translation in Canvas drawing; corrected it and all three capture tests passed, including offscreen bottom content, HTTP 503 rejection, and identical static-page image bytes.
- Host suite now has 138 passing tests. Nine Room tests pass, including retention cleanup and concurrent form/status preservation. Snapshot retention preserves the latest two versions per page, limits history to 100 versions per page and applies a 250 MiB total target, with an explicit protected-pair exception. Android orphan cleanup runs periodically.
- CI no longer silently skips native Android verification when an unpublished dependency is unavailable. Signing/publishing reads external credential paths; owner-side credential rotation is still required, as documented separately.
- Latest Turbo redesign is verified in light, dark, and 1.8× text. Three UI tests pass. A final combined device suite, formatting check, lint, and debug APK build is running after formatting. The physical phone is disconnected; do not claim the new UI is installed there.
- Broad goal is still active: per-page capture options, low-storage behavior, lifecycle stress, remaining comparison/accessibility work, and cross-platform validation are not all completed.

Final redesign verification: all 20 Android device tests pass after updating stale comparison-label and combined-title assertions; all 138 host tests pass. Formatting checks pass. Debug APK assembles; Android app lint reports zero errors and 48 dependency-version warnings. The final full device run includes the three Turbo UI tests, three native capture tests and nine Room tests. Latest APK: `app/build/outputs/apk/debug/app-debug.apk`.

### Snapshot commit races — verified 2026-09-08

Previous goal turn classified as progress: Turbo redesign, standalone capture and full device verification changed code and supplied executable evidence. This pass reproduced three additional failures before fixing them: URL edits during file writes, cancelled partial-file cleanup, and notification preference changes during persistence (`build/snapshot-races-red.log`).

The snapshot commit now revalidates current URL, monitoring eligibility, threshold, and baseline identity inside the same Room write transaction. A short non-cancellable commit sets its durable outcome before cancellation can resume; Stop before the commit cleans up partial files, while Stop during commit never deletes committed history. Notifications use the latest transaction-read preferences and run after persistence. Unchanged checks also validate baseline identity so a concurrent fresh start stays waiting for a new baseline.

Evidence: 144 host tests pass, including six new race/cancellation regressions. Ten real SQLite/Room device tests pass; the new test forces failure after both SQL writes and proves row/status rollback plus file cleanup. Debug assembly is being re-run after the final guard. Remaining goal scope is unchanged; low-space resilience, page-specific capture options, lifecycle stress and broad cross-platform/UI checks remain open.

Final snapshot-integrity debug assembly passed (`build/snapshot-integrity-build.log`); `git diff --check` also passes. The new APK is built but this pass did not reinstall it on the physical phone.

### Android storage resilience — 2026-09-08

Previous goal turn classified as progress: six snapshot-race regressions were fixed and verified. This pass reproduced low-space overwrite and abandoned temporary-file detection failures on the emulator (`build/storage-resilience-red.log`), then fixed them.

Android snapshot writes now check for the incoming bytes plus a 32 MiB free-space reserve, write and flush a sibling `.partial` file, and publish via same-directory rename. Failed writes remove the temporary file; crash-abandoned partial files become eligible for the existing aged-orphan sweep. Existing committed content stays readable until replacement publication. This is verified for normal writes, low-space refusal and simulated abandoned partial files; it is not a physical power-loss endurance test.

Low-space errors now say “Not enough storage,” explain the next action, avoid automatic immediate retry, and mark the check failed while preserving existing history. Other file-write failures also retain their storage context. All 145 host tests pass, including the new storage-error/status regression. Three dedicated Android storage tests pass. Full 24-test device suite and debug APK assembly are running in `build/storage-device-verification.log`.

Remaining: evaluate orphan maintenance when no new snapshot can be saved, iOS file-write failure reporting, per-page capture options, further service/process-death tests, and the remaining review milestones. Goal stays active.

Final storage verification passed: all 24 Android device tests, formatting check, and debug APK assembly (`build/storage-device-verification.log`). No physical-phone installation or power-loss test was performed in this pass.

### Recovery without successful writes — 2026-09-08

Previous turn classified as progress: Android atomic writes, low-space reporting and tests were implemented. New Room regression proves the next gap: orphan cleanup cannot depend on successfully saving a snapshot. It failed before the fix and passes after adding a throttled orphan sweep before check-result processing, including unchanged and failed checks. The 24-hour grace period and referenced-file exclusion remain intact. All eleven Room tests pass (`build/orphan-recovery-green.log`).

The first test install hit the emulator's storage limit. Disposable-cache trimming was insufficient; temporarily removed only the app executable with `pm uninstall -k` (retaining application data), then the same test ran and reproduced the intended bug. Restore the APK after final build verification; do not claim the emulator app is currently installed until that succeeds.

Also corrected iOS storage to reject failed atomic writes and unreadable saved files instead of reporting success/empty content. Android host/debug and iOS simulator compilation are currently running in exec session 39856, log `build/orphan-ios-verification.log`. Do not start another Gradle process until that handle is terminal.

Recovery pass completed: 145 host tests pass; all 11 Room tests pass; Android debug assembly succeeded and APK reinstall on emulator-5558 succeeded, with retained `changes.db` verified. Initial iOS compilation exposed a missing multiplatform IO dispatcher import and an Int/Long schedule mismatch; corrected both. `:composeApp:compileKotlinIosSimulatorArm64` now passes (`build/ios-compatibility-retest.log`). This is compilation evidence, not an iOS runtime/background monitoring test. All Gradle handles from this pass are terminal. Goal remains active.

### Live Turbo cadence — 2026-09-08

Previous turn classified as progress: orphan recovery and iOS compatibility were fixed and tested. This pass reproduced an interaction defect: changing an active two-minute session to 15 seconds kept the original pending delay (`build/turbo-cadence-red.log`). Turbo's wait now observes cadence changes and recalculates the next tick without restarting capture or extending the parent deadline. Slow checks still skip missed ticks and never burst.

Four new host regressions cover shorter and longer cadence, cadence changes during capture, and retry after a page-local timeout. All 149 host tests pass; Android debug assembly and iOS simulator compilation pass (`build/turbo-live-cadence-verification.log`). Existing virtual-clock tests also continue proving selection edits preserve the three-hour deadline and Stop cancels active checks.

Remaining lifecycle evidence still needed: explicit expiry with an in-flight check, immediate Stop/restart generation isolation, and current-APK process-death/screen-off/offline/permission journeys. Per-page text/visual configuration and preview, remaining comparison/health UX, cross-platform runtime checks and final review audit remain open. Goal remains active.

### Turbo boundaries and lock lifetime — 2026-09-08

Previous goal turn classified as progress: live cadence changes were fixed and verified. Two additional virtual-time tests now prove expiry cancels an in-flight check and a late completion from a stopped session cannot change its replacement.

Replaced the engine's permanent page-lock map and Android runner's hash-striped locks with a shared `PageCheckLocks` module. Entries exist only while owned or awaited; cancellation releases references; equal-hash page IDs remain independent. Five regressions cover 10,000 completed keys without retention, cancelled waiters, failed/cancelled owners, hash collisions, and 1,000 operations on real parallel dispatchers without same-page overlap.

Cross-platform verification found that the storage IO dispatcher was not available on WebAssembly. Isolated that choice behind `fileIoDispatcher`: Android/iOS use IO and web uses Default. All 156 host tests, Android assembly, iOS simulator compilation and WebAssembly compilation pass (`build/lock-platform-final.log`). This is compilation evidence for web/iOS, not runtime monitoring evidence.

Full Android device suite is currently running in exec session 37870, log `build/lock-device-final.log`. Because the emulator's small data partition cannot stage app and test executables together reliably, its app executable was temporarily removed with `pm uninstall -k`, keeping user data. Restore `app/build/outputs/apk/debug/app-debug.apk` after this test run. Goal remains active; per-page capture/preview, operational health/lifecycle journeys, broader UX/accessibility and final audit remain unfinished.

Final lock/device verification passed: all 25 Android device tests (`build/lock-device-final.log`). Emulator APK restoration succeeded after tests; retained application data was not cleared. All build/test sessions from this pass are terminal.

### Per-page capture foundation — 2026-09-08

Previous turn classified as progress: Turbo boundary evidence, shared lock lifetime and platform compilation were completed. This pass begins the larger per-monitor capture choice requirement rather than leaving the global setting as the only control.

Added typed CaptureMode (DEFAULT, HTML, SCREENSHOT) to Site and schema 4. Shared MIGRATION_3_4 preserves all history and defaults existing pages to their current global behavior; Android still chains MIGRATION_1_3 for older installs. AndroidCheckRunner resolves each page's override, and SyncWorker promotes foreground execution when a single page or any page in a batch requires screenshots. Capture-mode edits invalidate stale in-flight results at commit.

Evidence: 159 host tests pass; 12 Room device tests pass, including a real schema-3 upgrade that preserves an existing page/snapshot and persists a new override. Android debug assembly plus iOS/WebAssembly compilation pass (`build/capture-foundation-final.log`). New APK installed and launched on emulator-5558; application data retained.

This feature is NOT yet exposed in the editor and is not complete. Next implementation priority: editor state/control, platform capability honesty, preview, and baseline handling when switching text/screenshot without deleting history or generating a false alert. Comparison already contains a mixed-method explanatory state (`comparison_capture_changed_*` resources); inspect and use that rather than assuming mixed pairs crash. SyncEngine currently treats any subsequent format as a change: separate baseline status from the previous snapshot ID used for transaction race validation. Then add form/engine/device tests and render the new UI. Broad goal remains active.

### Capture editor and quiet method switches — 2026-09-08

Previous turn classified as progress: schema 4, migration, and Android per-page routing were implemented. Added two regressions that reproduced false alerts/missing baselines when switching text/screenshot representations (`build/method-baseline-red.log`). A new representation now saves a baseline with hadChanges=false, preserves old snapshots, sends no notification, and preserves any existing unread status. Previous snapshot ID remains separately validated inside the commit to protect against racing history edits.

Add/edit now loads, drafts and persists per-page capture choice. Android offers App default, Page HTML, and Screenshot with plain descriptions and a baseline/history explanation. Native radio dialog changes the draft; Save commits it. iOS/web show Page HTML only through explicit platform capabilities rather than offering an unimplemented screenshot path. Existing Android monitors retain App default until edited. Four locale resource sets updated.

All 162 host tests pass, including persistence/reopen of the draft choice. All 27 Android device tests pass, including the real editor interaction and Save reachability. Android debug assembly plus iOS/WebAssembly compilation pass (`build/capture-editor-platform-final.log`). Screenshot visually reviewed: `build/capture-editor.png` (light editor, full form, fixed Save). APK restoration to emulator-5558 succeeded after testing; no user data cleared.

Next priority is still the live capture preview and meaningful-change choice/defaults, not more foundational detours. Current form has no live sample of the selected method. Review the existing AddEditFlow/browser preview structure before extending it. Consider adding a disclosure arrow to the capture selector and updating the edit subtitle to mention capture settings; large text/dark/landscape editor render checks remain. Broad operational health, process-death/offline/permission journeys, and final requirement audit are still open. Goal remains active.

### Live capture preview — 2026-09-08

Added a cancellable capture preview to Add/edit. It uses the selected draft URL and capture method without storing snapshots, scheduling checks, or sending notifications. URL/method changes invalidate outstanding results; dismissal releases preview state; timeout and failure remain retryable. Android uses the production screenshot browser factory; HTML previews use the existing fetch/parser with a bounded excerpt. Platform DI exposes supported capture behavior.

Verification carried forward from the preceding work: 167 host tests and 27 Android device tests passed, including the preview editor. Android assembly, iOS simulator and WASM compilation passed (`build/capture-preview-final.log`, `build/capture-preview-device-final.log`). Four targeted Android capture tests subsequently passed using the production browser factory, including a short styled page (`build/short-page-capture.log`). The full device suite has not been rerun with that fourth capture test. Fixture HTML preview was visually reviewed (`build/capture-live-preview.png`). Manual screenshot preview investigation remains inconclusive: nested image scrolling and stale UI automation coordinates obscured the sample; do not call this verified. App executable was restored with data retained.

Latest user-directed focus: another Turbo visual/motion refinement. Broader goal remains active; this does not close the outstanding lifecycle, capture defaults, comparison/accessibility and final audit requirements.

### Resumed Turbo and explicit history cleanup — 2026-09-19

Previous goal turn classified as progress: Turbo layout implementation and a Start/Stop geometry regression were added; device execution revealed an external UiAutomation conflict. Current reinspection confirmed those source changes remain and old emulator handles are gone. An independent Resizable_Experimental emulator was started on port 5560; emulator-5554 belongs to another active automation and is not modified.

Found and fixed a concrete cleanup contract mismatch: the UI promised one retained version, while the implementation deduplicated hashes and kept ten. Explicit cleanup now atomically retains the latest chronological pair, returns deleted IDs for row-first file cleanup, survives navigation cancellation, and never deduplicates protected repeated content. Confirmation in all four locales describes two versions for comparison. A Room regression covers repeated hashes, page isolation, and idempotence. Automatic 100-version/250MiB retention remains separate from user-requested cleanup.

Verification in progress: online Gradle build/test session 54952, log build/turbo-history-online.log. The local cache was incomplete after the environment changed, so offline checks could not resolve Room/ktlint. Online resolution now downloads artifacts successfully with bounded HTTP timeouts. Do not report this pass as verified until the build and current device APK tests complete.

September 19 verification completed: all 167 host tests passed; debug APK and current device test APK assembled; formatting and git diff --check passed. All 17 targeted tests passed, followed by the complete 30-test Android device suite (`build/sept19-full-device.log`). This includes the new cleanup transaction test, stable Turbo cadence bounds across Start/Stop, light/dark/large-text interactions, and all four native capture tests. Screenshots visually reviewed in `build/turbo-review-sept19/`.

The inherited Resizable_Experimental disk also lacked installation space. Its temporary emulator was stopped without modifying its base data. Created a fresh, dedicated `CodexChangeDetectionReview` AVD at `build/turbo-qa.avd`, registered by `~/.android/avd/CodexChangeDetectionReview.ini`, running as emulator-5560 (exec 45495). This has an 8G data partition. The current debug APK is installed there for subsequent runtime checks. Emulator-5554 and its automation were untouched. All build/test sessions are terminal; only the dedicated emulator remains running.

Goal remains active. Next substantive work: verify actual capture preview visually on the fresh emulator, finish meaningful-change configuration/defaults, and exercise process-death/offline/permission and normal-schedule behavior. Final requirement audit, broader comparison/accessibility and cross-platform runtime evidence remain incomplete. Do not infer 9/10 or full reliability from the green unit/device suites.

### Screenshot preview and final-tile capture defect — 2026-09-19

Previous turn classified as progress: Turbo refinement and explicit history cleanup were verified by 167 host/30 device tests. Reinspected current preview implementation and replaced its nested scrolling image inside the form with a fitted thumbnail plus an explicit full-page dialog. The dialog keeps Done visible while the screenshot scrolls; closing returns to the unsaved editor. Added localized action text in all four locales and a UI regression using bytes from the real production screenshot browser factory. Shared the local HTTP fixture server between capture and editor device tests.

Visual verification exposed a real engine defect: the settled bottom view omitted the page footer, and exporting the raw captured PNG proved it was capture loss rather than display clipping. A new native regression with text, margins, 1800px content and a red footer failed while the older 4000px plain-block test passed. Temporary geometry instrumentation showed View.scrollTo retained requested offsets past the document's final valid viewport. The capture loop now explicitly clamps viewportTop to pageHeight minus viewport height before calculating its tile offset. Removed temporary production debug logs. The new regression passes; the real preview now visibly contains “End of captured page” at its scroll limit (`build/capture-expanded-bottom.png`). This protects monitoring content, not just appearance.

Validation: Android debug assembly passes (`build/partial-tile-fix-build.log`); full device suite passes all 32 tests (`build/preview-capture-full-device.log`). Shared preview UI and preceding cleanup DAO compile for iOS simulator and WebAssembly (`build/preview-expansion-final.log`). The final engine fix is Android-only. Both editor tests pass, proving expansion, scrolling to the real limit, return to the draft, and no Save callback. Previous 167 host tests remain the latest host run; no host logic changed in this pass. Current APK installed/launched on dedicated emulator-5560. All build/test processes terminal; only emulator remains live. Other emulator untouched.

Next priority: actual monitoring operational journeys (process death, screen off/Doze, offline recovery, notification denial, and normal cadence), then meaningful-change configuration/defaults and final requirement-by-requirement audit. Broader goal remains active and unproven; do not assign a 9/10 rating from these checks alone.

### Turbo operational journeys and restart recovery — 2026-09-19

Previous goal turn classified as progress: full-page preview and the missing-footer capture defect were fixed and verified. On dedicated emulator-5560, created a real HTML monitor through the editor for http://127.0.0.1:8766/ (site 647c6dd4-33d6-4cd5-ab23-be9d2aada64d). Fixture server runs from build/lifecycle-fixture in exec 68239; adb reverse tcp:8766 is active. HTTP log: build/lifecycle-http.log. This is local synthetic data, not a user's website.

Runtime evidence: denied notification permission; started Turbo at 15s; left app and turned screen off. Requests arrived at 01:34:53, 01:35:07, 01:35:22, 01:35:37 while dumpsys reported Asleep and an active bounded Turbo wake lock. Removing only the fixture's adb reverse caused connection-refused failures at 01:35:52 and 01:36:07. Restoring the reverse recovered requests at 01:36:22 and 01:36:37. This proves connection-failure recovery and screen-off execution, NOT forced Doze or general internet-offline behavior. Saved pre-kill app log: build/lifecycle-process-before-kill.log.

Killed the app process with run-as kill -9. Service was absent afterward, wake lock was released, and no extra HTTP requests occurred. Reopening correctly reported Interrupted, but lost the prior cadence/configuration and exposed repeat permission prompting. Added TurboSession.restore for inactive configuration/outcome recovery, guarded by generation so late preference IO cannot overwrite a user action. WorkerHelper restores saved IDs/times/cadence/end reason; active persisted sessions become Interrupted, never resume automatically. TurboScreen reacts to late configuration restoration. Automatic notification permission requests now persist a one-time flag and respect an existing denial rationale. The watchlist's Turbo badge now also requires isActive, rather than treating retained selection as live monitoring.

Repeated on updated APK: Turbo started without another permission dialog despite permission remaining denied. Changed fixture A to B; captured one changed version, skipped the unavailable notification without failing the check. Killed/reopened again: UI retained 15s and selected page, showed Interrupted, and service remained absent (`build/lifecycle-restored-turbo.png`). Copied synthetic SQLite DB+WAL to build/lifecycle-database: exactly 2 snaps (baseline and change), site isSuccessful=1, isRead=0, captureMode=HTML. Final watchlist screenshot confirms no stale Turbo badge (`build/lifecycle-final-watchlist.png`). Turbo is currently stopped; fixture remains available for normal-schedule testing.

Validation: all 170 host tests pass (three new restoration regressions), full 33 Android device tests pass (`build/lifecycle-full-device.log`, including late-restoration UI test). Android APK plus iOS simulator and WASM compiles pass (`build/lifecycle-final-build.log`); formatting and diff checks pass. Current APK installed and launched on emulator-5560. All build/test commands terminal; dedicated emulator and fixture server remain live. Emulator-5554 untouched.

Next priority: normal 15min–7d scheduled execution and scheduling health, forced Doze, notification deep-link pair correctness, then meaningful-change defaults/configuration and remaining comparison/accessibility/final audit. Goal remains active; these bounded journeys do not establish universal device/background reliability.

## Turbo follow-up redesign — 2026-09-19

User explicitly requested another visual and motion redesign. Separated the timer from frequency controls, removed the large saturated background and repeated duration copy, made selected pages visibly tonal, and scoped restrained Material motion to Turbo. Preserved actual deadline countdown, cadence choices, session actions and page eligibility. Added English, Portuguese, Spanish and Italian labels. Android initial build and five Turbo device tests passed; visually inspected setup, running dark mode and 1.8x text. Final motion-scheme build and reduced-motion verification recorded in `build/turbo-refinement-*` logs.

Final verification: Android app/test APKs, iOS simulator and WASM compilation passed. All five Turbo UI tests passed with normal motion and again with animator duration scale zero. Restored the emulator's previous animation setting. Installed the current production APK on dedicated emulator-5560 and visually verified navigation/action spacing; screenshot `build/turbo-refinement/app.png`. No scheduling or session engine behavior changed in this pass. Broader product goal remains unfinished.

## Confirmed normal scheduling and reachable Save — 2026-09-19

Previous goal turn classified as progress: implemented and verified the requested second Turbo redesign.

Extracted AndroidMonitoringScheduler from WorkerHelper. Normal schedule reconciliation now serializes operations, reads the latest preferences after acquiring the lock, and awaits WorkManager enqueue/cancel completion. Settings save now observes Android operation failure instead of returning optimistic success. Removed obsolete WorkConstraints translation and idle/backoff workaround from the active path; current MonitoringSchedule directly defines supported constraints. The existing unique work name and UPDATE policy preserve job identity when editing a live schedule.

Four new host tests exercise cancellation completion, cancellation failure, queued serialization, and a queued caller reading changed preferences before enqueueing (including 15-minute cadence and all supported constraints). All 174 host tests pass. Android app builds and was installed on our dedicated emulator-5560.

Actual device verification using the app UI plus WorkManager SQLite and JobScheduler state:
- Before: unique periodic work `600e7cd9-3779-45e0-a0b8-eddf0b6dcb9e`, interval3600000ms.
- Save15min: same work ID, interval900000ms; overdue check ran against local HTML fixture at01:58:28; period_count2; next JobScheduler eligibility approximately14m59s.
- Save7days: same work ID and prior enqueue time, interval604800000ms.
- Pause: work state5(CANCELLED), no app JobScheduler job remained.
- Resume15min: one new work `63855040-d3e7-4849-9f81-a8db391c67de`, interval900000ms, successful HTTP request01:59:52 and period_count1. Leave enabled to observe natural subsequent execution; expected earliest around02:14:52, subject to Android constraints. Fixture server/reverse port8766 still available, Turbo inactive.
- Evidence: build/scheduler-db-{before,15min,7days,paused,resumed}, build/scheduler-*-jobs.txt and build/lifecycle-http.log.

Device interaction also revealed that the partially expanded schedule sheet hid Save below the long form. Changed the sheet to expanded-only presentation and separated the scrollable form from a pinned Save action. Visually verified the installed screen at build/scheduler-expanded.png. Current production APK includes both scheduler and sheet changes. This is not a long-duration or forced-Doze guarantee; those checks remain open along with the broader review requirements.

Shared compilation verification after the sheet change: iOS simulator and WASM compile tasks passed (`build/scheduler-shared-build.log`). No builds remain running.

## Monitoring health and failed-refresh recovery — 2026-09-19

Previous goal turn classified as progress: serialized and awaited normal scheduling plus fixed schedule-editor Save reachability.

Rechecked current review against code. Found unread saved changes were still filtered out whenever the latest check failed (`hasDetectedChange` incorrectly depended on isSuccessful). Removed that dependency; a saved unread snapshot pair remains a change independent of the latest attempt or pause state. SiteRow now exposes a separate localized “Saved change needs review” label and accessibility description when its current status is error/checking/paused. Added host regression for failed/paused/read states and device regression for combined error + saved change.

Added actual periodic job metadata to SyncJobInfo (periodic identity flag and nullable next eligible timestamp, filtered against WorkManager's Long.MAX sentinel). Watchlist now shows requested cadence and scheduler eligibility separately, with explicit uncertain timing, waiting, active-check and unconfirmed states. Cadence uses existing minute/hour/day plurals. New explanatory text is localized in English/Portuguese/Spanish/Italian. Added device test verifying eligible-now wording does not promise an exact check. No attempt to infer a specific current constraint failure from a historical stop reason.

Live connection-failure verification exposed another defect: optimistic manual refresh stayed active for the entire WorkManager ENQUEUED retry/backoff period, falsely showing Checking and blocking another tap. Changed this to a bounded request-acknowledgement handoff: clear tap feedback once a matching Android request is observed (including queued work), or at five seconds if it never appears. Actual execution remains driven by RUNNING work and SyncProgressTracker. Three virtual-time tests cover queued retry, unrelated request IDs and missing acknowledgement.

Validation: all178hosttests passed; 3 QuickMonitorUiTest device tests passed; Android app and iOS/WASM compilation passed. Production APK installed on dedicated emulator5560. Removed local fixture reverse port8766 to induce actual ECONNREFUSED, tapped Refresh, and observed Error + New1 + saved-change label with Refresh enabled (build/monitoring-health-failed-fixed.png). Tapped New and verified the failed page remained (build/monitoring-health-new-filter.png). Restored reverse port8766 and refreshed successfully at02:07:09; Errors0, New1, same saved change (build/monitoring-health-recovered.png). Initial eligibility screenshot build/monitoring-health-watchlist.png. All build handles terminal.

Current dedicated emulator: app in New filter, normal15min schedule enabled, Turbo inactive, fixture reverse8766 restored/server running. Next periodic eligibility remains about02:14:52; last successful manual check02:07:09 must not be mistaken for the periodic cadence. Doze was not forced; device was charging, screen on, ACTIVE at inspection. Persisted last-success-per-page history and specific current delay reasons still need fuller support, as do remaining product review gates. No claim of completion/9outof10.

## Durable last-success health and idle validation — 2026-09-19 (in progress)

Previous goal turn made progress: unread changes survive failed checks, manual retry feedback no longer falsely indicates execution, and watchlist exposes periodic eligibility.

Added nullable Site.lastSuccessfulCheck and schema5 migration. Successful sync metadata updates it atomically; failed attempts preserve it. Stale edit forms preserve it; Start fresh resets observation metadata with the history transaction. Migration4→5 backfills only latest successful nonzero timestamps, leaving failed/unobserved legacy pages unknown. The row displays last success independently for changed/error/paused/checking states. Relative labels now age every minute while composed rather than remaining “just now” until another database event.

Added real Room tests for success→failure→stale edit→recovery→fresh reset and version4 migration preserving unknown timestamps/history. Updated the version3 migration fixture for the full3→4→5 chain. Device execution pending idle-test completion. Host178tests and Android/iOS/WASM compilation pass. App lint passes with0errors/52warnings (46dependency-version,5AGP-version,1UseKtx). Narrowed the blanket JSON ignore rule for Room schema definitions only; schema3/4/5 are now included in the working tree for migration review.

Clean-source validation: copied601 git-visible current source files into ignored build/clean-source-verification, excluding preexisting generated/build files and sibling checkout. With shared downloaded Gradle dependencies and ANDROID_HOME, :composeApp:testAndroidHostTest :app:assembleDebug --offline succeeded,67tasks executed,178hosttests passed. This proves a clean source build on this host, not a remote CI execution or release signing.

Idle test: own emulator5560 put Home, battery unplugged (simulated), screen asleep, forced deep idle at02:08:18. Device reports IDLE/OVERRIDE, JobSchedulerjob22 readyNotDozingfalse and missing connectivity; no fixture requests since successful manual02:07:09. Periodic WorkSpec63855040-d3e7-4849-9f81-a8db391c67de remains due02:14:51.887. Observation helper86972 waits until20s beyond due. Must restore `dumpsys deviceidle unforce`, `dumpsys battery reset`, and wake the screen before device tests/install. Current installed APK still schema4; built APK schema5 awaits migration verification.

Completed verification for this pass:
- At02:15:25, verified job22 was33s overdue but still not ready (readyNotDozingfalse); no HTTP request had occurred. Restored deviceidle unforce, battery reset and screen wake. Device returned ACTIVE/ACTIVE, chargingtrue, networkconnectedtrue. Automatic GET succeeded02:15:29 with no app launch/manual refresh. Same WorkSpec63855040-d3e7-4849-9f81-a8db391c67de now period_count2 and next15min eligibility. Evidence build/idle-overdue-jobs.txt, build/idle-recovered-jobs.txt, build/idle-recovered-db/workdb, build/lifecycle-http.log. This proves one real forced-Doze delay/recovery journey, not arbitrary OEM or long-term endurance guarantees.
- Full current Android device suite passed37tests in17.19s, including15Roomtests and the new migration/last-success cases (build/check-health-full-device.log).
- Installed schema5 production APK and opened the existing fixture database. PRAGMA user_version5, both snapshots preserved, isRead0, lastSuccessfulCheck backfilled exactly to prior successful timestamp1789794928878 (build/health-migrated-app-db).
- Actual subsequent disconnected check advanced timestamp to1789795009852 with isSuccessful0, preserved lastSuccessfulCheck1789794928878 and2snapshots/unread0 (build/health-failed-app-db). Screenshot build/last-success-after-failure.png shows Error, earlier last-success and saved-change label. Restored reverse8766 and manually refreshed afterward. UI screenshot before failure build/last-success-upgraded.png.
- All build/instrumentation/observation handles are terminal. Emulation/server remain running. Current emulator5560 is awake/charging with forced idle disabled; normal15min enabled, Turbo inactive, schema5 installed. Next periodic eligibility around02:30:29. SnapshotB fixture and its two-version history retained. Broader goal remains open.

## Watchlist hierarchy and action routing — 2026-09-19

Previous goal turn made progress: last-success persistence/schema5, migration tests, clean-source build, and actual forced-Doze delay/recovery.

Replaced the active watchlist's oversized count/ribbon presentation with an independently testable WatchlistSummary: headline, short explanation and relevant actions. Removed the data-like decorative ribbon from actual monitoring status (it remains only in empty-state artwork). Failed-page summaries now have a Review failed pages action wired to Errors; saved changes retain their own Review changes action. Both stay usable during new checks. Actions wrap naturally for large type; summary text is not line-clipped. Extracting this behavior removes roughly170lines from MainScreen and avoids presentation-only signal enums.

Added explicit SiteStatus.Waiting for pages without a baseline, instead of rendering “Checked” with an empty timestamp. Summary waiting counts now use the same missing-baseline evidence instead of assuming any timestamp means a baseline exists.

Verification: Android build and iOS/WASM compilation passed. Five QuickMonitorUiTest cases passed, including new-page waiting and independent error/change actions during checking. Installed current APK and visually reviewed compact light watchlist (build/watchlist-refined-light.png). Induced fixture connection failure, enabled dark mode and fontScale1.8, confirmed readable wrapped actions and full status (build/watchlist-refined-dark-large.png). Tapped Review failed pages and verified Errors selected with the failed page retained (build/watchlist-refined-errors-selected.png); then Review changes returned to New. Restored fontScale1.0/night modeno and reverse8766, then refreshed successfully. All build/test handles terminal. Own emulator5560 remains running, schema5, normal15min, Turboinactive, next periodic eligibility around02:30:29. Broader goal remains unfinished; detailed failure reasons, meaningful-change filters/defaults and remaining accessibility/lifecycle gates need further work.

## Turbo compact session redesign — 2026-09-19

User explicitly requested another Turbo visual/motion pass. Reworked TurboHero to a smaller left-aligned clock in a quiet session panel, small tonal bolt tile, stationary full-width remaining-time track and cadence controls directly on the page. Removed row-wide selection color animation and Start/Stop shape morph. Kept native checkbox/ripple and restrained connected-group feedback. Measured both captions with hidden semantics removed, preventing wrapped setup copy from shifting cadence when starting/stopping at large font sizes.

Verification: 178 host tests passed; final Android APK/device APK, iOS simulator and Wasm compilation succeeded. Six Turbo UI tests pass at normal animation scale and again with animator_duration_scale=0, including new 1.8x cadence-position regression. Reviewed light, dark/running and 1.8x screenshots. Restored emulator-5560 animator scale setting to its original absent/default value and installed the updated app there; emulator-5554 untouched. Logs: build/turbo-third-refinement-{build,final-build,final-device,no-motion}.log. Screenshots: build/turbo-third-refinement/. No new endurance claims; broader reliability goal remains active. Earlier pending notification/comparison work remains separate and needs its own runtime verification.

## Notification identity and comparison acknowledgement — 2026-09-19

Completed the pending notification/read-state work and added seven ComparisonReadStateTest cases. Android alerts use full site tags, with immutable PendingIntent identity including both site and capture. A same-hash Aa/BB device test proves independent alerts; replacing a site's alert produces a different tap token without stacking it. Test APK now declares POST_NOTIFICATIONS; a bounded delivery wait and enabled-permission assertion distinguish Android posting latency from harness setup failure.

Comparison acknowledgement now requires successfully loaded content and a SQL latest-snapshot identity guard; older/missing snapshots and failed loads do not clear newer unread state. Deterministic timestamp/snap-ID ordering aligns reads and acknowledgement. Missing requested history shows an explicit localized unavailable screen with a deliberate latest-version action. Pruning reloads by selected capture identity instead of leaving obsolete pair indices/content. Cancelling loads propagates cancellation; starting another page clears prior content, and read-state updates check page and latest capture identity. Notification routing observes both site and capture keys and comparison extras take precedence over shared-URL intake. Fixed the literal Previous change %1$d label found during device inspection.

Evidence: 185 host tests pass. Full 43-device-test suite passed before the final small routing/header/loading-state fixes; final targeted rerun recorded in build/comparison-routing-final-device.log. Final Android, iOS simulator and Wasm builds pass (build/comparison-routing-final-build.log). Device journey: synthetic fixture B→C generated an actual website-change notification; tapping opened the correct pair and SQL confirmed isRead=1. Fixture D then saved; explicitly opening C through the comparison intent showed B→C, with SQL isRead=0 and all four versions retained (build/older-capture-confirmed-db). Missing capture intent showed the explicit recovery screen. Final APK verified Previous change 1 and ACTION_VIEW+comparison extras opens comparison without the Add page sheet. Screenshots: build/change-alert.png, build/change-alert-open.png, build/older-capture-final.png, build/missing-alert.png.

Emulator-5560 fixture now has A/B/C/D, latest D ea3f16e5-c260-44c0-b880-487a56376a51, C f92669a6-a97a-4b6f-88d2-93410e6cfcbb, site647c6dd4-33d6-4cd5-ab23-be9d2aada64d. Root app notification permission restored to denied after real alert test. Fixture server/reverse8766 active with D content; normal15-minute schedule active. Test APK notification permission granted. No additional Turbo/endurance guarantee. Broader goal remains incomplete; comparison presentation still exposes raw HTML in text mode and needs the planned usability pass, alongside durable failure details and broader accessibility/device verification.

## Readable comparison and truthful preview — 2026-09-19

The prior real notification journey exposed raw HTML as the default Text comparison. Text now extracts readable HTML content with the existing Ksoup dependency, removes script/style/template/head and explicitly hidden content, decodes entities, preserves block boundaries and table cell spacing, and keeps plain-text input literal. Parsing/diffing stays on Dispatchers.Default. Source remains a separate mode backed by the original content, and unchanged readable text explicitly points to Rendered/Source for other differences. This is display processing only, not CSS visibility emulation or a change-detection tolerance policy.

Removed rendered-preview word-set similarity and its false no-differences shortcut: reordered/repeated words, attribute changes and other saved content can no longer be hidden by that approximation. The rendered before/after preview remains available. Text story panels use bodyLarge and show full content instead of four-line ellipses; the toolbar wraps modes and actions at large font sizes. Text export uses the same readable conversion; Source export preserves markup. Mode changes reset pending diff output instead of retaining the previous mode's rows during computation.

Verification: 189 host tests pass, including nested block/table boundaries, entity/hidden/script handling, literal plain text, ordered text diffing and readable-vs-source share output. Android APK, device APK, iOS simulator and Wasm compile successfully (build/readable-comparison-share-build.log). Android ComparisonUiTest checks 1.8x default readable text, Source accessibility and share visibility, plus the existing image-mode interaction. Added a rendered reordered/repeated-word regression; final result in build/readable-comparison-regression-device.log. Actual installed app screenshot build/readable-comparison.png shows only the B→C sentence changes; build/rendered-comparison.png shows the corresponding saved HTML preview. All changes remain in the dirty worktree; full goal remains active.

## Durable failure reasons and live recovery — 2026-09-19

Schema6 adds nullable Site.lastFailureCode with an explicit5→6 migration. Categories are stable names, never raw exceptions/responses; unknown legacy/future values remain unknown. HTTP404/410, access/login, rate limit, server, connection/timeout, storage-full/save, content-size/content and unknown cases map to localized titles/guidance in EN/PT/ES/IT. Ktor request timeouts now classify as timeouts. HTTP/capture-limit FetchResult failures retain typed exceptions so classification survives the fetch→commit boundary. Status commits persist failure codes, success clears them, stale edits preserve them, and fresh-start reset clears them atomically.

Watchlist and Turbo rows show the durable reason; comparison details show guidance. ComparisonViewModel observes database status while preserving its selected historical pair, so retries clear stale banners live. Schema3/4 migration fixtures updated for the current column set; a new5→6 test verifies history/unread/last-success preservation without inventing reasons. Room schema6 exported under composeApp/schemas.

Verification:193 host tests pass; Android debug/device APK and iOS/Wasm builds pass (build/check-failure-live-status-build.log).17 Room device tests pass, including migration5→6 and failure→stale edit→success state. Real root APK upgrade yielded schema6 with all4 fixture snapshots. Renaming only the synthetic local server directory produced HTTP404; UI showed Page not found and SQL held NOT_FOUND, previous lastSuccessfulCheck, isRead0 and4 snapshots (build/failure-404-db, build/failure-watchlist.png). Historical B→C comparison remained available with failure guidance. Restored fixture directory, retried from that screen; banner disappeared live while B→C stayed selected. SQL confirmed isSuccessful1, failureNULL, advanced last-success, isRead0 and4 snapshots (build/failure-recovered-db, build/failure-recovered.png). Final visual adjustment makes the failure notice a quiet semantic surface with an error-colored title, replacing the initial saturated full-width panel. Full final device results:build/check-failure-full-device.log.

Fixture server/reverse8766 restored; contentD, normal15-minute schedule active, Turbo inactive, rootnotification permissiondenied, defaultfont/lighttheme. Emulator5554 untouched. Broader goal remains active; full requirement audit, meaningful-change controls, large-page resource stress, broader accessibility and release/CI verification still need evidence.

Final verification for this pass:46 device tests pass. Final polished failure panel reviewed in build/failure-detail-final.png, fixture restored and retried successfully afterward. Final iOS/Wasm compile log:build/check-failure-polish-platforms.log. Android root APK installed with schema6 and live failure recovery.

## Long-page comparison performance — 2026-09-19

Audit found that the existing 2-million-cell LCS fallback collapsed everything between the first and last change into one replacement. Two distant edits on a40,000-line page therefore looked like tens of thousands of changes, and TextDiffComparison composed every changed/context line eagerly.

DiffUtils now aligns ordered unique-line anchors with a longest-increasing-subsequence pass and trims unchanged gap boundaries. Small gaps share one2-million-cell LCS work budget; the table is a flat IntArray instead of per-row arrays. Empty-side changes bypass LCS. Auxiliary anchor maps are bounded to200,000 combined input lines; unanchorable/oversized gaps retain a valid linear replacement fallback, which can be non-minimal. This bounds auxiliary alignment work rather than claiming fixed total memory regardless of input.

Text changes and optional source context now use one lazy list, with individual visible row components and preserved before/after grouping. Wide layouts keep side-by-side columns. Full source context wraps instead of clipping now that it no longer sits in an eager horizontal-scroll column. All source data remains available; no content truncation introduced.

Evidence:196 host tests pass. New tests verify two distant edits in40,000 lines yield only two removed/two added lines, and reconstruct empty/all-replaced/reordered/duplicate-heavy pages exactly. Those three host tests total0.116s on this host (not a device performance promise). Android device test loads20,000 changed rows, checks fewer than40 matching rows are composed at either end, and scrolls directly to the last added line. Four comparison device tests pass before the final context-wrapping fix; final result build/bounded-comparison-final-device.log. Android/iOS/Wasm final build passes (build/bounded-comparison-final-build.log). Remaining resource validation includes extreme single-line documents, full capture-size memory stress and wider accessibility/device classes. Broader goal active.

## Single-line and baseline text layout limits — 2026-09-19

Lazy rows alone left minified content as one multi-megabyte Text component. Added display-only fragmentation with a maximum of 2,048 UTF-16 code units and 64 newline characters per fragment. Surrogate pairs remain intact. Original saved content, source line numbers, exported patches, and change counts remain based on the unsplit data. Continuation fragments omit repeated context line numbers. Display fragments are computed once and shared by the before/after/context lists.

SingleSnapshotState's native text branch and RenderedHtmlComparison's native-text branch now use the same bounded lazy renderer, rather than one eager full-document Text. Baseline content has bottom padding so the timestamp label does not cover its final characters. HTML browser rendering remains a separate path.

Evidence: 200 host tests pass, including exact fragment reconstruction, surrogate boundaries, empty/ordinary lines and newline-heavy input. Six comparison device tests pass in 10.017 seconds total (build/bounded-text-rendering-device.log). The new Android stress case opens two single-line documents just below the 10 MiB capture limit, verifies +1/-1 source-line counts and reaches both final markers. Another test reaches the end of a 1 MiB baseline. Existing large-text, 20,000-row, rendered-order and image-mode tests continue to pass. Android debug/device APK, iOS simulator and Wasm builds pass (build/bounded-text-rendering-build.log). This establishes tested UI layout bounds; it is not a claim of fixed total memory independent of input or exhaustive capture/DOM stress coverage. Full product goal remains active.

### Turbo frequency-first redesign — 2026-09-19

Responded to the latest request to redesign Turbo again. `TurboHero` now leads with cadence inside one quiet session panel; the previous oversized countdown becomes compact secondary metadata. Idle progress is hidden from drawing and accessibility, while active progress remains tied to the actual deadline. Connected cadence controls have stable shapes, immediate selection, and clipped native ripple, removing the pill/square morph. Both timer labels and captions reserve their larger bounds so Start/Stop preserves the position of controls and page selection at large text. Session lifetime and scheduling are unchanged.

Android debug/device-test APKs, iOS simulator and Wasm compile successfully (`build/turbo-four-final-build.log`). Seven Turbo device tests pass (`build/turbo-four-final-device.log`): eligible page selection, running navigation/Stop, restored settings, large text, stable cadence and page-list origin at 1x/1.8x, and exactly one live cadence callback while repeated selection remains a no-op. The initial strengthened test incorrectly required an unchanged entire row height when the subtitle legitimately changes; corrected to assert row origin. Light/dark/large screenshots reviewed. Earlier 200 host tests remain the prior suite baseline; no core engine code changed in this refinement.

All seven tests also pass with system animator duration scale 0 (`build/turbo-four-reduced-device.log`). Restored the emulator's original absent/default scale afterward and installed the final debug APK. Only emulator-5560 was used. `git diff --check` passes. This completes the requested Turbo presentation refinement; the broader reliability/architecture goal remains active.

### Undo/deletion ownership and accessibility — 2026-09-19

The full-review follow-up found another history-management defect: MainViewModel's detached deletion timer and separately launched commit could race Undo, restoring a row visually after deletion had already been accepted. Failed deletion also left the page filtered out. The hard six-second fallback could expire while an accessibility-extended snackbar still offered Undo.

Added `UndoableDeletion`, an application-scoped owner with per-request serialized pending/commit decisions. MainScreen passes the actual snackbar decision through a request ticket. While that decision is displayed (or queued), its accessible UI timeout owns the window; cancellation on leaving the screen arms a fresh six-second application fallback. An accepted commit runs independently of the screen, cannot be undone or committed again, and preserves hiding until completion. Failure restores visibility and retains an explicit retry. Removed MainViewModel's detached CoroutineScope/Job map and scattered commit/undo paths. No monitoring cadence or snapshot persistence rules changed.

Verification: all205 host tests pass, zero failures/errors/skips. Five new tests cover a60-second visible Undo decision, cancellation/abandonment fallback at the deadline, expired ticket rejection, in-flight commit surviving screen cancellation and rejecting late Undo, and failed-delete visibility/retry. Android debug/device APKs, iOS simulator and Wasm compile successfully (`build/undo-ownership-build.log`). The initial build caught a recursive inferred property type around the retry callback; fixed with an explicit owner type. Full Android device suite is being run separately; results below. Broader goal remains active, including requirement-by-requirement completion audit and meaningful-change policy controls.

Final device result: all50 Android instrumentation tests pass in39.31 seconds (`build/undo-ownership-full-device.log`). This includes the current seven Turbo and six comparison tests along with Room, capture/lifecycle, notifications, settings and file-storage coverage. Installed the current debug APK on own emulator5560 after testing; emulator5554 remains untouched. `git diff --check` passes. No full three-hour/7-day endurance or release-signing claim is added by this turn.

### Watchlist read failures and recovery — 2026-09-19

Current-source audit found MainViewModel converted failed latest-snapshot and count queries into empty maps. This could hide saved changes and make existing monitors appear unobserved; it also swallowed cancellation. Loading/Undo emissions repeatedly executed these database reads.

Extracted `watchlistReader`: only a complete successful latest/count read emits rows. A failed query or observation reports one error per failure episode, preserves the last complete UI result, and resubscribes with1s/2s/4s backoff capped at30s. Cancellation propagates without a user error or retry. A successful load clears only the error owned by this reader, including when reopening the screen starts a new subscription. Loading indicators and pending-deletion filtering now combine after database reads, avoiding history queries on each transient UI-state change.

Evidence: all209 host tests pass, zero failures/errors/skips. Four new tests prove prior history retained during query failure and then updated on recovery, initial failure emits no fabricated history, failed site observation reconnects without navigation, and cancellation never retries or reports an error. Android debug/device APKs, iOS simulator and Wasm compile (`build/watchlist-read-recovery-final-build.log`). Targeted watchlist device result follows. Prior full50-device pass belongs to the preceding deletion build, not this new build. This change does not make the separate snapshot/count queries a database transaction; consistent aggregate reads and the full original-requirement completion matrix remain audit work.

Final targeted verification: five QuickMonitorUiTest device cases pass in7.645s (`build/watchlist-read-recovery-device.log`). Installed final debug APK on emulator5560, leaving emulator5554 untouched. `git diff --check` passes. Broader goal remains active; no new endurance/release claims.

### Full-scope audit and executed native CI gate — 2026-09-19

Created `plans/completion-audit.md`, preserving all28 original findings, six visual issues, seven release gates and the meaningful-change product direction. It separates current implementation evidence from missing runtime/accessibility/external proof. This is not a completion declaration.

Audit exposed that CI still only compiled device tests inside the web-demo job. Added a dedicated `Android verification` job: existing formatting/host/lint/debug/release checks, an API36 hardware-accelerated emulator running `:composeApp:connectedAndroidDeviceTest`, and always-retained test/lint reports. Android verification now runs independently from web demo/marketing jobs. The emulator action is pinned to the resolved v2 commit, configured from its upstream documentation; no credentials or publishing actions were added. README records the actual command and the need to configure the check as required externally.

Validation: actionlint1.7.7 passes. Inspected AGP9.1.1 ConnectedDeviceProvider/DeviceProviderInstrumentTestTask bytecode to confirm ANDROID_SERIAL filtering before running against own emulator5560, avoiding emulator5554. The first offline attempt needed uncached Android UTP dependencies; the online retry with bounded HTTP timeouts succeeded. Exact connected Gradle task ran50 tests, zero failures/errors/skips, generated HTML and XML in the configured artifact paths (`build/ci-connected-verification-online.log`, `composeApp/build/outputs/androidTest-results/connected/androidMain/`). Hosted GitHub execution and repository required-check settings remain unverified; local execution does not establish those. Exact native build/lint/release commands are being checked separately below.

Exact native workflow verification passed: formatting,209 host tests, device APK assembly, lint, debug assembly and release assembly (`build/ci-android-verification-online.log`,1m46s). The offline first attempt needed uncached Kotlin Compose group-mapping tooling; bounded-timeout online retry completed. Lint XML contains0 errors and52 warnings. R8/resource optimization produced the current11MiB `app/build/outputs/apk/release/app-release-unsigned.apk`; no exposed signing material was used. Actionlint and `git diff --check` pass. The next priority from the audit is meaningful-change/legacy threshold semantics, followed by resource/accessibility matrices and consolidated clean-source/lifecycle evidence. All build/test handles for this turn are terminal; goal remains active.

### Readable-text and explicit source detection rules — 2026-09-19

Added CaptureMode.TEXT and SOURCE, resolved through the existing HTTP capture path. New monitors now default to Readable text; existing DEFAULT/HTML/SCREENSHOT monitors keep their stored behavior. The editor exposes localized rule labels/explanations in EN/PT/ES/IT, scrollable choices, and previews based on the selected rule. Readable text uses the same extractor as comparison/sharing, moved into util rather than coupling the engine to UI. It ignores script/style/template/head and explicitly hidden content, preserves readable block text and word order, and does not claim JavaScript execution or external-CSS visibility. Source mode hashes response bytes without HTML cleanup. Text mode rejects image responses rather than decoding image bytes as text.

Detection hashes are policy-versioned (`text-v1:`/`source-v1:`), leaving legacy normalized hashes compatible. A policy change is a new baseline with existing history retained and no rule-switch alert. Stored matching-policy hashes avoid loading the previous document every check. Explicit TEXT/SOURCE rules ignore legacy byte thresholds; saving those choices clears the obsolete threshold. This does not resolve the remaining UI/semantics of positive thresholds on legacy modes, nor add region/ignore selectors.

Evidence:217 host tests pass. New tests cover markup/script/hidden-content noise, equal-length text replacements, reordered words, raw script/nonce/whitespace changes, legacy hash compatibility, no prior-file read for unchanged text, a silent baseline on rule change, explicit source changes despite an old large threshold, and new-editor default persistence. Android debug/device APKs, iOS simulator and Wasm compile (`build/detection-policy-final-build.log`, final test additions in `build/detection-policy-tests-build.log`). Full device suite includes new2x-text mode selection and real Room rule-persistence/history preservation; final outcome recorded below. No schema migration is needed: captureMode remains the existing TEXT-valued enum column; Room behavior is explicitly exercised.

Final result:52 Android device tests pass, zero failures/errors/skips (`build/detection-policy-device.log` and connected XML). Final debug APK installed on own emulator5560; emulator5554 untouched. `git diff --check` passes. Completion audit and README updated with the actual rule contract and remaining legacy/region-policy work. All tool handles terminal; broader goal remains active.

### Legacy threshold visibility and safe removal — 2026-09-19

Added an editor notice for existing positive byte filters on DEFAULT/HTML/SCREENSHOT monitors. It displays the stored byte threshold and explicitly distinguishes stored-data comparison from visible-pixel sensitivity. Remove filter on save is a draft change; cancelling/reopening keeps the original value, saving removes it, and ordinary renaming preserves it. New monitors do not offer this legacy filter. Explicit TEXT/SOURCE selection clears it on save, consistent with the engine's rule semantics. Notice/action localized in EN/PT/ES/IT, with a quiet Material surface.

Also fixed an execution race found while wiring removal: processFetchResultUnlocked checked URL/mode/pause changes but not threshold changes. It now rejects a fetch started under an obsolete threshold instead of silently recording its outdated decision; commit-time guards already protect later edits.

Verification:221 host tests pass, including new tests for rename preservation, draft/cancel/save removal, source-rule save semantics and stale-threshold capture rejection with no persistence/notification side effects. Android debug/device APKs, iOS simulator and Wasm compile (`build/legacy-filter-final-build.log`). Full current device suite adds2x-text readability/removal to capture editor tests; final result below. Original review17 now has an explicit legacy contract and safe retirement path; no claim of perceptual visual tolerance is made. Region/ignore controls, comprehensive resource/accessibility checks and other completion-audit gates remain active.

Final current device result:53 tests pass, zero failures/errors/skips (`build/legacy-filter-device.log`, connected XML). Installed final debug APK on emulator5560; emulator5554 untouched. `git diff --check` passes. All handles terminal; broader goal remains active.

### Region-selection core, not yet exposed — 2026-09-19

Prepared the shared region contract in `detection/TextRegion.kt` before adding editor/persistence state. It validates include/exclude CSS selectors, returns only selected markup, removes excluded elements, collapses nested matches to their outermost subtree, and preserves disjoint document ordering. Missing include matches fail explicitly; they never silently switch to whole-page monitoring. Non-HTML content rejects region rules. Empty rules preserve exact input. Selector length is capped at512 and regex pseudos are excluded. This module is not wired into captures, persistence or the UI yet, so region selection is still an unfinished product requirement.

Seven behavioral tests cover changing navigation/ignored countdowns vs changing selected content, nested roots, repeated ordered matches, missing/fully excluded region, ignore-only rules, invalid/oversized/regex selectors, and non-HTML/no-rule behavior. Initial validation test exposed Ksoup's specific SelectorParseException rather than IllegalArgumentException; caught the precise parser exception and reran. All228 host tests now pass, zero failures/errors/skips, and Android host/iOS simulator/Wasm compilation succeeds (`build/text-region-core-final-build.log`). `git diff --check` passes. No new APK was installed and no device-test/end-to-end region claim is made.

Next required vertical integration: persist include/exclude rules with a Room migration; apply the same filter once to preview and captured HTML; store the selected markup so later comparison/sharing reflects what was monitored; version the region identity in detection hashes so rule edits create silent baselines; include rules in stale-request guards; expose draft editor fields with validation/preview; persist actionable missing-region failure; test migration/history, rule changes, capture/preview equivalence and device editor flow. Existing whole-page text/source/screenshot monitoring stays unchanged until this is complete. Broader goal remains active.

### Text region persistence, capture, preview and editor — 2026-09-19

Integrated the prior TextRegion core as a full readable-text feature. Site stores include/exclude selectors; schema7 migration adds non-null empty defaults so existing monitors retain whole-page behavior and all history. Both early capture validation and commit-time guards compare active region rules, rejecting in-flight results from an edited rule. Rules remain inactive under screenshot/source/legacy modes.

PreparedTextCapture shares filtering between actual capture and preview. Captures save only selected markup with excluded elements removed, so subsequent comparison, Source view and sharing reflect the stored monitored region. New `text-region-v1:<rule digest>:<text digest>` hashes distinguish rule identities; region edits create silent baselines, while empty rules preserve existing text-v1 hashes. Nested selected roots are stored once, in document order. Missing/invalid regions produce persisted REGION failures with localized edit-and-preview guidance; no whole-page fallback or history deletion. This is HTML region selection, not visual screenshot cropping or JavaScript execution.

The editor exposes Whole page/Main content presets and optional Custom rules, shows the storage/alert implications, validates selector syntax before Save/Preview, keeps rules as drafts until Save, restores saved rules, and cancels stale previews on edits. Labels/help/failure guidance are localized EN/PT/ES/IT. Main content uses `main, [role=main], article`; unavailable semantic regions report failure instead of guessing another area.

Evidence:235 host tests pass. Added engine tests for selected-only storage, outside/ignored changes remaining quiet, selected changes detected, missing-region durable failure without persistence side effects, and stale-rule captures rejected. Added rule-identity/baseline compatibility, capture-preview equivalence, missing preview, editor draft/save/reopen, and invalid-selector preflight tests. Android/iOS simulator/Wasm builds pass (`build/text-region-editor-build.log`); final tests/APK build `build/text-region-tests-final-build.log`. Fixed a missing test assertion import found by device-test compilation. Schema3/4/5 migration fixtures now remove the new columns when recreating their historical layouts; new6→7 test checks empty defaults, history/unread/health preservation and persisted rules. Device result follows.

### Region integration final result and latest Turbo redesign — 2026-09-19

The region device run completed successfully: build/text-region-device.log records 55 tests and BUILD SUCCESSFUL. Host suite: 235 passing. Schema 7 and editor implementation are complete; final installed-app region journey remains separate evidence.

The latest Turbo request is implemented as frequency tiles and a compact session strip (plans/turbo-redesign.md). Replaced toggleable checkbox state with selectable radio semantics, adjusted the existing tests accordingly. Android debug/device APK build passes; seven Turbo tests pass (build/turbo-five-device.log), including Start/Stop geometry, running cadence updates, saved selection, paused-page exclusion, and 1.8× text. Reviewed screenshots build/turbo-five-{setup,running,large}.png. Broader product goal remains active; this is not a 9/10 or release-readiness claim.

Final Turbo verification: all seven UI tests also pass with animator_duration_scale=0 (build/turbo-five-reduced.log); the emulator setting was restored to its original absent/default state. iOS simulator and Wasm compile successfully (build/turbo-five-platforms.log). Installed current debug APK on own emulator-5560 and opened Turbo; reviewed real-app dynamic-color screenshot build/turbo-five-final.png. The other emulator was untouched. Current installed APK now also contains schema 7/region integration. No new background-endurance claim follows from these UI checks.

### Atomic watchlist summaries — 2026-09-19

Previous goal turn classified as progress: Turbo presentation changed and normal/reduced-motion device checks passed. Current audit found the watchlist combined a sites Flow with separate latest-snapshot and count reads, allowing summaries assembled from different committed states.

Added SitesDao.observeWatchlist: one SELECT joins each site's deterministically latest snapshot (timestamp DESC, snapId DESC) and counts its history. A local WatchlistRow projection maps to the existing UI model. The repository exposes the complete stream; WatchlistReader retains its retry/error/last-good behavior but no longer assembles database fragments. Generated Room code confirms invalidation observes both sites and snaps. No schema change.

235 host tests pass. All 21 SnapsDao device tests pass on own emulator-5560, including two new tests: empty pages/tied timestamps choose the same latest identity as history; 40 transactional version/status updates never mix versions and status-only edits, snapshot deletion and site deletion all invalidate correctly. Logs: build/watchlist-atomic-build.log and build/watchlist-atomic-device.log. Other emulator untouched. Cross-platform results follow.

Atomic-summary final checks: iOS simulator and Wasm compile passed (build/watchlist-atomic-platforms.log). Installed current debug APK on emulator-5560; real watchlist shows the existing changed page, no errors, last successful check and the normal 15-minute schedule with eligibility caveat (build/watchlist-atomic.xml). git diff --check passes. Full product completion remains unproven per completion-audit.md.

### Selected-HTML resource stress — 2026-09-19

Previous goal turn was progress: atomic watchlist reads and real Room consistency checks completed. Resource audit then reproduced region serialization inflation: a 22,021-character deeply nested selected fragment expanded to 98,900 characters through default pretty-printing. TextRegion now disables output pretty-printing; no rule or detection policy changed.

Two host regressions cover 2,000 nested elements and a large selected region containing 20,000 products with changing discarded navigation and a changed final product. The first failed before the fix (build/region-resource-before.log). All 237 host tests now pass, with debug/device APK assembly (build/region-resource-after.log). Added Android allocator/parser tests for repeated captures of >6 MB pages and the same nesting case; device result follows. This validates parser/capture preparation, not WebView screenshot memory or physical-OEM endurance.

Both Android resource tests passed (2 tests, 0 failures/errors, 3.538 s suite time in connected XML; build/region-resource-device.log). Repeated >6 MB inputs retain under 700 KB of selected markup, preserve all 20,000 product lines, ignore changing navigation and detect the changed final line. Deep markup stays within original size plus 100 bytes and retains its readable text. Current debug APK installed on own emulator-5560. This is bounded synthetic stress evidence, not a general heap or endurance guarantee.

Resource-fix cross-platform verification: iOS simulator and Wasm compilation passed (build/region-resource-platforms.log). git diff --check passes. Full completion remains unproven; continue the remaining gates in completion-audit.md.

### Screenshot ownership and resource checks — 2026-09-19

Previous goal turn was progress: selected-HTML serialization growth corrected with host/device resource evidence. Screenshot audit found a batch reused the same WebView after a capture failure, including renderer death. Each screenshot page now owns and closes its browser, preventing later pages from inheriting that dead session. Paused pages allocate no browser; browser construction failure is recorded for that page and subsequent pages still run. Settings-load failure remains a batch configuration failure. Factory cleanup also destroys a view if layout/configuration fails before ownership can be returned.

Bitmap cleanup now uses a nested finally so scroll restoration failure cannot skip recycling the output and tile. Two host regressions verify browser failure/creation failure isolation and cleanup ordering. All 239 host tests pass; Android debug/device APKs assemble (build/screenshot-isolation-final-build.log). Added real-WebView tests for three changing 18,000 CSS-pixel pages with distinct footer colors and oversize rejection followed by successful capture; device results follow. This ownership change favors isolation over WebView reuse; pages remain sequential and bounded.

Screenshot device result: all seven AndroidPageCapture tests pass on own emulator-5560 (7 tests, 0 failures/errors, 7.177 s XML suite time; build/screenshot-resource-device.log). The new changing tall-page test verifies decoded footer colors through three fresh captures and both output bounds (4,000,000 pixels and 10,000 height). A 200,000 CSS-pixel page fails with the explicit too-tall guidance, followed by a successful normal-page capture. Existing short-text, static-image stability, partial-tile footer and HTTP-error tests still pass. Current debug APK installed and launched. git diff --check passes. No physical-OEM/renderer-kill/endurance or image-heavy-DOM exhaustion guarantee is claimed.

### One-surface history navigation — 2026-09-19

Previous goal turn was progress: isolated screenshot browser ownership and bounded real-WebView stress checks completed. History audit found the sheet exposed only three pairs and nested an Older dialog to reach the rest, always beginning near the latest rather than the selected pair. Replaced this with one LazyColumn anchored at the selected pair, stable snapshot keys and an always-visible header/Close control. Page facts are in the same scrolling content, so they cannot push the version list outside a short screen. Removed the nested dialog and redundant Latest badge. Date rows explicitly label Before/After rather than relying on a direction-sensitive arrow.

New device test uses 1,001 snapshots, initially selects pair 900, verifies bounded composed rows, reaches oldest and latest, invokes correct pair callbacks, and closes at 2× text in RTL. Initial comparison suite passed all seven tests; final date-label/screenshot check follows. Android debug/device, iOS simulator and Wasm compilation pass (build/history-list-final-build.log).

Final one-list comparison suite passed seven device tests, and installed-app history sheet was visually checked (build/history-sheet-final.png). Component-only 2×/RTL screenshot build/history-large-rtl.png verifies row layout, not system insets; actual normal-size bottom-sheet screenshot verifies its real container.

Installed-app review also found two Turbo captures within the same minute had indistinguishable displayed dates. Added optional seconds to shared timestamp formatting and enabled them for comparison headers and version-history dates. A host regression proves captures 15 seconds apart have distinct history labels. All Android/iOS/Wasm builds pass (build/history-seconds-build.log); final device suite follows.

History final verification: all seven comparison device tests pass after the timestamp change (build/history-seconds-device.log). The final host suite, including the two existing calendar-grouping tests and the new 15-second timestamp regression, passes all 240 tests (build/history-seconds-host-final.log). Current debug APK installed on emulator-5560. git diff --check passes. Original findings 24/25 now have stronger history-label/navigation evidence; append-load failure/retry and the wider accessibility matrix remain outstanding.

### History append recovery — 2026-09-19

Previous goal turn was progress: a single lazy version-history surface and second-precision capture dates were implemented and verified. For original finding 26, extracted the loaded-history presentation into HistoryList while LogsScreen retains search/navigation/delete/snackbar coordination. The append-error footer now explains that more history could not load and that loaded entries remain available; Retry stays attached to LazyPagingItems.retry. Copy is localized in EN/PT/ES/IT.

HistoryPagingUiTest uses a real Android Paging Pager/PagingSource, deliberately fails the first append, verifies Page 1 still opens its original snapshot, clicks Retry, opens the final newly loaded snapshot, and returns to Page 1. It asserts exactly one refresh and two append attempts, proving recovery did not discard/reload the first page. Device test passed on own emulator-5560 (build/history-retry-device.log), Android debug/device APK build passed (build/history-retry-build.log). Cross-platform results follow.

History retry final checks: 240 host tests pass; Android debug, iOS simulator and Wasm compile pass (build/history-retry-final-build.log). Current APK installed and launched on emulator-5560. git diff --check passes. Full objective remains active, with the remaining lifecycle/resource/accessibility gates and external release controls listed in completion-audit.md.

### Changes summary hierarchy and large text — 2026-09-19

Previous goal turn was progress: real append-failure/retry behavior was verified and given a clear recovery message. Visual audit found the Changes summary still reserved width for a decorative mini-chart with invented bar heights and forced its label to a single ellipsized line. Removed that glyph and replaced count/label fragments with one wrapping, plural-aware saved-version sentence. The page-count subtitle now uses the full semantic onSecondaryContainer color instead of a reduced-opacity value. EN/PT/ES/IT plurals added.

Android debug/device APK build passes (build/history-summary-build.log). New UI checks render 999,999 versions in 280dp at 2× text, inspect TextLayoutResult for clipping, and measure the actual Material foreground/background contrast in both Android dynamic light and dark themes. Device results follow.

Large-type diagnostics: the first two new tests failed their no-overflow assertion on the subtitle. Screenshot showed the heading wrapping and the subtitle visually present; TextLayoutResult reported subtitle width overflow at a tight 553px intrinsic width. Both summary lines now explicitly take the full available width, preserving wrap space and avoiding tight intrinsic bounds. The no-overflow assertion remains unchanged. Diagnostic logs/screenshots: build/history-summary-diagnostic-device.log and build/history-summary-diagnostic.png. Final Android/iOS/Wasm builds pass (build/history-summary-final-build.log); final UI rerun follows.

Summary final device result: all three HistoryPagingUiTest cases pass (build/history-summary-final-device.log). Both light/dark dynamic-color scenarios satisfy measured contrast >=4.5 and no TextLayoutResult overflow in 280dp at 2× font; paging append recovery still passes. Current debug APK installed and launched on own emulator-5560. This verifies this summary with the emulator's current dynamic palette, not every screen/palette/locale. Broader completion remains unproven.

## 2026-09-22 — Turbo publication recovery and v4 direction

TurboStatePublisher now retries initial preference reads, state writes and widget updates with bounded backoff, preserving cancellation and restoring configuration only before the first live generation. Four host tests cover delayed initial reads, Stop during failed publication, transient counters and cancellation. All 244 host tests plus Android/iOS/Wasm compilation pass (build/turbo-publication-build.log). Actual own-emulator Start/Stop cycle confirms foreground service while active and no QuickMonitorService after Stop; stopped UI displays Start Turbo and session-stopped outcome (build/turbo-publisher-{active,stopped}*.xml / *services.txt). No claim that all widget paths are proven.

User supplied v4 reference and requested a substantial native redesign. Read README/HANDOFF/routes and viewed preview. It is explicitly a nonfunctional fixture prototype. Integration decisions/acceptance tracked in plans/v4-integration.md; preserve tested monitoring and history ownership while rebuilding the product flows.

### Desktop Turbo recovery and real check journey (September 22)
- Shared durable Turbo preference format now used by Android, Desktop, and iOS. Retains selected pages, cadence, duration, deadline, and end reason; restoring an active record reports Interrupted and never resumes checks. Existing publication retry/generation guards retained, Android widget callback still runs after storage commit.
- Desktop scheduler now publishes actual next eligibility instead of an unconfirmed status while its timer is queued. iOS manual checks now distinguish SyncResult.Failed from success and propagate cancellation.
- Real Desktop UI journey via official Compose Hot Reload MCP: selected 15-minute duration / 30-second cadence, started the existing local Conference program watch, observed live countdown and checks. Changed local fixture opening time from 10:00 to 10:30; Room recorded two snapshots and isRead=0; Turbo row displayed Changed. Restarted through MCP during the session: startup retained unread history; Turbo showed interruption notice, 15-minute duration selected, 30-second cadence selected, same page selected, and Start Turbo (not running).
- New shared preference tests cover interruption restoration, legacy defaults, and preserving unrelated regular-schedule settings. Replaced obsolete fixed-position Turbo assertions with actual start/stop/cadence/action reachability checks and ported journeys to Desktop. Further compact captures/plural correction in verification.
- Final verification `build/v4-recovery-final.log`: BUILD SUCCESSFUL; 43 Desktop tests and 255 Android host tests, zero failures; Android debug assembly, iOS Simulator ARM64 shared compilation, Wasm shared compilation, and Desktop Hot Reload passed. Rendered compact light setup, dark running, and enlarged setup captures inspected under build/desktop-review. Check-count text is correctly pluralized in EN/PT/ES/IT; countdown uses a smaller headline at narrow/large-font sizes. The preceding platform attempt raced a string-to-plural source edit against already-generated accessors; the final uninterrupted batch regenerated accessors and passed all targets.
- README documents Hot Reload, semantic MCP, separate Desktop data directory, real checks/persistence, and current host limitations. Next work: explicit undoable comparison read state, creation/selection completion flow, then remaining capability/access/visual consistency acceptance.

### Explicit comparison acknowledgement (September 22)
- Removed all automatic read mutation from comparison content loading. Latest comparisons expose an explicit Mark as read action; old pairs, failed loads and unavailable versions cannot acknowledge. Successful acknowledgement offers an accessible timed snackbar with Undo; loading or navigating versions alone preserves attention.
- Both SQL mutations are conditional on the exact latest snapshot ID, ordered by timestamp then ID. Undo carries the concrete acknowledged Snap receipt, never a mutable current selection. Existing read flags and other site/check metadata are preserved on stale or duplicate operations. A concurrently newer capture offers View latest saved version instead of silently changing the displayed pair.
- Actual Desktop MCP journey on the local Conference program watch: opened saved comparison showing 10:00 vs 10:30 and queried Room isRead=0; clicked Mark as read and observed isRead=1 plus Read/Undo; clicked Undo and observed isRead=0. Comparison stayed on the same saved content.
- `build/explicit-read-final.log`: 44 Desktop tests and 258 Android host tests pass. New real bundled-SQLite Room test covers exact capture acknowledgement/undo, same-timestamp ordering, a newer already-reviewed capture protected from stale Undo, and preservation of unrelated fields. Updated model tests cover explicit action, old pair refusal, newer capture race, undo, storage failure/retry and load failure. One first-run failure was a Mockito restub invoking its previously configured throwing answer; changed it to doReturn and reran successfully. Platform verification passed in build/explicit-read-platforms.log: Android debug assembly and iOS/Wasm shared compilation.

### 2026-09-22 — Receipt redesign after screenshot rejection
- Replaced the oversized checkmark, timestamp panel, repeated baseline copy and competing bottom actions with a compact layered document mark, saved page title, actual stored content preview, compact saved-status footer and one primary Go to watchlist action. The page card opens the saved page destination.
- Text captures use an explicitly labeled document preview; image captures use their actual bytes. No fictional website screenshot or premature successful-check state is used. Missing/read-failed/pending/error states remain distinct.
- Creation now separates URL entry from capture/content selection; editing remains a single form. A real Desktop run entered the local fixture URL, loaded its text preview, saved Design conference and displayed the durable first version and its actual excerpt.
- Validation: 49 Desktop tests, zero failures; Android debug assembly succeeded; Desktop hot reload succeeded; git diff --check clean. Normal light/dark and 200% text captures inspected under build/desktop-review/setup-receipt-*.png. No emulator used.
- This addresses the rejected completion screen. It does not establish final visual acceptance of the entire app or complete the broader goal.

### 2026-09-22 — Creation hierarchy and journey coverage
- Capture method is now a compact grouped row. Its chooser explains each method; baseline-reset guidance appears only when editing an existing monitor.
- Two-step creation progress is accurately localized in English, Portuguese, Spanish and Italian. URL entry no longer repeats the baseline explanation. Continue uses a forward arrow and the primary action follows the reference's horizontal spacing.
- Added a Desktop Compose journey test that enters a URL, advances without saving, edits the name, navigates back, verifies preserved input, advances and saves once. Inspected normal-size creation-page-light/content-light rendered captures.
- Validation: desktopTest and hotReloadDesktopMain succeeded; 50 Desktop tests, zero failures; git diff --check clean. Broader creation selection and preview polish remain open under the full goal.

### 2026-09-22 — Saved comparison composition
- Moved comparison mode, version history and sharing above the reader; a single available mode no longer renders a redundant selector.
- Replaced the combined timestamp string with separately labeled Before/After capture identities. Narrow/large-font layouts stack the identities rather than squeezing the chronology.
- Reduced text-diff block rounding and removed the decorative arrow; previous text is neutral, newer text remains violet. Reader labels consistently say Before/After.
- Added Desktop comparison coverage for the rendered changed text, opening the real version-history sheet and 280dp/200% chronology readability. Initial two-test run passed; final combined Desktop/Android validation is recorded in build/comparison-final-validation.log.
- Final validation succeeded: 52 Desktop tests with zero failures, Android debug assembly and Desktop hot reload. Final dark comparison capture inspected at build/desktop-review/comparison-workspace-dark.png; no emulator used.

### 2026-09-22 — Settings hierarchy and truthful capability controls
- Schedule summaries use exact whole-day/hour units, preserving minutes for mixed intervals. A seven-day schedule reads Sync every 7 days.
- Desktop hides browser-delay and screenshot-default controls because its capture implementation does not support visual monitoring. Android keeps the controls. Renamed Change view to Default capture method with localized text explaining that it affects pages configured to follow the app setting.
- Saved history is a compact grouped row that opens the actual retention explanation. Nonzero storage below 1 MB is shown as <1 MB instead of 0 MB.
- Desktop UI test verifies schedule action, week summary, absence of unsupported controls, and opening/dismissing storage details. Inspected settings-groups-light.png.
- Validation: desktopTest and Android debug assembly passed; 53 Desktop tests, zero failures; git diff --check clean. Previous Desktop hot-run was authoritatively terminal (Gradle daemon stop command); restarted via hotRunDesktop for continued UI work. No emulator used.

### 2026-09-22 — Watchlist rebuilt after user screenshot rejection
- User explicitly rejected the thick grouped tiles and overall mismatch to the HTML watchlist; FAB use is acceptable. Changed direction to the reference's individually separated rows rather than further polishing the grouped block.
- Rows now use transparent idle surfaces, fine separators, 42dp identity marks, smaller regular initials, domain-only metadata, quiet 11sp secondary lines, and compact New badges. Ordinary successful checks have no decorative status icon; repeated last-success labels remain only for failures. Page details retain full URL/evidence.
- Kept explicit options buttons, long press, page navigation, selected semantics and Turbo markers. Paused filter is shown when useful; three primary filters remain when no pages are paused. Schedule eligibility can be expanded from the quiet cadence row.
- Compact navigation uses outline icons and violet selected roles with safe-area insets preserved; wide layouts retain adaptive navigation.
- Added full watchlist composition render and page/options interaction test; first combined Desktop/Android build succeeded. Final verification log: build/watchlist-final-validation.log. Visual artifact: build/desktop-review/watchlist-reference-dark.png.
- Concurrent recovery correction: failed page checks now appear before saved content and expose category-specific Edit/Open in browser/Schedule/Retry actions without losing saved-version navigation. Detail tests verify region editing, retry and retained saved evidence, including lazy-list scrolling at 200% text.
- Final watchlist validation passed: 56 Desktop tests, zero failures; Android debug assembly and Desktop hot reload succeeded. Final idle-state full composition capture inspected, without pressed-state ripple artifacts. git diff --check clean.

### 2026-09-22 — Narrow settings accessibility
- Settings action rows stack their label and value when width/font scale makes an inline arrangement too cramped. Values and names remain fully readable in that mode.
- A 280dp, 200% text-size Desktop test inspects TextLayoutResult for visual overflow and ellipsis on both the setting name and schedule value, then verifies the action still fires.
- Desktop tests passed (57 total). Hot Reload was skipped because the prior hot-run session had exited normally; no live session was restarted merely on a timeout. Broader Android host tests/lint and iOS/Wasm compilation are running in build/cross-platform-audit.log.

### 2026-09-22 — User-requested comprehensive review
- Separate standards/spec reviews and local M3/UI verification produced plans/2026-09-22-implementation-review.md. Full sign-off remains blocked by concrete open findings, not by hypothetical polish.
- Fixed a confirmed response-buffering loophole: prepareGet.execute streaming scope now enforces the body cap before EOF. An endless oversized MockEngine response reproduces the old failure and passes after the fix.
- Restored semantic 12sp metadata and 24dp/labelMedium navigation sizing while preserving the lighter watchlist composition; mirrored the detail external-link icon for RTL.
- Verified 259 Android host + 57 Desktop tests, iOS/Wasm compilation. Prior combined lint run passed. Applied repository formatting and verified Spotless and git diff --check.

### 2026-09-22 — Review follow-through: cadence and preview recovery
- Replaced three cadence implementations with one semantic, plural-aware label shared by settings, watchlist and details. Exact whole hours/days are used; mixed intervals remain exact minutes. Page detail's seven-day route is tested through its global schedule action.
- Preview failures carry TIMEOUT/REGION/UNAVAILABLE causes instead of raw exception messages. UI resolves existing localized recovery copy; retry and cancellation behavior are preserved. The existing failure/retry/capture-method-change ViewModel test passes with typed failure state.
- Validation: 259 Android host tests and initial 57 Desktop tests passed; final Desktop suite with the week-detail journey passed (58 tests). Spotless and git diff --check passed. Larger direct selection, notification capability and retention findings remain open.

### 2026-09-22 — Cancellable, batched snapshot maintenance
- Removed broad NonCancellable scopes from pre-check orphan cleanup and post-save retention. Mutex acquisition, DB work and inter-file/batch checkpoints now participate in cancellation. Room transactions protect row deletion consistency; cancellation after commit leaves recoverable orphan files rather than deleting durable evidence.
- Retention planning moved to a SQLite query returning at most 500 obsolete IDs per transaction. It enforces per-page count first, then the byte budget, always protecting each page's newest two captures. Full snapshot metadata is no longer loaded/sorted/grouped in app memory for every saved version.
- Orphan cleanup checks candidate references individually rather than loading the entire archive. The existing filesystem candidate-enumeration API still returns a list, and a single synchronous filesystem call is not forcibly interruptible; no absolute hard-stop latency is claimed.
- Real bundled-SQLite Desktop tests compare batched SQL with the independent retention policy across 400 captures with timestamp ties/negative sizes, and interrupt cleanup of 602 captures after the first DB batch. Retry retains exactly the newest pair and removes leftover orphan files. Queued cancellation and owner cancellation are exercised.
- Validation: 60 Desktop tests passed; 259 Android host tests passed; Android debug assembly, iOS/Wasm compilation, Spotless and git diff --check passed. Logs: retention-tests.log and retention-platform-final.log. One initial Android test fake lacked the new DAO method; updated and rerun successfully.
- Query execution can still scan/rank a large archive within SQLite. Very-large-archive performance remains a separate measurement gate; result materialization/deletion batches and cancellation scope are now bounded appropriately.

### 2026-09-22 — Notification capability and permission recovery
- ChangeNotifier exposes delivery capability; NoOpChangeNotifier explicitly reports unavailable. Unsupported platforms show localized explanatory copy instead of an ineffective notification toggle.
- Android retains per-page notification preferences, routes enablement through its permission request callback, and exposes System notification settings for recovery after denial or external disabling.
- Desktop UI tests verify unsupported platforms expose no notification actions and supported platforms invoke both preference and settings callbacks.
- Validation: build/notification-capability-final.log passed with 62 Desktop tests (zero failures/errors/skips), Android debug assembly, iOS and Wasm compilation, and Compose formatting. git diff --check passed. Android host tests last passed at 259 during retention verification; they were not rerun in this capability pass.
- Android permission/settings navigation was compiled, not device-tested. No emulator was used. Desktop system notification delivery remains unavailable and is now represented honestly.

### 2026-09-22 — Adaptive detail hierarchy and saved-version footer
- Replaced the fixed headlineLarge page title with semantic headlineMedium/titleLarge/titleMedium roles selected from available width and system font scale. Font scale itself is preserved; narrow screens also use 16dp horizontal margins.
- Removed the empty error lazy item and its extra spacing. Error content remains adjacent to page identity and before saved evidence.
- Render inspection at 280dp/200% exposed an additional squeezed saved-version footer and clipped URL. The footer now stacks metadata/action when reading width is tight, and the full-address link uses a constrained full-width row with bodyMedium text and a 48dp minimum interaction height.
- Added a narrow/large-text regression checking title word boundaries, no overflow and reachable concrete Compare navigation. Initial test failures caught actual mid-word wrapping; final render detail-narrow-large-dark.png shows whole title words, full address and legible saved timestamp.
- Validation: build/detail-adaptive-validation.log passed with 63 Desktop tests, zero failures, Android debug assembly and Compose formatting. git diff --check passed. This pass used Desktop Compose test rendering, not an emulator; the standalone Hot Reload application was not relaunched.
- Arbitrarily long unbroken page titles/URLs can still wrap within words; no universal no-wrap claim is made. Direct region selection and the short-height comparison workspace remain open.

### 2026-09-22 — Source-backed text region selection
- Text previews now carry selectable regions from the same fetched HTML used by text monitoring. A shared Compose picker supports selection, explicit confirmation, cancellation and an automatic selected-content preview. Applied choices use existing TextRegion persistence; existing exclusion rules are preserved. Whole page remains the reset action. Custom rules is renamed Advanced CSS in all four locales.
- Selector generation preserves the original DOM positions, prefers unique generated selectors and falls back to an unambiguous structural path for duplicate IDs. Missing identified regions continue to fail explicitly through existing capture rules. Positional selectors can change meaning after structural edits; browser-backed selection and stronger selector resilience remain separate work.
- Enumeration stops at 300 choices, 5,000 visited elements or depth 32 and reports its limit; excerpts cap at 1,200 characters. Traversal checks cancellation. Parsing and individual selector/text operations are synchronous within the existing bounded HTTP input and are not hard-latency guarantees.
- Tests cover identified regions across sibling insertion/content changes, missing-region errors, preserved positions around hidden nodes, duplicate IDs, limits, confirmation and cancel-without-apply. Desktop picker render inspected at build/desktop-review/region-picker-light.png.
- Validation: 69 Desktop tests and 259 Android host tests passed; Android debug assembly and iOS/Wasm compilation passed. Logs: region-picker-final.log and region-picker-journey.log. Spotless and git diff --check passed.
- Live Desktop Hot Reload app launched (session 63333) after the previous app handle was confirmed missing. The old MCP child was confirmed broken-pipe, then replaced (client session 75152). Connection reports healthy. Actual local fixture journey: Workshop schedule → Edit → Preview capture → Choose text → opening-session paragraph → Watch selected text; refreshed preview contained only that paragraph. Navigated back without saving the fixture edit; detail still reports Whole page. No emulator used.
- This provides direct selection of source text, not a styled source-page/browser overlay. The broader reference selection experience, clearer selected-region summary, non-color selection indicator, large-text picker matrix and short comparison workspace remain open.

### 2026-09-22 — Region selection feedback and accessible sizing
- Picker choices now expose a radio role/group and a visible radio indicator, so selection does not rely on color. One row remains one actionable choice.
- The heading scrolls with the introduction and choices, preserving space for content in short windows. Confirmation and Cancel remain visible below the list.
- Existing selected rules open with Advanced CSS collapsed and a plain-language selected-region/reset explanation. Validation errors still expose the affected rule fields.
- Added Desktop journeys at 280dp × 480dp with 200% text: scroll to an initially uncomposed choice, select, confirm, and verify the callback. Added saved-rule summary and Whole page reset coverage. Inspected region-picker-large-dark.png; the selected choice and both actions are legible.
- Validation: 71 Desktop tests passed, Android debug assembly and Compose formatting passed (build/region-picker-accessibility.log); git diff --check clean. Initial compile/test-harness issues were corrected and the full suite rerun.
- Live Desktop Hot Reload connection was revalidated; reload succeeded with reloaded=true on client session 75152. No emulator used. This is a bounded picker accessibility check, not complete keyboard/screen-reader/RTL or browser-overlay acceptance.

### 2026-09-22 — Short-window comparison reader bounds
- Comparison controls and chronology now share a scrollable region capped at 45% of workspace height; the reader receives the remainder. Normal-height headers retain their natural size. Font scaling and access to timestamps, modes, sharing and history are preserved.
- Added a complete DiffFirstContent test at 280dp × 360dp with 200% text. It waits for the asynchronous diff, scrolls to the initially uncomposed added row, verifies the reader has more height than controls, checks changed-text/chronology access and opens version history. The preceding isolated header test did not prove reader space.
- Inspected build/desktop-review/comparison-short-large-dark.png. Both areas scroll independently in this constrained layout; the full chronology is not simultaneously visible. Broader image-mode and full outer-screen/landscape checks remain open.
- Validation: 72 Desktop tests passed; Android debug assembly, Compose formatting and git diff --check passed (build/comparison-short-workspace.log). Live Desktop Hot Reload status was healthy and reload returned reloaded=true. No emulator used.

### 2026-09-22 — Image comparison recovery and keyboard access
- Before/After image modes now show their available saved image even when the other image bytes are missing. Two-image modes retain the unavailable state when either input is absent. Desktop decode failure copy now uses localized resources.
- Swipe divider is focusable, has a visible focus border, and handles Left/Right in 5% increments plus Home/End with clamping. Accessibility SetProgress rejects NaN/infinity instead of permitting invalid layout coordinates.
- The 56dp drag hit region stays within viewport bounds at both extremes instead of extending halfway outside. Image split coordinates retain their full 0–100% range.
- Three Desktop tests cover available single-image recovery, keyboard progress/bounds and invalid accessibility values. Focused edge-handle render inspected at build/desktop-review/image-swipe-keyboard-focus.png.
- Validation: full Desktop suite and Android debug assembly, iOS/Wasm compilation, Compose formatting passed (build/image-comparison-final.log); targeted three-test screenshot run also passed (build/image-comparison-render.log). git diff --check clean. Live Desktop Hot Reload connection was healthy; reload returned reloaded=true. No emulator used.
- Pixel-diff summary/dimension-change layouts at short heights and RTL swipe label ordering still need verification/refinement; this does not close all image-workspace or accessibility requirements.

### 2026-09-22 — RTL image swipe coordinates
- Confirmed an RTL defect with a failing regression: Before/After labels mirrored while the image clip remained in physical coordinates. Divider offsets were also direction-relative.
- Labels now use absolute left/right alignment, and divider/handle use absolute offsets anchored at physical top-left. Text and surrounding UI retain their RTL context. Keyboard Right continues to move the divider physically right.
- Regression verifies RTL label ordering, keyboard movement, viewport bounds, and actual red-Before/blue-After pixels. Inspected build/desktop-review/image-swipe-rtl.png.
- Validation: 76 Desktop tests passed; Android debug assembly, Compose formatting and git diff --check passed. Logs: image-rtl-red.log (expected failing old behavior), image-rtl-validation.log and image-rtl-render.log. Live Desktop connection was healthy and Hot Reload returned reloaded=true. No emulator used.
- This closes the identified swipe-coordinate/label mismatch; pixel-diff short-height states and the broader RTL/full-app accessibility matrix remain open.

### 2026-09-22 — Pixel-diff space and dimension disclosure
- Pixel-diff summary and size information now share one scrollable area capped at 35% of the image panel, leaving the remainder for the output or saved image pair. Removed the second size-warning header that consumed image space again.
- An ordinary dimension change is no longer presented as a large red error banner. A localized Size changed action opens full before/after dimensions in a dismissible dialog.
- Added a 280dp × 200dp/200% text test covering image-space bounds, both saved-image labels, opening dimension details and dismissing them. Inspected pixel-diff-short-large.png; image space remains present below the disclosure.
- Validation: 77 Desktop tests passed, Android debug assembly and Compose formatting passed; git diff --check clean (build/pixel-diff-final.log). Live Desktop Hot Reload connection revalidated and reload succeeded. No emulator used.
- Loading, identical-image and unavailable-state readability at extreme sizes still require a separate check; browser-rendered region selection and the full product acceptance audit remain open.

### 2026-09-22 — Readable image comparison status states
- Missing-image and identical-image notices now use a compact, scrollable panel with semantic title/body roles, explicit surface/text colors and a small status icon. Removed the large shared empty-state illustration and duplicate padding.
- Identical images omit the redundant pixel-diff summary; the single explanation receives the full reader panel.
- Added 280dp × 200dp/200% text tests that scroll to each explanation; identical-state test also checks the redundant summary is absent. Final image-identical-large.png was visually inspected. Render inspection caught and corrected inherited black title text on a dark surface.
- Validation: 79 Desktop tests passed, Android debug assembly and Compose formatting passed; git diff --check clean (build/image-comparison-states-final.log). Live Desktop Hot Reload applied the final changes successfully. No emulator used.
- This closes the inspected missing/identical message readability defects. Browser-rendered selection, full end-to-end accessibility/lifecycle acceptance and the remaining product gates are still open.

### Desktop browser compatibility review — 2026-09-22

Found a runtime-only NoSuchMethodError in the WebView 2.0.3 SwingPanel bridge against current Compose. Added a lazy process-scoped KCEF host and required JVM access flags, plus a direct SwingPanel adapter compiled with the app. KCEF bundled-runtime startup skips its progress callback; readiness now also handles successful return. A native main-frame load signal makes pending state observable. Fresh-process navigation returned safely to details, but saved HTML did not finish loading. Added a 20-second failure fallback displaying selectable saved text; native rendering and browser-rendered selection remain open, not accepted.

Validation: build/browser-review-final.log passes 79 Desktop tests, Android assembly and iOS/Wasm compilation. build/browser-review-format.log passes Spotless. Live evidence: build/browser-direct-runtime.log; the subsequent Hot Reload crashed JBR in enhanced class redefinition, so a fresh logged process is required to verify the fallback. Black screenshots are explicitly not visual acceptance evidence. Full product goal stays active.

Final live follow-through: a fresh Gradle hotRunDesktop (which regenerates the Hot Reload classpath) successfully displayed the localized 20-second fallback and the complete fixture saved text; the Compose semantic tree verified both. Reusing an earlier argfile had reused stale Hot Reload classes and is not a valid fresh-build test. Native HTML load is still unverified; the fallback is an implemented recovery path, not a substitute for the reference’s rendered-page selector. Final Desktop suite remains 79 passing tests (build/browser-fallback-final.log); Spotless and git diff checks pass.

Visual limitation: browser-saved-text-fallback.png is also entirely black even though the semantic tree contains the fallback and saved text. Therefore only fallback state/content is verified; actual visible recovery is NOT accepted. The native integration may affect the whole Compose rendering surface, not just the embedded page. Keep this P1 open.

### Native Desktop browser initialization fixed — 2026-09-22

Progress from the previous turn’s open rendering defect: isolated runtime configuration from screenshot failures with a native probe. Bundled JCEF now uses JetBrains configuration directly; Ready follows the native initialization event. Actual app saved-page load reaches Done. DesktopBrowserSession owns browser/client disposal, native startup has a deadline, and app shutdown closes the runtime. Non-bundled KCEF installation remains supported but not live-tested.

Added :composeApp:desktopBrowserSmoke with synthetic sanitized HTML, a temporary cache, two open/read/dispose cycles and a Chromium-generated screenshot. Regression proof: temporarily selecting the old initialization path failed on load timeout; restored fixed code passes and native-browser-smoke.png visibly contains the expected text. Full validation in browser-integration-final.log passes 79 Desktop tests, native smoke, Android assembly and iOS/Wasm compile; browser-native-smoke-green.log passes the added pixel capture. Full-window MCP captures also fail on a pre-browser watchlist control, so earlier black captures cannot be attributed to the browser. The macOS Java restore-windows modal was isolated separately; no user preferences were changed.

Remaining: whole-app visual verification, browser-rendered direct selection, native recovery/packaged runtime coverage, and the broader product gates. Goal remains active.

### Rendered text-region selection — 2026-09-22

Added Page/Text list views to the existing picker. The Page view renders sanitized fetched HTML with annotated candidate IDs computed against the original source tree; a SHA-256-authorized app script handles click/Enter/Space, highlights the selected element and sends only its opaque index through a rejected navigation request. Source scripts/handlers/resources and forged candidate attributes are removed. The native side validates the exact origin and bounded index before mapping back to the original selector. Selection remains a draft until confirmation, survives switching between views, and then feeds the existing selected-capture preview/save path. Existing exclusions are preserved by the outer editor callback. Empty candidate sets stay in the explanatory text view; enumeration limits are disclosed in both views.

Android/iOS adapters use the library request interceptor; Desktop uses its native request handler. Wasm stays on the text list because iframe navigation interception is not proven. The rendered preview intentionally remains inert fetched HTML: external styles/JS layouts are not reproduced. This does not close the reference’s complete selection workspace: direct exclusion, multiple areas, Smaller/Larger, Undo and live-page visual fidelity remain open.

Evidence: 83 Desktop tests and 259 Android host tests pass (build/region-selection-host-final.log). Model tests cover duplicate IDs/source positions, script and attribute stripping, strict navigation IDs and restoration of highlight without changing the script hash. The real native smoke task clicks a candidate through the browser script, receives #stock through the same native callback used by the app, and captures the highlighted region in native-region-selection.png. The 280dp × 480dp/200% UI test checks useful preview height, selection, mode switching and explicit confirmation; native rendering is independently exercised rather than faked in that test. build/region-rendered-final.log also passes Android assembly and iOS/Wasm compilation.

Live Desktop: edit Workshop schedule → Preview → Choose text → Page reaches Done → Text list → opening paragraph → Page reaches Done → confirm → capture preview contains only the paragraph. Navigated back without saving fixture settings. Native pointer-to-rule behavior is proven by the smoke check; the live app journey used the list for the pointer action because MCP exposes Compose semantics, not embedded-browser elements. No emulator was used. Whole-app visual acceptance and Android/iOS physical interaction remain unverified.

### Selection source-identity review follow-through

Confirmed three failing regressions in TextRegion: positional includes shift after an excluded sibling is removed; structural includes can match only because an exclusion changes the tree; entirely ignored text can appear as a healthy region. Resolve both rules before mutation, reject excluded ancestors and empty readable results, and serialize outermost surviving selections in original order. Region policy v2 establishes a fresh baseline for v1 captures; SyncEngine regression verifies no alert or history deletion. Focused red/green logs: build/region-source-identity-red.log and build/region-source-identity-green.log. Full checks: 263 Android host tests, 83 Desktop tests, Spotless, clean diff check. Full selection workspace, whole-flow visual/accessibility acceptance and physical-device endurance remain open.

Selection identity platform validation also passes: Android debug assembly, iOS simulator-target compilation (no simulator launched), and Wasm compilation; build/region-source-platform-final.log.

### Scope controls and reversible selection — 2026-09-22

The source-backed picker now has labeled Smaller, Larger and Undo actions in Page and Text list modes. Enumeration records each candidate's nearest selectable ancestor against the original tree. Larger selects that ancestor; Smaller returns to the previously enlarged child when available, otherwise the first selectable child. Both disable at their boundaries. Repeated or unknown choices do not add history, and undo history is bounded to 100 entries. Undo can return to no selection, disabling confirmation. Page scope actions recreate the sanitized preview with the correct initial highlight; native clicks continue updating the existing page without reloading. A scope action currently resets page scroll because the browser view is recreated; preserving viewport during scope changes remains a polish item.

The action row scrolls horizontally at large text to avoid burying actions in a stacked header and to preserve content height. Source hierarchy and reversible state have model coverage; Compose tests verify selection, enlargement, shrinking, Undo, changed preview identity, explicit confirmation and boundary disabling. The 280dp/200% case retains useful content and confirmation space. Labels are localized in English, Portuguese, Spanish and Italian.

Validation: build/region-scope-final.log passes 85 Desktop tests, 263 Android host tests, Spotless, Android debug assembly, iOS simulator-target compilation and Wasm compilation. build/region-scope-native.log passes real native browser loading/reopening, trusted selection-script interception and rendered capture. No emulator or simulator was launched. These controls advance the reference workspace but do not close visual Ignore, multiple areas, complete page fidelity or whole-flow acceptance.

### Multiple watched areas — 2026-09-22

Added explicit Add area to the rendered picker: it retains the active area while another is chosen. Retained areas have independent dashed highlights and removable summary chips; the current choice retains its solid highlight. Text-list checkboxes support adding/removing multiple choices directly. A watched-area count is shown. Undo restores inclusion sets as well as the active candidate. The resulting comma-separated selector uses the existing capture/save path; extraction retains source order and deduplicates overlapping subtrees. Confirmation is disabled with localized recovery text if the combined selector exceeds the existing 512-character rule limit. Labels/hints are translated in EN/PT/ES/IT. Selection remains a draft until confirmed, and editor persistence still requires Save.

Validation: build/region-multiple-final.log passes 88 Desktop tests, 263 Android host tests, native browser smoke, formatting, Android assembly and iOS/Wasm compilation. Tests cover multi-area capture excluding unrelated content, source ordering, removal/Undo, two checked list choices and final rule confirmation. Native smoke exercises the trusted picker script and verifies the retained area remains highlighted/aria-pressed after another selection. An additional selector-limit regression is recorded in build/region-multiple-limits.log.

Live Desktop Hot Reload succeeded. Workshop edit → Preview → Choose text → Text list → heading → Add area → Morning sessions shows two watched areas; switching to Page reaches Done and preserves the count; confirmation produces precisely “A day for different perspectives” and “Morning sessions” in the capture preview. Left the editor without saving fixture changes. Live selection clicks used Compose list semantics; native HTML pointer behavior is separately covered by smoke. No emulator used. Visual Ignore, full page fidelity, retained scroll on scope actions and full accessibility/device-endurance acceptance remain open; this does not mark the overall goal complete.

### Visual Ignore and complete-rule validation — 2026-09-22

Added Watch/Ignore tools to both picker modes. Ignore toggles source-backed exclusion rules; restore chips and Undo reverse changes, including pre-existing Advanced CSS exclusions. The editor now passes its exclusions into the picker and accepts the complete confirmed rule, so exclusions are not silently dropped or merged after validation. Ignored page content uses a neutral dashed/struck-through treatment without removal or geometry collapse. Candidate descendants of an excluded source ancestor are marked against the original DOM, avoiding selector shifts in sanitized HTML.

The picker retains original fetched HTML and validates its full include/exclude rule off the UI thread before enabling confirmation. Stale validation cannot approve a changed draft. A missing/entirely ignored readable region produces a localized recovery message and disables confirmation. Exclusions persist through the existing editor Save path (covered by AddEditViewModelTest). Scope adjustment currently targets watched areas only; ignored areas can be toggled/restored, not yet enlarged/shrunk.

Validation: build/region-ignore-final.log passes 93 Desktop tests and 263 Android host tests, native browser smoke, Spotless, Android assembly and iOS/Wasm compilation. New regressions cover ignore/restore/Undo, ignoring every descendant, restoration of a pre-existing exclusion, unchanged geometry under excluded ancestors and confirmed-rule extraction. Native smoke verifies the ignored node remains nonzero-height with its neutral treatment while independent retained/active highlights survive. Its rendered capture was inspected.

Live Desktop Hot Reload succeeded. Workshop edit → Preview → Choose text → Text list → watch main content → Ignore opening paragraph → Page reaches Done → confirm. Capture preview contains heading, sessions and workshops while omitting the ignored opening paragraph. Left without saving fixture edits. List semantics drove the live choice; the native smoke independently exercises browser script/navigation. No emulator used. Full visual/accessibility acceptance, live-page styling, viewport preservation, ignored-area scope controls and physical-device endurance remain open.

### Ignored-area scope controls — 2026-09-22

Smaller/Larger now act on the active exclusion in Ignore mode and on the active watched area in Watch mode. Independent focus is stored for the exclusion; its scope changes replace only that exclusion and retain other rules. Enlarging then shrinking returns to the chosen child. Undo restores both the prior exclusion set and active ignored area. Removing the active exclusion disables scope actions until another is selected. Resizing into an independently retained exclusion is disabled to prevent scope changes from consuming it. The page distinguishes the active ignored region with a solid neutral outline, and switching tools restores the appropriate active highlight.

Model and Compose journey regressions verify exclusion scope, unaffected watched content, independent retained exclusions, shrink/Undo and the final confirmed rule. build/region-ignore-scope-final.log passes 95 Desktop tests, 263 Android host tests, native browser smoke, Spotless, Android assembly and iOS/Wasm compilation. The extra independent-parent boundary regression passes in build/region-ignore-scope-boundary.log. Desktop Hot Reload succeeded on the existing app process. No emulator was used. This closes the basic ignored-area scope-control gap; viewport preservation, full page fidelity and overall visual/accessibility/device-endurance acceptance remain open.

### Preserved selection viewport — 2026-09-22

The picker now owns browser scroll position outside the keyed browser subtree, so scope changes, Ignore actions, tool changes and Page/Text list switching retain the last viewport. The trusted script coalesces scroll reports to animation frames and sends bounded integer coordinates through rejected navigation messages. A selection message includes its current coordinates atomically; native handling stores those before changing the draft. Pending scroll reporting is cancelled when a selection is sent. Reloaded sanitized HTML restores the position on load through body attributes; the authorized script/CSP remains unchanged. Scroll state is local to one fetched document and is not persisted into the watch rule or history.

Strict parsing rejects malformed coordinates, negative/oversized values, extra path/query data and origin spoofing. Legacy index-only selection messages remain recognized. Native smoke now opens five sessions: a long document reports a 600px scroll, then a newly created browser restores exactly that viewport. Existing selection/retained-highlight/ignored-geometry checks still pass. Compose coverage verifies that the same position survives scope changes and Page/Text list switching.

Validation: build/region-viewport-final.log passes 98 Desktop tests, 263 Android host tests, five-session native browser smoke, Spotless, Android assembly and iOS/Wasm compilation. Desktop Hot Reload succeeded; git diff check is clean. No emulator used. Scroll restoration is verified on Desktop; Android/iOS compile but physical scroll behavior is unverified. Actions still recreate the browser view and can briefly redraw; this change does not claim zero flicker or complete visual acceptance. Full page fidelity and broader visual/accessibility/device-endurance acceptance remain open.

### Restored selection editing — 2026-09-22

Fixed the picker reopening with only exclusions: the editor and preview panel now pass the complete TextRegion. Existing watched groups and exclusions initialize the draft, with no Undo entry until the user edits. Reopening and confirming unchanged preserves the exact original selector strings, avoiding a gratuitous capture-policy/baseline change from comma-spacing normalization. Group splitting respects quoted attributes, escapes and nested pseudo-class arguments. Exclusion-only rules retain their whole-page inclusion. Advanced rule summaries have a localized fallback rather than requiring an exact generated-candidate match; their rendered matching candidates are highlighted against the original source.

Choosing a retained watched area in Page mode moves it into the active editable slot. Subsequent Smaller/Larger therefore replaces that area instead of accidentally retaining the original broader rule. Undo restores the original rule. Exact-original preservation is snapshot state, independent of history length: after history trimming, undoing the bounded 100 entries cannot incorrectly resurrect an older initial rule.

Validation: build/region-reopen-final.log passes 103 Desktop tests, 263 Android host tests, native browser smoke, Spotless, Android assembly and iOS/Wasm compilation. New model/UI regressions cover reopening multiple watched areas, unchanged confirmation, complex groups, original-rule restoration, bounded Undo, narrowing a restored area and highlighting Advanced CSS matches. Existing empty-result/exclusion restoration checks pass. Desktop Hot Reload succeeded; git diff check is clean. No emulator used. Full visual/accessibility/device-endurance acceptance and page fidelity remain open.

### Selection workspace hierarchy and actual text preview — 2026-09-22

Reworked the accumulated picker controls into a fixed, compact workspace. The title and accessible close action stay visible. Page/Text list/Preview are tabs; Watch/Ignore remains a persistent segmented control with primary-container selection color. Scope/Undo actions are quieter text buttons and Add area is secondary tonal. Unselected text rows use the page surface, with tighter spacing and bounded excerpts. The primary action is now the short “Use selection”, avoiding the previous multi-line footer at 200% text. Selection/removal/restoration details live in Preview rather than stacking in the header. Error output has a bounded scroll region so it cannot consume the complete workspace.

| Before | After | Why |
| --- | --- | --- |
| Title scrolled partly out of view during tool use | Fixed compact title and close action | Keeps orientation and cancellation clear |
| Outlined controls and stacked details competed with content | Quiet scope toolbar, details in Preview | Preserves useful reader space |
| “Watch selected text” wrapped into a large footer | “Use selection” remains compact | Gives content more room at large text |
| Source choices acted as the only in-picker text view | Actual extracted-text Preview, excluding ignored content | Lets users inspect the rule before confirming |

Preview uses the same validated original-source extraction as confirmation, capped to an 8,000-character excerpt for display. It distinguishes loading, empty and valid content, and explains that future checks compare this text. Labels are localized in EN/PT/ES/IT. The 280dp/200% dark render, normal light picker and actual-text preview were inspected: build/desktop-review/region-picker-large-dark.png, region-picker-light.png and region-preview-light.png. These are bounded Compose renders, not whole-product visual acceptance.

Validation: build/region-layout-final.log passes the full Desktop/Android host suites, native browser smoke and platform builds. Final copy/loading refinements pass picker regressions and Android/iOS/Wasm compilation in build/region-layout-polish-final.log; final full host/Desktop checks are in build/region-layout-tests-final.log. A Compose implicit-scope compile error in the initial height cap was corrected before these passes. Hot Reload succeeded after the fix. No emulator used. Browser redraw/page fidelity, complete accessibility/RTL/motion journeys and physical-device endurance remain open.

### Turbo outcomes and session health — 2026-09-22

Fixed the expired-session copy that incorrectly said “Three hours complete” for every configured duration. Expiry is now duration-neutral; NoSites has its own explanation. Terminal outcomes appear before setup controls so the reason is visible immediately. The running card retains its countdown and now explicitly shows the configured session length, saved updates, successful checks and failed checks. Failed checks are no longer blended into an undifferentiated completed count.

TurboSession increments updatesSaved only for SyncResult.Success with hadChanges=true and a non-null saved snapshot ID. Baselines, skips, failed/throwing captures and successes without a saved update ID do not increment it. The count survives Stop within the current process and resets for a new session. Like existing live check counters it is excluded from durable-state/widget publication, avoiding per-check preference writes; this is session telemetry, not a reconstructed historical total after process restart.

| Before | After | Why |
| --- | --- | --- |
| Every expired session claimed three hours | Accurate duration-neutral completion text | Correct for 15/30/60/180-minute sessions |
| Stop reason followed the large setup section | Outcome shown first | Makes interruption/expiry understandable immediately |
| Successful and failed checks were combined | Separate success/failure counts and saved updates | Shows actual monitoring health without implying every attempt succeeded |
| Only remaining time was shown while running | Configured duration remains visible too | Separates session length from remaining time and cadence |

Validation: build/turbo-session-health-final.log passes Android assembly and iOS/Wasm compilation plus host/Desktop checks. Final suites in build/turbo-session-health-tests-final.log pass 107 Desktop tests and 264 Android host tests, zero failures/skips. Model regressions verify saved-update accounting/reset and that telemetry does not trigger durable/widget publication every tick. UI regressions verify short-session outcome visibility, separate counts, configured duration and reachable Stop at 200% text; a final Help/Stop visibility check passes in build/turbo-health-accessibility.log. Desktop Hot Reload succeeded. Normal and large-text renders were inspected (turbo-session-health-dark.png and turbo-session-health-large.png). No emulator used. This does not establish physical Android endurance, notification delivery, whole-product visual acceptance or complete accessibility coverage.

### Turbo countdown and actual lifetime alignment — 2026-09-22

The running countdown previously subtracted the device wall clock from a persisted deadline while actual expiry used elapsed time. Clock corrections could therefore show a full or expired countdown while the real session had a different amount of time left. TurboState now exposes a process-only remaining-time source bound to the session's injected elapsed clock. The running UI samples that source; wall time remains only a fallback for synthetic/legacy states without a live source. Stop, expiry and restore clear the live source, and publication excludes it alongside live counters. No callable clock is persisted or resumed.

Also corrected launch delay: the coroutine's timeout is now the remaining time to the elapsed deadline announced at Start, rather than a fresh full duration beginning after dispatch. Cadence/selection changes continue to preserve that original deadline.

Validation: build/turbo-clock-final.log passes 110 Desktop tests, 266 Android host tests, Spotless, Android assembly and iOS/Wasm compilation. New regressions move wall time forward/backward by a day, verify unchanged elapsed remaining time through cadence changes, expire at the original deadline after a simulated five-second dispatch delay, and verify the UI uses the live source even with an obsolete wall deadline. Publication tests verify the source is absent from durable/widget state. Unused imports in the touched UI file were subsequently removed. No emulator used. Physical-device suspend/Doze/OEM endurance and complete product acceptance remain unverified.

### History refresh and search recovery — 2026-09-22

Initial history load failures now show localized retry guidance instead of the exception's raw message. Refresh failure with existing entries is no longer silent: a distinct inline notice explains that previously loaded versions are being shown and offers Retry. Existing entries remain openable while refresh is failed. Append errors retain their existing recovery path; its extra notice is suppressed when refresh already has an error, avoiding duplicate Retry controls. Filtered history uses “Matching saved versions” instead of presenting whole-archive counts as if they described the search results.

New Desktop tests use real Pager/PagingSource behavior to load an entry, fail refresh, open the retained entry, retry successfully and open the new result. The existing append-failure/retry journey has also been ported from the Android instrumented test to Desktop and passes without an emulator. A filtered-history test verifies archive totals are absent. The recovery render was inspected at build/desktop-review/history-refresh-recovery.png.

Validation: build/history-recovery-final.log passes 113 Desktop tests, 266 Android host tests, Spotless, Android debug assembly and iOS/Wasm compilation. Hot Reload succeeded on the existing Desktop process; git diff check is clean. No emulator used. This validates bounded paging recovery and search labeling, not the entire history/accessibility/large-archive performance matrix or overall product completion.


### M3 follow-up review and bounded corrections — 2026-09-22

Reviewed monitoring, capture guards and principal shared UI seams. Fixed active Turbo losing Stop during loading/empty lists, semantic container/content color mismatches in identities and Turbo selection, and history timestamps being truncated after long addresses. Removed duplicate history color decoration and allowed two-line titles. New Desktop regressions cover Stop in both transient list states and narrow 200% history time/open/menu access.

115 Desktop and266 Android host tests pass with zero failures/skips; formatting, Android assembly, iOS/Wasm compilation and diff checks pass (`build/m3-review-validation.log`). Desktop Hot Reload succeeded, request193. New history captures inspected. Full findings and remaining RTL-motion, browser-focus/recreation, toolbar discoverability and localized accessibility work are in `2026-09-22-m3-followup-review.md`. Overall goal remains active and unproven.


### Turbo accessible options and accurate help — 2026-09-22

The preceding goal turn made concrete progress through Stop availability, Material color-role corrections, history layout fixes and verification. This continuation addresses the review's localized Turbo-option finding.

Turbo options now carry a full localized accessible label alongside their compact visual text. Radio-button roles, selected state and click behavior remain on the single actionable surface; the abbreviated child text is excluded from semantics to prevent duplicate announcements. Cadence uses new seconds/minutes plurals in EN/PT/ES/IT; duration reuses localized session-length plurals. Four real Desktop resource-selection tests change Locale, select a minute/hour option through its accessible name, verify selected radio semantics, and confirm all eight choices are represented. Existing Turbo journey tests now target those names.

Help copy in all four locales now explains the chosen 15-minute–3-hour lifetime, Stop and continuing normal schedule. It no longer asserts an unconditional three-hour duration or a persistent notification on platforms that do not implement one.

Validation: build/turbo-options-final.log passes119 Desktop and266 Android host tests with zero failures/errors/skips, Spotless, Android debug assembly, iOS simulator-target and Wasm compilation. No emulator. This verifies Compose semantics and resource resolution, not physical TalkBack/VoiceOver speech. Overall goal remains active; navigation RTL, native-browser selection focus/redraw and the wider acceptance gates remain open.


### Navigation follows reading direction — 2026-09-22

The previous goal continuation made concrete progress with localized Turbo semantics and accurate help. This turn fixes the review's navigation-direction finding. Extracted the shared push/pop metadata into navigation/NavigationMotion.kt and keyed it on both Material motion scheme and LocalLayoutDirection. Forward entry and backward exit now use the logical trailing edge: right in LTR, left in RTL. Existing single-pane detail/comparison routing uses the same metadata; adaptive pane behavior is preserved.

Four Desktop tests mount a real NavDisplay using the production metadata and sample actual page coordinates mid-transition. They verify LTR push/pop, RTL push/pop, runtime direction change after initial composition, and zero-duration motion settling within the first frames (including immediate removal after pop). This tests renderer behavior rather than a sign-computation helper.

Validation: build/navigation-motion-final.log passes123 Desktop and266 Android host tests, zero failures/errors/skips, plus Android debug assembly and iOS/Wasm compilation. Formatting and git diff checks pass. No emulator was used. Full objective remains active; this is navigation-motion evidence, not whole-product screen-reader or lifecycle sign-off.


### Discoverable selection tools — 2026-09-22

Previous continuation was concrete progress: real NavDisplay RTL and zero-motion verification. This turn replaces the selection toolbar's unmarked horizontal scroll with an adaptive inline row or labeled Tools menu. The fit decision measures actual localized text using the active typography/density, with explicit button padding; it does not rely on a hardcoded device-width breakpoint. Tools is localized in all four languages. The standard menu preserves disabled states and routes the same smaller/larger/add-area/undo operations.

A narrow 280dp/200% regression now opens the visible menu, verifies all four actions are displayed, invokes Undo and completes selection. Existing inline scope/multi-area/ignore tests still pass; their obsolete horizontal-scroll actions were removed. The rendered-preview test retains at least80dp preview height. Both region-tools-large-dark.png and region-picker-large-dark.png were inspected. Repeated clicks on the current Watch/Ignore mode no longer increment the browser revision; a renderer-mount regression verifies this. Actual edit-driven native-browser recreation remains open.

Validation:124 Desktop and266 Android host tests pass with zero failures/errors/skips, Android debug assembly, iOS/Wasm compilation (`build/selection-toolbar-final.log`), formatting and diff checks. No emulator used. The broader goal remains active.


### Live selection highlights without page recreation — 2026-09-22

Previous continuation was progress: adaptive selection tools and no-op mode guard. This turn implements the trusted native highlight-update seam. RegionHighlightUpdate contains only nonnegative candidate indices, an optional active index and a body-watched flag; generated JavaScript never interpolates source selectors or page text. Its fixed operation updates only watched/ignored/selected attributes and aria-pressed on the existing DOM. Original HTML sanitization, hash-authorized picker script and navigation interception remain.

RenderedRegionPreview remembers the browser document by source identity/regions while deriving current highlights separately. Picker mode/scope/add/undo/ignore changes no longer key-dispose the renderer. Android/iOS apply typed updates through the existing navigator after Finished; Desktop applies them on Swing after main-frame load. Wasm still uses the text-list picker. Returning from another view may recreate the browser intentionally, preserving stored viewport; actual edits while Page is mounted do not.

Native Chromium proof (`build/region-live-native.log`): all five browser sessions passed. A long page changed watched/ignored/active areas through the same executeJavaScript mechanism as production, cleared previous highlights, and retained document-body identity, focused stock element and scrollY600 with exactly one load. Existing selection bridge, sanitizer/readback, geometry, scroll restoration and disposal checks passed too. Compose mount regression now performs Watch/Ignore, Add area, Undo and ignored selection without remount.

`build/region-live-final.log`:124 Desktop/266 Android host tests pass with zero failures/errors/skips, plus Android assembly and iOS/Wasm compilation. Formatting and diff checks pass. Desktop Hot Reload197 succeeded. No emulator used. Native Android/iOS focus/scroll behavior remains unverified on hardware; compilation is not that proof. Full objective remains active.


### Keep watched context visible while ignoring content — 2026-09-22

The preceding goal continuation implemented live native updates and proved preserved focus/scroll. Follow-up inspection found the active watched area was omitted from retained highlights on entering Ignore: the active slot switched to the ignored candidate, so the watched region disappeared visually. Picker highlight derivation now includes all watched areas in Ignore mode, while Watch mode retains the separate active and pinned treatments. Rules and captured data are unchanged.

A real Compose picker regression failed with “Watched area disappeared when entering Ignore” before the fix (`build/region-ignore-context-red.log`). It now chooses a whole page, switches to Ignore, selects a countdown, returns to Watch and confirms the combined rule. It verifies watched/ignored/active identities at the renderer boundary and checks extraction keeps stock text while excluding the countdown.

`build/region-ignore-context-final.log`:125 Desktop/266 Android host tests pass with zero failures/errors/skips, Android assembly and iOS/Wasm compilation. Formatting/diff checks pass. No emulator used. This closes the mode-context bug; broader visual, hardware accessibility and lifecycle acceptance remain open.


### Navigation entry lifetime fixes stale edit drafts — 2026-09-22

Previous continuation made progress by retaining watched context in Ignore mode. This turn inspected the real running Desktop journey. Workshop schedule's saved detail said Whole page, but reopening Edit showed a previous unsaved region draft. The root cause was missing ViewModel entry scoping: AppContent supplied only the saveable-state decorator, so parameterized koinViewModel lookups could reuse the root owner across routes.

Added the existing lifecycle-viewmodel-navigation3 dependency to commonMain and a shared rememberAppEntryDecorators helper combining saved-state and ViewModel entry decorators. AppContent now uses that helper. The standard approach is documented in https://kotlinlang.org/docs/multiplatform/compose-viewmodel.html. This scopes all route-owned models, while MainViewModel passed from the app and AddPageDialog's explicit owner retain their intended lifetimes.

NavigationEntryScopeTest mounts a real NavDisplay with the production decorators: separate editor routes get distinct models; preview navigation preserves an existing draft; popping clears the model; reopening creates a fresh draft. `build/entry-scope-final.log` passes126 Desktop and266 Android host tests, Spotless, Android debug assembly, iOS/Wasm compilation. Diff checks pass.

Hot Reload208 initially reported success but the live process log proved ClassNotFoundException for the newly added lifecycle library. After confirming the original Desktop process was still live, terminated only session78667 and launched Desktop with the new runtime classpath. Current process session9067 (`build/entry-scope-live.log`); MCP75152 remains connected. This was a dependency-required restart, not an observation-timeout retry.

Post-restart live proof: edit Workshop schedule, Whole page selected; choose Main content; navigate back without Save; detail remains Whole page; reopen editor and Whole page is selected again (MCP213–220). Left back at details without saving. No emulator or persisted fixture edits. Overall goal remains active; broader lifecycle/hardware and visual gates remain unproven.


### Preserve comparison identity across return and recovery — 2026-09-22

Previous turn made progress by fixing navigation-owned ViewModel lifetimes and verifying editor cancellation live. This follow-through found ComparisonScreen's entry effect unconditionally reloaded the initial pair after returning from a child route, undoing the user's historical selection. Added idempotent openComparison keyed by entry request; explicit loadSiteAndSnaps remains available for intentional refresh/latest actions.

Added retryComparison, which reloads by visible snapshot identity or, if the initial database load failed, the original requested identity. Database-error Retry previously dropped initialSnapId; pair-content Retry previously retried against the cached list/index. All Retry paths now resolve the intended identity against current history; missing versions report unavailable rather than silently switching. Explicit View latest is preserved.

Four host regressions verify return preserves an older pair without another repository load, explicit latest still works, content/database failure recovery keeps the requested version, and deletion before retry produces the unavailable state without acknowledgement. Initial test authoring errors (ByteArray fixture type and Mockito throwing-restub syntax) were corrected before final verification.

`build/comparison-entry-final.log`:126 Desktop/270 Android host tests pass, zero failures/errors/skips; Android assembly, iOS/Wasm compilation, formatting and diff checks pass. No emulator. This is ViewModel/recovery plus production-screen wiring evidence, not a new physical-device journey. Overall goal remains active.


### Localized, reachable comparison recovery — 2026-09-22

Previous turn preserved comparison identity through return/retry. This pass removes raw exception text from ComparisonUiState: typed Page/Content failures now represent load errors, while diagnostic exceptions go to Napier. The page-load failure screen gives localized retry guidance in EN/PT/ES/IT instead of displaying database or filesystem messages. Existing content errors remain localized. Host failure tests assert the typed classifications.

SharedEmptyState previously had no scroll container, so large text in a short viewport could push recovery actions out of reach. Its content now scrolls when needed and has a480dp reading-width cap, retaining centered presentation when it fits. Two Desktop tests verify normal action visibility and both recovery callbacks after scrolling at280×300dp/200% type. recovery-light.png and recovery-large-dark.png inspected; the latter is intentionally captured at the action end of the scroll.

`build/comparison-recovery-copy.log`:128 Desktop/270 Android host tests pass with zero failures/errors/skips, Android debug assembly and iOS/Wasm compilation. Formatting and diff checks pass. No emulator. Overall goal remains active; these are bounded recovery/visual checks, not whole-app accessibility or Android lifecycle acceptance.


### Real snapshot-file failure and recovery — 2026-09-22

Previous turn made progress with typed/localized recovery and scrollable shared states. This turn verifies missing captures through actual DesktopFileStorage, SQLite/Room, local sources, repositories and ComparisonViewModel, using isolated temporary data. Two integration cases cover one missing baseline and a comparison whose older content file is missing. Both produce Content failure, leave history rows intact and cannot mark the failed comparison read. Writing the missing file back and Retry loads the same original identities/content; the database remains unread. No missing data is fabricated as an empty capture. Monitoring/share mocks are unused dependencies, not substitutes for persistence in these tests.

SingleSnapshotState and DiffFirstContent had separate unscrollable content-error layouts. These now scroll within constrained readers. A180dp-high/280dp-wide/200% Desktop baseline failure test scrolls to Retry and invokes its callback. This complements the preceding page-level error layout verification.

`build/missing-snapshot-recovery.log`:131 Desktop/270 Android host tests pass, zero failures/errors/skips; Android assembly and iOS/Wasm compilation pass. Formatting and diff checks pass. No emulator or user-data mutation. These tests establish missing-file recovery on Desktop, not disk-exhaustion recovery or physical Android storage behavior. Overall goal remains active.


### Desktop snapshot-write failure and recovery — 2026-09-22

Previous turn verified missing-file recovery. This continuation found DesktopFileStorage lacked Android's free-space reserve and typed storage-write classification. Desktop now rejects writes below payload size plus32MiB reserve, flushes file data before atomic rename, wraps creation/write/rename failures as SnapshotStorageError, and cleans temporary files even when publication fails. File-name validation stays outside the IO wrapper.

StorageRecoveryIntegrationTest uses the real SyncEngine, SQLite and Desktop file storage with injected free-space measurements: baseline save; low-space change rejection; unchanged baseline bytes/rows/last successful time and no notification; available-space recovery; exactly one new version and one notification; repeated identical capture produces neither another version nor another notification. A second test forces a real atomic-replacement failure against a nonempty directory, verifying its existing data survives and temporary files are removed. No user disk was filled. This proves guard/transaction/recovery behavior and a real publication failure, not physical ENOSPC/power-loss durability.

`build/storage-recovery-final.log`:133 Desktop/270 Android host tests pass, zero failures/errors/skips, formatting, Android assembly and iOS/Wasm compilation. Diff checks and Desktop Hot Reload225 succeed. No emulator. Full goal remains active.


### Content-sized page-detail teaser — 2026-09-22

Previous turn improved storage failure behavior. This turn returned to visual acceptance, inspecting current watchlist/detail captures alongside the supplied v4 preview.png. The most obvious detail mismatch was a one-line text excerpt sitting in a208dp image-sized panel, followed by a needlessly stacked footer. SavedPageTeaser now uses96dp minimum for text/document states and lets actual text grow; image previews retain208dp. Text padding is20dp. Footer stacking is based on measured localized metadata/action text plus icon/gaps rather than a280dp effective-width cutoff.

Fresh detail-light.png shows timestamp and Compare in one row, with Turbo and Watching controls substantially higher. detail-narrow-large-dark.png retains stacked readable metadata/action at200% type. Both inspected. Existing detail behavior/large-text tests pass; no implementation-mirroring test was added for this reversible spacing change.

`build/detail-preview-final.log`:133 Desktop/270 host tests pass with zero failures/errors/skips, formatting, Android assembly and iOS/Wasm compilation. Diff checks pass. No emulator. Whole-product visual acceptance remains open; this closes a concrete mismatch rather than claiming the full HTML design has been reproduced.


### Detail hierarchy and review reconciliation — 2026-09-22

Completed the pending detail-row refinement: quieter divided rows,72dp minimum, bodyLarge labels, preserved callbacks. Inspected normal/light and200% dark detail captures. Follow-up review found physically right-pointing chevrons in detail/settings despite mirrored navigation; switched both to AutoMirrored KeyboardArrowRight. Reconciled remaining component-policy, action-affordance and preview-fidelity findings in2026-09-22-m3-followup-review.md rather than claiming whole-product completion.

Full verification (`build/detail-review-final.log`):133 Desktop/270 Android host tests, no failures/errors/skips; Android assembly, iOS/Wasm compilation pass. Spotless and diff checks pass; Desktop Hot Reload227 succeeds. No emulator. Existing tests cover detail actions and constrained large text; no implementation-mirroring tests added for spacing or framework icon choice. Overall goal remains active.


### Distinguish detail commands from destinations — 2026-09-22

Previous goal turn made progress: quieter detail rows, mirrored destination chevrons, review evidence and full test/build verification. This continuation closes the action-affordance finding: Check and Pause/Resume omit the destination chevron, while Watching and Regular checks retain it. Disabled Checking text uses a neutral supporting color. All rows retain full-surface interaction.

New Desktop regressions exercise in-progress check suppression with Pause still reachable, and manual checking/resuming a paused page. Fresh normal/light and200% dark detail captures inspected; command text gains usable width. `build/detail-actions-final.log`:135 Desktop/270 host tests, no failures/errors/skips; Android assembly, iOS/Wasm compilation, Spotless and diff checks pass. Desktop Hot Reload228 succeeds. No emulator. Overall goal remains active; full product acceptance is not yet established.


### Settings hierarchy and text-width refinement — 2026-09-22

Previous continuation made progress by distinguishing detail commands from navigation and verifying disabled/manual-check states. This turn compared settings with the actual v4 settings-group/setting-line CSS. Settings now use one outlined Material surface per group, inset dividers, quiet22dp icons and regular body labels rather than separate filled rounded rows with icon containers. Material colors/shapes and full-row button semantics remain. Noninteractive rows do not show destination chevrons.

SettingsRowText measures actual localized title/value widths before choosing inline or stacked layout. Explanations no longer truncate at two lines. A280dp/200% long-value regression initially passed overflow checks but its capture exposed broken words because side icons consumed text width. Constrained rows now place icons in a small header and give text full width; the test now verifies word boundaries as well as layout/callback behavior. The existing standalone-row regression then caught cadence width overflow; stacked text now explicitly fills available width. These findings were corrected rather than relaxing existing assertions.

Final verification: build/settings-refinement-final.log passes136 Desktop/270 Android host tests with zero failures/errors/skips, Android debug assembly and iOS/Wasm compilation. Spotless check and diff check pass; Desktop Hot Reload231 succeeds. Normal/light and constrained/dark captures inspected. No emulator. This resolves the settings-row component mismatch and specific large-text failures, not the entire typography/visual acceptance matrix; overall goal remains active.


### Platform-aware automatic-check settings — 2026-09-22

Previous goal turn made progress with the reference-aligned settings groups and verified large-text corrections. This turn opened the actual running Desktop schedule sheet and found misleading controls: it claimed background monitoring and exposed Wi-Fi/charging/battery constraints, while DesktopMonitoringManager only runs a coroutine timer while open and ignores those constraints.

Added explicit platform monitoring capabilities (Background/WhileOpen/Unavailable plus constraint support). Android exposes constraints; Desktop declares WhileOpen; iOS retains its BGTaskScheduler-backed background mode but hides constraints its manager does not pass through; the Wasm demo declares unavailable. Shared settings/sheet copy now reflects those capabilities, with EN/PT/ES/IT strings. Unsupported constraint controls are hidden, and unavailable scheduling cannot be toggled/saved through the sheet. Stored constraint preferences are preserved. Existing Desktop settings regression now asserts the Automatic checks title and while-open explanation.

Live post-Hot-Reload235 journey: Settings → Automatic checks shows the while-open statement and no constraint controls; choose7d (days/7d selected) → Close sheet without Save → reopen shows hours/1h selected, with the settings summary still1hour (MCP237–246). Closed the sheet afterward. Hot Reload reset navigation to Watchlist during the code update; the cancellation evidence comes from the subsequent fresh journey, not that reset. No schedule changes were saved.

`build/schedule-capabilities-final.log`:136 Desktop/270 host tests pass, no failures/errors/skips; Android assembly and iOS/Wasm compilation pass. Spotless and diff checks pass. No emulator. This corrects platform promises and verifies cancellation; it does not prove Android/iOS background endurance or whole-product completion. Goal remains active.


### Accessible, adaptive schedule intervals — 2026-09-22

Previous continuation made progress by correcting platform scheduling promises and verifying live draft cancellation. This turn addresses the interval editor: unit chips no longer divide a fixed row equally; FlowRow wraps intrinsic-width labels when needed. Labels use measured type widths so full words fit. Compact presets now expose complete localized cadence descriptions through the existing plural resources, with compact child labels excluded from duplicate semantics; radio selection/click semantics remain.

Two Desktop regressions verify280dp/200% unit text layout, fully named15-minute/7-day selection, invalid0/overflowing numeric input preserving the last valid interval, and preset recovery. Initial diagnosis incorrectly suspected chip height; detailed results showed width overflow with no height overflow, so the final change uses measured explicit label widths and retains the normal minimum touch target. The final schedule-interval-large-dark.png was inspected: whole unit names, wrapped chips, readable presets and selected state.

`build/schedule-interval-final.log`:138 Desktop/270 host tests pass, zero failures/errors/skips; Android assembly, iOS/Wasm compilation and Spotless pass. Diff check passes. Desktop Hot Reload248 succeeds. No emulator. These checks cover English rendering and use existing localized cadence resources; physical screen-reader speech and the full translated layout matrix remain unverified. Overall goal remains active.


### Persisted schedule initialization race — 2026-09-22

Previous continuation made progress with adaptive interval controls and localized preset semantics. Current source inspection found BackgroundSyncDialog remembered the initial default SettingsUiState before DataStore necessarily emitted, so a freshly entered sheet could retain the wrong schedule. Added explicit loaded state to the ViewModel's initial value, disabled Save/controls while pending, and initialized the draft when loaded changes to true. Later state emissions intentionally preserve edits. saveMonitoringSettings also rejects an unloaded state.

Settings preferences previously waited for the first storage-byte observation through combine. A zero initial byte emission now lets actual preferences become available independently of that query. This does not claim handling a terminal storage/preferences failure; error recovery remains a separate concern.

Extracted BackgroundSyncDialogContent with a suspend save callback for a real-sheet regression: disabled Save before load, persisted7d appears, user chooses15m, later storage/cadence state emission does not replace it, Save delivers15m and dismisses. A host ViewModel test holds storage observation indefinitely and proves delayed persisted7d/disabled preferences still load. Initial host test cleanup reset Dispatchers.Main too early; corrected by joining collector/ViewModel cancellation before reset.

`build/schedule-loading-final.log`:139 Desktop/271 host tests pass, zero failures/errors/skips; Android assembly and iOS/Wasm compilation pass. Spotless/diff checks pass; Desktop Hot Reload249 succeeds. No emulator. Overall goal remains active.


### Retryable settings loading — 2026-09-22

Previous goal turn made progress by gating schedule drafts on real preferences and separating initial preference availability from storage-size latency. This follow-through handles terminal observation errors: the combined settings observation catches failure into a localized loadFailed/unloaded state, and an explicit retry generation resubscribes through flatMapLatest. Diagnostics remain in logs; raw exception text is not rendered. SettingsScreen hides editable default sections while pending/failed and shows a scrollable loading/retry state. The schedule sheet uses the same message within its existing scroll area and keeps Save disabled. EN/PT/ES/IT resources added.

Changed draft initialization from a loaded-state key to an explicit initialized flag: a later failure/retry no longer discards an existing user draft. The real-sheet regression now edits15m, receives failure, verifies disabled Save, retries with different source settings and still saves15m. New host regression throws on the first preference subscription, verifies failure/unloaded state, retries, and gets the persisted7d interval on exactly the second subscription. Storage exceptions share the catch path; this test specifically injects the preference failure.

`build/settings-recovery-final.log`:139 Desktop/272 host tests pass, zero failures/errors/skips; Android assembly and iOS/Wasm compilation pass. Spotless/diff checks pass; Desktop Hot Reload251 succeeds. No emulator. The overall goal remains active; this closes a concrete recovery gap rather than claiming complete settings/platform fault coverage.


### Distinguish pending storage from measured zero — 2026-09-22

Previous continuation made progress with retryable settings observation and preserved schedule drafts. This turn reviewed the initial storage value added to keep preferences responsive. A synthetic0 could make saved history report0MB before the actual query completed. SettingsUiState.storageBytes is now nullable, with null emitted before measurement. HistoryStorageSection shows localized Loading for pending data and formats only measured values. Policy details remain accessible while pending.

A Desktop regression opens the pending summary, verifies no invented0MB, then emits actual0 and4MB and verifies the open dialog plus the row update. Host stalled-storage tests assert that successfully loaded preferences retain unknown storage rather than0. `build/storage-summary-final.log`:140 Desktop/272 host tests pass, zero failures/errors/skips; Android assembly and iOS/Wasm compilation pass. Spotless/diff checks pass; Desktop Hot Reload252 succeeds. No emulator. Overall goal remains active.


### Readable, theme-aware creation receipt — 2026-09-22

Previous continuation made progress by distinguishing pending storage from measured zero. This turn returned to the creation completion screen and inspected existing normal/200% captures. ReceiptPreview used a fixed180dp height even for text, causing its document title and excerpt to truncate aggressively at large text, plus a hardcoded light cream surface in dark mode. Text previews now grow from a180dp minimum; actual image previews retain fixed height. Colors use surfaceContainerLow/onSurface/onSurfaceVariant. Footer content wraps as needed instead of squeezing baseline status beside Open page.

Extended the existing200% receipt test to verify the saved document title is not ellipsized or height-clipped, while retaining Open page/Go to watchlist action checks. Inspected regenerated normal/light, preview-large-dark and footer-large-dark captures: full document title, readable excerpt, dark surface and stacked footer at200%. This remains a bounded excerpt, not a fabricated website screenshot.

`build/receipt-refinement-final.log`:140 Desktop/272 host tests pass, zero failures/errors/skips; Android assembly, iOS/Wasm compilation and Spotless pass. Diff checks pass; Desktop Hot Reload253 succeeds. No emulator. Full visual acceptance and broader goal remain open.


### Truthful receipt loading and failed-read states — 2026-09-22

Previous continuation improved receipt text sizing and theme colors. Source review then found two state issues: before any read, the receipt showed Waiting for first check; after a successful baseline read, a later failure replaced the evidence with only an unavailable notice. The route now passes explicit initial loading. Loading shows Page added/Loading, not invented check status. A failed refresh keeps known baseline content and Open page, with an unavailable notice. An initial failed read still offers access to the saved page. Unknown/paused states avoid the active-watching headline. Existing watchlistReader continues its bounded-backoff retries.

Confirmed deletion takes priority over subsequent unavailability: no contradictory saved-status notice or stale Open page action. Four new Desktop regressions cover initial loading, failed refresh retaining a baseline, initial failure retaining an exit, and failure after confirmed deletion.

`build/receipt-state-final-confirmed.log`:144 Desktop/272 host tests pass, zero failures/errors/skips; Android assembly, iOS/Wasm compilation and Spotless pass. Diff check passes; Desktop Hot Reload255 succeeds. No emulator. Broader goal remains active; this is receipt state/layout evidence, not whole-product completion.


### Adaptive appearance choices — 2026-09-22

Previous continuation made progress with truthful receipt observation states. This turn reviewed appearance controls: Light/Dark/System were forced into equal columns with single-line labels, risking narrow/large-text clipping. ThemeSelector now measures localized labels plus button padding and keeps connected Material toggle shapes only when all choices fit; otherwise it uses full-width stacked Material toggles. Selection/callback behavior remains. Dynamic palette text/check now use onPrimaryContainer when selected, matching the actual container role.

Two Desktop tests at390dp/default text and280dp/200% verify whole theme labels, theme toggle state/callback, named palette selection and reachability. Fresh appearance-regular-dark.png and appearance-large-dark.png inspected. Tests verify component behavior rather than claiming device theme persistence or measured dynamic-color contrast.

`build/appearance-final.log`:146 Desktop/272 host tests pass, zero failures/errors/skips; Android assembly, iOS/Wasm compilation and Spotless pass. Diff checks pass; Desktop Hot Reload256 succeeds. No emulator. Full visual/accessibility acceptance and overall goal remain active.


### Appearance dialog and durable preferences verification — 2026-09-22

Previous continuation made progress with adaptive appearance controls. This turn attempted the real Desktop Settings→Appearance journey via connected Hot Reload MCP. Click returned success, but get_semantic_tree exposed only underlying Settings, and list_windows exposed only the main window. Inspected tool schemas via a short-lived discovery bridge (without restarting the app); no dialog controls were available. No theme/palette preference was changed in that live attempt. This is an observation limitation/unverified journey, not proof the live dialog works or is broken.

Added a full AppearanceSettingsSection Desktop test: open dialog, choose Dark/Blue, Done, reopen, confirm both selected states, Done. Its first assertion ambiguously matched Dark in both the underlying summary and dialog; narrowed it to toggle semantics. Added a real PreferenceDataStoreFactory disk-backed test: write Dark/Blue through SettingsViewModel, await actual preference emissions, close ViewModel/store scopes, open a new store on the same temporary file, verify both persisted choices. Temporary data and coroutine scopes are cleaned up; user preferences are untouched.

`build/appearance-persistence-final.log`:147 Desktop/273 host tests pass, zero failures/errors/skips; platform builds pass. Spotless/diff checks pass. No runtime implementation changed, so no new Hot Reload was required. No emulator. This adds real persistence and dialog-flow evidence while keeping the unobserved live window journey explicit. Overall goal remains active.


### Recoverable preference-write failures — 2026-09-22

Previous continuation verified complete appearance dialog reopening and real file-backed preference persistence. Review then found theme/palette/comparison/browser-delay writes launched DataStore edits without exception handling. These now share a serialized write path with cancellation propagation, logged diagnostics and observable failure state. Localized Retry feedback is shown on Settings and inside the open Appearance dialog. A new user choice clears the old pending retry; generation checks prevent a late old failure from becoming the current retry. Successful settings remain driven by persisted values rather than optimistic UI claims.

Host fault-injection regression verifies failed Dark write leaves the stored key absent, retry succeeds, then failed Blue followed by successful Green cannot be undone by invoking the stale retry. Desktop whole-dialog regression verifies the localized error is visible and Retry callable without closing Appearance. EN/PT/ES/IT strings added. Tests use isolated/in-memory failure injection, not a physical disk-exhaustion claim.

`build/preference-write-final.log`:148 Desktop/274 host tests pass, zero failures/errors/skips; Android assembly and iOS/Wasm compilation pass. Spotless/diff checks pass; Desktop Hot Reload262 succeeds. No emulator or user preference mutation. Overall goal remains active.


### Stable history ordering across paging and search — 2026-09-22

Previous goal turn made progress with recoverable preference writes. This turn inspected saved-history queries and found timestamp-only ordering in paged, searched and several legacy history reads, inconsistent with latest-version lookup's timestamp/snapId ordering. Added snapId DESC as the secondary order for those reads and removed irrelevant ORDER BY from a count query. No schema change or migration is required.

A new real Room/SQLite Desktop test seeds3000 captures across50 pages with repeated timestamps and shuffled insertion order, then drains37-item pages. It checks exact expected descending timestamp/identity order, unique complete identities, parity between plain and empty-search paging, filtered title search, and nonpaged changed-history reads. It failed before the query correction (`build/history-order-red.log`) and passes afterward. This is metadata correctness/paging evidence at3000 captures, not image-heavy memory or very-large-archive performance proof.

`build/history-order-final.log`:149 Desktop/274 host tests pass, zero failures/errors/skips; Android assembly, iOS/Wasm compilation and Spotless pass. Diff check passes; Desktop Hot Reload263 succeeds. No emulator. Overall goal remains active.


### Literal punctuation in history search — 2026-09-22

Previous continuation made progress with deterministic history ordering and real3000-capture paging coverage. This turn checked search semantics: raw LIKE input interpreted% and_ as wildcards, unlike a user's literal title/URL search. The SQL now escapes its chosen! escape character and both wildcard characters before applying substring matching, for title and URL paths. Queries remain bound parameters.

Extended the same real database archive with percent-bearing titles and underscore/exclamation URLs. The regression failed before correction (`build/history-search-red.log`); it now verifies literal%,_,!,combined URL text, and a nonmatching punctuation combination while preserving the existing full paging/search ordering assertions.

`build/history-search-final.log`:149 Desktop/274 host tests pass, zero failures/errors/skips; Android assembly, iOS/Wasm compilation and Spotless pass. Diff check passes; Desktop Hot Reload264 succeeds. No emulator. Overall goal remains active.


### Bounded manual old-version cleanup — 2026-09-22

Previous continuation made progress with literal history search. This turn found manual pruning fetched every snapshot metadata record, sorted it again in Kotlin and retained every obsolete ID before deletion. Replaced that DAO operation with a bounded transaction: query IDs beyond the newest timestamp/snapId pair using LIMIT/OFFSET, enforce1..500 batch size, delete those rows, return only that batch. SnapsLocalDataSource drains batches and deletes their files before requesting another. Existing NonCancellable confirmed-cleanup behavior and row-before-file ordering remain. No schema migration is needed.

Real Room/SQLite Desktop regression seeds1202 versions with repeated content and timestamp ties plus another page. It verifies an explicit37-ID batch, then actual local-source cleanup drains the remainder, retains the newest pair, deletes exactly1200 obsolete file identities, preserves the unrelated page/file and is idempotent. FileStorage is an in-memory tracking double; this test does not claim physical file deletion endurance. Updated the existing instrumentation test to the bounded DAO API and a host fake DAO caught by the full compile; no instrumentation was run.

`build/manual-cleanup-final.log`:150 Desktop/274 host tests pass, zero failures/errors/skips; Android assembly and iOS/Wasm compilation pass. Spotless/diff checks pass; Desktop Hot Reload265 succeeds. No emulator. Other full-page deletion/legacy archive paths remain distinct from this manual-prune change. Overall goal stays active.
### Atomic page deletion and file identities — 2026-09-22

The preceding review was progress: it recorded concrete current-source findings and visual evidence. This continuation corrected its concurrent-capture/page-deletion race. SitesDao now reads snapshot IDs and deletes the page in one Room write transaction, returning those identities for the existing NonCancellable file cleanup. SnapsLocalDataSource no longer performs a separate pre-transaction full-Snap read. Generated Desktop DAO code confirms the transaction wrapper. No schema migration is required.

A real SQLite and DesktopFileStorage test overlaps a held capture transaction with page deletion, verifies the committed capture's row/file are removed, preserves an unrelated page/file, rejects a later orphan insert through the foreign key, and checks repeated deletion. This is database contention and real-file evidence, not physical power-loss testing or bounded-memory deletion of arbitrarily large archives. The operation still collects all file IDs; a durable bounded cleanup queue remains a separate improvement.

`build/page-delete-final.log`:151 Desktop/274 Android host tests pass with zero failures/errors/skips, plus Spotless, Android debug assembly and iOS/Wasm compilation. `git diff --check` passes. No emulator used. Remaining review findings and whole-product acceptance remain open; the goal is active.


### Compact Turbo setup with visible page choices — 2026-09-22

The previous continuation made progress by fixing atomic page-deletion/file identity collection. This turn addressed the review's Turbo setup hierarchy finding against the supplied HTML: removed the redundant oversized cadence numeral and its unit line, grouped the cadence label directly with its existing accessible choices, reduced internal gaps to16dp, and used MaterialTheme.shapes.extraLarge for the setup container. The active countdown remains unchanged as the meaningful prominent timer. Duration, selection, Start/Stop and all callbacks remain wired.

Inspected fresh normal and180% Compose captures: build/desktop-review/turbo-setup-light.png and turbo-large-type.png. Three selected pages now fit above the pinned action in the normal capture; large text wraps choices and scrolls content while retaining Start. Extended the existing setup journey to assert the second page is visible above Start before any scrolling. Existing localized option, duration/page-selection and active-session tests pass. This is bounded layout/journey evidence, not complete visual acceptance.

`build/turbo-compact-final.log`:151 Desktop/274 Android host tests pass with zero failures/errors/skips; Spotless, Android assembly and iOS/Wasm compilation pass. Diff check passes; Desktop Hot Reload268 succeeds. No emulator used. Remaining review findings, full accessibility/device endurance and overall product acceptance remain open.


### Quiet Android Turbo notification publication — 2026-09-22

The previous continuation made progress with compact Turbo setup and inspected layouts. This turn corrected the review's redundant foreground-notification updates. QuickMonitorService now collects a distinct projection containing only active/end state, displayed page count and cadence. Capture counters, update counts, timestamps and same-count page identity changes no longer cause its observer to call NotificationManager.notify. The initial startForeground notification uses the requested page count/cadence instead of the previous session state. Stop and expiry remain immediate observer events.

A host regression runs the real TurboSession with virtual time, verifies repeated successful captures/saved-update accounting produce no extra projected notification emissions, verifies same-count replacement stays quiet, and checks changed count/cadence, Stop, restart and expiry. This proves the service's collected flow contract, not actual OS notification delivery or battery consumption.

`build/turbo-notification-final.log`:151 Desktop/275 Android host tests pass with zero failures/errors/skips; Spotless, Android debug assembly and iOS/Wasm compilation pass. Diff check passes. Android-only implementation changes require no Desktop Hot Reload. No emulator used. Remaining review findings and physical-device/whole-product acceptance remain open; the goal is active.


### Isolated storage-summary recovery in Settings — 2026-09-22

The previous turn made progress with quiet Android Turbo notification publication. This turn corrected the review's Settings failure coupling. History size now has its own failed/loading state and retry trigger; storage exceptions no longer terminate preference observation or mark Settings unloaded. The Saved history row and its open dialog show localized failure copy with Retry in EN/PT/ES/IT. Unknown size is not presented as zero.

Host fault injection verifies failure after an initial size reading leaves seven-day settings loaded, subsequent cadence/theme preference emissions continue, and storage Retry recovers without another preference subscription. Desktop tests verify retry outside/inside the history dialog, and the schedule-draft test now checks that a storage failure preserves the edited15-minute interval and enabled Save. Inspected settings-storage-retry.png and aligned the inline Retry with section padding. These are isolated recovery/layout tests, not a physical filesystem-failure claim.

`build/settings-isolation-final.log`:152 Desktop/276 host tests pass with zero failures/errors/skips; formatting, Android debug assembly and iOS/Wasm compilation pass. Desktop Hot Reload269 succeeds. No emulator used. Other review findings and whole-product acceptance remain open.


### Paired selected-watchlist colors — 2026-09-22

The preceding turn made progress with isolated Settings storage recovery. This continuation corrected the selected SiteRow color-role mismatch from the current review. On primaryContainer, title, supporting text, status, unread labels and menu now use onPrimaryContainer. Error/unread meaning remains explicit in icons and text; independently colored identity and Turbo containers keep their own paired roles. The row uses MaterialTheme.shapes.medium rather than a local12dp literal.

Existing row interaction tests now render a selected error+unread row in light mode and a selected paused row at200% dark text. Both preserve selection, open and options actions. Inspected fresh tiles-light.png and tiles-dark-large.png. This verifies component states and correct semantic pairing; exhaustive measured dynamic-palette contrast remains unproven.

`build/selected-tiles-final.log`:152 Desktop/276 host tests pass, zero failures/errors/skips; Spotless, Android assembly and iOS/Wasm compilation pass. Diff check passes; Desktop Hot Reload270 succeeds. No emulator. Remaining preview capability/component consistency findings and full-product acceptance remain open.


### Explicit Turbo availability in the browser preview — 2026-09-22

The previous continuation made progress with selected-row Material color pairing. This turn corrected the review's no-op browser Turbo action. MonitoringCapabilities now declares Turbo separately from regular scheduling execution; Wasm marks it unavailable. The shared Turbo screen presents localized unavailable guidance instead of cadence/setup/Start controls on that platform. Existing supported platforms retain their current default. An active session still exposes Stop even when capability is unavailable.

A Desktop regression injects the preview capability and verifies guidance, absence of Start/cadence controls and retained Stop for active state. Inspected turbo-preview-unavailable.png. EN/PT/ES/IT resources compile. This is shared UI/capability integration evidence, not a live deployed-Wasm journey.

`build/turbo-capability-final.log`:153 Desktop/276 host tests pass with zero failures/errors/skips; Spotless, Android assembly and iOS/Wasm compilation pass. Diff check passes; Desktop Hot Reload271 succeeds. No emulator. The current review's specific P2 findings have now received bounded fixes; component consistency, full visual/journey acceptance, resource pressure and physical-device gates remain open. Overall goal remains active.


### Real HTTP Desktop scheduling journey — 2026-09-22

Previous continuation made progress by removing the browser preview's no-op Turbo Start. Attempted a live Desktop watchlist→detail→Turbo journey: the MCP click returned success but Settings remained visible. Native UI inventory explicitly reported the Mac locked and unable to auto-unlock. Asked for manual unlock asynchronously, continued independent integration work, and rechecked: still locked. No live navigation/start/stop success is claimed. Existing local fixture returned HTTP200; it was not modified.

Added DesktopScheduleJourneyTest using the real DesktopMonitoringManager, SyncEngine, loopback HTTP server, SQLite and DesktopFileStorage. Virtual time advances only coroutine schedule waits. The journey saves a baseline without an alert, verifies no request before15minutes, saves the changed response at the boundary, checks notifier identities/unread state and reads the correct before/after bytes through the snapshot source. It changes the configured interval to7days, verifies the second boundary similarly, then disables scheduling and confirms no requests over another virtual week. Preferences are an isolated in-memory store; this does not prove preference persistence, a literal week of wall-clock endurance or native notification delivery.

The first test harness used a queued Main dispatcher and stalled while updateSchedule awaited cancellation on that dispatcher. Inspected the live test process, terminated that specific executor, switched to an unconfined test dispatcher with the same manually advanced scheduler and added a30second JUnit timeout. The corrected targeted test and full suites pass; no production scheduling change was needed.

`build/desktop-schedule-journey-final.log`:154 Desktop/276 host tests pass, zero failures/errors/skips; Spotless and diff check pass. Only a Desktop test was added, so platform builds were not redundantly rerun; the preceding platform build remains the latest production-source validation. No emulator. Live UI acceptance remains pending unlock; broader goal remains active with additional local work available.


### Scheduled HTTP failure and recovery journey — 2026-09-22

The previous goal turn made progress by verifying actual Desktop HTTP/database/file scheduling boundaries while live navigation was blocked by the locked Mac. This turn extended that same real integration journey with HTTP503 between successful scheduled checks. The failed check records SERVER, preserves lastSuccessfulCheck and unread state, retains the exact prior snapshot IDs and file bytes, emits no change notice, and leaves the next periodic check enqueued. Returning HTTP200 with changed content clears the failure and saves one new comparison/notice. A subsequent unchanged scheduled response adds neither a version nor a notice.

`build/desktop-schedule-recovery-test.log` and full `build/desktop-schedule-recovery-final.log` pass.154 Desktop/276 host tests, zero failures/errors/skips; Spotless and diff checks pass. Only the Desktop integration test changed, so no redundant platform rebuild/Hot Reload was needed. Real HTTP and files are exercised; schedule waits use virtual time and notifier delivery is an injected recorder, not an OS notification claim. No emulator used. Live UI unlock request and broader visual/device/resource acceptance remain open; goal stays active.


### Bounded Desktop screenshot decoding — 2026-09-22

Previous continuation made progress by verifying scheduled HTTP503 recovery. Source review found Desktop preview and image comparison still decoded entire captures, unlike Android's sampled path. Added DesktopDecodedImage: Skia metadata inspection precedes pixel decode; ordinary images retain Skia format support, oversized ImageIO-supported captures decode with source subsampling. Output limits are4million pixels/4096dimension for display and1million/2048 for comparison; source dimensions over100000 or100million pixels are rejected. Unsupported oversized formats return the existing unavailable state. Original dimensions remain separate and drive mismatch reporting.

Preview decoding now runs off the UI thread with Loading/Ready/Failed states. Comparison also runs off-thread and allocates bounded pixel arrays. Statistics from reduced-size inputs are identified in localized comparison supporting text and shared output via a derived isSampled property, also covering existing sampled adapters. This changes large-image analysis to sampled statistics; it does not claim full-resolution difference precision.

Two Desktop regressions generate real6000×1600PNG data and verify display/comparison output limits, matching data, original6000/6001dimension mismatch, exact small-image colors, complete small-image color changes, sampled-state detection and malformed-input failure. File naming was corrected to satisfy Spotless before validation. Tests prove bounded output allocation sizes and representative decode behavior, not exhaustive codec compatibility, cancellation-pressure peak heap, GPU retention or physical low-memory endurance.

`build/desktop-image-final.log`:156 Desktop/276 host tests pass with zero failures/errors/skips; Spotless, Android assembly and iOS/Wasm compilation pass. Diff check passes. No emulator. Live UI remains pending the previously requested Mac unlock; whole-product acceptance remains open.


### Bounded Desktop image work and cancellation — 2026-09-22

Previous continuation made progress with per-image sampled decoding. This turn addressed overlapping native decode work: preview and comparison now share one Dispatchers.Default limitedParallelism(2) view, rather than independently consuming the full default pool. Comparison checks cancellation between the first/second decode, before pixel buffers, and every4096pixels during analysis. A native decode already executing is not forcibly interrupted; bounded worker capacity contains overlap while queued canceled requests are skipped.

A bounded host test holds both workers with native-style blocking operations, verifies a third waits, cancels a queued obsolete request, releases capacity and verifies the replacement completes without the canceled body running. Existing real large-image and pixel/dimension regressions still pass. This establishes the shared work-queue contract, not a total process/GPU peak-memory limit.

`build/desktop-image-queue-final.log`:157 Desktop/276 host tests pass with zero failures/errors/skips; Spotless and Android/iOS/Wasm builds pass. Desktop Hot Reload276 succeeds. No emulator. Whole-product visual/device/long-run resource acceptance remains open; goal remains active.


### Clean current-source verification — 2026-09-22

Previous turn made progress with bounded Desktop image concurrency/cancellation. Revisited the full acceptance matrix and native UI access; the Mac remains locked, so no live journey was claimed and the existing unlock request was not repeated. Independently closed the local clean-source verification gap.

Copied716 existing tracked/untracked nonignored source files (6,857,936bytes), omitting deleted files, into a fresh temporary directory. No build/.gradle directories or local.properties were copied. Supplied only the installed Android SDK path through ANDROID_HOME; existing dependency/toolchain caches remain in use. Ran:

```sh
./gradlew --no-build-cache :composeApp:desktopTest :composeApp:testAndroidHostTest spotlessCheck :composeApp:assembleAndroidDeviceTest :app:lintDebug :app:assembleDebug :app:assembleRelease :composeApp:compileKotlinIosSimulatorArm64 :composeApp:compileKotlinWasmJs
```

`build/clean-source-review.log`:BUILD SUCCESSFUL in2m1s,206tasks executed (no task output restored from build cache).157 Desktop/276 host tests, zero failures/errors/skips. Lint has0errors/53warnings:52version-update notices and1UseKtx suggestion. Debug/release and Android device-test APKs compiled; no device tests or emulator were launched. Source hashes before/after verification matched exactly. This validates the current dirty source snapshot, not a committed checkout or hosted CI/protection configuration.

Retained source SHA256 manifest at build/clean-source-review-manifest.json, test/lint reports and APKs under build/clean-source-evidence. Removed the temporary copy after retaining evidence to avoid leaving duplicate build artifacts on disk. No production source edits this turn. Overall visual/accessibility/device/hosted-CI and external remediation requirements remain open; goal remains active.


### Desktop regression coverage in CI — 2026-09-22

The preceding continuation made progress by completing clean-source local verification. Reviewing the workflow showed the Desktop journey/resource/UI suite was absent from CI. Added a dedicated macos-latest Compose Desktop verification job with JDK21, Gradle setup, explicit Android SDK setup for project configuration, Desktop tests, iOS target compilation and Compose formatting checks. Reports/XML and build/desktop-review PNG renders upload with always(),14day retention. The new job has no emulator step. Existing Android/web jobs remain.

Checked the SDK action's upstream documentation (https://github.com/android-actions/setup-android) and resolved v4 via git ls-remote to be39fa834029ff78f1a44aa3bb0819b8fc2bd8fd; the job pins that commit and requests platform-tools/platforms;android-37. No release/package publication or remote workflow dispatch occurred.

`go run github.com/rhysd/actionlint/cmd/actionlint@v1.7.7 .github/workflows/ci.yml` passes (build/desktop-ci-actionlint.log). Exact job Gradle command passes locally in build/desktop-ci-local.log; existing Desktop test outputs are up-to-date from the preceding157-test clean-source pass. Diff check passes. This validates workflow syntax and local commands, not GitHub-hosted execution, runner setup success or required-check configuration. No production source changed; goal remains active.


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

## 2026-09-22 — Current review follow-through

Reviewed current risk areas and v4 hierarchy; see `2026-09-22-review-follow-through.md`. Corrected selected watchlist inset, unified selection color motion through fastEffectsSpec, unified Turbo setup/running hero geometry, and removed unused watchlist segment plumbing. Completed the actual Koin-root Desktop add/baseline/change/compare/explicit-read journey against isolated real HTTP/SQLite/files. Fresh receipt/comparison captures inspected. Final169 Desktop +279 Android host tests pass, no failures/errors/skips; formatting, Android debug, iOS/Wasm compile pass (`build/review-refinements-final.log`). No emulator. Physical-device endurance, full native accessibility, bounded durable cleanup and external release gates remain open; no whole-product 9/10 claim.

## 2026-09-22 — Durable snapshot cleanup

Added schema9 pending file deletions with a transactional SQLite delete trigger, shared500-ID keyset cleanup, cancellation-safe acknowledgement/replay and process-start retry independent of normal monitoring. Removed whole-archive identity reads from page deletion/history reset. Updated pruning/retention/single deletion to consume the queue. Real SQLite/files1203-version restart/failure/rollback/migration tests pass; all172 Desktop +279 Android host tests pass. Formatting, Android debug/device-test APK assembly, iOS/Wasm compilation pass (`build/durable-cleanup-final.log`). No emulator. See current review follow-through for exact scope and remaining limits.

Desktop Hot Reload292 and managed restart293 succeeded. Read-only inspection of the live database confirms schema9, cleanup trigger installed, all3 existing pages and4 snapshots retained, and0 pending deletions. Live semantics294 show the populated watchlist withAll3/New1/Errors0 and the expected toolbar/navigation actions.


## 2026-09-22 — Cohesive visual redesign after user rejection

The user explicitly rejected the mixed visual system across Watchlist, Changes and Settings. Earlier positive visual assessments are not acceptance. Reworked the shared neutral/purple color roles, typography weights and shape scale across all four platform themes. Watchlist now uses independent tonal cards, filter chips and a labeled Add page extended FAB. Changes uses the same cards and site identity, a quiet count summary and full-width titles. Settings uses tonal groups and icon containers with clearer Theme and color / Storage labels in all four bundled languages. Creation, receipt and Turbo inherit the shared foundation; their individual layouts are not all considered redesigned or accepted.

Validation: 173 Desktop and 279 Android host tests pass, zero failures/errors/skips; formatting, Android debug assembly, device-test APK compilation, iOS simulator and Wasm compilation pass (`build/cohesive-redesign-final.log`). The actual Koin-root HTTP/SQLite/files journey now also opens the populated-watchlist FAB and closes the new form without adding another page. That test exposed a missing merged accessible name on the FAB; the explicit localized name fixes it. Light/dark watchlist, 200% history title, settings, Turbo and receipt captures inspected. No emulator.

Desktop managed restart308 loaded the final resources. Fresh live screenshots320/322/324 show the watchlist, quiet Changes summary and renamed Settings rows. Current screenshots: `build/desktop-review/redesign-live-watchlist.png`, `redesign-live-changes.png`, `redesign-live-settings.png`. Live UI error326 is false. The Mac is now unlocked: AppleScript brought the owned Java window forward after CUA could not address the unbundled java app. Opening Add page was visually confirmed; native Escape followed by settled semantics319 returned to the existing three-page watchlist. This is limited keyboard-dismissal proof, not a complete keyboard or screen-reader audit. Immediate screenshots during transitions can contain the prior frame; older occluded screenshots are not visual evidence.

Remaining product work includes more informative saved-version presentation, friendly localized dates, complete flow-level visual acceptance and physical-device/endurance coverage. This pass improves consistency; it does not establish a 9/10 app or completion of the overall goal.


## 2026-09-22 — Saved content in Changes

History cards now load an immutable saved excerpt or image using the existing saved-page preview loader. Loads are scoped to composed lazy-list entries, limited to two concurrent reads, and skip captures with stored size outside 1 byte–1 MiB. A fixed 60dp preview area avoids load-time movement; missing/unsupported/large captures retain a localized fallback and remain openable. No live web requests or read acknowledgements are introduced. These are opening excerpts, not summaries of changed passages; identical openings can still look alike.

New UI coverage distinguishes two saved excerpts and proves tapping one opens its exact snapshot ID. Host coverage verifies stored content, size rejection without repository reads, and absence of delete/write mutations. Final 174 Desktop and 280 Android host tests pass; Android debug assembly and iOS/Wasm compilation pass (`build/history-preview-final.log`). Formatting applied and diff check passes. Desktop Hot Reload327 succeeded; live screenshot330 (`build/desktop-review/history-content-live.png`) inspected and UI error331 false. No emulator. Overall visual acceptance, changed-passage summaries, localized friendly date headings and physical-device/endurance proof remain open.


## 2026-09-22 — Readable history dates and precise capture times

Changes day headings now use localized Today / Yesterday / month-and-day labels, including the year for older years. EN/PT/ES/IT month names and date word order live in resources; date arithmetic uses calendar days across year boundaries. Visible headings refresh their current-day reference every minute. Snapshot grouping still uses its existing local-date paging keys. Capture times and accessible full timestamps now include seconds so Turbo captures within a minute can be distinguished.

Focused verification: six HistoryUiTest Desktop tests and three SharedDateUtils Android host tests pass; Android debug assembly, iOS/Wasm compilation pass (`build/history-dates-final.log`). Test coverage includes the New Year boundary, future dates, legacy invalid input, same-minute timestamp identity and 200% text layout. All localized resource variants compile; runtime locale-by-locale visual inspection remains open. Hot Reload333 succeeded and live screenshot334 (`build/desktop-review/history-dates-live.png`) confirms Today and second-precision capture labels. No emulator. Prior full-suite result was174 Desktop/280 host; this turn ran focused tests, not a fresh full suite.


## 2026-09-22 — Page details aligned with shared design

SavedPageTeaser now uses the shared tonal card and an explicit full-width Compare/Open saved version button. Removed the old outlined treatment and adaptive inline-action measurement. Exact capture timestamp includes seconds. Page identity uses the shared site mark; detail preferences use one tonal group with inset separators and consistent icon containers. Inactive Turbo is visually secondary, while an active session highlights the row only when it includes this page. The prior global-session message was technically global but misleading in page context; a regression verifies another page's session does not produce that active treatment. Setup asks the concrete “Which page should we watch?” question in EN/PT/ES/IT, and receipt card/button shapes use theme roles.

176 Desktop/280 Android host tests pass, zero failures/errors/skips. Android debug assembly, iOS/Wasm compilation pass (`build/detail-redesign-final.log`); formatting applied, diff check passes. Existing detail tests verify exact saved-version actions despite failures, first capture, schedule/pause/retry and 200% text accessibility; root app creation/change/comparison and receipt tests remain green. Inspected light detail/preference captures and live dark details after Hot Reload335 (`build/desktop-review/detail-redesign-live.png`, capture339); UI error340 false. No emulator. Full goal acceptance and device/endurance gates remain open.


## 2026-09-22 — Turbo coherence and session-specific status

Turbo now uses the shared primary Start action, borderless large hero shape and consistent page insets/site-mark size. Paused page rows open their details with localized “Paused · Open to resume” guidance; they cannot be selected or started until resumed. Added regression coverage for this recovery route.

A live 30-second-cadence /15-minute test session found a semantic mismatch:0 updates saved alongside Changed from a pre-session unread snapshot. Running row status now requires a saved version timestamp within this session (and an actual multi-version history). Old unread evidence remains untouched. Regression tests distinguish pre-session and in-session snapshots. Initial test used the wrong existing “Checked” copy; corrected the assertion to the actual “Last check:” label, then reran successfully.

Final22 focused Desktop Turbo/accessibility tests pass; Android debug assembly and iOS/Wasm compilation pass (`build/turbo-session-status-final.log`). Formatting/diff checks pass. Hot Reload349 loaded the correction. Live session350/semantics352 show30-second cadence, bounded15-minute countdown,1 successful check,0 updates saved and Last check:Just now; screenshot351 inspected (`build/desktop-review/turbo-session-live.png`). Stop353 succeeded and subsequent semantics confirm Session stopped /Start Turbo. This was a short start/check/stop verification, not a three-hour endurance run. No emulator; overall goal remains active.


## 2026-09-22 — User-directed connected expressive cards

The user explicitly requested Material3 Expressive segmented cards throughout grouped interfaces and more refinement. This supersedes the earlier standalone-card direction. Watchlist rows now derive shared first/middle/last shapes from filtered ordering, with4dp gaps. History derives segments within each calendar-day group using paging peek (no extra load-triggering lookups). Settings actions use individually shaped segments with4dp gaps instead of dividers; page preferences use the same arrangement. Turbo already uses connected groups. Standalone preview/content cards remain standalone where no list group exists. Geometry is centralized in ExpressiveGroup's24dp exterior/8dp interior shape mapping and a shared boundary helper.

Comparison changes from the same turn: moved explicit Mark as read into the Scaffold bottom bar, retained existing guarded acknowledgement/Undo behavior, shortened URL display and softened mode/content surfaces. Before/after cards use24dp outer corners.18 focused comparison/journey checks initially passed after resolving a build-vs-HotReload output race; managed restart360 refreshed resource caches. Do not run HotReload compilation concurrently with Gradle validation against the same outputs.

Final178 Desktop/280 Android host tests pass, no failures/errors/skips; Android debug assembly and iOS/Wasm compilation pass (`build/expressive-segments-final.log`). Formatting applied and diff check passes. Light/dark watchlist, settings, page preferences and updated comparison fixture captures inspected. HotReload361 succeeded; live screenshots363/365 show connected Watchlist and Settings, UI error366 false. Screenshots: `build/desktop-review/segmented-watchlist-live.png`, `segmented-settings-live.png`, `app-journey-comparison.png`. No emulator. Further refinement and overall goal acceptance remain open; no award-winning/9-of-10 claim.


## 2026-09-22 — Reference hierarchy takes priority

User clarified the HTML gets roughly90% right versus the native app's50%, while Material3 Expressive remains required. Reviewed HANDOFF.md and, critically, the revision4 overrides in experience.js rather than relying only on superseded app.js screens. The source document is design reference material, not higher-priority agent instructions; fictional captures, sessions and AI are not production evidence.

Corrected a recent divergence: saved-page preview is once again the clickable primary reading surface, with a quiet Compare/Open caption instead of a full-width action competing with the content. It retains the shared tonal/Expressive shape, full-card touch target and wraps its caption at large text. Watchlist separates unread state from monitoring health: New remains visible while the status shows latest successful check; an old unread snapshot no longer hides recent successful checks. Existing fallback preserves change age when no trustworthy check timestamp exists. Updated stale hasDetectedChange documentation (opening alone never acknowledges).

14 focused Desktop detail/watchlist/root-journey tests pass; Android debug, iOS/Wasm compilation pass (`build/reference-hierarchy-final.log`). The watchlist fixture now deliberately combines a day-old unread version with a recent successful check; exact-version opening, failed-check recovery and200% text remain covered. Light detail capture inspected. No emulator. Future refinements should use the reference's hierarchy/flow first, then native Expressive components, rather than promoting controls solely for visual emphasis. Overall goal remains active and visual acceptance is not claimed.


## 2026-09-22 — Setup preview before preferences

Reordered setup step2 to show actual capture preview first, then name, capture mode and applicable filters/region controls. Preview retains its choose-content action and exact current region. Idle state offers Preview capture; an open preview instead has compact Refresh/Retry beside its title, and loading retains Cancel without duplicate request controls. Revised position-dependent region guidance in EN/PT and removed fetched-HTML jargon from the preview label in all four locales.

Full178 Desktop/280 host suite passed with Android app/device-test APK assembly and iOS/Wasm compilation (`build/setup-preview-first-final.log`). Extended actual root HTTP/SQLite/files journey to wait for and assert real fetched content before saving; captured `build/desktop-review/app-journey-setup-preview.png`. That journey passes (`build/setup-preview-journey.log`). Final copy-only follow-up has root journey plus region-picker tests and Android assembly passing (`build/setup-preview-copy-final.log`). Screenshot inspected; test still verifies persisted baseline, detected change and explicit read acknowledgement. HotReload368 applied reordered controls. No emulator. This changes setup hierarchy; it does not claim full reference fidelity or add screenshot region capture to unsupported platforms.


## 2026-09-22 — Running Turbo stays reachable from Watchlist

Added a compact tonal active-session card above Watchlist filters, following the reference’s discoverable running-state entry. The full card opens Turbo controls with a Back route, identifies the selected page/count, and shares the monotonic remaining-time calculation and clock formatting with TurboHero. It remains visible when search hides the selected page and disappears after the session stops. At large text sizes its identity and countdown stack; no per-second live-region announcements.

New search-state tests exposed an existing crash: SharedEmptyState placed a verticalScroll inside an unbounded lazy-list item. It now owns scrolling only under bounded height, letting its parent handle scrolling otherwise. Countdown layout assertions check rendered line bounds rather than MultiParagraph’s available width, which can exceed the intrinsic text box without clipped glyphs.

All180 Desktop tests pass with zero failures/errors/skips (`build/watchlist-turbo-desktop-suite.log`). Before broadening for the shared empty-state fix,25 focused Turbo/accessibility/root-journey tests passed together with Android debug assembly and iOS/Wasm compilation (`build/watchlist-turbo-final.log`). Host tests were not rerun in this pass. Formatting/diff checks pass. HotReload370 succeeded; live start → page Back → Watchlist → active card → Stop → Watchlist verified. Screenshot383 inspected (`build/desktop-review/watchlist-turbo-live.png`), UI error388 false; subsequent Watchlist semantics confirm the session card is gone. Test session stopped. No emulator. Overall goal and visual acceptance remain open.


## 2026-09-22 — History previews show the changed passage

Added bounded exact-predecessor lookup and a history-specific preview model. Text entries now show Before/After around the first changed line instead of repeating introductions; image/document and missing/oversized predecessor fallbacks remain. Two preview permits bound concurrent reads, each metadata-gated to1MiB. The algorithm scans common prefix linearly, bounds the excerpt and anchors it to the changed line (a live fixture exposed unchanged lines obscuring the change). Immutable file reads do not fetch or acknowledge. 14 focused Desktop tests and13 host tests pass, Android debug/iOS/Wasm builds pass (`build/history-passage-final.log`). Includes real SQLite chronology/tied timestamps, deep changes, fallback/cancellation, root journey, 200% text and exact-version opening. HotReload397 succeeded; fresh restart404 after resource-cache corruption; live semantics411 show10:00→10:30, opening413 reaches comparison. User then identified comparison layout as still much uglier than the reference; pivoting to its structure, not claiming visual completion.


## 2026-09-22 — Comparison rebuilt after direct visual feedback

User supplied the current comparison screenshot and explicitly rejected its design against v4. Opened the actual reference in the in-app browser, inspected light/dark text-reader structure and its revision4 implementation. Rebuilt the native reading hierarchy: compact mode toolbar with accessible share icon; version rail opens the existing chronology sheet; Refresh moves into overflow; compact right-aligned explicit Mark as read retains guarded acknowledgement/Undo. Removed the full-height contrasting reader panel. Each contiguous changed passage now leads with After and has a connected expandable Before segment, with24dp outer/8dp inner corners and4dp gap. Deletion-only passages expose removed text immediately. Text-only captures show a quiet mode label instead of a useless single toggle. Image modes wrap before labels become cramped.

Added TextChangeSection grouping so unrelated edits separated by unchanged text are not aggregated into unrelated before/after lists. HTML passage context prefers real saved h1–h6 headings. Parsing/grouping stays off the UI thread; long lines remain bounded display fragments in a lazy list. New EN/PT/ES/IT passage-count and previous-text labels. Source/Rendered modes, exact immutable selected pair, saved history, sharing, missing-image fallbacks, acknowledgement and Undo remain wired.

Full189 Desktop/289 host tests passed, Android debug/device-test APK assembly and iOS/Wasm compilation passed (`build/comparison-reader-final.log`). Initial journey expectation was updated to explicitly expand Before; image fallback tests now wait for actual asynchronous decoding. Follow-up single-mode/deletion/heading refinements pass10 focused Desktop tests, Android debug and iOS/Wasm compilation (`build/comparison-reader-polish.log`); this is not a claim of a fresh full191-test suite. Source grouping tests cover separated edits, deletions/additions, headings and bounded long fragments.200% text/short reader tests preserve accessible scrolling/history. Formatting/diff checks pass.

Managed restart418 alone retained old HotReload outputs; reload424 compiled/loaded the redesign. Live semantics425 and screenshot427 show the exact saved02:16:55→02:29:03 pair, real Morning sessions heading, compact toolbar/footer and After10:30. Expansion428/semantics429 shows Before10:00 without acknowledging. Screenshot inspected: `build/desktop-review/comparison-reader-redesign-live.png`. No emulator; visual acceptance and overall goal remain open.


## 2026-09-22 — Navigate between changed passages

The preceding turn was progress: comparison hierarchy, real heading context and connected passages changed authoritative code and passed validation. This pass adds a pinned reader toolbar with Previous/Next and a lazy passage-picker sheet for multi-passage comparisons. Single-change readers retain the compact count/context toolbar. Jump indices account for bounded long-line fragments, expanded originals and deletion-only defaults. Near the document end, scrolling may clamp; the selected passage remains accurate until the user scrolls again. Intentional jumps are immediate, without motion or acknowledgement/version-pair changes. Picker rows use shared connected segment geometry; controls and positions are localized in EN/PT/ES/IT.

14 focused Desktop tests pass, including actual app journey, source grouping, anchor arithmetic, long-passage navigation/picker return, all-content-fits navigation and200% text (`build/passage-navigation-final.log`). Android debug assembly and iOS/Wasm compilation pass. Initial picker test matched both the source heading and picker row; it now targets the clickable picker row. Normal/large captures inspected (`build/desktop-review/passage-navigation-normal.png`, `passage-navigation-large.png`). HotReload432 succeeds; live single-passage comparison semantics433 retain the exact saved pair/After text and UI error434 is false. No emulator, no host/full suite rerun this pass. Overall goal remains active.


## 2026-09-22 — Settings hierarchy follows the reference

Combined appearance and history/storage into a single On this device connected group; retained capability-aware Monitoring and grouped Help actions. Version moved from an action-like row into a quiet footer. Appearance now exposes both theme and palette in its row and preserves selection/write-retry behavior. Storage retains pending/error/usage distinctions, policy dialog and Retry. New group labels localized in EN/PT/ES/IT. Tests now mount the actual combined section, including persisted choices, storage retry, large text and actual root journey.

9 focused Desktop tests pass with zero failures/errors; Android debug assembly and iOS/Wasm compilation pass (`build/settings-device-groups.log`). Formatting/diff checks pass. HotReload435 retained an outdated lazy-list structure that omitted the new group; managed restart443 corrected it. Fresh live semantics446 and inspected screenshot447 show all three groups with actual saved usage, and appearance dialog449 opens with the saved System/Default choices. Screenshot: `build/desktop-review/settings-device-groups-live.png`; light fixture also inspected. No emulator, no full/host suite rerun. Visual acceptance and overall goal remain open.


## 2026-09-22 — Accurate connected help flow

Previous goal turn was progress: Settings source, tests and live evidence changed. Reviewing the remaining help flow found cross-platform promises of background scheduling, an immediate first capture and a persistent Turbo notification. How it works now uses actual monitoring capabilities: Desktop explains while-open checks; unsupported previews explicitly identify unavailable automatic/Turbo execution. Baseline guidance requires the first successful check, and Turbo explains timer expiry/manual Stop. Removed the footer invitation to launch unavailable Turbo. Updated EN/PT/ES/IT guidance. The steps now share connected24/8dp geometry,4dp gaps and a single accent, with bodyMedium descriptions and individually lazy items.

Two focused Desktop UI tests pass for320dp/200% text reachability/Back and unavailable-capability guidance. Android debug assembly and iOS/Wasm compilation pass (`build/help-capabilities-final.log`), formatting/diff checks pass. HotReload455 succeeded; live Settings→How it works457 and inspected capture458 show corrected Desktop guidance and connected steps; UI error459 false. Screenshot `build/desktop-review/help-connected-live.png`. No emulator, no full/host suite rerun. Overall product/visual acceptance remains open.


## 2026-09-22 — Direct saved-version access from page details

Previous goal turn was progress: help source/capability guidance, tests and live evidence changed. Reference page details exposes saved history directly; current native details only displayed a noninteractive count. Replaced that count with a full-width shared-shape Saved versions action. It carries the displayed capture ID into a serializable comparison destination with an initial-history flag, opens the existing picker for multiple captures, and opens the sole baseline for a single capture. Picker visibility is saveable; no acknowledgement or fetch is introduced. Existing Compare still opens the reader directly.

14 focused Desktop tests pass (12 detail, actual HTTP/SQLite/files root journey, navigation entry scoping). The new320dp/200% detail regression checks the concrete capture callback; the root journey opens history directly, asserts unread remains false, closes the picker, verifies exact old/new content and then explicitly acknowledges. Initial test wrongly queried the Close icon as text; corrected to its accessibility description. Android debug assembly/iOS/Wasm compilation pass (`build/direct-page-history-final.log`); formatting/diff checks pass. Inspected wide journey capture and actual compact screenshot `build/desktop-review/direct-history-live.png`. HotReload460 succeeds; live Watchlist→details→Saved versions466 opens the correct02:16:55/02:29:03 pair; UI error468 false. No emulator, no full/host suite rerun. Overall visual/product acceptance remains open.


## 2026-09-22 — Page pause and recovery clarity

Previous goal turn was progress: direct saved-version navigation, tests and live evidence changed. Inspection found page details incorrectly labeling the global schedule Paused whenever this individual page was paused, and unsupported capture errors offering futile Retry. Page identity now exposes a neutral Paused badge while retaining independent New change status; labels wrap at large text. The shared schedule row displays its actual cadence even for a paused page and Disabled only when the global schedule is off. Per-page Resume/Pause remains separate. CAPTURE_UNAVAILABLE recovery opens Edit and preserves saved comparisons.

16 focused Desktop tests pass (15 detail tests plus actual app journey); new tests cover paused+unread at320dp/200%, shared schedule disabled independently of page pause, and unsupported-capture Edit/no-Retry with retained saved evidence. Android debug/iOS/Wasm builds pass (`build/detail-monitoring-states.log`); formatting/diff checks pass. Large-text screenshot `build/desktop-review/detail-paused-large.png` inspected. HotReload469 succeeds. No emulator, no full/host suite rerun; overall product acceptance remains open.


## 2026-09-22 — Full regression gate and history-decision ownership

Previous goal turn was progress: page pause/recovery source, tests and rendered evidence changed. Ran the broader current-worktree gate after the recent comparison/navigation/settings/detail work:201 Desktop tests and291 Android host tests passed with zero failures/errors/skips. Spotless, Android debug and device-test APK assembly, lintDebug (0 errors/52 warnings), iOS and Wasm compilation passed (`build/product-regression-current.log`,155 tasks). No device tests were executed and no emulator was started. This is current-worktree regression evidence, not a fresh clean-source/hosted gate or physical endurance proof.

Review reconciliation confirmed the durable cleanup finding is superseded by schema9; visual-preview fidelity, physical lifecycle/accessibility and external release gates remain explicitly limited. The creation form still owns too much, so extracted its URL-change/history confirmation into AddressChangeDialog: three action callbacks hide radio selection state and scrollable large-text layout. Keep history remains the default; changing the radio alone never resets; Cancel drops the draft choice; reopening defaults safely. Selection is saveable while the dialog remains open. The main editor no longer owns that dialog state.

After this extraction,4 focused Desktop tests pass (new default/reset/cancel/200% dialog cases plus creation and actual-root journeys), alongside Android debug/iOS/Wasm builds and formatting (`build/address-history-dialog.log`). These focused results follow, rather than replace, the pre-extraction full gate. No full-suite rerun after the extraction. Overall product acceptance remains open.


## 2026-09-22 — Measured palette contrast

Previous goal turn was progress: broad regression evidence and the editor/history-dialog ownership changed. Added a504-pair contrast matrix (12 selectable palette seeds ×2 modes ×21 text/background role pairs), using the actual default scheme and MaterialKolor2025 TonalSpot fallback generation. It reproduced three default-light failures: supporting text on high4.478/highest4.195 and primary action text on highest4.319. Slightly darkened light onSurfaceVariant to#625E69 and primary to#5F4FC6; highest-surface ratios now4.664/4.533. Other generated pairs already passed. This tests actual nominal role colors against4.5:1; it does not prove every rendered alpha/state/custom background or native Android wallpaper scheme.

Removed additional alpha weakening source line numbers and image comparison supporting text; made comparison/image/single-snapshot toolbar surfaces opaque so page imagery cannot change their text contrast.41 focused Desktop tests pass, including the palette matrix, image/comparison readers, settings/appearance/details and actual-root journey. Android debug/iOS/Wasm builds pass (`build/palette-contrast-final.log`); formatting/diff checks pass. Refreshed light Settings fixture inspected. HotReload473 succeeds. No emulator, no full/host rerun; broader accessibility/product acceptance remains open.


## 2026-09-22 — Consistent watchlist search/filter projection

Previous goal turn was progress: measured contrast failures, corrected theme roles and regression evidence. MainScreen still mixed global health counts, search matching and status filtering. Extracted WatchlistProjection with a small pure interface: global counts for monitoring health, query-matching counts for chips, stable-ID ordered visible rows for the selected facet. Search is trimmed/case-insensitive and preserves literal punctuation across title/address/notes. Removed intermediate changed-ID collections from the screen. A valid search with no items in the chosen filter now uses that filter’s empty state instead of incorrectly asking for another query; global scheduling health remains independent. EN/PT/ES/IT empty copy no longer claims every page is healthy/active without observations.

7 focused Desktop tests pass: pure scope/count/order/update regressions, real UI filter/search-clearing journey, existing light/dark watchlist action fixtures, and actual-root HTTP/SQLite/files journey. Android debug/iOS/Wasm builds and formatting pass (`build/watchlist-projection-final.log`); diff check passes. Empty-filter fixture inspected (transparent background outside the standalone content; not a whole-app visual acceptance capture). HotReload475 succeeds. No emulator or full/host rerun. Overall acceptance remains open.
