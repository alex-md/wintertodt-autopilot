# Live Wintertodt validation

Status: **in progress; final full validation pending**. Validate mass behavior before publishing. Experimental Solo requires the separate gates below. Run `./gradlew run`, enable the plugin's debug overlay, and perform all gameplay manually.

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
- [ ] Toggle walking paths and standing tiles independently. Check target outlines and inventory highlights at common interface scales.
- [ ] Follow walking lines around arena walls to crates, herbs, trees and braziers. Change the target, move, disable the line and confirm route updates/clears.
- [ ] Existing game/Quest Helper arrows remain intact; disable the plugin and confirm only its own arrow is removed.
- [ ] Confirm idle grace, inactivity delay and notification cooldown on actual action timing.
- [ ] Confirm automatic corner stability when moving at the center; intentionally change corners and test manual corner selection.
- [ ] Leave, log out, hop, reconnect, finish and start new rounds; confirm no stale arrows, objects, points or batch state.
- [ ] Record decision-change reasons and timing mismatches. Tune heuristics only from observed rounds.

Do not mark these checks complete from unit tests. Solo energy stabilization remains separate advanced work. Collision-aware paths require the live checks above.


## Built-in settings, pause and finish forecast

- [ ] Confirm no plugin sidebar icon is added. Open Wintertodt Autopilot through RuneLite settings and through the overlay Configure menu.
- [ ] Change strategy, goal, fletching mode and corner in built-in settings; confirm the next tick uses the new plan. Re-enabling retains these choices.
- [ ] Pause while an arrow/path is visible; verify overlays, highlights and owned arrows clear and notifications stop. Resume restores current guidance. Check hopping/loading and restarting while paused; the saved pause setting remains in effect.
- [ ] Watch the finish estimate during a mass round; compare observed finishing time to the forecast and usable budget. Record lit braziers, energy and any abrupt pace change.
- [ ] Confirm a stalled drain/brazier change removes the stale forecast and eventually relearns it. Missing HUD observations do not display a false zero countdown.
- [ ] With roots held near the end, confirm chopping stops soon enough to walk and feed; test both roots-only and fletching strategies.
- [ ] Confirm a partial timed fletching batch turns into feeding when its requested count is completed or the available time shrinks.
- [ ] Confirm normal fletching routes from the roots to the adjacent brazier tile before highlighting knife/roots; test all four corners and a broken/unlit brazier.
- [ ] Check Normal acknowledges an action in progress, Resume appears after interruptions, and urgent warmth interrupts the routine cue. Minimal still shows warmth. Check wrapping at common interface scales.


## Follow-up checks from initial live feedback

- [ ] At or below the configured restore threshold with a potion held, guidance says "Drink rejuvenation potion" and highlights its dose variant, even when food is also held. Compare displayed Warmth to the game HUD; record raw Warmth from debug mode if they disagree.
- [ ] An extinguished brazier while feeding produces a visible 1.2-second border pulse, even without an interruption message. Relighting stops the pulse and takes priority over empty potion reserves.
- [ ] Repairs, urgent feeding, low Warmth and inventory blocking produce attention cues; already performing the corresponding action settles them. Disabling attention pulses removes the animation.
- [ ] With only a few roots and roughly forty usable seconds remaining, guidance does not force an immediate feeding trip. With at least 500 points and only time for a tiny new batch, reward-oriented strategies avoid another trip unless it could reach the next reward threshold; Firemaking XP mode still uses every root that fits.
- [ ] Observe predictions through ordinary energy bursts and changes between three/four lit braziers; they should retain a learned pace rather than flicker back to learning. Sustained acceleration should shorten the estimate gradually.


## Scene highlights and contrast

- [ ] Orange-yellow normal highlights remain visible against snow and stone. Orange/red attention colors and border pulses retain their existing behavior.
- [ ] Southwest chopping uses the ordinary reachable interaction tile again; there is no fixed safespot setting or route.


- [ ] In Firemaking XP mode at 8–10% energy, relight, then verify guidance chops a small viable batch rather than declaring the round finished. After feeding, it should offer another batch if at least one additional root fits.
- [ ] During a temporary loss of the forecast after relighting, Firemaking XP mode continues with its small-batch fallback; at truly imminent energy with no estimate, it still waits.
- [ ] Orange-yellow outlines have subtle feathering, while route cores remain thin and urgent colors/pulses stay distinct.


