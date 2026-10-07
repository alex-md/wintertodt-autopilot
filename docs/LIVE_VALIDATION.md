# Live Wintertodt validation

Status: **not performed**. Use a mass world before publishing or adding Solo. Run `./gradlew run`, enable the plugin's debug overlay, and perform all gameplay manually.

## Observation gate

- [ ] Enable while already inside the arena: correct brazier/tree/pyromancer appear without requiring a region reload.
- [ ] Compare Warmth, energy, points and countdown against the game HUD.
- [ ] Enter mid-round and confirm active guidance in the safe preparation area south of Y 3988; verify the four corner anchors.
- [ ] Confirm base scene IDs and current impostor IDs across lit, unlit and broken brazier transitions.
- [ ] Confirm pyromancer healthy/down transformations and correct local association.
- [ ] Equip main/offhand bruma torches and Imcando hammers; confirm tool availability independently from knife/tinderbox.
- [ ] Check ordinary axe variants and food, including cake at full Hitpoints and food that heals less than 4 HP.
- [ ] Confirm potion dose counts and mixing with and without Druidic Ritual.
- [ ] Enter with only tools: obtain recommended potions before chopping; check herb and vial targets.
- [ ] Compare reserve at low/high Firemaking, with zero/four warm items, and entering partway through a round.
- [ ] Drink one dose: stay on the resource loop. Finish the round with partial potions: restock before the next round.
- [ ] Check ingredient gathering leaves a mixing slot; a full fuel inventory can be fed to make space.

## Planner gate

- [ ] Complete three consecutive Balanced rounds following only the current instruction.
- [ ] Confirm cold interrupts fletching/feeding and chopping remains correctly detected.
- [ ] Break the brazier while chopping: the chopping objective stays active; finish the batch before maintenance guidance.
- [ ] Cold interrupts fletching, feeding, lighting or mixing: relevant items/objects/standing tile gently pulse with RESUME. Restart or walk to the target: verify pulsing clears.
- [ ] Interrupt fletching with repair and Warmth restoration; confirm the same batch resumes.
- [ ] Test XP, Points / Rewards and Low Attention; confirm materially different batch/fletching behavior.
- [ ] Test projected points near 500 and 1,000 without redundant gathering before feeding.
- [ ] Confirm healthy pyromancer is required for lighting and potion preparation stays local where possible.
- [ ] At low energy, confirm immediate feeding replaces long chopping/fletching.
- [ ] Check full inventories blocking ingredients/tools produce achievable guidance.
- [ ] With low Warmth and no supplies, confirm the highlighted door leads to the safe lobby.

## Presentation and lifecycle gate

- [ ] Read Minimal, Normal, Detailed and debug panels at common interface scales; move and resize the overlay.
- [ ] Only the current object/NPC and applicable inventory items receive strong highlights.
- [ ] Toggle walking paths and standing tiles independently. Check large outlines/labels and inventory highlights at common interface scales.
- [ ] Follow walking lines around arena walls to crates, herbs, trees and braziers. Change the target, move, disable the line and confirm route updates/clears.
- [ ] Existing game/Quest Helper arrows remain intact; disable the plugin and confirm only its own arrow is removed.
- [ ] Confirm idle grace, inactivity delay and notification cooldown on actual action timing.
- [ ] Confirm automatic corner stability when moving at the center; intentionally change corners and test manual corner selection.
- [ ] Leave, log out, hop, reconnect, finish and start new rounds; confirm no stale arrows, objects, points or batch state.
- [ ] Record decision-change reasons and timing mismatches. Tune heuristics only from observed rounds.

Do not mark these checks complete from unit tests. Solo energy stabilization remains separate advanced work. Collision-aware paths require the live checks above.
