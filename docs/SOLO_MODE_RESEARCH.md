# Solo mode: behavior and research

Research started 13 October 2026. Updated for 0.3.0 on 21 October 2026. Solo is available in built-in settings with Comfortable and Active effort choices. Mass remains the default. See the [Phase 3 behavior notes](PHASE_3_EFFORT_AND_READINESS.md) for the current action-evidence and supply policies.

## Using Solo

1. Choose an empty world yourself, then select **Mode → Solo**. Visible player count does not prove exclusive control of the arena.
2. Choose **Solo strategy**: Points / Rewards fletches, Firemaking XP feeds roots. The existing Fletch roots override applies to both. The Mass strategy and Reward goal settings continue to apply only to Mass.
3. Set **Solo point goal** (default 5,000). This is a fixed earned-score target, not an estimate of reward capacity. Carried fuel is shown separately and must be fed before it counts. Raising this goal can resume sustain after an automatic finish decision.
4. Leave Working brazier on Automatic to follow your nearby working corner, including after opening/finish routes visit another corner. Chopping, fletching and routine feeding use that corner. Selection has a distance margin to avoid flickering near the center and respects intentional movement toward a clicked brazier/root. Select a specific corner to keep the routine pinned there.
5. To finish early, enable **Finish this Solo round**. The request latches for the current observed round; unchecking the setting does not cancel a latched request. Verified completion or a verified countdown clears the request. Changing mode, hopping worlds or logging out clears it too. Scene loading and temporary connection loss preserve latched intent while clearing observations. Low warmth still takes priority.

You perform every interaction. This implementation does not control clicks, dodge attacks, extinguish braziers, or guarantee that a lit brazier will go out before energy reaches zero.

## Implemented controller

`WintertodtPlanner` separates `WintertodtDecisionEngine` (Mass) from `WintertodtSoloEngine`. Both use the same warmth/retreat safety policy. Solo uses its own energy observations and supply loop; it never uses the Mass finishing budget to stop gathering.

| Phase | Behavior |
| --- | --- |
| Prepare | Acquire tools and an initial potion reserve; position at home before the round. |
| Reduce energy | Maintain additional braziers at explicit destinations. Stop multi-corner maintenance at energy ≤28%. If energy later exceeds 35%, resume reducing until ≤28% again. |
| Sustain | Gather, fletch and feed at home. Leave home unlit while another observed brazier is lit. |
| Recover energy | Enter at energy ≤10%. Suppress maintenance that restores drain; fletch, gather, replenish supplies or heal observed pyromancers at unlit/broken braziers. Existing lit braziers can still accept fuel. |
| Finish | Triggered by earned points reaching the fixed goal or the explicit finish request. Feed carried fuel first. Comfortable maintains one selected brazier; Active maintains an adjacent pair when known run energy is above 10%, otherwise only the original anchor. Low-energy relights are intentional in this phase. Stop gathering. |
| Reassess | Explain unavailable signals/global coverage or a rapid energy drop. Feed existing fuel at an observed lit brazier when practical. No claim that another player caused a drop. |

Recovery exits after **three distinct observations at or above 12%**, with a minimum observation interval. Attaching/reloading at ≤10% also enters recovery. Energy loss of more than 2 percentage points within six observed ticks triggers a fuel-feeding advisory during sustain/recovery; ordinary opening and intentional finishing are exempt; an intervening recovery increase ends that drain episode. Trend uses a rolling window of up to 17 ticks and remains unknown until enough observations exist. No precise recovery countdown is displayed.

The sustain relight floor is **10%**, retaining a small margin above the strategy guide's approximate 8% warning. Recovery resumes at 12%, replacing the original 15% → 24% cycle that unnecessarily prolonged recovery. Finishing deliberately bypasses sustain's floor: ending the round is the chosen outcome. It still requires a directly observed destination and known global state before lighting. Thresholds remain implementation choices awaiting live tuning, not guarantees of indefinite sustain.

