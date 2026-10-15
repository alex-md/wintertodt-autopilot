# Plugin Hub release checks

Release: **0.2.0 — experimental Solo**, prepared for submission 15 October 2026.

## What changed

- Added **Solo (experimental)** alongside the default Mass mode. Solo has its own strategies, fixed point goal, energy phases, repeat potion prep and early-finish setting.
- Added per-corner brazier and pyromancer observations, HUD coverage checks and debug traces. Unknown observations stay unknown; carried fuel must be fed before it counts toward the goal.
- Added level-aware northern gap routing to the optional walking line. Solo can use the three manual jumps with at least 60 base/current Agility, when shorter than walking around. Mass and lower-level players keep normal walking routes.
- Kept controls in RuneLite settings and simplified the README's Solo setup and experimental notice. Existing pictures are unchanged.
- Updated local RuneLite dependencies to **1.13.2**, matching the Hub's current target. No new runtime dependencies.

## Validation

- `./gradlew clean build previewOverlay --no-daemon --console=plain`: **177 tests passed**, no failures, errors or skipped tests. The preview is synthetic and generated only under ignored build output.
- Production sources compile and package with the official Hub standard-build template, its Lombok 1.18.30 dependency and RuneLite 1.13.2 in a separate temporary project. This does not run the Hub's API scanner or establish reviewer approval.
- Local and standard-build jars include the plugin entry point, warm-clothing data and both legal notices. The existing listing icon stays at the repository root. Class files target Java 11.
- Existing root icon meets the Hub's dimensions/size requirements. Packaged license copies match the root originals. README local links resolve and `git diff --check` passes.

Solo still needs [live validation](LIVE_VALIDATION.md), including the HUD from all corners, energy recovery, potion margins, early finishing and northern gap alignment in both directions. Automated tests and a successful build do not close those checks. See [Solo implementation notes](SOLO_MODE_RESEARCH.md).

## Source review

The plugin observes game state and provides guidance, highlights and notifications. The player performs every interaction, including shortcut jumps. It does not synthesize game actions, dodge attacks or guarantee a solo arena. Paths lead to the current interaction target and obey scene collision, apart from the explicit level-gated gap jumps. Pause suppresses guidance and notifications, including warmth alerts. Owned hint arrows respect existing arrows.

Production sources contain no runtime downloads, reflection, subprocesses, network requests or filesystem writes. Bundled resources use classpath streams. Configuration writes only clear this plugin's own finish request or open its settings.

## Submission status

The live Hub manifest still pins **0.1.1**, source commit `7ba2afd3e1afa8424dc8e3399e93f9943f52758d`. [PR #18518](https://github.com/runelite/plugin-hub/pull/18518) merged on 14 October 2026. No open update PR by alex-md was found on 15 October.

The 0.2.0 update is submitted by pinning the pushed source commit in a new Hub manifest PR. Full live Solo validation remains pending; submission and a successful build do not establish gameplay validation or maintainer approval.

Submission steps:

1. Review the release and outstanding live checks, then commit and push the complete source to [alex-md/wintertodt-autopilot](https://github.com/alex-md/wintertodt-autopilot).
2. Update only `plugins/wintertodt-autopilot` in a separate Hub branch. Keep `repository=https://github.com/alex-md/wintertodt-autopilot.git` and set `commit=` to the full 40-character hash of the pushed release commit.
3. Open the update PR with the description below and check the Hub CI result. Maintainer review and merge remain separate from local package checks.

Suggested title: **wintertodt-autopilot: experimental Solo mode (0.2.0)**

Suggested description:

> Adds an experimental Solo mode with its own point goal, energy management, potion prep and early-finish option. The optional walking line can use the northern gap at 60 Agility. Mass stays the default, and the player still does every click.
>
> The README explains setup and the experimental status. Local build and 177 tests pass against RuneLite 1.13.2, and production sources compile with the Hub standard-build template. Full live Solo validation is still pending.

## References

- [Hub submission and update instructions](https://github.com/runelite/plugin-hub)
- [Official standard build](https://github.com/runelite/plugin-hub-tooling/blob/master/package/src/main/resources/net/runelite/pluginhub/packager/standard-build.gradle)
- [Rejected features and APIs](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features)
- [Plugin Hub review scope](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review)

Original icon artwork was generated with the image tool. Existing project and RuneLite attribution remains in `LICENSE`, `THIRD_PARTY_NOTICES` and the packaged copies under `META-INF`.
