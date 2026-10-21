# Phase 3 implementation progress

Started 19 October 2026, from 0.2.5. Branch: `cmf/solo-phase-3-reliability`.

This records the reliability work for the [Phase 3 plan](PHASE_3_SOLO_PLAN.md), now being released as 0.3.0. The maintainer reports successful manual testing and requested the removal of the Solo preview label. The dated development notes below retain their original evidence scope; no unchecked live scenario is marked complete by automated results.

## Implemented

- Scene loading and temporary connection loss clear scene references and energy samples while retaining Solo recovery, opening, batch, restock and manual finish intent. A verified countdown/completion, logout, hop, departure or mode change still resets the appropriate session state.
- Recovery cannot exit merely because loading occurred at 11%. After reacquisition, its exit condition needs new, distinct observations. Reassessing missing data no longer cancels the opening destination or selected finish pair.
- A score lower than the round's observed high-water mark holds for reassessment. A missing score after loading also holds, even with manual finish latched. Fresh matching observations can resume; a verified boundary resets. If the game has lost the round without a visible boundary, check the encounter and switch mode away/back to explicitly start a new plan.
- Energy is observed even when warmth temporarily preempts work, so low energy cannot be missed during a drinking instruction.
- Routine guidance stays behind warmth restoration until the actual warmth reading recovers. Clicking Drink/Eat records a request; it does not prove success or dismiss the safety instruction. Consumption plus a warmth rise can confirm a recent request, including partial-food transformations. An unconfirmed request expires as an activity signal.
- Between game ticks, recovered warmth clears its obsolete cue and briefly shows “Warmth restored.” It does not run either planner again. The next game tick chooses the next action.
- Missing inventory/equipment data remains unknown, rather than a fabricated empty bag or a missing-tool prompt. Low warmth with unobserved supplies explicitly directs the player to check supplies or move to safety.
- Changing an unrelated Mass goal or a warmth threshold no longer discards a Solo batch. Deliberately changing the effective strategy/fletching preference still cancels incompatible work.
- Debug captures include raw visible energy/points text, source/validity, HUD sprites and geometry, HUD/scene disagreement, complete planner input, controller checkpoint, settings, lifecycle event, expected objective and phase. Existing human-readable `solo_observation_v1` lines remain available.
- A local replay tool checks state continuity and exact decisions. Summaries label their scope as the observed portion of a run. Graphics effects can be recorded for later warning/impact correlation; these recordings do not create hazard cues.

All gameplay interactions remain manual. The existing energy thresholds, potion reserve sizes, strategy defaults and release-notice marker are unchanged.

## Refinement pass: decisions, routes and potion batches

Continued on the same branch. The regression suite now has **270 passing tests**; the clean build, updated synthetic overlay preview and seven-decision replay pass. Live acceptance remains open.

| Reported issue | Root cause established from code | Implemented change |
| --- | --- | --- |
| Strategic recommendations lack context | Normal guidance used generic reason strings, while goal/phase data lived separately in the planner and overlay. | The chosen decision carries typed strategy facts: its purpose, configured goal, earned points, energy, lit-fire coverage and manual-finish intent. Normal/Detailed use those facts to explain recovery holds, remote fires, opening, goal cash-out, finishing and useful intermediate stops. Routine chopping/fletching/feeding stays concise. |
| Diagonal travel passes useful unlit braziers | Chebyshev tile distance can tie an opposite corner with adjacent corners; the selected destination stays latched without checking intermediate interactions. | A single bounded scene search measures all candidate walking routes, using existing collision and shortcut rules. Selection includes relative interaction costs. A verified stop on the route is serviced before continuing to the original destination; the stop remains committed during approach. Recovery cannot add fires, and finishing only services the selected finish fires. |
| Mixing after one of six requested herbs | The ingredient method returned Mix whenever any pair existed, ahead of collection completion. | Independently count prepared doses, unfinished potions and herbs. Collect the complete feasible batch, then continue mixing it. Capacity can justify smaller batches; an empty single slot cannot trigger an unpairable vial request. Existing ingredients and partial doses reduce only their actual deficits. Brew'ma follows the same batch rule. |
| Repeated preparation after round transitions | An unconditional supply-planner reset discarded readiness and mixing progress, forcing a reusable partial reserve through the fresh-opening target. A full unused 24-dose reserve already bypassed preparation. | Keep supply readiness and unfinished preparation across verified round boundaries, while clearing encounter-specific intent. Recheck actual inventory each tick. Preserve the existing restock thresholds (Points below 8 doses, XP below 4), and replenish only the deficit toward the working target when a restock is needed. Fresh-session opening preparation still aims for 24 doses. |

