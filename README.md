<p align="center">
  <img src="docs/images/plugin-icon.png" alt="Wintertodt Autopilot brazier icon" width="120" height="120">
</p>

# Wintertodt Autopilot

Less thinking, more Wintertodt. Pick your goal and follow the highlighted action. The plugin handles the planning: supplies, warmth, when to fletch, and when to feed before the round ends.

Instructions stay in one movable panel. The highlighted target shows where to click.

You still do every click. The plugin doesn't play for you.

## Get started

1. Install **Wintertodt Autopilot** from RuneLite's Plugin Hub.
2. Join a Wintertodt mass world.
3. Leave the strategy on **Mass - Balanced**, with fletching and working brazier on **Automatic**.
4. Follow the instruction and highlighted target. Missing tools or potions? It'll walk you through getting them.

All options are in RuneLite settings → **Wintertodt Autopilot**. No extra sidebar. Walking paths and floor markers are off by default. Right-click the overlay to open settings, or hold **Alt** to move it.

## What it does

- Shows one action at a time, with the target or inventory items highlighted.
- Gets the highlights out of the way while you're working. Interrupted actions bring them back, with pulsing fuel when the brazier goes out.
- Keeps track of warmth and guides potion prep, repairs, relighting, and pyromancer healing.
- Adjusts batches around your reward goal and the time left in the round.
- Keeps earned points separate from fuel you still need to feed.

## Pick your goal

| Strategy | What to expect |
| --- | --- |
| **Mass - Balanced** | Smaller batches, with enough fletching to work toward your reward goal. Start here. |
| **Mass - Firemaking XP** | Feed roots directly and use small late-round batches when there's time. |
| **Mass - Points / Rewards** | Larger batches and more kindling. |
| **Low Attention** | Fill available inventory space for fewer task changes. |

Automatic fletching follows your strategy. You can override it in settings. Once you reach a fixed reward goal, guidance keeps working toward the next 500-point threshold.

## Keep in mind

Mass worlds only for now. [Solo mode is phase 2](docs/SOLO_MODE_RESEARCH.md).

Finish times and potion reserves are estimates. You still need to watch the game and react to attacks. Walking cues don't dodge for you.

**Pause guidance pauses all notifications too, including low warmth.** Uncheck it in settings to resume. Your pause choice is saved across restarts.

## Bugs and feedback

[Open an issue](https://github.com/alex-md/wintertodt-autopilot/issues) with what happened, what you expected, and your strategy. A screenshot helps. Enable **Debug overlay and logs** if you need the decision reason.

## Build locally

Use Java 17. The project targets Java 11 and uses RuneLite 1.13.1.

```sh
./gradlew build
./gradlew run
```

This launches RuneLite in developer mode with the plugin loaded. Jagex accounts need RuneLite's [developer-client setup](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

[Live validation checklist](docs/LIVE_VALIDATION.md) · [Research](docs/RESEARCH.md) · [Hub submission notes](docs/PLUGIN_HUB_SUBMISSION.md)

By [alex-md](https://github.com/alex-md). Licensed under [BSD 3-Clause](LICENSE). Third-party attribution is in [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES).
