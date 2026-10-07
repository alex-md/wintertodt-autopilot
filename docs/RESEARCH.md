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
| Ending category | Energy, loaded lit braziers and recent observed drain over 30 ticks |

`VarPlayerID.WINT_INT` is opaque without a verified packing definition. It is not guessed or unpacked. The widget parser strips color tags, accepts grouped point values, and returns -1 for absent/malformed text. It does not infer a completed round merely because a widget disappears.

Scene references are cached via spawn/despawn and NPC change events. One scene bootstrap occurs when enabling the plugin in a loaded region or after a scene load. Object/NPC transformations and the small inventory are sampled each game tick. Overlays use only cached decisions/targets and never scan the scene or plan strategy.

## Validation status

Automated scenario tests exercise the independent planner and mocked observation signals. A RuneLite 1.13.1 developer-client smoke launch reached the login screen, and RuneLite logged `WintertodtAutopilotPlugin is now running` with its event handlers registered. No game login, movement or interactions were performed. The upstream client logged a non-fatal Java 17 `ReflectUtil` annotation-cache diagnostic while continuing initialization.

Live minigame validation has not been performed. The accompanying checklist records that release gate explicitly.

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

Walking routes now use a dark outline, a five-pixel colored core and a narrow bright center. The final walkable tile gets a filled outline and action label; occasional route tiles provide floor breadcrumbs. Route calculation remains enabled for standing tiles even when the line is hidden. Fletching/mixing mark the current standing tile rather than manufacturing a movement destination.

Interrupted actions use one shared restart condition: active round, interruption observed, actionable objective, and idle activity. The panel shows RESUME and scene/inventory highlights modulate gently over a 1.4-second cycle. Walking, restarted activity, round transitions, or disabling the pulse removes the emphasis. A stale animation event within one tick of an interruption cannot erase the restart cue. Rendering changes only presentation; no click or movement is dispatched.
