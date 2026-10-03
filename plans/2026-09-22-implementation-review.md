# Implementation review — 2026-09-22

**Verdict: Blocked for final product sign-off.** The redesign is improving, but the full functional and usability goal is not yet achieved. This is a review verdict, not a request to stop the active goal.

Scope: current working tree against HEAD, including new common/platform files; monitoring/capture/storage seams; creation, watchlist, comparison, settings, recovery and Turbo; supplied v4 screenshots and current Desktop renders. Separate standards and requirements reviews were followed by verification of their findings. This was not a claim that every line or every device state has been exhaustively tested. No emulator was used.

## Current disposition after follow-through

- **Fixed with regression coverage:** streaming response cap, cancellable retention batches, truthful notification capability, cadence labels and typed/localized preview failures.
- **M3/accessibility refinements verified in bounded Desktop cases:** semantic metadata/navigation sizing, narrow large-text detail layout, explicit region-selection semantics, comparison reader space, RTL swipe geometry and compact image-result states.
- **Desktop browser follow-through:** bundled JCEF startup and saved-HTML loading are now fixed and checked by a real native smoke task (including rendered pixels and reopen/disposal). The actual app reaches its main-frame Done state. Full-window visual capture remains unverified: a pre-browser watchlist control also captured black, so this is not evidence that the browser breaks the whole window. See the latest follow-through below.
- **Open requirements:** the complete selection workspace (basic rendered selection and the source-text picker are implemented; live-page fidelity and redraw polish remain), whole-flow motion/keyboard/screen-reader/RTL coverage, real-device Android lifecycle/notification endurance, and very-large-archive performance.

The findings below record the original review; the follow-through section records their implementation and validation. They are not all still open.

## Additional selection correctness review

**P1 — Exclusions could silently change the watched element — fixed.** TextRegion previously removed ignored nodes before resolving the watched selector. Ignoring an earlier sibling shifted `nth-child`; removing descendants could also change `:has` matching. Both rules now resolve against the original DOM. Ignored selected elements and selected descendants of ignored ancestors are rejected before mutation. Outermost surviving regions retain source order, and an entirely empty readable result fails visibly instead of becoming a healthy capture.

Region capture policy is now v2. Existing v1 captures establish a new baseline on their next successful check, without a false change notification or deletion of history. Three extraction regressions failed against the old code; they and a SyncEngine upgrade regression now pass. Current validation: 263 Android host tests and 83 Desktop tests, zero failures/skips, plus Spotless and git diff checks (build/region-source-identity-final.log).

### Remaining priorities for M3 Expressive acceptance

1. Complete source-selection controls: understandable watched/ignored states, scope adjustment and undo. The current Page/Text list picker is only the basic selection journey.
2. Review every main flow at narrow widths and large text with real content. Bounded regression cases pass; they do not establish whole-product visual acceptance.
3. Verify keyboard focus, screen-reader announcements, RTL and disabled/reduced motion through complete journeys. Keep essential state changes understandable without animation.
4. Keep expressive emphasis concentrated on primary actions, selection and Turbo. Preserve the lighter divided watchlist, semantic type roles, readable metadata, generous action targets and quiet reading surfaces. Extra container shapes and perpetual motion would add noise without making the app easier to operate.
5. Validate actual Android notification and background lifecycle endurance on hardware. Host tests and platform compilation cannot establish delivery or scheduling reliability under OS restrictions.

## Standards

### P1 — HTTP response limit applied after buffering — fixed

`composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/sync/SyncEngine.kt:407`

The prior `httpClient.get` used Ktor's buffered response path. Installed Ktor bytecode confirmed the body was saved before the later Content-Length and channel-size checks. A large or endless response could consume unbounded memory before the claimed 10 MiB limit took effect, with additional pressure during Turbo.

Changed to `prepareGet(...).execute { response -> ... }`, keeping status/header checks and the capped read inside the streaming response lifetime. A regression test supplies more than the limit without closing the channel: the old implementation timed out after five seconds; the fixed implementation returns CONTENT_TOO_LARGE without waiting for EOF. The scope also releases the response on early rejection/cancellation.

