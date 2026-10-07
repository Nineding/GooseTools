# Flight regression

Run `gradlew runFlightRegressionTest` with Java 25 and the matching GooseThings JAR
in `build/flight-regression-test/mods`, along with the required Sound Physics
Remastered and Cloth Config client dependencies. This isolated client creates a new flat
test world, checks the real integrated-server command and client payload path,
then exercises vanilla forward/jump/shift input at all four role speed multipliers.
It repeatedly injects stale ground contact, tests double-tap flight cancellation,
checks hover stability, releases the force lock into toggleable ghost permission,
and checks full cleanup and restoration of vanilla speed. It also invokes the real
server abilities handler and checks cancellation is rejected before the periodic
flight tick, then kills and respawns a forced-flight player and checks both locks clear.

Outputs: `build/flight-regression-test/result.txt` and `logs/latest.log`.
This test mod and its save are excluded from release JARs. The test exercises shared
flight mechanics; it does not play full matches or verify role-specific timers,
proxy bodies, meetings, shader compatibility, or dedicated-server networking.

Run `gradlew runProjectionRegressionTest` for the Esper body regression. Put the
matching GooseThings JAR plus Cloth Config and Sound Physics Remastered into
`build/projection-regression-test/mods`. It creates its own flat save and exercises
the actual projection scene handler, entity lifecycle and avatar renderer with
both the local owner and a simulated remote source, standing and crouching.
Checks cover a non-local possession camera, an overlapping authority before its
spectator update, full-body render flags, equipment, entity-unload recovery,
return de-duplication and cleanup. Results are written to
`build/projection-regression-test/projection-result.txt`. The remote source is
simulated in one real client; this is not a two-client full-match or shader test.
