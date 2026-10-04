# v6 redesign review — 4 October 2026

The direction is strong. Home answers “what changed?” quickly, the previews keep noisy pages under control, and the paper/ink/marker system makes the app distinctive without fighting familiar navigation. The text reader is the strongest screen: changed passages have context and unchanged material stays quiet. The original implementation needed interaction and accessibility fixes before shipping; its normal-size screenshots hid several of those problems.

Visually, the hierarchy, restrained color and readable content treatment feel coherent and polished. The main journey—watch a page, see a change, compare it—is intuitive. Secondary actions needed clearer entry points, and settings needed more honest scope and platform language. After these fixes, large text, long pages and recovery states fit the same design standard as the happy path. The fixes preserve the redesign’s direction.

Reviewed the actual shared Compose implementation and rendered flows: first run, Home, page selection/search, Activity, page details and version history, text/source/image comparison, Turbo setup/live/outcome, add/review/receipt, automatic checks, appearance, About, How it works, and licenses. Review covers compact phones, enlarged text, a wide list/detail window, light/dark appearance, keyboard/screen-reader semantics, and error/loading/empty states.

| Area | Finding | Implemented correction |
| --- | --- | --- |
| Home | Batch actions required discovering long press; large type hid page identity/time and clipped the heading. | Explicit Select pages menu action, empty selection mode with disabled actions, spoken selection counts, adaptive heading and page metadata. |
| Activity | No saved versions could conceal failing pages; paging failures looked empty; deletion was hidden behind long press. | Failures stay visible, loading errors expose Retry, and each saved version has a visible actions menu. |
| Delete and Undo | Success was announced before deletion finished. Timed backups could expire while Undo was still visible; failed restore could throw silently. | Await real deletion, retain the backup through the snackbar lifetime, restore bytes before the row, and offer Retry after a failed restore. |
| Details | Failed checks looked successful; switches did not expose state; small targets and wide action bars weakened usability. | Explicit failed status, one accessible toggle per row, adaptive action layout, bounded desktop width, and an accurate spoken calendar summary. |
| Schedule | “How often” on a page could appear to change only that page. Late settings initialization lost Custom state. | App check schedule label and explicit global scope; correct late draft initialization, saving locks, and capability-aware controls. |
| Reader | Change tracking advanced too early and got stuck near the bottom; switching versions retained state; context could not be collapsed. | Reliable immediate navigation, version-specific reader state, 48dp fold/expand/collapse controls, and stacked navigation at large text sizes. |
| Long changes | Expanded content became one enormous item; oversized source lines could make text layout expensive. | Lazy context and changed-line items, bounded source-line fragments, and an explicit collapse action. |
| Saved first version | Opening a real baseline from history could falsely report an unavailable comparison. | Display its actual saved content and preserve its identity through Retry and pruning. |
| Screenshot comparison | Tall pages shrank into unreadable thumbnails; Highlight could silently turn into Slider; size differences showed misleading statistics. | Full-width vertically scrollable captures, synchronized Slider scrolling, Retry for failed loading/highlighting, truthful size mismatch explanations, and space for screenshot labels. |
| Change previews | Blank edits could displace real content; replacements could report more changed lines than existed; insertion/deletion spacing was lost. | Choose meaningful sections, count replaced lines once, preserve boundary spaces, and give screen readers explicit Before/After descriptions. |
| Add/edit | Inputs and navigation remained active while saving; Continue left the keyboard open; connected controls could clip. | Lock pending saves/dismissal, hide the keyboard on Continue, and share measured adaptive choice controls. |
| Turbo | Paused selections could target the wrong pages; ended sessions could claim later normal changes; countdowns implied unavailable per-page timing. Start could scroll offscreen. | Start only selected active pages, bound saved outcomes to the session, use authoritative counts/cadence, show real checking state, and pin Start beneath independently scrollable setup content. |
| Shared presentation | Recovery actions and settings descriptions clipped at large type; native notification controls appeared on unsupported platforms. | Wrapping recovery actions, readable descriptions, proper toggle semantics, and capability-based notification controls. |
| Appearance and help | Wallpaper colors were exposed where unsupported; some dialogs and license titles clipped. New strings mixed English into other languages. | Platform-aware colors, readable palette selection, bounded/scrollable dialogs, and Portuguese/Spanish/Italian translations with matching keys and arguments. |
| App identity | The launcher, splash and notification mark still used the previous visual language. | Android vector marks now match the in-app ink and marker design, including the themed monochrome icon. |

## Verification

Passed verification:

- **198 desktop tests**, including all five rendered review tours and regressions for selection, accessible/error states, reader navigation, baseline loading, tall screenshots, preview content, saving, delayed settings and Turbo outcomes.
- **306 Android host tests**, including preview, comparison read state, delete/Undo and sync coverage.
- Android debug app and device-test APK assembly; Android lint with **0 errors** (58 existing dependency-update/style warnings).
- iOS simulator Arm64 compilation and web/Wasm compilation.
- Formatting checks and Portuguese/Spanish/Italian parity for all **235 v6 resource keys**, plural quantities and arguments; no duplicate resource names.

The desktop captures render the real shared application with local fixtures at 1080×2340 pixels (density 2.625), in light/dark appearance and at 1.8× text size. The wide review uses 1680×1050 pixels (density 1.25). There are 74 captured screens across these tours.

An earlier native run passed 42 of 49 tests. Its seven failures came from outdated capture-editor flows/labels and reconstructed old database fixtures retaining newer schema fields. Those tests were corrected against the current flows and exported schema history. The emulator was stopped at the user’s request; the native corrections have not been rerun. Final verification uses desktop Compose and web compilation.

Gradle now uses at most two workers, and both Gradle and Kotlin JVMs see two processors. The final web compilation and formatting checks passed with this configuration.

Usability judgments here are based on code, interaction tests and rendered screens. They are not a substitute for observing first-time users. Android screenshot capture remains platform-specific; desktop/web retain the explicit unsupported-capture explanation.

## Rendered review sheets

- [Light appearance](../design/v6-reviewed-light.jpg)
- [Dark appearance](../design/v6-reviewed-dark.jpg)
- [1.8× text](../design/v6-reviewed-large.jpg)
- [Wide list/detail window](../design/v6-reviewed-wide.jpg)