Route evidence is available even when Show walking path is off. It is cached while the player, scene and shortcut eligibility remain unchanged, invalidated by scene events, and refreshed within five ticks. Unknown routes use conservative corner-distance estimates; observed unreachable targets are not treated as reachable. This is a bounded greedy sequence with useful intermediate stops, not a claim of globally optimal routing. Interaction penalties rank work; they are not server timing predictions.

Regression coverage includes Southeast → Northwest and Northeast → Southwest, collision walls and blocked approaches, weighted northern jumps, missing geometry, unreachable goals, interaction costs, route cache invalidation, fresh evidence requirements, no third-fire finish detours, and cancelling opening travel when the earned goal is reached. Preparation tests apply individual collection/mixing actions across 7–20 available slots and repeated round interruptions; they also cover full/partial reserves, remaining ingredients, constrained space and Brew'ma.

Synthetic preview panels include a 13,500-point recovery explanation and a manual finish below that goal. No fixed 13,500 reward cap is assumed. The refinement pass used controller revision `phase3-refinements-2`; earlier captures require the matching earlier source checkout.

## Capture a manual encounter

1. Build and launch this checkout with `./gradlew run` using the existing local development setup.
2. Select Solo and enable **Debug overlay and logs** in the built-in plugin settings. Capture a short ordinary session first; disable debug after collecting it to limit log volume.
3. Play manually. Record visible energy, score, warmth and brazier/pyromancer state alongside the log when possible. Include opening, local work, a recovery, an intentional finish and the next countdown. Capture loading/reconnection only when it naturally occurs or can be tested safely.
4. Preserve the relevant RuneLite log. Do not trim lines inside an uninterrupted `solo_capture_v1` segment. Starting capture mid-round and switching debug off/on are supported as explicit segment starts.
5. Replay with the matching source checkout:

   ```sh
   ./gradlew replaySolo -PtraceFile=/absolute/path/to/client.log --no-daemon --console=plain
   ```

The command fails on missing state transitions, unexpected decisions, incomplete/unsupported checkpoints or a file with no capture records. It accepts normal logger prefixes. Checkpoints are tied to a controller revision; replay older captures using the matching checkout. They are local diagnostics, not a saved game session and not input to the live plugin.

`solo_effect_v1` records effect ID, world position, plane, start cycle and observed cycle. These IDs and timings must be compared with actual snow warnings and impacts before adding hazard behavior. `solo_run_v1` records observed samples, minimum warmth, maximum observed score, action switches and recovery phase entries; it does not claim complete XP/hour or full-run coverage.

Captures omit chat and redact interaction names. Review a log before sharing it: the surrounding RuneLite log may contain information unrelated to these capture records. No capture is uploaded automatically. If capture formatting fails, it stops until debug is toggled, while gameplay guidance continues.

## Automated evidence

The implementation pass passed **247 tests** with no failures/errors/skips, a clean build and synthetic overlay preview. The replay command matched all seven decisions in its synthetic fixture. Packaged license/notices and Java 11 bytecode were checked. These results are local automated evidence; they do not close the live gates below.

The foundation adds regression coverage for reloads at 9/11/12%, backward/duplicate ticks, partial chop/fletch/feed commitments, opening destination continuity, missing/regressed scores, inventory validity, failed drinking, confirmed restoration, natural regeneration, pyromancer healing, partial food, planner-free client-frame updates, sequential replay, missing-record detection and diagnostic failure isolation. Existing Mass and Solo tests remain in the suite.

`WintertodtSoloCaptureTest` writes `build/reports/solo-replay/synthetic.log`. This fixture is explicitly synthetic and supports a command-line smoke check:

```sh
./gradlew replaySolo -PtraceFile=build/reports/solo-replay/synthetic.log --no-daemon --console=plain
```

Synthetic replay and overlay previews do not establish that the game data is decoded correctly.

## Milestone status and next gate

| Milestone | Current state | Remaining acceptance work |
| --- | --- | --- |
| M1: reproducible observations | Capture, replay and bounded summaries implemented | Manual full-loop capture, matching replay, and outcome/overhead inspection |
| M2: lifecycle continuity | Core changes and regression tests implemented | Live reload/reconnect and subsequent-round checks |
| M3: observations and action completion | Inventory validity and personal warmth confirmation implemented | Live HUD mapping, scale/visibility checks; correlation of the new action/health evidence with live events remains open |
| M4: commitments and effort preference | Batch continuity, typed decision context and committed route stops implemented | Explicit work-value refactor remains deferred; both effort preferences need live acceptance |
| M5: energy control | Recovery continuity strengthened; thresholds retained | Recorded sustain/recovery outcomes for the shared eligibility rule |
| M6: complete readiness/finish/reset | Batched potion collection/mixing and inventory-aware cross-round readiness implemented | Live supply/readiness checks and measured reserve/finish-effort outcomes |
| M7: hazard evidence | Graphics diagnostics implemented | Matched live warning/impact samples before any hazard cue or route exclusion |
| M8: release and comparative validation | Maintainer manual testing reported; 0.3.0 release requested | Full repeated-session matrix and measured comparisons remain open |
| M9: optional optimization | Deferred | Evidence from prior milestones |

