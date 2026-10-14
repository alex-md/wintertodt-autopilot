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

## Component Architecture

To support Solo mode without destabilizing the verified mass planner, the system isolates Solo logic behind a shared planner interface. Shared infrastructure—such as warmth tracking, local object detection, and baseline inventory parsing—remains in the core pipeline, while state evaluation and action selection fork into specialized engines.

```
                   ┌────────────────────────────────────────┐
                   │        WintertodtPlugin (Core)         │
                   └───────────────────┬────────────────────┘
                                       │ emits Snapshot each tick
                                       ▼
                   ┌────────────────────────────────────────┐
                   │      WintertodtSnapshotAggregator      │
                   │  - Local Objects    - Inventory/Gear   │
                   │  - Global Widgets   - Player Warmth    │
                   │  - Corner Trackers  - Supply Tally     │
                   └───────────────────┬────────────────────┘
                                       │
                    ┌──────────────────┴──────────────────┐
                    ▼                                     ▼
     ┌─────────────────────────────┐       ┌─────────────────────────────┐
     │  WintertodtDecisionEngine   │       │   WintertodtSoloEngine      │
     │        (Mass Mode)          │       │        (Solo Mode)          │
     │ - Rapid energy burn         │       │ - Multi-corner observation  │
     │ - Local brazier focus       │       │ - Energy band hysteresis    │
     │ - Fixed batch heuristics    │       │ - Phase-driven action loops │
     └──────────────┬──────────────┘       └──────────────┬──────────────┘
                    │                                     │
                    └──────────────────┬──────────────────┘
                                       ▼
                   ┌────────────────────────────────────────┐
                   │        WintertodtActionEnvelope        │
                   │  - Primary Action   - Target Tile/Obj  │
                   │  - Secondary Note   - Urgency Tier     │
                   └────────────────────────────────────────┘
```

### 1. Corner Observation Tracker (`WintertodtCornerTracker`)

Mass mode only requires awareness of the player's immediate corner. Solo mode requires an explicit world model representing all four corners (`SW`, `SE`, `NW`, `NE`).

Each corner instance maintains:
- **Brazier state**: `UNLIT`, `BURNING`, `BROKEN`, or `UNKNOWN`.
- **Pyromancer state**: `ALIVE` (with hitpoint percentage), `INCAPACITATED`, or `UNKNOWN`.
- **Observation source**: `DIRECT_SCENE` (object/NPC is loaded in the local client scene) or `HUD_WIDGET` (inferred from arena status bar icons).
- **Freshness**: Game tick of the last direct scene or widget update. Any observation older than 8 ticks (4.8 seconds) without a corroborating HUD pulse degrades to stale.

```java
public enum BrazierCorner {
    SOUTH_WEST,
    SOUTH_EAST,
    NORTH_WEST,
    NORTH_EAST;
}

public final class CornerObservation {
    private final BrazierCorner corner;
    private final BrazierStatus brazierStatus;
    private final PyromancerStatus pyromancerStatus;
    private final int pyromancerHealthPercent;
    private final ObservationSource source;
    private final int lastObservedTick;

    // Immutable representation; updated each tick via scanner merge
}
```

#### HUD vs. Scene Reconciliation
- When the player is stationed in the SW corner, the NE corner objects are beyond standard render distance.
- The top-level Wintertodt HUD widgets provide binary alive/broken/burning indicators for remote corners.
- **Rule**: Direct scene entities override HUD widgets. If direct scene data is unavailable, HUD widget state is accepted, but flagged as low-fidelity (e.g., widget indicates "lit", but cannot confirm exact repair state if it was broken immediately prior).
- If neither scene nor widget provides data (e.g., during scene rebuild or widget redraw delays), the corner status transitions to `UNKNOWN`, triggering safety-first fallback logic.

---

### 2. Solo Energy Controller & Hysteresis (`WintertodtSoloEnergyModel`)

The energy controller prevents catastrophic round completions and avoids instruction flickering by enforcing an asymmetric band model with deadbands.

