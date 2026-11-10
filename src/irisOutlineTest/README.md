# Iris outline GPU fixture

Client-only test source, excluded from the release JAR. Run through iris-outline.init.gradle with irisTestMods (directory containing Iris/Sodium and optionally ImmediatelyFast), irisTestShader (a shader pack ZIP or directory), and irisTestRun (isolated output directory).

The fixture opens a disposable flat world, creates a yellow glowing block display, red player-head item and aqua mannequin, captures stage-0.png without shaders, enables the specified shader pack, then captures stage-1.png. Inspect texture visibility and colored edges; a screenshot capture alone does not assert the absence of all rendering artifacts. result.txt signals capture completion. It never edits the installed game config or saves.

Validated locally on Iris 1.11.6 / Sodium 0.9.2 / ImmediatelyFast 1.17.1 with Complementary r5.9.3 and Euphoria 1.10.5. The reported solid-fill issue did not reproduce in the isolated baseline; the guard eliminates Iris' missing-outline-pipeline errors and keeps these renders correct. Full multiplayer meeting acceptance remains necessary.

2026-11-10: The Alpha0.11 guard has been withdrawn after a multiplayer report of severe black stretched geometry on Alpha0.14. This fixture never reproduced that scene and must not be treated as proof of a fix. It remains an isolated rendering baseline.
