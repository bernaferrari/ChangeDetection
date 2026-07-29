# Implementation Plans

Generated on 2026-07-28 from an architecture, visual-design, and release-readiness review. Execute in order. The working tree already contains user changes: executors must preserve them, never reset or discard them, and stage only files belonging to their plan.

## Execution order & status

| Plan | Title | Priority | Effort | Depends on | Status |
|---|---|---|---|---|---|
| 001 | Restore reproducible Android verification | P1 | M | — | TODO |
| 002 | Publish only verified Change Detection imagery | P1 | M | 001 | TODO |
| 003 | Establish one Compose visual language | P1 | L | 001 | TODO |
| 004 | Decompose the comparison and Turbo screens | P2 | L | 003 | TODO |
| 005 | Add focused UI regression coverage | P2 | M | 001, 003, 004 | TODO |

Status values: `TODO` | `IN PROGRESS` | `DONE` | `BLOCKED` (include reason) | `REJECTED` (include rationale).

## Dependency notes

- Plan 001 comes first because the current checkout cannot run the documented Android host tests: its local composite-build substitutions name projects that do not exist in the sibling Webwright checkout.
- Plan 002 depends on a buildable app to capture truthful screenshots. Do not use stock, generated, or another product's UI as an app screenshot.
- Plan 003 defines the component contract that Plan 004 must use; otherwise a split risks codifying the present mix of tokens and custom animation constants.
- Plan 005 lands after the screen extraction so tests protect the intended public composables rather than temporary private implementation details.

## Findings considered and rejected

- A wholesale rewrite of Change Detection to match SDKMonitor: rejected. The products have different complexity and Change Detection already has a viable KMP/domain/data architecture.
- Replacing the existing theme with SDKMonitor's palette or components: rejected. Preserve Change Detection's dynamic Material You identity; improve consistency and craft, not product identity cloning.
- Treating QuietGuard as a source-structure benchmark: rejected. It is visually polished but contains significantly larger legacy files than Change Detection.