```
 Energy %
  100% ┌────────────────────────────────────────────────────────┐
       │ OPENING DRAIN PHASE                                    │
       │ Light 3-4 braziers to drop Wintertodt energy rapidly.  │
   30% ├────────────────────────────────────────────────────────┤ ◄── Upper Sustain Bound
       │ SUSTAIN PHASE                                          │
       │ Hold 1 lit brazier (home). Gather, fletch, feed roots. │
       │ Energy fluctuates slowly based on pyromancer uptime.   │
   15% ├────────────────────────────────────────────────────────┤ ◄── Recovery Trigger Bound
       │ RECOVERY PHASE                                         │
       │ Extinguish / do not relight home brazier.              │
       │ Pyromancer must stay alive. Convert fuel or chop.      │
   10% ├────────────────────────────────────────────────────────┤ ◄── Critical Relight Floor
       │ SAFETY LOCKOUT                                         │
       │ Never relight any brazier under any circumstance.      │
    0% └────────────────────────────────────────────────────────┘
```

#### Threshold Configurations and Invariants

| Boundary | Default Value | Purpose | Invariant Rule |
| --- | --- | --- | --- |
| **Opening Cutoff** | 28% | Stops initial multi-brazier burn. | Transition to Sustain immediately once energy $\le 28\%$. Extinguish/allow remote braziers to die. |
| **Recovery Trigger** | 15% | Initiates unlit phase. | Home brazier must **not** be relit when energy falls below this mark. |
| **Recovery Exit** | 24% | Exits recovery back to sustain. | Energy must rise **above** 24% before relighting the home brazier is permitted. Deadband between 15% and 24% prevents single-tick flip-flopping. |
| **Relight Hard Floor** | 10% | Round-loss safeguard. | Absolute software interlock: zero relight recommendations are generated if energy $\le 10\%$, even if the user manually presses finish. |
| **Finish Target** | User-defined or Finish toggle | Triggers final energy burn. | Lights all available braziers and feeds carried roots until Wintertodt reaches 0%. |

---

### 3. Supply and Warmth Planner (`WintertodtSoloSupplyPlan`)

Solo rounds last significantly longer (typically 40–55 minutes for high-point runs) than 3-minute mass rounds. The supply manager must budget for sustained attrition:

1. **Rejuvenation Potion Reserves**:
   - Mass mode requires only 1–3 doses.
   - Solo mode models potion consumption against two sinks:
     - **Personal Warmth/Hitpoints**: Periodic ambient cold damage and brazier shattering.
     - **Pyromancer Sustenance**: Healing the home pyromancer when downed or low on health.
   - Restock trip threshold: If remaining unfinished potion doses fall below 4 *and* inventory contains fewer than 2 unpicked Bruma herbs during a safe window (energy stable in Sustain band), recommend a harvest/concoction cycle before resuming fuel processing.

2. **Inventory Partitioning**:
   - Fixed slots: Axe, Tinderbox, Knife, Hammer (or equivalents/wearable variants).
   - Dynamic slots:
     - $S_{\text{pot}}$: Active rejuvenation potions (typically 3–5 slots of 4-dose potions).
     - $S_{\text{herb}}$: Reserve Bruma herbs (1–2 slots).
     - $S_{\text{free}}$: Working capacity for Bruma roots and fletched kindling ($28 - \text{fixed} - S_{\text{pot}} - S_{\text{herb}}$).
   - If free slots fall below 10 due to excessive potion hoarding, generate a restock rebalance instruction.

---

## State Machine Specification

The solo engine runs a deterministic finite state machine (FSM) evaluated once per client tick.