### P2 — Retention work is too broad inside NonCancellable — fixed; performance limits remain

`composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/storage/SnapshotMaintenance.kt:30–51`

Every saved version loads/sorts/groups the archive while holding a write transaction, then deletes obsolete files and may scan orphans, within NonCancellable. A large legacy archive or slow disk can delay other writes and Turbo cancellation. The persistence safety intent is valid; the boundary is too broad. Move archive planning and file cleanup into bounded/cancellable maintenance and keep only the minimal consistency-critical commit protected. Preserve newest-pair safety and orphan recovery. This is an architectural/resource-bound finding; no measured stop-latency claim is being made.

No project-specific coding standards beyond configured tooling were found. The formatter initially failed on accumulated edits; repository Spotless formatting was applied and its check now passes.

## Spec

### P1 — Intuitive direct region selection remains missing

`composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/addedit/AddEditConfirmComponents.kt:407–409`
`composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/addedit/TextRegionFields.kt`

The goal explicitly includes intuitive creation and selection. The implemented path offers whole-page/main-content presets and raw CSS selectors, followed by a read-only preview. It does not yet provide the reference's direct source-page selection journey. Keep CSS under Advanced and implement a backed picker with selected-region preview, remove/reset, missing-region recovery and persisted rules. This is functional work, not a surface-polish item.

### P2 — Desktop exposes ineffective notification actions

`composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/main/SiteOptionsBottomSheet.kt:106–110`
`composeApp/src/desktopMain/kotlin/com/bernaferrari/changedetection/di/PlatformDiModule.desktop.kt:48`

The menu permits enabling notifications while Desktop injects NoOpChangeNotifier. Persistence of a preference does not make the advertised action functional. Implement notifications or capability-gate the control with an honest explanation. Desktop being a development host does not require platform parity, but visible controls must remain truthful.

### P2 — Page detail still formats long schedules as minutes

`composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/ui/detail/PageDetailScreen.kt:190`

Settings now says “Sync every 7 days”; detail still formats 10,080 minutes. Share one exact plural-aware cadence formatter across detail, watchlist and settings. The detail label already states that regular checks apply to all pages; the independently confirmed flaw here is inconsistent units, not a missing scope label.

### P2 — Preview errors bypass localization and curated recovery copy

`composeApp/src/commonMain/kotlin/com/bernaferrari/changedetection/addedit/AddEditViewModel.kt:111–117`

Timeout and fallback strings are English, and arbitrary exception messages can reach CapturePreviewPanel. Use typed preview failures with localized UI copy, preserving cancellation and meaningful Retry/Edit choices. Validate failure journeys in the supported locales.

No clear scope creep was identified. The reference's fictional billing, AI and example data were not treated as compulsory product features.

## M3 Expressive and visual review

Context: Android compact/medium/expanded interfaces, with Compose Desktop used for iteration. Recommended intensity: foundational expression in everyday navigation, stronger emphasis for the primary add/start action and Turbo cadence. One or two focal elements per screen.

| Area | Judgment and refinement |
|---|---|
| Watchlist | Keep the latest light, divided rows. Do not restore thick connected card blocks the user rejected. Preserve the 48dp options hit target and explicit state semantics. |
| Typography | Fixed: removed 11sp overrides on body metadata in favor of semantic bodySmall (12sp/16sp). Small labels can remain small; essential status copy should not be reduced merely to imitate a screenshot. |
| Compact navigation | Fixed: restored 24dp outline icons and labelMedium (12sp) while retaining the quiet background and violet selected roles. The standard bottom safe-area inset remains. |
| RTL | Fixed the page-detail external-link glyph to its AutoMirrored variant. A complete RTL journey is still unverified. |
| Shape | Use expressive shape changes on actions/selection, with restrained containers for reading. Connected groups suit cadence choices and related settings, not every watchlist entry. |
| Turbo | The cadence-led olive panel is a useful focal point. Keep the bounded duration visible, use tabular countdown digits, and preserve the original deadline when controls change. Do not add idle pulsing or celebratory perpetual motion. |
| Detail at large text | Current 200% render breaks a long page title mid-word and pushes saved evidence well down the page. Use an adaptive headline role and review header density at narrow widths; do not silently lower system font scale. |
| Comparison | Before/After identities and explicit acknowledgement are improvements. Short-height + large-type layouts still need a full workspace test; isolated header tests do not prove the reader retains useful height. |
| Creation/receipt | Keep the real captured document as the focal point and one primary completion action. Direct selection is the major missing interaction. Do not fake a source-page screenshot from plain text. |
| Settings | The narrow 200% test now proves labels/values are not ellipsized. Long translations, state-saving errors and capability messaging need broader verification. |
| Motion | Existing Compose animations and tabular Turbo digits are a sound base. Apply expressive motion to meaningful press/selection/navigation feedback, not constant decoration. System duration scale and zero-motion behavior need final coverage across the full flow. |

