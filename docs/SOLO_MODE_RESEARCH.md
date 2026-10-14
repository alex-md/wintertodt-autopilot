# Phase 2: Solo mode

Research started 13 October 2026. This is a design and validation plan; Solo mode is not enabled in the plugin.

## Findings and evidence

The current mass planner cannot safely become a Solo planner by changing batch sizes. It prioritizes relighting the local brazier and uses low energy to trigger finishing. A Solo planner sometimes needs the opposite decision: leave a brazier unlit and continue gathering while energy recovers.

Sources inspected:

- [Wintertodt strategies](https://oldschool.runescape.wiki/w/Wintertodt/Strategies), retrieved through its [raw source](https://oldschool.runescape.wiki/w/Wintertodt/Strategies?action=raw) on 13 October 2026. The normal web reader failed; a direct HTTPS fetch of the raw page succeeded. This is community gameplay documentation, not a verified client contract.
- [Solo Helper source, pinned at c5761f0](https://github.com/aquaosrs/wintertodt-solo-helper/tree/c5761f0224c834b79d76c0f1b25b7ce4964a2211), specifically `WintertodtBrazier`, `WintertodtSoloHelperPlugin`, and `WintertodtSoloHelperOverlay`. The brazier renderer gates relighting by energy, inventory and main/secondary brazier choice. Its widget parsing and numeric NPC/object IDs require independent verification. No code copied.
- This repository's `WintertodtDecisionEngine`, `WintertodtRoundForecast`, `WintertodtObjectTracker`, `WintertodtNpcTracker`, `WintertodtSnapshot`, and `WintertodtSupplyPlan`.

Gameplay evidence from the strategy page:

| Observation | Design implication |
| --- | --- |
| Feeding fuel earns points but does not speed energy loss. | Track fuel conversion separately from energy control. |
| With no lit braziers, energy recovers; the first recovery has a random delay. A lit brazier prevents recovery. | Observe direction and brazier coverage; do not display a precise recovery countdown. |
| Opening guidance reduces energy to roughly 20–30%, then concentrates on one brazier. | Use separate opening and sustain phases. |
| Below roughly 8%, another relight can end the round unexpectedly. | Treat a lower relight boundary as a conservative guard, never a guarantee. |
| The page recommends the southwest corner for movement efficiency and describes both points-focused and XP-focused Solo play. | Evaluate southwest as a Solo default; keep the player's objective explicit. |

These are starting hypotheses for live validation. No fixed energy interval guarantees indefinite survival. Do not carry forward old guides' 13,500-point ceiling without checking current reward behavior.

## Proposed experience

The player selects **Solo** in the existing RuneLite settings and chooses a goal. Everything else should start with usable defaults. The overlay keeps the same one-action format as mass mode: “Light the southwest brazier,” “Let energy recover — fletch your roots,” or “Finish the round — light the next brazier.” Explain deliberate waiting so the player can distinguish it from a stalled guide.

Use an explicit **Finish this round** control in built-in settings. Latch that request for the current round, reset it on a verified new round, and show its state clearly. Do not require the player to change several energy thresholds or infer how to leave sustain mode. A points goal must count earned points separately from carried fuel. A long session must not silently continue beyond the player's chosen goal.

## Proposed phase model

| Phase | Responsibility | Exit condition |
| --- | --- | --- |
| Prepare | Tools, warmth, enough ingredients and inventory room | Observed readiness |
| Reduce energy | Route to selected additional braziers, heal where necessary | Energy enters the validated sustain band |
| Sustain | Gather, fletch and feed; relight only when permitted | Goal/finish request, or recovery needed |
| Recover energy | Avoid relighting; use productive actions where possible | Observed recovery above a separate upper boundary |
| Finish | Feed carried fuel where practical and maintain drain | Verified completion |
| Reassess | Explain missing or conflicting observations | Enough fresh observations to choose safely |

Warmth recovery and emergency retreat retain priority in every phase. Use hysteresis and minimum observation periods to avoid repeatedly switching sustain/recovery after a single energy update. A player's change of intent must override a previous sustain latch.

## Required implementation changes

1. **Separate planners.** Add explicit Mass/Solo mode selection and a Solo state machine. Reuse safety and tool/supply objectives, inventory detection and presentation. Never silently select Solo from a small visible player count: loaded players do not establish exclusive control of the arena.
2. **Observe every corner.** Add immutable per-corner brazier and pyromancer state with freshness/visibility metadata. `litBraziers` currently counts loaded objects, and one local state does not establish that all four braziers are out. Evaluate the four HUD indicators as a global source against live objects. Unknown coverage must remain unknown.
3. **Target an explicit corner.** Object targeting currently anchors roots and braziers to the working corner. Opening/finishing needs a distinct objective destination so a remote lighting instruction cannot highlight the local brazier. Associate each remote pyromancer and path with that same destination.
4. **Model recovery separately.** The existing forecast resets on energy increases or zero loaded lit braziers. That behavior suits mass finishing but is not a sustain controller. Keep it for mass mode; add trend observations and uncertain drain/recovery estimates for Solo.
5. **Rework supplies.** The current 1–3 bottle reserve assumes a short mass round. Solo requires a phase-aware reserve and repeated restocking that accounts for pyromancer healing, inventory capacity and travel. Do not simply multiply the mass estimate or claim a reserve covers an unlimited session.
6. **Handle other players and stale data.** Energy can change when another player lights an unseen brazier. Reassess unexplained drops, retain warmth guidance, and communicate uncertainty. Loading, hopping and lost HUD values must invalidate remote decisions.

## First implementation slice

Build read-only per-corner observations and a debug trace first. Record tick, phase, energy, visibility/freshness, all observed brazier/pyromancer states, destination, warmth, supplies, earned/carried points and reason for each phase change. Keep logs opt-in. Use these traces to build deterministic scenarios before exposing a Solo selector.

Next implement prepare → opening → sustain → recovery with one selected home corner. Add explicit finishing only after the destination and global-state checks work. Defer optional roaming healing and run-energy optimizations until the basic loop is validated.

## Validation gates

- Verify current energy drain/recovery and observation coverage from every corner, including the safe preparation area. Record delayed recovery rather than assuming it failed.
- Test low energy with one or more remote braziers lit, unknown remote states, a downed home pyromancer, and another player joining. No relight instruction may rely on unknown global state.
- Replay energy oscillation around both boundaries. Confirm no flickering route changes or accidental use of the mass ending heuristic.
- Test ingredient shortages, full inventories, no Druidic Ritual, depleted run energy, interrupted movement and low warmth during each phase.
- Request finishing while chopping, fletching, feeding and recovering; verify destination, fuel conversion and request reset after completion.
- Complete observed Solo rounds with recorded transitions and compare instructions against actual game state. Unit tests alone do not close this gate.

Open questions: which global HUD indicators remain reliable throughout scene loads; how much hysteresis live variation needs; how to detect unsolicited remote lighting early enough; whether a points goal or a deliberate finish request should be the initial default. These decisions remain open until observation traces support them.
