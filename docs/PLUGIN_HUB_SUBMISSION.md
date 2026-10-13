# Plugin Hub submission checks

Checked October 13, 2026, for the initial Wintertodt Autopilot submission.

## Listing and packaging

- Public source: https://github.com/alex-md/wintertodt-autopilot
- Name and entry point agree between `runelite-plugin.properties` and `@PluginDescriptor`.
- Description explicitly says that the player performs the clicks.
- Search tags cover Wintertodt / WT, Firemaking / FM, Woodcutting, Fletching, skilling, minigame, helper, guide, brazier, bruma, warmth, points, rewards, and mass.
- `build=standard` is explicit. No extra runtime dependencies or custom production build steps are needed.
- Version is explicitly `0.1.0` for standard builds.
- Root `icon.png` is a valid transparent 48 × 48 PNG, under the Hub's 256 KiB limit. The README has a larger copy; the sidebar uses a packaged 24 × 24 copy.
- The README covers setup, controls, strategies, limitations, previews, and issue reporting. It does not claim the plugin is already available in the Hub.
- BSD 3-Clause project license and the adapted RuneLite animation table's BSD attribution are retained. Both notices are packaged under `src/main/resources/META-INF`, including when the Hub replaces the local build script.
- Resource loading uses classpath resources and works with jar packaging.

## Build verification

- `./gradlew clean build previewOverlay previewSidebar --no-daemon --console=plain` passed.
- After the listing, resources, and sidebar icon changed, `./gradlew build previewSidebar --no-daemon --console=plain` passed: **112 tests, zero failures, errors, or skipped tests**.
- Production sources compiled successfully with the official Hub `standard-build.gradle`, its Lombok 1.18.30 dependency, and the Hub's current RuneLite target, **1.13.1**. The version override was restricted to `client`, `runelite-api`, and `http-api`, matching the Hub initializer.
- Inspected jar contents: plugin entry point, warmth registry, sidebar icon, project license, and third-party notices are present. Plugin bytecode targets Java 11 (class version 55).
- Panel previews were generated and visually inspected. These are constructed states, not evidence of live gameplay.
- `git diff --check` passed before submission.

The template build checks compilation and packaging; it does not replace the Hub's own API scanner, CI, or rule review. The Hub manifest must reference the full 40-character hash of the pushed source commit.

## Source review scope

Production code observes client state and displays guidance, highlights, paths, a sidebar, and notifications. All gameplay actions are performed by the player. The observed menu-click event tracks player activity; the overlay menu entry opens plugin configuration.

The production source scan found no reflection, native code, subprocess execution, runtime code downloads, networking, filesystem writes, synthesized mouse/keyboard input, chat insertion, menu swapping, or server-action calls. The warmth registry reads a bundled resource, not a filesystem path. Configuration persistence uses RuneLite's `ConfigManager`. The hint arrow only replaces an unused arrow and clears it while still owned by this plugin.

Walking cues lead to interaction targets. They do not predict attacks, mark attack impact locations, or choose positions to dodge boss mechanics. RuneLite reviewers still need to assess the dynamic guidance, paths, and interaction tiles under their game-rule policy. This document does not claim prior approval from RuneLite or Jagex.

## Remaining checks

The full [live validation checklist](LIVE_VALIDATION.md) is incomplete. Automated tests are not being counted as live acceptance. Corner mapping, HUD observations, interruption timing, crowded-world presentation, lifecycle behavior, and actual round pacing still need recorded in-game checks.

Hub CI and reviewer approval are external steps. Opening the submission PR does not make the plugin available to install; it needs to be accepted and merged.

## Icon

Original artwork generated with the built-in image tool, then resized for the Hub, README, and sidebar. The brief was a compact dark iron brazier with orange/gold flame and icy cyan rim accents, a strong pixel-art silhouette readable at 48 × 48, transparent background, and no text or logos.

## Requirements checked

- [Hub authoring, metadata, icon, build, licensing, and submission instructions](https://github.com/runelite/plugin-hub)
- [Official standard build template](https://github.com/runelite/plugin-hub-tooling/blob/master/package/src/main/resources/net/runelite/pluginhub/packager/standard-build.gradle)
- [Hub packager validation](https://github.com/runelite/plugin-hub-tooling/blob/master/package/src/main/java/net/runelite/pluginhub/packager/Plugin.java)
- [Rejected features and forbidden APIs](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features)
- [Jagex third-party client guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1)
- [RuneLite's review scope](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review)