```
                          ┌──────────────┐
                          │   PREPARE    │
                          └──────┬───────┘
                                 │ Round starts (Boss energy = 100%)
                                 ▼
                          ┌──────────────┐
             ┌───────────►│ REDUCE ENERGY│◄───────────┐
             │            └──────┬───────┘            │
             │                   │ Energy <= 28%      │ Energy > 35%
             │                   ▼                    │ (e.g. stalled)
             │            ┌──────────────┐            │
             │            │   SUSTAIN    ├────────────┘
             │            └──────┬───────┘
             │                   │ Energy <= 15%
             │                   ▼
             │            ┌──────────────┐
             │            │RECOVER ENERGY│
             │            └──────┬───────┘
             │                   │ Energy >= 24%
             │                   ▼
             │               [SUSTAIN]
             │
             │ User triggers finish OR Point goal met
             └───────────────────┬────────────────────┐
                                 ▼                    ▼
                          ┌──────────────┐     ┌──────────────┐
                          │    FINISH    │     │  REASSESS /  │
                          └──────────────┘     │   RETREAT    │
                                               └──────────────┘
```

### Detailed State Contracts

#### 1. `PREPARE`
- **Entry Conditions**: Player is in Wintertodt arena or staging area before round starts; boss energy is uninitialized or 100% with doors closed.
- **Planner Goals**:
  - Verify axe, knife, hammer, and tinderbox presence.
  - Guide player to harvest Bruma herbs and take empty potion vials from supply crates.
  - Produce initial potion stock (minimum 4 doses).
  - Position player at the chosen Home Corner (SW recommended).
- **Exit Condition**: Game start event detected (doors open, boss energy active).

#### 2. `REDUCE_ENERGY` (Opening)
- **Entry Conditions**: Round active; boss energy > 28%.
- **Planner Goals**:
  - Light Home Corner brazier immediately.
  - Route player along southern perimeter to light the adjacent corner brazier (SE) if reachable safely.
  - Maintain awareness of remote brazier health; if pyromancers fall, do not cross center paths to heal unless local brazier is burning.
  - Accelerate initial burn to enter safe sustain band within 90–120 seconds.
- **Exit Condition**: Boss energy $\le 28\%$.

#### 3. `SUSTAIN`
- **Entry Conditions**: Boss energy $\le 28\%$ and $> 15\%$; finish not requested.
- **Planner Goals**:
  - Keep Home Corner brazier lit.
  - If Home brazier breaks: repair immediately (earns points and maintains pyromancer safety).
  - If Home pyromancer falls: heal with rejuvenation potion immediately.
  - If Home brazier extinguishes: relight immediately (only if energy $> 12\%$).
  - When Home brazier is burning and pyromancer is healthy:
    - If inventory has capacity: chop Bruma roots.
    - If inventory is full of roots: fletch into kindling (for point maximization) or feed directly (for XP maximization, governed by user setting).
    - If inventory contains kindling/roots and energy is rising toward 28%: feed brazier to regulate energy.
- **Exit Conditions**:
  - Boss energy $\le 15\%$ $\rightarrow$ `RECOVER_ENERGY`.
  - Finish requested or point target reached $\rightarrow$ `FINISH`.
  - Boss energy unexpectedly rises above $35\%$ $\rightarrow$ `REDUCE_ENERGY`.

#### 4. `RECOVER_ENERGY`
- **Entry Conditions**: Boss energy $\le 15\%$; finish not requested.
- **Planner Goals**:
  - **Do NOT relight the brazier.** If it goes out or breaks, leave it alone.
  - If Home pyromancer is down, **heal them immediately**. (Incapacitated pyromancers prevent ambient energy recovery).
  - Direct player to productive downtime activities:
    - Fletch existing inventory of roots into kindling.
    - Harvest Bruma herbs and brew replacement potions.
    - Chop roots if inventory permits, but do not interact with the brazier.
- **Exit Conditions**:
  - Boss energy $\ge 24\%$ $\rightarrow$ `SUSTAIN`.
  - Boss energy falls $\le 8\%$ with an active lit brazier (abnormal condition: intruder or accidental light) $\rightarrow$ emergency notification to suppress all interactions.

