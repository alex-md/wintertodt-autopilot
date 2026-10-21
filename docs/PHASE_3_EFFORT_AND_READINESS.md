# Phase 3 effort, readiness and observed actions

Included in release 0.3.0, prepared 21 October 2026. Solo is presented without the preview label at the maintainer’s request after manual testing. Outstanding gameplay scenarios remain on the live checklist; implementation, automated evidence and full live acceptance are recorded separately.

## Player choices and migration

`Solo effort` is independent of `Solo strategy` and fletching. The new `soloEffort` key defaults to **Comfortable**, including existing profiles without that key. Existing strategy, goal, corner, fletching and finish keys remain intact. The one-time release-notice marker is now 0.3.0 so existing users receive the Solo-ready message too.

- **Comfortable:** finish productive batches, skip optional injury healing, and maintain one selected working brazier when finishing. Heal an incapacitated working pyromancer when needed to resume productive work, subject to the common maintenance rule. Recovery never restores drain.
- **Active:** preserve the same batch commitments. Points mode permits optional healing based on fresh injury observations, with personal doses protected. Remote healing requires a processing opportunity, a visible target and known run energy above 10%. XP mode continues to avoid optional healing. Finishing maintains a stable adjacent pair when run energy is known and above 10%; otherwise it uses the original finish anchor alone.

An effort change clears incompatible travel/finish choices on the next decision without cancelling a fuel batch or a manual finish request. Warmth restoration still takes priority. Fuel is fed before adding finish fires. Unknown coverage can still prevent a finish relight.

## Readiness and consumption

Normal shows supply readiness during preparation and supply work. Detailed also shows the effort, working allowance, protected personal minimum, observed personal/healing consumption and action evidence. Missing inventory/equipment remains unknown. Standing pyromancers are labelled **Standing**, not assumed fully healthy. Fewer than four warm items is an advisory, not a blocked start.

Initial working reserve remains **24 doses**. For Active Points the initial allowance is 16 personal + 8 healing; other preferences allocate all 24 to personal warmth. These are supply-planning allowances, not promises about how long supplies last. Optional healing initially leaves at least two personal doses; essential maintenance retains the existing last-dose protection.

Consumption changes targets only after **300 consecutive valid game-tick intervals** (three minutes) and at least three attributed potion uses. A five-minute supply horizon (500 ticks) plus buffers of four personal and two healing doses can increase the next working target, up to **40 total doses**. Personal allowance takes priority at the cap. The protected personal minimum can increase to eight doses using a one-minute observed-use horizon plus two doses. Targets never fall below the existing 24-dose working baseline; the finish reserve remains smaller, 8–12 doses depending on the protected personal minimum.

The horizons, buffers and cap are conservative engineering choices. They are not published optimal potion counts or calibrated full-round predictions. A separate observation window ends at 500 intervals; targets retain the largest supported requirement within the current loadout/preference context. Loading, gaps, invalid inventory/equipment/warmth and round boundaries break the sample window. Strategy, effort, Firemaking level or warm-clothing-count changes clear calibration. Full session resets clear all learned targets. No history is persisted to disk.

Only a requested personal restoration with confirmed warmth rise and potion consumption counts as a personal dose. Healing needs a targeted request, one spent dose and a fresh injury improvement or incapacitated-to-standing transition. Inventory loss by itself, food, regeneration and other players' healing do not count as attributed use. Ambiguous use is omitted. Totals cover only observed portions of this plugin session. Existing restock deferral and feasible ingredient batches remain in place; an in-progress preparation target is latched instead of growing during a trip. Raising the desired target does not force an immediate top-up if the current reserve is usable.

## Action evidence and health freshness

Snapshots and debug traces distinguish **REQUESTED**, **IN_PROGRESS**, **CONFIRMED**, **INTERRUPTED** and **UNCONFIRMED**. Inventory transformations support chop/fletch/feed/mix confirmation. Repairs and lighting require a recent request and a corresponding change at that specific visible corner. These are observed correlations, not proof of exclusive responsibility in a shared arena. Mixing confirmation also works between rounds.

Requests expire after three ticks and never become success merely through elapsed time. Healing clicks no longer manufacture a healing activity. A pending observed target gets a bounded check-result instruction; failed requests become actionable again. Invalid inventory, lifecycle boundaries and tick gaps discard pending confirmation. Duplicate observations cannot count use twice. Missing or interrupted actions cannot dismiss warmth safety.

Pyromancer standing/down state and injury freshness are separate. RuneLite exposes a health-bar ratio, not exact server HP. Visibility alone does not refresh its timestamp: an unchanged ratio expires after eight ticks. A changed ratio/scale, new NPC identity or observed transformation supplies new bounded evidence. A spent healing dose invalidates cached injury evidence until a new health value arrives, preventing repeated optional healing from an old ratio. Missing information stays unknown. Mandatory recovery of a downed pyromancer uses its observed transformation state rather than an assumed health percentage.

## Shared maintenance rule

`WintertodtMaintenancePolicy` is used by ordinary maintenance, optional healing and supply-space maintenance. Outside intentional finishing, low energy or a latched recovery blocks lighting, repair and healing that could restore drain. Healing at a freshly known unlit/broken fire is allowed because the fire remains inactive. Recovery healing cannot use unknown or lit-fire evidence. Sustain does not restore working-corner drain while a remote brazier is lit. Intentional finishing permits drain but still requires fresh coverage and target evidence.

A full fuel bag can feed an observed existing fire to make potion space. If there is no usable fire during recovery, guidance explicitly asks for manual inventory space; it cannot light a fire just to empty the bag. No action is automated.

## Source basis and remaining acceptance

- [Jagex: Wintertodt rework, 9 October 2024](https://secure.runescape.com/m=news/wintertodt-updates--varlamore-drop-rates?oldschool=1): warmth, 30% per rejuvenation dose, four doses per bottle, and the pyromancer Help interaction.
- [RuneLite Actor API](https://static.runelite.net/runelite-api/apidocs/net/runelite/api/Actor.html): health ratio/scale may be unavailable and do not provide real HP. Retrieved 21 October 2026; the implementation still builds against the repository's 1.13.2 dependency.
- [Phase 3 plan](PHASE_3_SOLO_PLAN.md): preserve productive work, separate effort from fuel strategy, gate drain restoration, and require measured gameplay before graduation.

Run the sequence tests, clean build, overlay preview and matching capture replay. Capture revision is now `phase3-healing-cues-4`; older captures require their matching checkout. Synthetic verification does not close the [live checks](LIVE_VALIDATION.md). In particular, verify actual menu/scene events, health expiry, supply consumption and full round outcomes for both effort settings and XP, alongside Mass behavior.
