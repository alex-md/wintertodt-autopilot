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
- [ ] Cold interrupts fletching, feeding, lighting or mixing: relevant items/objects/standing tile visibly pulse with RESUME. Restart or walk to the target: verify pulsing clears.
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


## Sidebar, pause and finish forecast

- [ ] Open the brazier sidebar icon before entering Wintertodt. Supply observations show unknown rather than missing.
- [ ] Change strategy, goal, fletching mode and corner in the sidebar; confirm the plugin settings reflect them, and vice versa. Re-enabling retains these choices.
- [ ] Pause while an arrow/path is visible; verify overlays, highlights and owned arrows clear and notifications stop. Resume restores current guidance. Check hopping/loading while paused.
- [ ] Watch the finish estimate during a mass round; compare observed finishing time to the forecast and usable budget. Record lit braziers, energy and any abrupt pace change.
- [ ] Confirm a stalled drain/brazier change removes the stale forecast and eventually relearns it. Missing HUD observations do not display a false zero countdown.
- [ ] With roots held near the end, confirm chopping stops soon enough to walk and feed; test both roots-only and fletching strategies.
- [ ] Confirm a partial timed fletching batch turns into feeding when its requested count is completed or the available time shrinks.
- [ ] Confirm normal fletching routes from the roots to the adjacent brazier tile before highlighting knife/roots; test all four corners and a broken/unlit brazier.
- [ ] Check sidebar text wrapping and scrolling at RuneLite's standard width and at UI scaling settings.


## Follow-up checks from initial live feedback

- [ ] At or below the configured restore threshold with a potion held, guidance says "Drink rejuvenation potion" and highlights its dose variant, even when food is also held. Compare displayed Warmth to the game HUD; record raw Warmth from debug mode if they disagree.
- [ ] An extinguished brazier while feeding produces a visible 1.2-second border pulse, even without an interruption message. Relighting stops the pulse and takes priority over empty potion reserves.
- [ ] Repairs, urgent feeding, low Warmth and inventory blocking produce attention cues; already performing the corresponding action settles them. Disabling attention pulses removes the animation.
- [ ] With only a few roots and roughly forty usable seconds remaining, guidance does not force an immediate feeding trip. With at least 500 points and only time for a tiny new batch, reward-oriented strategies avoid another trip unless it could reach the next reward threshold; Firemaking XP mode still uses every root that fits.
- [ ] Observe predictions through ordinary energy bursts and changes between three/four lit braziers; they should retain a learned pace rather than flicker back to learning. Sustained acceleration should shorten the estimate gradually.


## Foreground callouts and contrast

- [ ] In a crowded mass world, all action callouts remain readable in front of actors and are offset beside their marked tile. Check fletching, chopping, feeding, repairs, relighting and supplies.
- [ ] Near viewport edges, the callout switches sides or clamps inside the game area. It must not spill onto chat, inventory or the sidebar.
- [ ] Teal normal highlights remain visible against snow and stone. Orange/red attention colors and border pulses retain their existing behavior.
- [ ] Southwest chopping uses the ordinary reachable interaction tile again; there is no fixed safespot setting or route.


- [ ] In Firemaking XP mode at 8–10% energy, relight, then verify guidance chops a small viable batch rather than declaring the round finished. After feeding, it should offer another batch if at least one additional root fits.
- [ ] During a temporary loss of the forecast after relighting, Firemaking XP mode continues with its small-batch fallback; at truly imminent energy with no estimate, it still waits.
- [ ] Teal outlines have subtle feathering, while route cores remain thin and urgent colors/pulses stay distinct.