The next evidence gate is a manually played capture and HUD comparison. Continue implementation from those observations; keep unverified timing, additional effort modes and new route behavior out of a public release until their acceptance checks pass. Source publication, Plugin Hub submission/review, distribution and live validation are separate steps and have not been performed for this foundation.

## Effort, readiness, confirmation and shared maintenance — 21 October

Implemented Comfortable/Active independently of Points/XP, a stable one-fire versus adjacent-pair finish, supply readiness, attributed consumption and bounded upward reserve adaptation, explicit action evidence, separate pyromancer injury freshness, and one maintenance eligibility rule across relights/healing/supply space. See [behavior, parameters, migration and source basis](PHASE_3_EFFORT_AND_READINESS.md).

The default is Comfortable. Existing saved keys and the update-notice marker remain intact. Three-tick pending requests never prove completion. Cached injury readings expire and are invalidated after a healing dose. The 24-dose baseline, energy thresholds and player-controlled interactions remain in place. Capture revision is now `phase3-effort-3`; use matching code for older captures.

These changes close the four requested implementation areas, with live acceptance still pending. They do not complete M8 graduation or hazard work. The explicit committed-work value refactor remains a separate plan item; existing tested batch commitments are retained.

Automated verification for this pass: **304 tests passed** with zero failures/errors/skips; `clean build previewOverlay` and `git diff --check` passed. New sequence coverage includes both effort preferences, shared recovery eligibility, pending/failed actions, wrong-corner changes, ratio expiry/invalidation, attributed consumption, calibration gaps/limits and a 316-decision replay spanning learned reserves and effort changes. The command-line seven-decision synthetic replay also passed. The expanded overlay preview was visually inspected. These are local automated/synthetic results, not recorded live acceptance.

## Healing outline continuity — 21 October

The pending-heal instruction previously used an untargeted Wait objective, clearing the NPC outline after Help was requested. It now retains the intended pyromancer and the healing cue color during approach/arrival until the request resolves or expires. Active optional healing also retains its selected destination through movement and arrival while current injury/supply/maintenance evidence remains valid. This does not extend expired health evidence or override warmth restoration, recovery rules or observed healing.

Three new regressions cover the real outline-renderer call across pending approach, arrival and failed requests; optional movement continuity without starting new detours; and cancellation for expired injury, warmth and observed healing. Live acceptance remains open. Current capture revision is `phase3-healing-cues-4`; earlier revisions require matching code.

Healing-cue pass verification: **307 tests passed**, clean build and synthetic overlay preview passed, seven-decision command-line replay matched, and `git diff --check` passed. Renderer regressions exercise the NPC outline while approaching, arriving and retrying. The in-game symptom still needs a retest with this build.

## 0.3.0 release preparation — 21 October

Solo now uses its final settings label. README, controller notes, discovery tags and description have been aligned with the shipped behavior. The one-time chat notice uses the existing `lastUpdateNotice` key with marker `0.3.0`; users with an old marker receive the new message once. Earlier entries describing an unchanged marker refer to the development passes before this release request.

The maintainer reports successful manual plugin testing. The broad live matrix remains an honest follow-up record, including the newest healing-outline correction. Hazard avoidance, optional round-duration finishing, the explicit work-value refactor and comparative rate claims are outside this release. No unsupported hazard timing or performance claims have been added.

0.3.0 release verification: **308 tests passed**, clean build and inspected synthetic preview passed, command-line replay matched all seven fixture decisions, Java 11 bytecode and packaged licenses/notices passed, 48×48 transparent icon is 4,985 bytes, all local Markdown links passed, and `git diff --check` passed. Notice regressions cover the existing 0.2.1 marker receiving this release and the saved 0.3.0 marker suppressing repeats. Source and Hub CI are recorded separately after publication.

The first Hub package check rejected a fresh Gson instance in diagnostics. Capture/replay now uses RuneLite’s injected Gson, and local replay tests use the same client serializer. The corrected clean build, preview and all 308 tests pass; the same Hub PR is updated rather than creating a second submission.
