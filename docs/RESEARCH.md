# Research and detection choices

Inspected 7 October 2026 before implementing the planner. Upstream source was cloned into a temporary research directory; this plugin has no runtime dependency on those projects.

## Sources inspected

- [RuneLite Wintertodt implementation](https://github.com/runelite/runelite/tree/42a6f17a6a2e8e478aa763890ecd0181a59dad38/runelite-client/src/main/java/net/runelite/client/plugins/wintertodt): `WintertodtPlugin`, `WintertodtOverlay`, `WintertodtActivity`, `WintertodtConfig` and interruption types. Mirrored the animation cases, damage exceptions and roughly three-second inactivity grace. The adapted animation table retains the upstream BSD notice in `WintertodtConstants.java`.
- [RuneLite gameval constants](https://github.com/runelite/runelite/tree/42a6f17a6a2e8e478aa763890ecd0181a59dad38/runelite-api/src/main/java/net/runelite/api/gameval): named item, object, NPC, animation, interface and varbit definitions. The project compiles against published RuneLite 1.13.1.
- [RuneLite status bars](https://github.com/runelite/runelite/blob/42a6f17a6a2e8e478aa763890ecd0181a59dad38/runelite-client/src/main/java/net/runelite/client/plugins/statusbars/StatusBarsOverlay.java): confirms `WINT_WARMTH / 10` is the percentage.
- [RuneLite item stats](https://github.com/runelite/runelite/tree/42a6f17a6a2e8e478aa763890ecd0181a59dad38/runelite-client/src/main/java/net/runelite/client/plugins/itemstats): theoretical healing remains useful at full HP; combined with an Eat action to avoid ordinary healing potions.
- [GOTR Autopilot](https://github.com/FabulousOtter/gotr-autopilot/tree/feb04c81d2e58bb88294ef313be3744fc57b2611): inspected state snapshots, planner/strategy types, step presentation, scene/item overlays, pathfinder and hint-arrow lifecycle. Adopted the observation → planner → cached presentation separation; did not copy the GOTR route or pathfinder.
- [Quest Helper](https://github.com/Zoinkwiz/quest-helper/tree/75b623a6bc14237831fddc10b44765c0910a4eb0/src/main/java/com/questhelper/steps): inspected `DetailedQuestStep`, `ObjectStep`, `NpcStep`, inventory requirement rendering and arrow presentation. Followed the single current step and item/object/NPC highlighting pattern, without adding Quest Helper as a dependency.
- [Wintertodt Solo Helper](https://github.com/aquaosrs/wintertodt-solo-helper/tree/c5761f0224c834b79d76c0f1b25b7ce4964a2211): inspected fixed brazier locations, local targets, HUD state and conditional relighting. Its old widget offsets and reversed-looking pyromancer names were not used as authoritative signals. Solo has intentionally not been implemented.
- [Current Wintertodt mechanics](https://oldschool.runescape.wiki/w/Wintertodt) and [Warmth](https://oldschool.runescape.wiki/w/Warmth): fetched current wiki source, including the April 2025 healing-point change and October 2024 Warmth changes. These describe gameplay mechanics, rather than RuneLite API behavior.

## Confirmed mechanics used

| Action / condition | Value / behavior |
|---|---|
| Feed a root | 10 points |
| Feed kindling | 25 points |
| Light or repair | 25 points |
| Heal a pyromancer | 75 points; one rejuvenation dose |
| Minimum rewarded round | 500 points |
| Additional guaranteed reward | Each additional 500 points; partial thresholds can improve reward probability |
| Rejuvenation drink | 30% Warmth |
| Qualifying food | At least 4 HP healing, 35% Warmth; known exceptions include triangle sandwiches and bottles of wine |
| No remaining Warmth | Death |
| Incapacitated pyromancer | Cannot drain energy or permit relighting until healed |
| Feeding | Generates XP/points; does not accelerate energy drain |

## Signal mapping

| Observation | Source |
|---|---|
| Region | Local world location, region 6462, matching RuneLite |
| Inside prison | Region 6462 includes the safe preparation area; no Y 3988 entrance cutoff |
| Warmth | `VarbitID.WINT_WARMTH`, scaled by 10 |
| Start countdown | `VarbitID.WINT_TRANSMIT_RESPAWNDELAY * 30 / 50`, matching RuneLite |
| Energy | Visible `InterfaceID.WintStatus.ENERGY_TITLE` text, with legacy `ENERGY` fallback |
| Current round points | Visible `InterfaceID.WintStatus.POINTS` text |
| Roots, kindling, herbs, unfinished potion, doses | `InventoryID.INV`, `ItemID.WINT_*` |
| Equipped torch / hammer / axe | `InventoryID.WORN`; both offhand and main variants supported |
| Brazier state | Cached scene objects with current impostor composition, `ObjectID.WINT_BRAZIER*` |
| Pyromancer state | Cached NPCs with transformed composition, `NpcID.WINT_WIZARD` / `WINT_WIZARD_DOWN` |
| Activity | Current named animations, resource deltas, game messages and recent menu observations |
| Healing / consumption intent | Observed user menu actions; no interactions are dispatched |
| Potion mixing eligibility | RuneLite `Quest.DRUIDIC_RITUAL.getState`, sampled on entry |
| Ending category | Energy, loaded lit braziers and smoothed observed drain over 60–150 ticks |

`VarPlayerID.WINT_INT` is opaque without a verified packing definition. It is not guessed or unpacked. The widget parser strips color tags, accepts grouped point values, and returns -1 for absent/malformed text. It does not infer a completed round merely because a widget disappears.

Scene references are cached via spawn/despawn and NPC change events. One scene bootstrap occurs when enabling the plugin in a loaded region or after a scene load. Object/NPC transformations and the small inventory are sampled each game tick. Overlays use only cached decisions/targets and never scan the scene or plan strategy.

## Validation status

Automated scenario tests exercise the independent planner and mocked observation signals. A RuneLite 1.13.1 developer-client smoke launch reached the login screen, and RuneLite logged `WintertodtAutopilotPlugin is now running` with its event handlers registered. No game login, movement or interactions were performed. The upstream client logged a non-fatal Java 17 `ReflectUtil` annotation-cache diagnostic while continuing initialization.

This original research pass did not perform live minigame validation. For the current 0.3.0 release decision, reported maintainer testing and outstanding scenario checks, see [Phase 3 progress](PHASE_3_PROGRESS.md) and the [live checklist](LIVE_VALIDATION.md).

## Mid-round entry correction

A live screenshot showed the player inside the prison preparation area with 70% energy, while guidance said to enter. Removed the incorrect Y >= 3988 entrance check: the safe preparation area is already inside the prison. The current visible energy text is read from `ENERGY_TITLE`; the older `ENERGY` widget can be hidden. Regression tests cover this combination, preparation-area location, and hidden stale HUD text.

## Preparation and potion reserve

The helper checks axe, hammer, knife (when fletching is enabled), and tinderbox or bruma torch before the resource loop. It then gathers unfinished rejuvenation potions and bruma herbs and guides mixing. Prepared reserves are latched during a round; an empty reserve or the between-round preparation phase triggers replenishment. Low Warmth, necessary local repairs/healing, and urgent feeding retain priority.

The [Warmth mechanics](https://oldschool.runescape.wiki/w/Warmth), [Wintertodt strategies](https://oldschool.runescape.wiki/w/Wintertodt/Strategies), and [damage calculator module](https://oldschool.runescape.wiki/w/Module:Wintertodt_damage_calculator) were inspected for the reserve estimate. Standard damage is `floor((16 - W - min(2B, 6)) * 100 / FM)`, with at most four warm items and three contributing lit braziers. Hitpoints is absent from this formula. Passive restoration is 8% per minute plus 1% per equipped warm item; a potion dose restores 30%. The equipment registry was extracted only from the warm-clothing section, excluding the non-warm list.

Planning assumes three braziers in a typical four-minute mass round, approximately twenty ordinary attacks plus two special attacks, and one spare pyromancer dose. Remaining energy scales ordinary exposure quadratically and special exposure linearly, reflecting diminishing attack likelihood. Current Warmth and carried food reduce the estimate. The result is rounded up to full four-dose bottles and bounded to 1–3 bottles. These attack counts are heuristic assumptions, not a verified prediction of a player's round. Unknown clothing uses two warm items as a fallback. The panel exposes observed Firemaking/warm-item counts and the latched dose reserve for live tuning.

## Collision-aware walking line

A bounded breadth-first search uses the current plane's RuneLite collision map. It validates directional walls on both sides of every step, blocked objects/floor tiles, and unloaded tiles. Cardinal steps avoid diagonal corner cutting. Object footprints use `GameObject` scene bounds; NPC footprints use transformed size. The destination is an orthogonally adjacent interaction tile. Unreachable targets produce no path, without a straight-line fallback.

Routes are computed on game ticks, reused while the player/target are unchanged, invalidated by object scene events, and refreshed at least every five ticks. Rendering projects every cached tile edge without joining across missing projections. Tests cover wall detours, one-sided directional walls, multi-tile targets, unreachable tiles, cache invalidation and disabling/resetting the path. They do not replace live collision/door checks.

## Continuous resource guidance and restart emphasis

An observed chopping action keeps the normal resource batch ahead of brazier repair/healing/lighting. When that batch is ready, guidance advances to processing/feeding; Warmth restoration and ending-round priorities still preempt chopping. This avoids sending a chopping player to a broken brazier they do not yet need.

Walking routes now use a four-pixel dark outline and a 1.6-pixel colored core. The final walkable tile gets a filled outline and action label; occasional route tiles provide floor breadcrumbs. Route calculation remains enabled for standing tiles even when the line is hidden. Fletching/mixing mark the current standing tile rather than manufacturing a movement destination.

Interrupted actions use one shared restart condition: active round, interruption observed, actionable objective, and idle activity. The panel shows RESUME. Attention pulses additionally cover required repairs, relighting, healing, low Warmth, urgent feeding, mismatched chopping/fletching/feeding activity, and inventory blocking. They do not depend on an interruption chat message. Opacity ranges from 25% to 100% over a 1.2-second cycle, with a visible border-width change. Scene, inventory and main-panel borders share the cue. Starting the corresponding action settles it; walking settles ordinary cues but preserves low-Warmth emphasis. A stale animation event within one tick of an interruption cannot erase the restart cue. Rendering changes only presentation; no click or movement is dispatched.

## Plugin experience and predictive finishing (13 October 2026)

Revisited [GOTR Autopilot at feb04c8](https://github.com/FabulousOtter/gotr-autopilot/tree/feb04c81d2e58bb88294ef313be3744fc57b2611), particularly `InstructionOverlay` and config sections; [Quest Helper's documented sidebar and requirements workflow](https://github.com/Zoinkwiz/quest-helper/blob/master/README.md); and [One Click Summer Garden at c294707](https://github.com/LlemonDuck/one-click-summer-garden/tree/c294707f2c3653236b5882d824574ef8d495d879), particularly `SummerGardenOverlay`, `SummerGardenConfig` and the countdown/notification lifecycle. Used these as interaction references: one clear step, requirements alongside instructions, and visible timing uncertainty. No source from these projects was copied and no runtime dependency was added.

The new RuneLite sidebar provides the current instruction, a plain-language explanation, observed supplies, point progress and persisted quick settings. Outside Wintertodt, observations remain unknown rather than falsely marking tools missing. A transient pause clears owned arrows/routes/targets and suppresses all guidance highlights and notifications, including emergency warmth alerts. Observations continue. Pausing survives scene loads and world hops but clears when disabling/re-enabling the plugin. Swing receives immutable panel state; programmatic control updates do not save settings.

The finish forecast fits energy against ticks over up to 150 ticks and over the latest 60 ticks. At least thirty ticks and three separate energy drops are required. Valid slopes are blended (65% longer window, 35% recent window), then smoothed on energy drops (60% previous rate, 40% new rate). This replaces the earlier fastest-window rule, which chased bursts. Changes between nonzero lit-brazier counts keep the learned history. Energy increases, non-monotonic ticks, unavailable signals and zero lit braziers invalidate the history. Twenty ticks without a drop invalidate the displayed prediction. This is a pace estimate, not an exact countdown or a guarantee that braziers stay lit.

Planning uses 90% of that estimate minus three seconds, reserving margin for interruptions and player response. Current planning allowances are 1.8 seconds per feed, 1.8 seconds per fletch, and 3.6 seconds per additional root. A [fletching knife](https://oldschool.runescape.wiki/w/Fletching_knife), carried or wielded in the off-hand, saves one tick on every root after the first in a batch, so planning allows 1.2 seconds for each of those roots. These are explicit heuristic costs, not measured personal production rates. Walking uses a conservative cardinal distance to an adjacent tile on the observed brazier footprint, at 0.6 seconds per tile plus 1.2 seconds to begin interaction. The allowance does not assume running; it does not yet measure collision detours or individual chopping rates. Missing brazier distance reserves ten seconds.

The planner caps the current chopping instruction against travel, useful fletching upgrades and feeding costs, without permanently shrinking the underlying batch when an estimate is corrected. It commits carried fuel to finishing when another root would not fit, rather than forcing a final batch at an arbitrary 45-second cutoff. It may fletch a bounded number of useful upgrades, reducing that number if pace accelerates, then feeds all carried fuel. Once feeding is committed it does not return to fletching/chopping while fuel remains. Losing the forecast during a committed finish switches to feeding. After securing 500 points, reward-oriented strategies normally require room for at least six roots on a new trip; smaller trips remain allowed if they could reach the next reward threshold. Firemaking XP mode requires only one root that fits the available time, and can resume a small late batch after feeding or relighting. When its estimate is unavailable at 8–10% energy, XP mode uses the existing small-batch fallback rather than remaining latched as finished. Truly imminent rounds without a usable estimate still wait. With no usable estimate, the original conservative energy categories still apply. Warmth restoration and necessary brazier maintenance retain priority. Relighting precedes replenishing a spent potion reserve. A partial reserve or carried fuel also prevents a late supply trip just to round doses up to a full bottle; low-Warmth recovery still takes priority.

Normal and timed fletching first guide the player onto the walkable interaction tile beside the working brazier. The knife/root inventory highlights appear after arrival. All guidance remains advisory; no movement or interactions are dispatched.

Scenario tests cover acceleration, stalls, resets, shortened batches, partial fletching, travel allowances, the transition to feeding, low-energy opportunities, strategy overrides, fletching location, and pause behavior. Sidebar previews use RuneLite's actual look and feel at its 225-pixel panel width. Live timings and object-footprint checks remain on the acceptance checklist.


## Live feedback corrections

The fixed southwest chopping tile was removed after the player confirmed it no longer works as a safespot. Chopping uses ordinary collision-aware interaction-tile selection in every corner. Normal fletching still takes place beside the brazier.

Scene labels now use white 12-pixel sans-serif text on a dark label background. Main instructions and sidebar text also use sans-serif fonts, with white routine instructions and colored urgent warnings. Routine target, route and inventory highlights now use teal (RGB 0, 160, 145), with slight feathering around their crisp cores. Normal scene outlines are two pixels; the walking core is 1.6 pixels. Pulse peaks temporarily expand the attention border, rather than making all normal outlines thick.

Rechecked [RuneLite's current Warmth renderer](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/statusbars/StatusBarsOverlay.java): it still reads `WINT_WARMTH / 10`. This did not establish a live varbit failure. The plugin now preserves a valid positive Warmth reading even if round widgets are absent, avoids silently clamping out-of-range raw values to 100%, and shows raw Warmth and the restore threshold in diagnostics. Warmth recovery precedes preparation and round-HUD decisions. Held rejuvenation potions take priority over food and the instruction explicitly says to drink; food remains a fallback. The new default restore threshold is 50%, while saved choices remain authoritative. Tests exercise observed Warmth → snapshot → planner → potion item and attention cue. Live confirmation is still required.


## Foreground direction callouts

The live screenshot showed actor models obscuring the direction text drawn in `ABOVE_SCENE`. Labels now have a separate `UNDER_WIDGETS` overlay, which RuneLite renders in [drawAboveOverheads](https://github.com/runelite/runelite/blob/runelite-parent-1.13.1/runelite-client/src/main/java/net/runelite/client/callback/Hooks.java) after actors and overheads; model outlines, floor tiles and routes retain their scene layer. The foreground overlay clips to the game viewport, and interface widgets are drawn afterward so chat/inventory panels stay above the callout. Callouts use opaque dark backgrounds, white bold sans-serif text, a teal/action-colored edge, and a short connector to the standing tile. They sit beside the projected tile, switch to its left near the right viewport edge, and clamp to the viewport. Offscreen destinations or viewports too small for the label produce no callout. Both overlays use the same standing-point selection, and pause/standing-tile settings suppress both.


## Firemaking XP finishing correction

The six-root minimum introduced to avoid tiny reward-focused trips was incorrectly shared by Firemaking XP mode. It could report no time remaining even when the budget admitted two or three raw roots. XP mode now treats each root as useful, retains the feeding deadline, and resumes when a small additional batch fits. It also bypasses the finished-batch latch when timing temporarily disappears around relighting, while keeping the conservative imminent-round stop. Regression scenarios cover 8% energy, relighting, three roots followed by another two-root opportunity, missing forecasts, and a genuinely insufficient time budget.

Normal highlights now use teal. Scene model outlines increase their feathering from two to three pixels. Walking, tile and inventory borders add two low-opacity edge strokes (18/255 and 42/255), extending only 1.5 pixels beyond the core, with antialiasing. Existing thin route cores and attention-pulse behavior are retained.


## Settings and guidance polish — 13 October 2026

Removed the custom Swing sidebar and its toolbar entry. RuneLite configuration is now the only controls surface, with saved pause, plan choices, and optional display/notification settings. Pause continues observing the round but clears owned cues and suppresses notifications. It now persists across disabling/re-enabling and restarts, replacing the transient pause described above.

The overlay uses an observed-action status, a short contextual instruction, warmth even in Minimal, and separate earned/carried points. Detailed/debug views retain estimates and diagnostics. Synthetic previews cover active, interrupted, urgent, paused, Minimal and debug states; live scaling and game checks remain open.

See [Phase 2: Solo mode](SOLO_MODE_RESEARCH.md) for fresh source inspection, the proposed phase model, limitations of existing observations and the implementation/validation sequence. No Solo behavior has been enabled.


## Scene clutter and interrupted fuel — 13 October 2026

Live screenshot feedback showed overlapping object/tile/arrow/callout cues, including a CHOP label while chopping was already underway. Scene and inventory overlays now use the same observed-action check as the main panel: matching activity suppresses those cues. The client-tick path and owned hint arrow also settle until the player needs to act again. A new maintenance objective still wakes cues even if the previous feeding animation persists briefly.

When an unlit/broken brazier blocks feeding, carried roots and kindling join the relight/repair/healing attention cues. Once the matching maintenance or feeding action starts, inventory borders disappear. Warmth guidance retains priority, and the pulse/highlight toggles still apply.

Pending callouts avoid nearby projected player head/feet bounds and the target NPC, use an opaque backing, and omit the leader line. If nearby placements are all occupied, the main panel remains the instruction source. Head/feet projections avoid per-frame model-hull calculations for mass-world crowds. Paths and floor markers use a separate viewport-clipped graphics context. These changes still require the new live checks above.


## Specific supply callouts — 13 October 2026

Replaced generic SUPPLIES / TAKE HERE labels with explicit remaining quantities for tools, unfinished potions, herbs and Brew'ma mixing. Tool objectives now carry quantity one and use the same text on the main panel and crate. Ingredient callouts follow the planner's current remaining count, including inventory-space limits. Supply callouts anchor to the observed target itself and remain available when target highlighting is enabled but paths/standing tiles are off. Supplies use brighter teal and a four-pixel normal model outline.


## Remove floor clutter — 13 October 2026

Walking paths and standing-tile highlights now default to off. Removed intermediate breadcrumb squares entirely; opting into standing tiles marks only the destination. All action labels can anchor to the target object/NPC independently of path and standing-tile settings, keeping the instruction without floor clutter. Existing explicitly saved settings are retained and can be turned off in configuration.


## Final presentation decision: remove floating text — 13 October 2026

This decision supersedes the earlier callout-placement and supply-callout work. Removed the floating-label overlay class and registration entirely, plus its placement code/tests and preview samples. Instructions and remaining supply quantities now appear only in the main movable panel; the scene retains target outlines, owned arrows and opt-in floor/path cues. No setting can restore the removed floating boxes.

Reinspected [GOTR Autopilot's InstructionOverlay](https://github.com/FabulousOtter/gotr-autopilot/blob/feb04c81d2e58bb88294ef313be3744fc57b2611/src/main/java/com/fabulousotter/gotr/overlay/InstructionOverlay.java) and [SceneOverlay](https://github.com/FabulousOtter/gotr-autopilot/blob/feb04c81d2e58bb88294ef313be3744fc57b2611/src/main/java/com/fabulousotter/gotr/overlay/SceneOverlay.java), and [Quest Helper's README](https://github.com/Zoinkwiz/quest-helper/blob/master/README.md) and [contribution guide](https://github.com/Zoinkwiz/quest-helper/wiki/Contribution-guide). GOTR separates its instruction panel from outlines/item highlights; its scene labels are optional. Quest Helper documents a current-step overlay with object/NPC/tile highlights, and recommends conveying positioning setup in panel text. Our choice is to keep the panel-plus-highlight pattern and remove scene text entirely in response to live feedback. No source code copied or dependency added.


## Flame highlight palette — 13 October 2026

Live feedback found the teal difficult to distinguish. Normal target, inventory, optional path/tile and panel accents now use flame orange-yellow (#FFB428); supply targets use a lighter gold (#FFD24B). This replaces the earlier teal palette. Emergency warmth remains red, with pale red text for readability. Existing dark edging and pulse behavior are retained.

## Highlight controls and healing — 17 October 2026

Scene and inventory borders now default to one pixel, with a shared 1–4 pixel setting in RuneLite's configuration panel. Attention pulses change opacity rather than border width. Inventory borders no longer use the broad glow; scene feathering is reduced to one pixel. Healing explicitly accepts only finished rejuvenation potions from the current objective. Regression checks cover every dose variant, stale feeding/fletching animations, Mass and Solo healing, and fuel accidentally included in a healing objective. Live interface-scale and gameplay checks remain pending.
