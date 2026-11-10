# Sakura / iaido runtime regression

Uses the packaged runtime JAR, an isolated dedicated server, and two clients (A: vanilla renderer; B: Punchy 3.2 + Moves 1.0.8 + Fluid & Foolish 1.0.1). The fixture is never included in the release JAR. The server sends actual skill stacks and the existing approved-motion packet; it does not simulate GooseThings damage or charge eligibility.

From the GooseTools project, with JDK 25 and the sibling Whoiskiller resource pack:

```powershell
.\gradlew.bat --offline --console=plain -I src/sakuraRegressionTest/launch.gradle test build exportSakuraLaunch
python scripts/validate_sakura.py
```

The runner binds the server to loopback on an ephemeral port and hides only its own test windows. It uses the sibling `mods/` Punchy JARs and `resourcepacks/Fluid & Foolish 1.0.1.zip`. Output is in a fresh `build/sakura-native-validation/run-*` directory. The result files and GPU captures must be inspected; a successful build alone is insufficient.

Coverage: local front/back view vs remote/detached/spectator; vanilla/Punchy countdown refresh vs actual slot switches; actual Minecraft attacks select the same two native sword clips for iron, Sakura and charging Sakura; both view modes run charge/approved dash/recovery; no owner animation in corpse snapshots; slot cancellation. The earlier client stays connected until both finish, so observer assertions cannot pass merely because the other client disappeared. The fixture dismisses the optional Punchy first-run tutorial before testing input.

`SakuraIaidoTimelineTest` separately checks server approval, rejected charge recovery, duplicate motion notifications, interruption and blade-tip clearance before the cutting rotation. Left-handed visuals and shader combinations still need their corresponding visual checks; the fixture does not claim those from its default right-handed captures.

To repeat only the rendering phase with Iris 1.11.7, compatible Sodium 0.9.2 and the local Complementary Reimagined r5.9.3 ZIP, set `SAKURA_QUICK=true` and `SAKURA_IRIS=true` before running the Python script. This supplemental run does not replace the full native-attack regression above.