#### 5. `FINISH`
- **Entry Conditions**: Explicit user toggle (`Finish this round` = true) OR (`soloGoalType == POINTS` AND `earnedPoints + carriedPotentialPoints >= targetPoints`).
- **Planner Goals**:
  - Feed all remaining carried kindling and roots to the Home brazier.
  - If Home brazier is out and energy $> 10\%$: relight.
  - If Home brazier is running and adjacent braziers are accessible, route to adjacent braziers to extinguish Wintertodt completely.
  - Prevent unnecessary gathering or fletching actions.
- **Exit Conditions**: Boss energy reaches 0% (Round End event fired).

#### 6. `REASSESS` (Emergency / Retreat)
- **Entry Conditions**:
  - Player health falls below emergency threshold ($\le 25\%$ or 2 consecutive hits from death).
  - Player running low on warmth supplies while brazier breaks.
  - More than 2 corners report `UNKNOWN` state for $> 12$ ticks.
- **Planner Goals**:
  - Cease skilling interactions.
  - Consume potion for hitpoints immediately.
  - Move away from damaged brazier hazard area (1-tile blast zone).
  - Provide explanatory overlay: *"Supplies low / Scene desynced — Stabilizing before resuming."*
- **Exit Condition**: Health recovered above safety threshold and observation state restored.

---

## Configuration Specification

To be integrated into the existing `WintertodtConfig` interface:

```java
@ConfigSection(
    name = "Solo Mode",
    description = "Settings for solo Wintertodt planning and automation guidance",
    position = 10,
    closedByDefault = false
)
String soloSection = "soloSection";

@ConfigItem(
    keyName = "plannerMode",
    name = "Planner Mode",
    description = "Switch between Mass round planner and Solo round planner",
    section = soloSection,
    position = 1
)
default WintertodtPlannerMode plannerMode() {
    return WintertodtPlannerMode.MASS;
}

@ConfigItem(
    keyName = "soloGoalType",
    name = "Solo Goal",
    description = "Determines when the solo planner recommends finishing the encounter",
    section = soloSection,
    position = 2
)
default SoloGoalType soloGoalType() {
    return SoloGoalType.POINTS;
}

@Range(min = 500, max = 13500)
@ConfigItem(
    keyName = "soloTargetPoints",
    name = "Target Points",
    description = "Target points before triggering the Finish phase (Max 13,500 for roll cap)",
    section = soloSection,
    position = 3
)
default int soloTargetPoints() {
    return 13500;
}

@ConfigItem(
    keyName = "soloHomeCorner",
    name = "Home Corner",
    description = "The working corner for gathering, fletching, and brazier maintenance",
    section = soloSection,
    position = 4
)
default BrazierCorner soloHomeCorner() {
    return BrazierCorner.SOUTH_WEST;
}

@ConfigItem(
    keyName = "soloPrioritizeKindling",
    name = "Fletch Kindling",
    description = "Prioritize fletching for maximum points and construction/firemaking efficiency",
    section = soloSection,
    position = 5
)
default boolean soloPrioritizeKindling() {
    return true;
}

@ConfigItem(
    keyName = "soloFinishNow",
    name = "Finish This Round",
    description = "Manual latch: immediately exit sustain/recovery and finish the encounter",
    section = soloSection,
    position = 6
)
default boolean soloFinishNow() {
    return false;
}
```

---

## Overlay & UX Contract

Solo mode introduces distinct situational states that must be communicated without cluttering the screen or confusing the player. The overlay uses a 3-tier presentation hierarchy:

```
┌────────────────────────────────────────────────────────┐
│ WINTERTODT SOLO: SUSTAIN (SW CORNER)                   │
├────────────────────────────────────────────────────────┤
│ Primary:   Fletch Bruma Roots (Kindling: 14/22)        │
│ Energy:    21% [STABLE] (Drain: -0.0% / 10s)           │
│ Points:    8,420 / 13,500 (Carried: +320)              │
│ Supplies:  4 Doses Potion | 2 Herbs                    │
├────────────────────────────────────────────────────────┤
│ Pyromancer: 82% HP (Healthy)                           │
│ Home Brazier: BURNING (Safe to process fuel)           │
└────────────────────────────────────────────────────────┘
```

