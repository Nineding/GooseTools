# Xaero menu regression

Run `gradlew runXaeroRegressionTest` with JDK 25. This isolated client mod is never packaged in the release JAR. It creates a fresh flat world under `build/xaero-regression-test` and exits with a `result.txt` report.

The checks exercise actual Xaero screens and shortcuts with the disabled-waypoint effect present, both GUI entry paths, ordinary/owner client permissions, repeated blocked menus, Right Shift events through Minecraft's keyboard handler and Xaero ticks, allowed player settings/style/close, and normal settings while disconnected. A wrong Mixin target or injection order reproduces the original `Gui` to `Minecraft` cast failure.

The same run verifies that both locked cave depths are 64, consumes the actual debug hitbox handler with its default and rebound key, clears a pre-enabled debug entry, and invokes the hitbox renderer to check cancellation during a match. It then creates a small two-height room in the isolated world. Xaero's actual world-map tile must record the lower floor at Y=60 under the Y=105 cave cap, beyond the old 30-block scan depth.

Run `gradlew runFairPlayRegressionTest` for the startup block screen. Its isolated source set supplies a harmless mod with Fabric ID `freecam` and a different display name, without any camera code. The run checks actual loaded-mod detection, Esc/close handling, both screen entry points, direct multiplayer connections, QuickPlay, and local world open/create, then captures `build/fairplay-regression-test/blocked-mods.png` after the loading overlay closes. The fixture and regression classes are excluded from the published JAR.

The startup policy currently matches exact IDs `tweakeroo`, `freecam`, `meteor-client`, and `wurst`, including Fabric `provides` aliases. A matching installation is blocked even when its camera setting is off. MaLiLib, ordinary rendering mods, and GooseTools' own dream mechanics are not matched. This is a known-mod check in the client; a modified client can remove the guard or disguise a mod's identity. It is not a server attestation mechanism.

For the dedicated-server smoke check, copy the previously accepted `run/eula.txt` into `build/xaero-server-smoke-test`, configure that test directory, run `gradlew runXaeroServerSmokeTest`, wait for `Done`, and enter `stop`. These runs do not open the user's game saves.
