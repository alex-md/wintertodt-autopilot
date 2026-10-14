# Plugin Hub release checks

Release: **0.1.1**, reviewed 13 October 2026. Updates the published manifest's source commit `79b527e9da79b9788f4ac6bc6c227c5be6ecc883`.

## Final changes

- Removed the custom sidebar and floating scene-label overlay, including their classes, resources and previews.
- Controls use RuneLite's built-in settings. Pause is saved and suppresses guidance and notifications, including emergency warmth alerts.
- Normal guidance shows the action, observed progress, warmth and reward progress. Supply instructions name the item and remaining quantity.
- Matching activity suppresses scene/inventory cues and owned arrows/routes. Interrupted fuel can pulse while relighting or repairs are needed; warmth retains priority.
- Highlights use flame orange-yellow and gold. Walking paths and standing tiles default off; intermediate breadcrumb squares are removed. Existing saved preferences are retained.
- Updated author metadata to `alex-md` and shortened the README. Solo mode remains research only.

## Packaging and compatibility

- Public source: [alex-md/wintertodt-autopilot](https://github.com/alex-md/wintertodt-autopilot).
- Plugin name/entry point agree between metadata and `@PluginDescriptor`; description states that the player performs the clicks.
- `version=0.1.1`, `build=standard`; no new runtime dependencies or custom production build steps.
- Java 11 bytecode, built with Java 17 against RuneLite 1.13.1, matching the Hub's `runelite.version` at review time.
- Root icon is a transparent 48 × 48 PNG below 256 KiB. Packaged warm-clothing data and both license notices are required in the jar.
- Official Hub standard-build compatibility is checked separately using its Lombok 1.18.30 dependency and the Hub's RuneLite target. This checks compilation/packaging, not the Hub's API scanner or reviewer approval.

## Validation

The final release review runs `./gradlew clean build previewOverlay --no-daemon --console=plain`, inspects the synthetic preview and clean jar, checks license/resource packaging and bytecode, and runs `git diff --check` before publishing the source commit. Final results:

- Clean local build and preview generation passed: **116 tests, zero failures, errors or skipped tests**.
- The final production sources compiled and packaged with the official standard-build template, Lombok 1.18.30 and the Hub's RuneLite 1.13.1 target in a separate temporary project.
- Clean jar inspection confirmed required resources/notices, Java 11 bytecode, and absence of the removed sidebar/floating-label classes.
- License copies match their root originals, icon dimensions/size are valid, README local links resolve, and `git diff --check` passed.
- Reviewed the final synthetic panel/highlight preview. It is not a live gameplay screenshot.

The user supplied live screenshots and iterative feedback on supplies and crowded-world presentation. The full [live checklist](LIVE_VALIDATION.md) is still incomplete; unit tests and synthetic previews are not substitutes for full live acceptance. Unverified checks remain unchecked.

## Source review

Production code observes state and displays guidance, highlights and notifications. Observed menu clicks track player activity; the overlay menu only opens configuration. There are no synthesized game actions, reflection, subprocesses, runtime downloads, network requests, filesystem writes or modifications to other plugins' configuration. Bundled resources use classpath streams. Owned hint arrows respect existing arrows.

Optional paths lead to interaction targets. They do not predict attacks or choose positions to dodge mechanics. The Hub's CI and maintainers still make the final compatibility and policy decisions.

## Submission

Update only `plugins/wintertodt-autopilot` in [runelite/plugin-hub](https://github.com/runelite/plugin-hub), keeping the repository URL and pinning the full 40-character hash of the pushed source commit. The upstream Hub file was checked before preparing the update; no open update PR by this author was found at review time.

## References

- [Hub submission and update instructions](https://github.com/runelite/plugin-hub)
- [Official standard build](https://github.com/runelite/plugin-hub-tooling/blob/master/package/src/main/resources/net/runelite/pluginhub/packager/standard-build.gradle)
- [Rejected features and APIs](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features)
- [Plugin Hub review scope](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review)

Original icon artwork was generated with the image tool. Project and adapted RuneLite animation-table attribution remain in `LICENSE`, `THIRD_PARTY_NOTICES` and the packaged copies under `META-INF`.
