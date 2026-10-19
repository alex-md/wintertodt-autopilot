# Plugin Hub release checks

Release: **0.2.5 — Stable batches, potion trips and healing cues**, prepared 18 October 2026.

## What changed

- Solo gathering survives local maintenance, optional healing, remote-brazier feeding and noncritical rapid-drain advice. Feeding completes the current fuel batch after the first inventory slot opens. Critical rapid drain, warmth safety and intentional finishing can still interrupt.
- Mass chopping survives brief idle animation gaps before returning for maintenance.
- New routine Solo restocks wait until carried fuel is fed when more than one dose remains. A usable partial reserve is accepted when enabling mid-round. Started restock trips still finish their target.
- Healing highlights only the pyromancer. Inventory potion borders now mean drinking for personal warmth; thin borders and fading pulses are unchanged.
- Full warmth cannot request restoration or a low-warmth alert. Obsolete drink instructions clear between game ticks once warmth recovers.
- No new in-game release notice. The existing 0.2.1 notice and saved marker are unchanged.

## Validation

`./gradlew clean build previewOverlay --no-daemon --console=plain` passed: 221 tests, no failures, errors or skips. The synthetic overlay preview was inspected. Release packaging checks cover legal resources, Java 11 classes, icon limits, README local links and `git diff --check`.

The synthetic preview does not establish live gameplay validation. Outstanding checks remain in [LIVE_VALIDATION.md](LIVE_VALIDATION.md).

## Submission

The previous [0.2.4 submission](https://github.com/runelite/plugin-hub/pull/18829) is merged. Submit 0.2.5 as a new Plugin Hub PR after publishing the source commit. Change only `plugins/wintertodt-autopilot` and use the full source hash. Hub CI, maintainer merge and distribution are separate from local validation.

The player performs every gameplay interaction. No new runtime dependencies or resources are introduced. The existing icon and legal notices are unchanged.
