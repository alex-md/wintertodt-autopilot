# Plugin Hub release checks

Release: **0.2.1 — knife support and quality of life**, prepared 16 October 2026.

## What changed

- Incorporated the fletching knife recognition and timing changes from [cxar's PR #2](https://github.com/alex-md/wintertodt-autopilot/pull/2), and included the knife in Solo inventory highlights too. A wielded fletching knife supplies its speed benefit but still requires an inventory knife to start fletching.
- Separated emergency warmth and idle notification cooldowns. Idle alerts cannot delay emergency warmth, and the five-second round-start alert is independent.
- Kept **Reward secured: 500 points** visible beside the next Mass target or fixed Solo goal. An optional, default-off notification announces the observed earned-point crossing once per round; carried fuel does not count. A simultaneous warmth warning takes priority.
- Separated Mass and experimental Solo settings in the native RuneLite settings panel. Existing setting keys and saved preferences are retained.
- Added a short 0.2.1 chatbox notice after login, anywhere in the game. A saved configuration marker prevents repeats in the same RuneLite profile across plugin restarts, logins and world hops. This informational release notice also appears while gameplay guidance is paused.

## Validation

- Clean build and synthetic overlay preview pass. After the final notice integration, `./gradlew build --no-daemon --console=plain` passes with **191 tests**, no failures, errors or skips.
- Tests cover carried/wielded knife detection, both planners' knife highlights, first-root and batch timing, notification priority, reward episode handling and persisted release notices.
- Local jar includes the plugin entry point, warm-clothing data and both legal notices. The packaged legal notices match the root originals. Class files target Java 11.
- The project uses RuneLite **1.13.2**, matching the current Hub target, and `build=standard`. No new runtime dependencies.
- `git diff --check` passes. The existing listing icon and public images are unchanged.

The preview uses constructed snapshots. In-game checks for the new notice, notification timing, settings layout and knife behavior remain in [LIVE_VALIDATION.md](LIVE_VALIDATION.md). Experimental Solo retains its outstanding guided-round validation checks. Automated tests and Hub CI do not establish gameplay validation.

## Submission

The Hub manifest was verified on 16 October 2026 at source commit `1e34cda2caf61aaccf14b4efc23213b95782c9c9` (0.2.0). No open Hub update PR by alex-md was present.

Publish the 0.2.1 source commit and update only `plugins/wintertodt-autopilot` in a separate Hub branch, preserving `repository=https://github.com/alex-md/wintertodt-autopilot.git` and pinning the full 40-character source hash. The submitted PR and its checks are the authority for current submission status. Maintainer approval and distribution remain separate from submission.

## Source review

The player performs every gameplay interaction. The plugin provides guidance, highlights, paths, notifications and a local chatbox notice. It does not synthesize gameplay actions. Gameplay guidance and attention alerts remain muted while paused; the one-time release notice is independent.

Configuration writes are confined to this plugin's settings and release-notice marker. Production sources contain no runtime downloads, reflection, subprocesses or network requests. Bundled resources use classpath streams.

## References

- [Hub submission and update instructions](https://github.com/runelite/plugin-hub)
- [Official standard build](https://github.com/runelite/plugin-hub-tooling/blob/master/package/src/main/resources/net/runelite/pluginhub/packager/standard-build.gradle)
- [Rejected features and APIs](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features)
- [Plugin Hub review scope](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review)

Original icon artwork was generated with the image tool. Existing project and RuneLite attribution remains in `LICENSE`, `THIRD_PARTY_NOTICES` and the packaged copies under `META-INF`.
