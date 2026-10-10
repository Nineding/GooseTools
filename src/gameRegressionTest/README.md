# Native arcade integration regression

Run `gradlew runGameRegressionTest` using Java 25 in a graphics-capable desktop.
It creates a separate flat world, loads Chinese, opens all seven games through real
commands, sends real screen key/mouse events, receives authoritative packets and
captures menus, playing screens and win/loss states at GUI scales 1/2/3.

Flappy/Snake/Whack/Traffic use bots against the actual visible board. Traffic also
checks held braking and release. `-PtrafficOnly` limits the run to driving and cleanup.
Pong and 2048 also use
isolated deterministic near-win board/ball fixtures on the test server to exercise
classic victory, continued endless play and loss through normal physics and input.
Mines are read only by the test-side solver; the runtime packet exposes no hidden mines.
Fixtures use reflection solely in this source set; no hooks/test commands are shipped.

Also verifies registration of all 18 arcade sound events and Ogg files, actual
playback through the UI sound category, sound feedback from real game inputs,
and rejection of duplicate/out-of-order state revisions after each game. A
final sound probe covers less frequent cues; pure audio-policy tests cover
no-op moves, mine flag cycling, slide vs merge, flap echo suppression and held brakes.

Includes pause/time exclusion, retry, mine difficulties, task/game replacement,
ESC, administrator close, meeting and Minecraft death cleanup. Inspect
`build/game-regression-test/game-*.png`, `result.txt` and `logs/latest.log`.
The runtime JAR never includes this source set. Human feel and public-server latency
still require player feedback.