Token basis: `.agents/skills/material-3-expressive/references/m3-navigation-bar-specs-tokens.md` (`md.comp.nav-bar.item.icon.size`: 24dp, vertical label text: 12pt); `m3-typography-type-scale-tokens.md`; semantic color/shape/state roles from the project M3 skill. No claim of complete M3 compliance or a 9/10 score is justified yet.

## Evidence

- `build/streaming-cap-red.log`: regression fails on the old buffering path with TimeoutCancellationException.
- `build/review-fixes-validation.log`: 259 Android host tests and 57 Desktop tests passed; iOS simulator-target compilation and Wasm compilation passed. Compiling an iOS simulator target does not launch a simulator.
- `build/cross-platform-audit.log`: preceding full Android lint + host/iOS/Wasm verification passed. Lint reported 31 warnings, predominantly dependency-update notices; those do not justify upgrading dependencies during a UI review.
- `build/review-format-fix.log`: Spotless apply/check passed after the semantic fixes. No semantic changes were intended by formatting.
- Current Desktop renders inspected: watchlist-reference-dark, turbo-setup-light, detail-large-dark; earlier creation, receipt, comparison and settings captures also informed the review.
- `git diff --check`: clean.

Still not proven: full visual acceptance, direct region selection, all platform capability messaging, end-to-end notification/lifecycle behavior on the final build, full keyboard/TalkBack/RTL/landscape/large-text matrix, large-archive cancellation/resource behavior, external credential revocation and hosted CI protection. These remain in the original goal.

Standards: two substantive findings (one fixed, one remaining). Spec: four remaining findings. Worst remaining Standards concern is retention cancellation; worst Spec gap is direct selection.

## Follow-through after review

The long-schedule-unit finding and preview-localization finding have now been addressed: one cadence formatter serves all three screens; typed preview failure causes resolve localized recovery copy. `build/review-followthrough.log` and `build/cadence-journey-validation.log` record passing host/Desktop checks, including retry and the seven-day detail-to-schedule journey. The remaining Spec gaps are direct selection and Desktop notification capability. The retention finding remains open.

Retention follow-through: broad NonCancellable maintenance has been replaced with cancellable SQLite-planned deletion batches (maximum 500 IDs), with recovery tests for cancellation after commit. The whole archive is no longer materialized in app memory during retention. See SnapshotMaintenanceDatabaseTest and build/retention-platform-final.log. This addresses the cancellation/materialization finding; very-large-archive query performance and individual synchronous filesystem-call latency remain unmeasured limits.

Notification follow-through: the ineffective Desktop notification action is now capability-gated through ChangeNotifier. Unsupported platforms explain that changes remain available in the app; supported platforms retain preference controls and expose system notification settings. Android callbacks are wired to permission handling and application notification settings. The 62-test Desktop suite and Android/iOS/Wasm compilation passed in build/notification-capability-final.log. This closes the misleading-control finding; it does not implement Desktop delivery or prove Android runtime permission behavior.

Current disposition: both substantive Standards findings have implementation fixes with regression coverage. Three of four Spec findings are addressed; direct region selection remains open. M3 refinements still requiring work include narrow/large-text detail hierarchy, useful reader height in short comparison windows, and complete motion/accessibility/RTL journey validation. No overall 9/10 or full completion claim is made.