Projected carried points can trigger a cash-out feed; they do not trigger finishing until earned points confirm the goal. Manual finishing interrupts chopping, fletching, feeding and recovery on the next game tick, subject to warmth safety and current destination/global observations.

## Observations and destinations

Every in-region snapshot has immutable per-corner brazier/pyromancer observations with independent source, scene visibility and last-observed tick. Health percentages are approximate client health-bar readings; unavailable health bars remain -1. Unchanged health-bar readings expire after eight ticks even while the NPC remains visible, and a spent healing dose invalidates cached injury. Standing state is separate from injury freshness. Historical freshness alone never establishes a current state.

Brazier HUD readings use the RuneLite 1.13.1 named `WintStatus.BRAZIER1`–`BRAZIER4` widgets. Compass directions are derived from the current four-icon screen layout rather than old numeric child offsets. Only sprites 1397/1398/1399 decode as broken/unlit/lit. Hidden/missing widgets, invalid geometry, duplicate compass positions and unknown sprites remain unknown. Scene state wins; any scene/HUD disagreement suppresses remote HUD readings for that tick. These runtime rules have deterministic tests; their live correspondence still needs verification.

`observedGlobalLitBraziers()` returns -1 unless all four distinct braziers have fresh current scene/HUD evidence. The existing loaded-scene count remains separate. Pyromancers require direct scene evidence; no unverified alive icon is accepted. Loading, hopping, disconnecting, leaving the region and round boundaries invalidate corner history.

Every Solo objective carries its destination. Object, NPC, arrow and path resolution use that destination. A HUD observation alone cannot produce a remote repair/light instruction: the planner first routes to the corner, acquires the scene object and pyromancer, then rechecks the current energy/global coverage. Missing HUD during an ongoing round enters reassessment without treating it as verified completion or clearing the finish latch.

## Northern gap route

The optional walking line includes the northern gap in Solo when both base and current Agility are at least 60. It uses three bidirectional jumps at y=4023, through x=1627, 1629, 1631 and 1633. Each jump has a four-walking-step cost (approximately two running ticks), so a short walking route still wins. The path rechecks eligibility immediately when mode or Agility changes. Missing/unloaded pillar tiles suppress the jump; ordinary walking continues to obey scene collision flags. The intermediate pillars are transport landing tiles, not ordinary walking tiles.

The player clicks each jump. The planner's destination stays on the intended brazier or pyromancer. No jumps or other interactions are automated.

