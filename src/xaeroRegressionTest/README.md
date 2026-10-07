# Xaero menu regression

Run `gradlew runXaeroRegressionTest` with JDK 25. This isolated client mod is never packaged in the release JAR. It creates a fresh flat world under `build/xaero-regression-test` and exits with a `result.txt` report.

The checks exercise actual Xaero screens and shortcuts with the disabled-waypoint effect present, both GUI entry paths, ordinary/owner client permissions, repeated blocked menus, Right Shift events through Minecraft's keyboard handler and Xaero ticks, allowed player settings/style/close, and normal settings while disconnected. A wrong Mixin target or injection order reproduces the original `Gui` to `Minecraft` cast failure.

For the dedicated-server smoke check, copy the previously accepted `run/eula.txt` into `build/xaero-server-smoke-test`, configure that test directory, run `gradlew runXaeroServerSmokeTest`, wait for `Done`, and enter `stop`. These runs do not open the user's game saves.
