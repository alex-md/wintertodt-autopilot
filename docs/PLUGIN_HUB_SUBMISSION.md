# Plugin Hub release checks

Release: **0.2.4 — Solo chopping and fletching batches**, prepared 18 October 2026.

## What changed

- Solo recovery and remote-brazier work finish a chopping batch before fletching. Leftover kindling no longer forces each new root into fletching.
- Finished partial fletching batches clear correctly. Chopping targets carry across recovery and relighting instead of falling back to one root.
- Optional remote pyromancer healing waits until processing, avoiding trips during recovery chopping. Warmth and required maintenance retain priority.
- Includes the pending 0.2.3 changes: thin adjustable highlight borders, opacity-only pulses, and potion-only pyromancer healing cues.
- No new in-game release notice. The existing 0.2.1 notice and saved marker are unchanged.

## Validation

`./gradlew clean build previewOverlay --no-daemon --console=plain` passed: 205 tests, no failures, errors or skips. The synthetic overlay preview was inspected; packaged legal resources match, all 56 classes target Java 11, the unchanged icon meets Hub limits, and `git diff --check` passed. Regression coverage includes partial fletching followed by gathering, recovery-to-sustain transitions with and without kindling, remote-brazier work, goal-sized batches, XP / Never fletch, warmth interruptions, optional healing, and a complete six-root Mass cycle.

The synthetic preview does not establish live gameplay validation. Outstanding checks remain in [LIVE_VALIDATION.md](LIVE_VALIDATION.md).

## Submission

Submit through the existing Plugin Hub PR [#18829](https://github.com/runelite/plugin-hub/pull/18829) for 0.2.4 after publishing the source commit. Change only `plugins/wintertodt-autopilot` and use the full source hash. Hub CI, maintainer merge and distribution are separate from local validation.

The player performs every gameplay interaction. No new runtime dependencies or resources are introduced. The existing icon and legal notices are unchanged.
