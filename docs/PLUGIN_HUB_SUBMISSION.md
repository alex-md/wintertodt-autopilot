# Plugin Hub release checks

Release: **0.3.0 — Solo mode is ready**, prepared 21 October 2026.

## What changed

Solo no longer carries the preview label. Comfortable keeps task changes down and finishes with one selected brazier. Active Points adds fresh injury-healing opportunities; Active finishing uses an adjacent pair when run energy is known and above 10%. Existing strategy, goal, fletching and corner choices carry over.

- Keep opening, recovery, finishing and unfinished fuel batches coherent through loading and temporary connection loss. Missing or regressed observations hold for reassessment.
- Collect feasible potion batches before mixing, reuse partial reserves across rounds, and keep fuel-first restock deferral. Confirmed consumption can increase the working allowance without lowering the initial 24-dose baseline.
- Show supply readiness and separate requested, in-progress, confirmed and interrupted actions. Stale health-bar readings cannot keep producing optional healing instructions.
- Keep pyromancer outlines attached during pending healing and valid optional approaches. Use one eligibility rule for maintenance, healing and making potion space during recovery.
- Improve route ordering and useful intermediate brazier stops while retaining collision checks and manual northern-gap jumps.
- Update the listing description and relevant tags for Mass/Solo, potions, kindling and pyromancers. Add one short 0.3.0 chat notice using the existing saved-marker key.

The player still performs every interaction. There are no automatic clicks, dodges or world hops. No new runtime dependencies are introduced. The icon and packaged legal notices remain unchanged.

## Validation and release scope

The maintainer reports successful manual plugin testing and requested this release. The complete [live scenario matrix](LIVE_VALIDATION.md) remains a follow-up record; automated tests do not mark those boxes complete. The newest healing-outline correction still needs its in-game retest.

Local verification uses `./gradlew clean build previewOverlay --no-daemon --console=plain`, command-line capture replay, packaged license/notices, Java 11 class checks, icon limits, documentation links and `git diff --check`. Results for this release are recorded in [Phase 3 progress](PHASE_3_PROGRESS.md).

Hazard avoidance, optional round-duration finishing, the explicit work-value refactor and comparative XP/points-rate claims are outside this release. Supply horizons are bounded engineering choices, not a promise that a reserve lasts a full round.

## Submission

The [0.2.5 submission](https://github.com/runelite/plugin-hub/pull/18894) is merged. Publish the 0.3.0 source commit, then submit a new Plugin Hub update changing only `plugins/wintertodt-autopilot` to its full public hash. Source CI, Hub CI, maintainer review/merge and distribution remain separate steps. Never describe an open update PR as already available in RuneLite.