### Visual Cue Specifications

1. **Phase Header**:
   - Shows active mode (`SOLO`), current phase (`OPENING`, `SUSTAIN`, `RECOVER`, `FINISH`), and selected home corner.
   - Distinct color coding:
     - `REDUCE_ENERGY`: Cyan / Light Blue.
     - `SUSTAIN`: Soft Green.
     - `RECOVER_ENERGY`: Amber / Warning Yellow.
     - `FINISH`: Bright Violet.
     - `REASSESS`: Alert Red.

2. **Energy Rate Indicator**:
   - Shows current boss energy with a 10-second trend vector:
     - `[DRAINING]` (downward red arrow): Boss losing health ($\ge 1\%$ per 10s).
     - `[RECOVERING]` (upward green arrow): Boss gaining health ($\ge 1\%$ per 10s).
     - `[STABLE]` (horizontal gray dash): No net change over the rolling 10-second window.

3. **Point Tally with Carried Potential**:
   - Explicitly displays: `Earned + Carried Potential = Projected Total`.
   - Formula:
     $$\text{Carried Potential} = (\text{Roots} \times 10) + (\text{Kindling} \times 25)$$
   - When `Projected Total` meets `soloTargetPoints`, the overlay triggers a golden highlight and updates primary instruction to *"Goal reached: Feed fuel and finish."*

4. **Deliberate Wait Messaging**:
   - When the planner enters `RECOVER_ENERGY` with no carried roots to fletch and potions fully stocked, the overlay must **never** be blank.
   - Explicit string: `"Stand by: Energy recovering to 24% (Do not relight)"`. This prevents users from assuming the plugin has stalled or disconnected.

---

## Edge Case Handling & Mitigations

### 1. Unsolicited Player Interference (The "Crashed Solo" Problem)
- **Symptom**: Another player enters the arena unannounced and lights the SE or NW brazier, causing boss energy to plummet rapidly while the player is at 12% energy in `RECOVER_ENERGY`.
- **Detection**:
  - Boss energy rate of drop exceeds single-brazier capacity ($\Delta \text{Energy} > 2\%$ per 6 ticks).
  - Remote brazier widget changes to `BURNING` without local instruction.
- **Mitigation**:
  - Immediately flag state as `EXTERNAL_INTERFERENCE`.
  - Override recovery loop: switch directly to `FINISH` or point dumping.
  - Warn player with high-priority banner: *"Intruder detected: Energy dropping rapidly. Dump fuel immediately."*

### 2. Pyromancer Incapacitation Cascades
- **Symptom**: The Home pyromancer falls to 0 HP while the brazier is broken. The player is low on health and cannot repair the brazier due to falling snow interruptions.
- **Detection**: Pyromancer status is `INCAPACITATED`, brazier status is `BROKEN`, player hitpoints are falling.
- **Mitigation Sequence**:
  1. Priority 1: Drink rejuvenation potion if player HP $< 40\%$.
  2. Priority 2: Use rejuvenation potion dose on fallen pyromancer.
  3. Priority 3: Only after pyromancer is standing, click brazier with hammer to repair.
  4. Priority 4: Re-evaluate boss energy before relighting.

### 3. Supply Starvation (Zero Herbs, Zero Potions)
- **Symptom**: Round extends past 45 minutes; all picked herbs have been converted and drunk; player sustains damage and runs out of potions while pyromancer falls.
- **Mitigation**:
  - If total remaining doses $= 0$ and pyromancer is incapacitated:
    - Lock out brazier repair/relight completely.
    - Path player to the Bruma herb patch immediately, harvest 3 herbs, fetch vials from crate, and brew.
    - Suppress all chopping and fletching until supply buffer is re-established ($\ge 4$ doses).