Requirement checked against [RuneLite's AgilityShortcut](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/game/AgilityShortcut.java); landing coordinates and duration checked against the [Shortest Path transport data](https://github.com/Skretzo/shortest-path/blob/master/src/main/resources/transports/transports.tsv) on 15 October 2026. No source code was copied. Live landing/line alignment remains on the validation checklist.

## Supplies

Preparation and normal restocks start at **24 doses (six bottles)**. Enough confirmed consumption can increase the working allowance, capped at 40 doses; missing data and unattributed losses cannot calibrate it. Points mode starts a restock below eight doses to support frequent healing; XP mode starts below four. A new restock waits until carried roots and kindling have been fed, provided more than one dose remains. At one dose or none, supplies take priority. Joining mid-round accepts a partial reserve above the restock threshold rather than requiring an opening top-up. Finishing uses a smaller 8–12-dose working reserve, depending on the observed personal minimum. Consuming one dose from a stocked reserve does not start another trip. A restock stays active until its target is met. These reserves cover a working interval and are repeatedly replenished; they do not promise to cover an entire long round.

Tools honor equipped alternatives and fletching preferences. Ingredient collection leaves a mixing slot; existing ingredients can be mixed with a full bag. Brew'ma is used without Druidic Ritual. A full fuel inventory can be fed at an observed lit brazier to make supply room. If no brazier is lit, an observed, safely relightable working brazier can be maintained first to make feeding possible. Otherwise guidance explicitly asks for inventory space. Healing preserves a spare dose for personal warmth.

Comfortable skips optional injury healing. Active Points mode heals fresh observed damage at the working corner and considers injured remote pyromancers while processing fuel. Optional healing waits for the chopping batch to finish, including at the working corner. Routine maintenance and feeding trips also preserve unfinished gathering batches; critical rapid drain can still trigger an early cash-out. Once feeding starts, it continues through the remaining fuel rather than restarting chopping when the first slot opens. Unknown run energy or run energy at/below 10% suppresses remote healing trips. XP mode waits for incapacitation to conserve doses. During recovery, healing opportunities are limited to known unlit/broken braziers so healing cannot restore drain. Unknown or stale health never creates a healing objective.

Solo fletching does not require an adjacent-brazier positioning trip: roots can be fletched where the player is, including while moving. The safe preparation area still requires moving into the arena first. Gathering batches use available inventory capacity, limited by the remaining fixed goal. While waiting for energy recovery or a remote fire to expire, the planner continues a gathering batch before fletching instead of alternating one chopped root with one fletch.

## Debug traces

Launch with `./gradlew run` and enable **Debug overlay and logs**. One `solo_observation_v1` key/value line per in-region game tick records planner, Solo phase, phase-transition reason, finish request, energy/trend, warmth, home/destination, objective/reason, activity, supplies, earned/carried points, loaded/global lit counts and every corner's states/sources/visibility/timestamps/freshness. Disabled by default. Mass traces explicitly say `soloPhase=NOT_RUNNING`.

The overlay shows Solo phase and energy trend, fixed goal progress, carried fuel and a latched finish request. It suppresses the Mass finish-time estimate in Solo. Minimal keeps phase, energy, goal and warmth visible.

## Evidence and remaining validation

Research sources:

- [Wintertodt strategies](https://oldschool.runescape.wiki/w/Wintertodt/Strategies), retrieved as raw source on 13 October 2026 and checked online again on 14 October through the [indexed Wiki copy](https://osrsindex.com/wiki/wintertodt-strategies?site=osrs_wiki) (copied revision dated 28 July 2026; direct Wiki reading was blocked by robots.txt): fuel conversion is separate from energy drain; no lit braziers permits delayed recovery; opening reduces energy before concentrating at home. Community gameplay evidence requires live confirmation.
- [Solo Helper source pinned at c5761f0](https://github.com/aquaosrs/wintertodt-solo-helper/tree/c5761f0224c834b79d76c0f1b25b7ce4964a2211), rechecked 14 October 2026: sprite semantics informed the HUD decoder. Its legacy child offsets and permissive unknown-pyromancer handling are not used. No source code copied.
- [RuneLite 1.13.1 InterfaceID](https://github.com/runelite/runelite/blob/runelite-parent-1.13.1/runelite-api/src/main/java/net/runelite/api/gameval/InterfaceID.java): current named Wintertodt HUD components, also checked against the pinned dependency jar.

Automated tests exercise full opening → sustain → recovery → cash-out → finish → next-round replay, oscillation, duplicate ticks, missing signals, partial/stale/HUD coverage, transformations, destination resolution, tools, mixing, full bags, repeated restocking, warmth safety, fixed goals and finish lifecycle. Rendering previews are synthetic.

**Still pending:** compare HUD states to physical braziers from every corner and preparation area, collect manual Solo traces, observe delayed recovery and other-player interference, validate potion margins, follow guided Solo rounds and tune thresholds from those observations. Unit tests do not establish gameplay safety or close the live gate. Remote healing and run-energy decisions use observed data; frame-perfect fletching/healing combinations are not automated or guaranteed.

See [live validation](LIVE_VALIDATION.md) for the manual checklist.


## Strategy audit — 14 October 2026

The online review confirmed that the original recovery band, downed-only Points healing, tiny reserves and four-corner finish circuit were planner choices that needed revision. The changes above cover those gaps. The guide prefers Southwest for a settled routine because of movement geometry; Automatic deliberately remains local per live user feedback, with Southwest available as an explicit choice. Opening still lights multiple braziers and stops remote maintenance around 28%. Optional third-brazier finishing and exact-tick healing remain manual choices rather than mandatory routes. See the live checklist before treating the revised controller as validated.
