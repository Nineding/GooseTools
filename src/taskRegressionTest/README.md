# GUI task regression

Run `gradlew runTaskRegressionTest` using Java 25 in a graphics-capable desktop session.
This isolated test creates a new flat save under `build/task-regression-test` and uses
the actual server commands, networking and production Screen mouse/keyboard handlers
to complete all five tasks. It checks GUI scales 1/2/3, reopening a task, ESC and
server-forced closing, and captures the five panels plus the completion screen.

Inspect `task-*.png`, `result.txt` and `logs/latest.log` in that run directory.
The test source set is never included in the published runtime JAR. This automated
test does not assess human difficulty, wide-area-network latency or resource-pack styling.
