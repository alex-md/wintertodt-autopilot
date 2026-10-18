# Plugin Hub release checks

Release: **0.2.3 — thinner highlights and pyromancer healing**, prepared 17 October 2026.

## What changed

- Scene and inventory borders default to one pixel. Highlight border width is adjustable from 1–4 pixels in the built-in settings.
- Attention pulses change opacity without expanding borders. Inventory borders no longer have the broad glow; scene feathering is reduced.
- Healing highlights only the selected finished rejuvenation potion, never roots or kindling.
- No new in-game release notice. The existing 0.2.1 notice and saved marker are unchanged.

## Validation

`./gradlew clean build previewOverlay --no-daemon --console=plain` passed: 196 tests, no failures, errors or skips. Tests cover all potion dose variants, stale feeding/fletching animations, Mass and Solo healing, and fixed border width during pulses. `git diff --check`, packaged legal-resource comparisons and Java 11 class-target checks passed. The synthetic overlay preview was inspected.

The synthetic preview does not establish live gameplay validation. Outstanding checks remain in [LIVE_VALIDATION.md](LIVE_VALIDATION.md).

## Submission

The prior 0.2.2 Plugin Hub PR [#18740](https://github.com/runelite/plugin-hub/pull/18740) is merged. Publish the 0.2.3 source commit, then update only `plugins/wintertodt-autopilot` with its full source hash. Hub CI, maintainer merge and distribution are separate from local validation.

The player performs every gameplay interaction. No new runtime dependencies or resources are introduced. The existing icon and legal notices are unchanged.