Large-text detail follow-through: semantic title roles now adapt to reading width without changing system font scale, an empty error-item gap is removed, and saved-version footer metadata stacks above its action when needed. The 280dp/200% Desktop render and word-boundary/navigation regression pass; full Desktop suite is 63 tests and Android assembly passes (build/detail-adaptive-validation.log). This addresses the observed detail title/footer defects, not the entire accessibility matrix.

Selection follow-through: a source-backed text picker is now wired through preview, selection, confirmation and refreshed selected-content preview. Model/UI tests plus a real Desktop fixture journey pass, with existing save-path tests remaining green. This is progress on the direct-selection finding; it does not close the reference's broader browser-rendered selection requirement. Enumeration limits and the source-only nature are disclosed in the UI. Remaining refinements include selected-region summary/indicator and large-text coverage.

Picker refinement follow-through: visible radio selection and radio/group semantics replace color-only feedback; a plain-language rule summary keeps CSS behind Advanced. A 280dp × 480dp/200% text journey verifies scrolling to a choice and confirmation with pinned actions. The 71-test Desktop suite, Android assembly and live Desktop Hot Reload passed. Full keyboard/RTL/screen-reader coverage and browser-rendered selection remain open.

Short-workspace follow-through: comparison controls/chronology are capped at 45% of available height and scroll independently, preserving a usable reader. Full DiffFirstContent at 280dp × 360dp/200% verifies changed-row navigation, chronology access, reader bounds and opening history. The 72-test Desktop suite, Android assembly and Hot Reload pass. Image modes and the complete outer-screen accessibility matrix remain unproven.

Image-workspace follow-through: available individual saved images remain viewable when their counterpart is missing; swipe supports focus feedback, keyboard control and finite accessibility progress, with the hit region retained inside viewport bounds. Desktop regression/render checks and Android/iOS/Wasm compilation pass. Pixel-diff short-height layouts and RTL label ordering remain open.

RTL swipe follow-through: a reproduced label/coordinate mismatch is fixed using absolute image-space alignment without overriding the interface text direction. RTL layout, keyboard movement and actual Before/After pixels are covered; 76 Desktop tests, Android assembly and Hot Reload pass. This is bounded swipe verification, not full RTL acceptance.

Pixel-diff follow-through: a bounded summary/disclosure area preserves image space, and size changes use a compact action with full dimensions in a dialog instead of a cropped error banner. The 280dp × 200dp/200% journey, 77-test Desktop suite, Android build and Hot Reload pass. Other image-state and whole-product acceptance gates remain open.

Image-status follow-through: missing/identical explanations use a compact scrolling panel with explicit semantic colors; identical results no longer duplicate a summary above the explanation. Large-text state journeys and render inspection pass, along with 79 Desktop tests, Android assembly and Hot Reload. Overall completion remains unproven.

Desktop browser follow-through: lazy KCEF startup, launch/package JVM access flags and a direct current-Compose Swing bridge replace the incompatible library bridge. Bundled-runtime initialization bypasses KCEF’s progress callback, so successful return also advances readiness. Browser instances are disposed on navigation. A saved-text fallback avoids an indefinite blank view after a 20-second load failure. Native rendering remains an open P1: the clean-process journey did not reach the main-frame load callback. During later Hot Reload, JBR crashed in VM_EnhancedRedefineClasses; this is separately recorded in build/browser-direct-runtime.log and must not be described as successful live verification. Build/browser-review-final.log records 79 passing Desktop tests plus Android assembly and iOS/Wasm compilation; build/browser-review-format.log records Spotless success. No emulator was used.

Final live follow-through: a fresh Gradle hotRunDesktop (which regenerates the Hot Reload classpath) successfully displayed the localized 20-second fallback and the complete fixture saved text; the Compose semantic tree verified both. Reusing an earlier argfile had reused stale Hot Reload classes and is not a valid fresh-build test. Native HTML load is still unverified; the fallback is an implemented recovery path, not a substitute for the reference’s rendered-page selector. Final Desktop suite remains 79 passing tests (build/browser-fallback-final.log); Spotless and git diff checks pass.

