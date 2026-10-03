# Turbo refinement — 2026-09-08

Intent: make a short, intensive monitoring session understandable at a glance. The broader reliability implementation remains active in `implementation-progress.md`.

Intensity: Excellent, with one hero moment: a tonal session panel. Phone-first, one scrollable content column capped at 720dp; a persistent bottom action stays outside scrolling content. Large text wraps cadence controls to two columns. Running rows open their results; Edit explicitly enters page-selection mode.

Components: primary-container surface, connected toggle-button group, native checkbox, native filled button with expressive press shapes, standard app bar and dialog. Stop uses secondary-container emphasis; stopping is routine, not destructive. The countdown and progress track use the real session deadline, and the completed-check count comes from the runner.

Token references: `.agents/skills/material-3-expressive/references/m3-button-groups-specs-tokens.md`, `m3-buttons-specs-tokens.md`, `m3-motion-physics.md`, and `m3-motion-foundation-tokens.md`. Compose mapping uses `MaterialTheme.colorScheme`, typography, shapes and native Material3 button/toggle shapes.

## Motion review

| Before | After | Why |
| --- | --- | --- |
| Whole configuration panel crossfades into a differently sized live panel | Stable page structure; content changes immediately | Avoid two overlapping layouts and a delayed response to Start/Stop |
| Decorative multicolor signal rail plus a central spinner | Removed | It represented no measured activity and competed with real results |
| Custom checkbox crossfade and per-row color animation | Native checkbox with native touch feedback | Standard accessibility semantics and predictable selection |
| Several simultaneous live/checking signals | A progress indicator only on the page actually checking | Motion now identifies real work |
| Large full-panel Stop in an error color | Fixed bottom action using semantic secondary color | Stopping is always reachable and reads as routine session control |

Motion verdict: removed the feel-breaking patterns. Native Compose interactions follow system animation scale, including scale 0; there is no custom perpetual decorative animation, entrance choreography or animated digit layout. The real clock updates text once a second without declaring an accessibility live region.

Validation: three emulator UI tests cover eligible-page selection/cadence, opening a running result and stopping, and control reachability at 1.8× text. Screenshot review and the broader service/lifecycle verification are tracked separately; UI tests alone do not establish background reliability.

## Second refinement

The timer and cadence now share one tonal panel. Setup displays the three-hour duration in the same clock slot as the running countdown; the timer uses displayMedium rather than displayLarge. The cadence control no longer sits beneath two differently sized hero layouts. A device regression measures cadence bounds across Start and Stop. No decorative animation was added; idle screens have no ticking coroutine.

The interrupted September 8 device run failed because another UiAutomation service owned the emulator. Its result is not evidence of a product failure or a passing redesign. September 19 verification uses an independent Resizable_Experimental instance on emulator-5560; the user's existing emulator-5554 is untouched. Current validation results will be recorded in implementation-progress.md.

## Timer and cadence separation — 2026-09-19

Following the request to redesign Turbo again, replaced the oversized primary-container hero with a centered typographic timer on the page surface. A separate low-emphasis surface groups the cadence controls. Idle mode labels the three-hour session limit; active mode reveals actual remaining-time progress in a reserved slot. Removed the repeated duration caption under the action. Selected pages use a tonal fill with 150 ms interruptible color feedback. Turbo locally uses Material's standard motion scheme to reduce control bounce while retaining expressive connected-button shapes. The clock never animates its digits or moves the controls when starting/stopping.

Compact Android layout, semantic dynamic colors, 52 dp cadence targets, pinned 56 dp minimum action. Large text wraps cadence into two rows. Compose animation duration scaling applies to the local color transition and native controls. No decorative looping motion. References: project skill `m3-button-groups.md`, `m3-motion-specs.md`, and Material color/shape/type tokens through `MaterialTheme`.

## Compact session panel — 2026-09-19

Latest user-requested redesign: a left-aligned, neutral session panel replaces the oversized centered green clock. A small primary-container bolt tile identifies the session. The timer uses displayMedium and onSurface; the real remaining-time track stays in a fixed slot. Cadence sits directly on the page, with 2dp spacing in the native connected group. Page rows retain quiet surfaces; checkboxes carry selection without whole-row color transitions. Start/Stop uses a fixed extraLarge shape and native ripple, removing its expressive shape morph. The native cadence selection remains the single small shape interaction, with the local standard motion scheme.

Both setup and running captions are measured so wrapped large text cannot move the controls. Hidden captions are excluded from semantics. Digits update directly, without animation, and there are no entrance or decorative looping effects. The timer only ticks during an active session. Semantic color and shape tokens remain compatible with dynamic color and dark mode.

Validation: Android debug and device-test APKs, iOS simulator and Wasm compilation passed. All 178 host tests passed before the final presentation-only caption/button adjustment. Six Turbo device tests pass, including stable cadence bounds across Start/Stop at both normal and 1.8x text. Screenshots reviewed for setup, running/dark and large type. This is a UI refinement; it does not establish additional background endurance guarantees.

## Frequency-first layout — 2026-09-19

The latest redesign makes frequency the first decision in one compact session surface. The three-hour clock is secondary title-sized information, alongside its label; it no longer dominates setup. The idle progress track is invisible and excluded from accessibility semantics. Live progress reflects the actual deadline. Both timer labels and both session captions reserve their larger bounds to keep page selection anchored across Start/Stop, including wrapped text.

Connected cadence choices now retain fixed corners and update selection immediately, with clipped native ripple feedback. This intentionally trades Expressive's optional selection morph for steadier frequent controls. The design retains semantic Material colors, expressive type hierarchy, connected 2dp spacing, 56dp minimum cadence targets, and the pinned primary action. Native checkbox feedback remains; no decorative animation or animated clock is introduced. Two-column controls accommodate large text.

Motion review: pill/square cadence morph → fixed connected geometry; dominant idle timer/progress → compact session metadata with active-only progress. Existing quiet Start/Stop feedback remains. Token references: project `m3-button-groups.md`, `m3-buttons-specs-tokens.md`, MaterialTheme color/typography/shape tokens. This presentation change does not alter session lifetime or scheduling.

## Frequency tiles and compact session status — 2026-09-19

Responding to the latest request to redesign Turbo again: removed the large enclosing configuration card, repeated idle clock and divider. Frequency is now a four-tile choice directly on the page, with headlineSmall type, primaryContainer selection and a checkmark. A separate quiet session strip explains the automatic stop and unchanged normal schedule; running mode uses the actual countdown and completed-check count. Both states are measured to preserve the position of controls. Large type wraps tiles into two columns.

Motion is restricted to a 150 ms interruptible color transition and native clipped ripple. There is no animated size, shape, digit movement or entrance choreography. Radio selection uses selectable semantics and one group across both rows. Start/Stop remains pinned, with a fixed shape. Compose animation scaling provides the reduced-motion path.

Intensity: foundational with one expressive emphasis, the frequency choice. Semantic Material color, typography and large-shape tokens preserve dynamic color and dark mode. References: project skill m3-motion-specs.md (standard utilitarian motion and fast effects), m3-buttons-specs-tokens.md, and m3-color-foundation-tokens.md. This is a deliberately restrained custom radio tile layout, not a claim to implement the connected-button shape-morph specification.

Android build and all seven Turbo UI tests passed; screenshots inspected in light, dark/running and 1.8× text. Reduced-motion and cross-platform verification are recorded in implementation-progress.md after completion.