### 4. Client Desynchronization and Loading Stalls
- **Symptom**: Scene reload or RuneLite memory prune clears remote game objects.
- **Mitigation**:
  - All decisions check `observation.getLastObservedTick()`.
  - If `currentTick - lastObservedTick > 8`, fall back strictly to conservative behavior: assume remote braziers are unlit unless the HUD status bar explicitly signals otherwise.
  - Never issue a brazier lighting command if the target brazier object reference is `null` or unrendered; path to target tile first, re-acquire object, verify state, then execute interaction.

---

## Test & Validation Plan

Before opening a pull request to expose the Solo toggle in user configurations, the following automated and live test batteries must pass.

### Automated Headless Test Matrix

Deterministic snapshots collected from telemetry traces will be fed through the `WintertodtSoloEngine` test harness:

| Test ID | Scenario | Injected Condition | Expected Engine Output |
| --- | --- | --- | --- |
| `TC-SOLO-01` | Normal Opening | Energy = 100%, inventory has basic tools, 4 potions. | Recommend: Light Home Brazier. |
| `TC-SOLO-02` | Opening Cutoff | Energy reaches 28%. | Transition from `REDUCE_ENERGY` to `SUSTAIN`. Stop routing to secondary braziers. |
| `TC-SOLO-03` | Sustain Loop (Fletch) | Energy = 22%, Brazier burning, 10 roots in inventory, fletch enabled. | Recommend: Fletch Bruma Roots. |
| `TC-SOLO-04` | Sustain Loop (Feed) | Energy = 26%, Brazier burning, 20 kindling, energy rising. | Recommend: Feed Kindling to Brazier. |
| `TC-SOLO-05` | Recovery Transition | Energy drops from 16% to 14%. Brazier breaks. | Transition to `RECOVER_ENERGY`. Do NOT recommend brazier repair/relight. |
| `TC-SOLO-06` | Recovery Hysteresis | Energy rises from 15% to 20% during recovery. | Remain in `RECOVER_ENERGY`. Brazier relight must remain suppressed until $\ge 24\%$. |
| `TC-SOLO-07` | Recovery Exit | Energy rises to 25% with 0 braziers lit. | Transition back to `SUSTAIN`. Recommend relighting Home Brazier. |
| `TC-SOLO-08` | Critical Relight Lockout | Energy = 9%, Brazier unlit, player presses "Finish Now". | Suppress relight recommendation. Display warning banner regarding imminent defeat. |
| `TC-SOLO-09` | Incapacitated Pyromancer | Energy = 18%, Pyromancer HP = 0, Potion doses = 3. | Recommend: Heal Pyromancer with Rejuvenation Potion. |
| `TC-SOLO-10` | Supply Restock Trigger | Doses = 1, Herbs = 0, Energy stable at 22%. | Recommend: Harvest Bruma Herbs before next gathering cycle. |
| `TC-SOLO-11` | Point Target Reached | Target = 13,500; Current = 13,200; Carried = 400 kindling pts. | Transition to `FINISH`. Recommend: Dump carried fuel and burn boss down. |

### Live Verification Protocol

1. **Trace Collection Run**:
   - Complete 3 unassisted solo games with opt-in trace logging enabled.
   - Record raw tick-by-tick logs containing: Game Tick, Energy %, Local HP, Pyromancer HP, Brazier State, Inventory Counts, and Active Actions.
   - Verify that HUD widget states match actual physical states across all 4 corners without latency artifacts.

2. **Shadow Run (Non-interfering)**:
   - Run the plugin in Solo mode with overlay active, but perform actions manually based on player judgment.
   - Flag every instance where the engine's primary recommendation differs from safe high-level solo play.
   - Quantify instruction stability: zero flickering (more than 1 recommendation change across 2 consecutive ticks under steady conditions).

3. **Active Guided Solo**:
   - Follow plugin recommendations strictly from 100% to 0%.
   - Success Criteria:
     - 0 unexpected round failures (Wintertodt dying before target points reached).
     - 0 player deaths.
     - Supply restocks recommended with safe timing margins.
     - Target score of 13,500 reached within expected tolerance ($\pm 250$ points).
