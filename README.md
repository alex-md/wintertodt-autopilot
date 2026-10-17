<p align="center">
  <img src="docs/images/plugin-icon.png" alt="Wintertodt Autopilot brazier icon" width="120" height="120">
</p>

# Wintertodt Autopilot

Less thinking, more Wintertodt. Pick your goal and follow the highlighted action. The plugin handles the planning: supplies, warmth, when to fletch, and when to feed before the round ends.

Instructions stay in one movable panel. The highlighted target shows where to click.

You still do every click. The plugin doesn't play for you.

## Latest update — 0.2.2

Relighting highlights the brazier and lighting tool without pulsing roots or kindling. Fletching uses amber for the knife and softer gold for the roots. Walking paths allow collision-checked diagonals and prefer fewer bends between equally short routes.

## Get started

1. Install **Wintertodt Autopilot** from RuneLite's Plugin Hub.
2. Join a Wintertodt mass world.
3. Leave the strategy on **Mass - Balanced**, with fletching and working brazier on **Automatic**.
4. Follow the instruction and highlighted target. Missing tools or potions? It'll walk you through getting them.

All options are in RuneLite settings → **Wintertodt Autopilot**. Mass and Solo each have their own settings section; mode, working brazier and fletching preferences are shared. No extra sidebar. Walking paths and floor markers are off by default. Right-click the overlay to open settings, or hold **Alt** to move it.

## What it does

- Shows one action at a time, with the target or inventory items highlighted.
- Gets the highlights out of the way while you're working. Interrupted actions bring the relevant cues back; an extinguished brazier pulses without highlighting fuel.
- Keeps track of warmth and guides potion prep, repairs, relighting, and pyromancer healing.
- Adjusts batches around your reward goal and the time left in the round.
- Keeps earned points separate from fuel you still need to feed. Once you earn 500 points, the panel keeps **Reward secured** visible alongside your next target.
- Recognizes a carried fletching knife and includes its faster batches in Mass finish estimates. A wielded fletching knife gives the speed benefit, but you still need a knife in your inventory to start fletching.

Enable **Notify reward secured** for a one-time alert when earned points cross 500 each round. It defaults to off. Emergency warmth has its own notification cooldown, so idle alerts cannot delay it; round-start alerts are independent too.

## Pick your goal

| Strategy | What to expect |
| --- | --- |
| **Mass - Balanced** | Smaller batches, with enough fletching to work toward your reward goal. Start here. |
| **Mass - Firemaking XP** | Feed roots directly and use small late-round batches when there's time. |
| **Mass - Points / Rewards** | Larger batches and more kindling. |
| **Low Attention** | Fill available inventory space for fewer task changes. |

Automatic fletching follows your strategy. You can override it in settings. Once you reach a fixed reward goal, guidance keeps working toward the next 500-point threshold.

## Solo mode (experimental)

Solo is new and still needs testing in game. Expect rough edges. Start on an empty world and keep an eye on the energy and your supplies.

1. Set **Mode** to **Solo (experimental)**.
2. Pick **Solo strategy**: **Points / Rewards** for kindling, or **Firemaking XP** for roots.
3. Set your **Solo point goal**. The default is **5,000**. Fuel in your inventory only counts once you've fed it.
4. Leave **Working brazier** on **Automatic** to work near your current corner, or pick a specific brazier to stay there.

The plugin helps light the opening braziers, lower the energy, then keep the round going while you earn points. When energy gets low, it'll tell you to leave braziers unlit so it can recover. Once you've earned your goal, it'll guide you through finishing the round.

Want to end it early? Enable **Finish this Solo round**. Once requested, it stays active for that round, even if you uncheck it. It clears when the round ends, you change mode, hop worlds or log out.

If you turn on **Show walking path**, Solo routes can use the northern gap between the back two braziers at **60 Agility**, when it's quicker than walking around. You still need to click each jump yourself. Below 60, the line takes the walking route.

Solo hasn't passed the full [live validation checklist](docs/LIVE_VALIDATION.md) yet. [Solo notes](docs/SOLO_MODE_RESEARCH.md) cover the current behavior and what still needs checking.

## Keep in mind

Finish times and potion reserves are estimates. You still need to watch the game and react to attacks. Walking cues don't dodge for you.

**Pause guidance pauses all notifications too, including low warmth.** Uncheck it in settings to resume. Your pause choice is saved across restarts.

## Bugs and feedback

[Open an issue](https://github.com/alex-md/wintertodt-autopilot/issues) with what happened, what you expected, and your strategy. A screenshot helps. Enable **Debug overlay and logs** if you need the decision reason.

## Build locally

Use Java 17. The project targets Java 11 and uses RuneLite 1.13.2.

```sh
./gradlew build
./gradlew run
```

This launches RuneLite in developer mode with the plugin loaded. Jagex accounts need RuneLite's [developer-client setup](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

[Live validation checklist](docs/LIVE_VALIDATION.md) · [Research](docs/RESEARCH.md) · [Hub submission notes](docs/PLUGIN_HUB_SUBMISSION.md)

By [alex-md](https://github.com/alex-md). Licensed under [BSD 3-Clause](LICENSE). Third-party attribution is in [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES).
