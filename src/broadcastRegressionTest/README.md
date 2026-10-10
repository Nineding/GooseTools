This optional integration run exercises the real Minecraft client, mouse gestures, task packets and bound completion, followed by command-driven checks of broadcast and sabotage state. It is excluded from the runtime JAR and ordinary CI build.

Run `gradlew runBroadcastRegressionTest` in the Whoiskiller development workspace after preparing the full data pack snapshot at `build/broadcast-smoke/world/datapacks/Whoiskiller`. The snapshot is the same one used for the dedicated server startup check. Keeping it fixed prevents unrelated, incomplete edits in other map features from changing the test while it runs. The surrounding workspace also provides the matching GooseThings and Carpet test dependencies, resource-pack names and task marker configuration.

The run creates an isolated flat world, verifies close/retry and real four-wire GUI completion, and checks broadcast readiness, immediate interruption, 600 ticks of disable followed by 900 ticks of shared sabotage cooldown, late unlock, meeting and DLC gates, dead/missing speakers, inventory slots, task menu/navigation, both Xaero render contexts, north-facing display and final cleanup. It saves PNGs and `result.txt` under `build/broadcast-regression-test`.

For a standalone GUI preview, use `/goosetools tasks open @s cutwires`. This trial cannot unlock the map sabotage item.

This does not verify live voice packets from multiple human clients or the appearance of the display against the production map's building geometry.