## Quiet while working, pulse when interrupted

- [ ] Start chopping, feeding and fletching: the matching target outline, standing tile, path, owned hint arrow and inventory borders disappear once that activity is observed. The main overlay still acknowledges progress.
- [ ] Interrupt feeding with fuel remaining: the fuel highlights pulse again. Extinguish or break the brazier: remaining roots/kindling pulse alongside the required relight/repair items and scene cue, even if the last feeding animation has not cleared.
- [ ] Begin the repair/relight: matching cues settle. Resume feeding: all fuel borders disappear again. Low warmth must still take priority over fuel attention.
- [ ] Disable attention pulses: needed highlights remain steady. Disable inventory highlights: fuel attention is suppressed. Other plugins' hint arrows remain intact.
- [ ] Move the camera near each viewport edge and test fixed/resizable layouts. Optional routes and tile strokes stay in the game viewport.


## Specific supply instructions

- [ ] With missing tools, the main panel says "Take 1 axe/hammer/knife/tinderbox" and the correct crate has a brighter four-pixel outline.
- [ ] While collecting potion ingredients, the main panel shows the remaining quantity (for example, "Take 2 unfinished potions", then "Take 1 unfinished potion", then "Pick 3 bruma herbs"). Check single/plural names and limited inventory room.


## Clean scene defaults

- [ ] With default settings, there is no walking line or floor square. Target outlines, owned hint arrows and inventory attention still work.
- [ ] Enable paths or standing tiles explicitly: paths draw a line; standing tiles mark only the destination. No intermediate breadcrumb squares appear. Turn both off and confirm the main instruction panel remains available.
- [ ] Existing profiles with saved true values need Show walking path and Highlight standing tiles switched off once; subsequent restarts retain those choices.


## No floating scene text

- [ ] During supplies, chopping, feeding, repairs, relighting and countdown positioning, no floating text box appears beside the target, even with optional paths/standing tiles enabled.
- [ ] All instructions and quantities appear in the main movable guidance panel. Object/NPC outlines and applicable inventory attention cues identify the target.
- [ ] Hide/show the main panel through settings and confirm there is no second instruction overlay in the scene.


## Phase 2 observation trace gate

The observation pipeline supports Mass and experimental Solo. Collect initial Solo traces using your own gameplay decisions before following experimental guidance.

- [ ] Launch with `./gradlew run`, enable **Debug overlay and logs**, and locate `solo_observation_v1` lines in the RuneLite debug log. Disable debug and verify tick traces stop.
- [ ] Visit all four corners and the safe preparation area. Compare corner names, brazier states and pyromancer states with scene entities; compare health percentages only when a health bar is visible.
- [ ] Observe lit/unlit/broken transformations and healthy/downed pyromancers. Verify separate observation timestamps and that missing health bars read -1.
- [ ] Move away or rebuild the scene: absent entities become UNKNOWN immediately, last-seen history expires after 8 ticks, and incomplete coverage reports `observedGlobalLit=-1` even when `loadedLit=0`.
- [ ] Hop, disconnect, leave/re-enter, and finish/start a round. Verify old corner timestamps do not survive resets. Previously collected snapshots must retain their original values.
- [ ] Record manual solo drain/recovery cycles, remote lighting and HUD changes alongside the tick trace. Compare the current compass-layout HUD decoder against each physical brazier, including sprite states and hidden interfaces. No precise recovery countdown is inferred.
- [ ] Confirm mass instructions, highlights and corner selection remain consistent with prior behavior while traces are enabled.


## Full Solo guidance gate (pending live validation)

