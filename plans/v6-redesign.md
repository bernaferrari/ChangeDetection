# v6 redesign — "Show the change"

Design reference: `design/change-detection-v6.html`. Branch: `redesign/v6`.

## Product decisions

- **Three tabs:** Home · Activity · Settings. Turbo is no longer a tab. You start it from the ⚡ in Home, from selection, or from page details (a setup sheet), and it runs on its own live screen (`TurboDestination`).
- **Home answers "what changed?" first.** Unread changes are cards with a *bounded* preview; every page follows in a stable, quiet list. Long-press selects; the top bar becomes the selection toolbar (Check now, Turbo, Delete, ⋮ Pause/Notify/Edit). The bottom bar never holds actions.
- **Bounded previews** (`preview/ChangePreview.kt`): screenshot pages show a before/after crop around the changed pixels; small text edits show one clamped sentence with its section heading; big or noisy changes say "Most of the page changed"; script-only changes say so. Previews are cached by version pair and computed off the main thread.
- **Reader:** unchanged text folds into "N unchanged lines"; each change is labelled with its heading; a floating ink bar steps between changes and marks the change read. Screenshots keep Slider, Highlight, Before and After.
- **Activity** is the list of saved versions, labelled by what changed, plus pages that are failing *right now*. No per-check log is invented.
- **Add/edit:** paste a link → choose Text or Screenshot → see a live preview → Start watching. The name auto-fills from the page's first heading. Technical modes live under Advanced. A page with almost no text suggests switching to Screenshot.
- **Notifications** (Android) quote the same bounded preview, posted as a quiet follow-up to the first alert.

## Visual system

- `ui/theme/ReferenceColorScheme.kt`: paper/ink with marker-yellow `primaryContainer`.
- `ui/theme/Marks.kt`: fixed diff colors (highlight, removed, healthy…) that stay legible under any palette or dynamic color. A serif `Reading` style is used for page content.
- `ui/kit/*`: PageAvatar, DiffText (draws a 2dp strike, since serif fonts draw hairlines), ListGroup/GroupRow, Callout, previews, dates and cadence text.
- M3 Expressive: LargeFlexibleTopAppBar, connected ToggleButton groups, LoadingIndicator, LinearWavyProgressIndicator.

## Engine change

`ChangedBounds` is now computed in every platform's image diff (`ChangedBoundsTracker`), so the UI can crop to the changed area.

## Verification

`DesignReviewTest` renders the real app at phone size (1080×2340, 2.625×) over local fixtures, plus a local HTTP page for the add flow. It writes light and dark captures of every screen to `build/desktop-review/v6/`. At the time of writing: 164 desktop tests and 299 Android host tests pass; the debug APK, device-test sources, iOS simulator and Wasm compile; spotless and lint are clean. Tests that only exercised removed components (old watchlist, Turbo screen, history list, comparison chrome) were deleted and replaced with tests for the new screens and the preview engine.

## Known gaps

- Screenshot crops depend on Android captures; desktop can't capture screenshots, so screenshot pages fail there by design.
- Translations exist for the new Android notification strings only; other new UI strings fall back to English.
- The launcher icon still uses the previous mark. `AppMark` is the in-app mark.