Visual limitation: browser-saved-text-fallback.png is also entirely black even though the semantic tree contains the fallback and saved text. Therefore only fallback state/content is verified; actual visible recovery is NOT accepted. The native integration may affect the whole Compose rendering surface, not just the embedded page. Keep this P1 open.

### Bundled browser root cause and native regression — 2026-09-22

A minimal native probe first failed because Chromium could not find ICU resources. JetBrains JCefAppConfig supplies the framework/helper paths. Passing these through the old KCEF initialization path still left the app in INITIALIZING; the bundled-JCEF branch now initializes CefApp directly using the runtime configuration, and reports Ready only on its actual initialization callback. Non-bundled runtimes retain KCEF installation support (not live-tested here). Startup has a 20-second failure deadline. Browser/client lifetime is isolated in DesktopBrowserSession, popup suppression is respected, disposal is idempotent, and app shutdown disposes the runtime.

The native smoke task uses the same runtime/session code as the app, sanitized synthetic HTML, an isolated temporary cache and two open/read/close cycles. The old initialization path was temporarily restored: the task failed with a 15-second load timeout (build/browser-native-smoke-red.log), then the fixed source was restored. The fixed task passes (build/browser-native-smoke-green.log) and saves actual Chromium-rendered pixels to build/desktop-review/native-browser-smoke.png; the capture was inspected and shows the expected heading with accented text. The real saved Workshop page also reached Done through the shared app’s browser route.

Separate observation limits: macOS presented a shared Java restore-windows modal after the earlier JVM crash; a development-only accessory-process launch let the diagnostic proceed. Whole-window MCP captures remain unreliable even on the watchlist before opening a browser (watchlist-browser-control.png). They are not proof of a native-browser regression. Compose-test captures and Chromium-native captures remain usable. Browser-rendered direct region selection, full-app visual acceptance and non-bundled/platform lifecycle coverage are still open.

Validation: build/browser-integration-final.log passes 79 Desktop tests, native browser smoke, Android assembly and iOS/Wasm compilation. Native capture addition also passes in build/browser-native-smoke-green.log. No emulator was used.

### Rendered text-region selection — 2026-09-22

Added Page/Text list views to the existing picker. The Page view renders sanitized fetched HTML with annotated candidate IDs computed against the original source tree; a SHA-256-authorized app script handles click/Enter/Space, highlights the selected element and sends only its opaque index through a rejected navigation request. Source scripts/handlers/resources and forged candidate attributes are removed. The native side validates the exact origin and bounded index before mapping back to the original selector. Selection remains a draft until confirmation, survives switching between views, and then feeds the existing selected-capture preview/save path. Existing exclusions are preserved by the outer editor callback. Empty candidate sets stay in the explanatory text view; enumeration limits are disclosed in both views.

Android/iOS adapters use the library request interceptor; Desktop uses its native request handler. Wasm stays on the text list because iframe navigation interception is not proven. The rendered preview intentionally remains inert fetched HTML: external styles/JS layouts are not reproduced. This does not close the reference’s complete selection workspace: direct exclusion, multiple areas, Smaller/Larger, Undo and live-page visual fidelity remain open.

Evidence: 83 Desktop tests and 259 Android host tests pass (build/region-selection-host-final.log). Model tests cover duplicate IDs/source positions, script and attribute stripping, strict navigation IDs and restoration of highlight without changing the script hash. The real native smoke task clicks a candidate through the browser script, receives #stock through the same native callback used by the app, and captures the highlighted region in native-region-selection.png. The 280dp × 480dp/200% UI test checks useful preview height, selection, mode switching and explicit confirmation; native rendering is independently exercised rather than faked in that test. build/region-rendered-final.log also passes Android assembly and iOS/Wasm compilation.

Live Desktop: edit Workshop schedule → Preview → Choose text → Page reaches Done → Text list → opening paragraph → Page reaches Done → confirm → capture preview contains only the paragraph. Navigated back without saving fixture settings. Native pointer-to-rule behavior is proven by the smoke check; the live app journey used the list for the pointer action because MCP exposes Compose semantics, not embedded-browser elements. No emulator was used. Whole-app visual acceptance and Android/iOS physical interaction remain unverified.

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