- [ ] Mode defaults to Mass. Select Solo, verify Automatic follows nearby working corners after remote lighting, and test an explicit pinned corner from the opposite side of the arena. Changing mode clears owned cues and previous planner state.
- [ ] Prepare with missing tools and no potions. Verify equipped torch/hammer/axe alternatives, knife overrides, the initial 24-dose reserve, mixing space, and Brew'ma without Druidic Ritual.
- [ ] At 100% energy, follow opening routes to multiple corners. Every object, NPC, arrow and path must agree with the displayed destination, including when an entity is initially unloaded.
- [ ] At ≤28%, stop remote maintenance. Above 35%, resume reducing until ≤28%; oscillation near 35% must not flicker between routes.
- [ ] At ≤10% during sustain, leave extinguished braziers unlit and avoid maintenance that resumes drain. Fletch/gather/restock productively or show an explicit wait. Recovery timing must not be promised.
- [ ] Hold recovery below 12%. Confirm three distinct observed ticks at or above 12% are required to resume, including after a loading gap.
- [ ] A lit remote brazier prevents adding home drain during sustain. Unknown/stale global states suppress relighting; scene/HUD disagreement invalidates remote readings immediately.
- [ ] A downed pyromancer is healed at the indicated destination before repair/light, with a spare personal warmth dose. At low warmth every phase restores or retreats first.
- [ ] Drink doses during sustain: no trip for each dose; below eight doses in Points or four in XP, replenish toward 24 and repeat later. Full fuel bags feed for ingredient room where a lit brazier is available; otherwise explain the required space.
- [ ] Compare Solo Points / Rewards and Firemaking XP, plus Always/Never fletch overrides. No Mass deadline or ending heuristic may terminate Solo gathering.
- [ ] Reach the goal with carried fuel: feed it before switching to Finish. Earned points trigger finishing; raising the goal resumes sustain if no manual finish is latched.
- [ ] Request Finish while chopping, fletching, feeding and recovering. Feed carried fuel before lighting additional braziers. After finishing is intentional, low-energy relights are permitted; sustain still forbids them at ≤10%. Maintain a stable pair of adjacent braziers, or only the working brazier at ≤10% run energy.
- [ ] Simulate a rapid drop or another player lighting remotely. Feed carried fuel promptly at an observed lit brazier; do not identify an intruder from energy alone.
- [ ] Lose the HUD temporarily: reassess, keep the finish latch, and preserve recovery intent. Verified completion/countdown clears the finish setting and the next round starts fresh.
- [ ] Pause/resume and change settings mid-round. No notifications or cues while paused; automatic/pinned corner selection, the fixed goal and finish intent remain understandable.
- [ ] Compare Normal, Minimal and Detailed Solo overlays at common scales. Show phase, energy and warmth; no Mass finish countdown appears.
- [ ] Complete manual trace runs, then shadow guidance, then guided Solo rounds with recorded transitions. Record unexpected early endings, supplies, point totals and every guidance mismatch before calling Solo validated.


## Strategy audit follow-up checks

- [ ] In Points mode, a visibly damaged but standing working pyromancer triggers healing. While fletching, an observed injured remote pyromancer can be targeted; unknown/stale health cannot. Low run energy avoids remote trips.
- [ ] In XP mode, damaged standing pyromancers do not interrupt the fuel loop; incapacitation does. During recovery, never heal a downed pyromancer at a lit or unknown-state brazier to restore drain.
- [ ] Fletch at roots or while moving without a mandatory brazier-adjacency trip. The preparation area still routes into the arena. Mass positioning rules remain unchanged.
- [ ] Opening destination persists while Automatic changes working corners mid-walk. Routine gathering then uses the local working corner.
- [ ] Full fuel bag, no potions, unlit working brazier: above the sustain floor with a healthy pyro and tools, relight/repair and feed to make room. In recovery, or with a downed pyro and no dose, explicitly request inventory space instead.
- [ ] An intentional finish at 5–10% relights the selected brazier without waiting for energy to recover. Unknown global coverage still blocks relighting.
- [ ] Finishing with two maintained braziers does not route to a third/fourth merely because the other corners are unlit. Confirm run energy is read as a percentage and depleted run chooses one working brazier.

## Northern gap routing

- [ ] In Solo with Show walking path enabled and at least 60 base and current Agility, travel between the back two braziers in both directions. The line follows all three manual jumps when shorter than walking around. Check the pillar landing tiles and interaction footprints against the scene.
- [ ] Below 60 Agility, with Agility drained below 60, or in Mass, use the normal walking route. Changing eligibility updates the line without moving.
- [ ] Short same-side trips stay on foot. Missing/unloaded gap tiles do not draw a jump. Disable paths, pause guidance or leave the region and confirm the route clears.
