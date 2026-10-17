# Plugin Hub release checks

Release: **0.2.2 — highlights and walking routes**, prepared 16 October 2026.

## What changed

- Relighting highlights the brazier and lighting tool without drawing attention to roots or kindling.
- Fletching distinguishes the amber knife from softer gold roots.
- Local walking routes use collision-checked diagonal steps and prefer fewer turns among equally short paths. Blocked corners and the explicit Solo gap remain respected.
- No new in-game release notice. The existing 0.2.1 notice and saved marker are unchanged.

## Validation

`./gradlew clean build previewOverlay --no-daemon --console=plain` passed: 194 tests, no failures, errors or skips. `git diff --check` passed. Packaged legal notices match the originals and the plugin class targets Java 11. Tests cover inventory cues, diagonal routes, blocked corners, object approaches and Solo gap routing. Verify packaged legal resources and Java 11 class targets.

The synthetic preview does not establish live gameplay validation. Outstanding checks remain in [LIVE_VALIDATION.md](LIVE_VALIDATION.md).

## Submission

The prior 0.2.1 Plugin Hub PR [#18704](https://github.com/runelite/plugin-hub/pull/18704) is merged. Publish the 0.2.2 source commit, then update only `plugins/wintertodt-autopilot` with its full source hash. Hub CI, maintainer merge and distribution are separate from local validation.

The player performs every gameplay interaction. No new runtime dependencies or resources are introduced. The existing icon and legal notices are unchanged.
