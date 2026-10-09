# Station and movement compatibility regression

Run `gradlew runStationRegressionTest` with Java 25 and a graphics-capable desktop.
It creates an isolated creative flat save and uses real commands, payloads and screen
handlers to solve the station, check interlocks, drag a load, inspect the handbook,
replay and render Chinese GUI scales 1/2/3. It probes movement protection on every
task/game screen. Test code and fixtures are never included in the published JAR.

Run `gradlew runStationRegressionTest -PwithInvMove` to load the locally installed
`../mods/InvMove-0.9.6+26.3-Fabric.jar` and `../mods/cloth-config-26.3.158.jar`.
This also exercises InvMove's actual allow/input hooks, ordinary inventory restoration
and an originally disabled setting without modifying the user's configuration.
Results and GPU screenshots are under `build/station-regression-test` and
`build/station-invmove-test` respectively. Minecraft 26.3 shortcuts use SDL keycodes
and modifiers, rather than the older GLFW event encoding.
