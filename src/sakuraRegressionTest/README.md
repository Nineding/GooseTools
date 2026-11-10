# Sakura / iaido runtime regression

Uses the packaged runtime JAR, an isolated dedicated server, and two clients (A: vanilla renderer; B: Punchy 3.2 + Moves 1.0.8 + Fluid & Foolish 1.0.1 + SkinLayers3D 1.11.4). The fixture is never included in the release JAR. The server sends actual skill stacks and the existing approved-motion packet; it does not simulate GooseThings damage or charge eligibility.

Third-person captures use a narrower FOV to inspect hand/sleeve clearance, include intermediate insertion poses and repeated hold frames, and move the player during cooldown to inspect sheath attachment. GUI alignment and ordinary first-person grip remain visible in the first-person HUD/idle captures. `SAKURA_SKINLAYERS=false` permits a shader-only diagnostic comparison without changing the user's installed mods or config.

The default SkinLayers3D fast renderer remains enabled for release checks. `SAKURA_SKIN_FAST=false` disables greedy sleeve shells only in the isolated instance for diagnosis; it is not a production workaround. `DanceVertexConsumerTest` covers outward face winding and UV-aligned subdivision of enlarged sleeve shells.

Approved-motion samples now move each player six blocks over eight ticks using the server's smoothstep profile, then send the normal stop/correction. Both clients assert actual displacement. `motion-<view>-<tick>.png` records the full blade sequence during that movement; the test does not substitute a stationary approval packet for a dash. Pose tests separately require the drawn tip to start on the owner's low-left side and finish high-right with forward travel of at least one block in both view rigs, including forward hand movement.

The isolated server is peaceful to prevent slime knockback from contaminating the trajectory. B's continuous captures allow only one pending GPU readback; skipped tick numbers are retained so previews can preserve timing instead of flooding a single rendered frame with duplicate requests. A retains the ordinary key-frame captures.

`SakuraIaidoArmTest` samples the complete pose paths with wide/slim arms and mirrored grips. It checks palm alignment, fixed upper/forearm lengths, body-attached shoulders and a single elbow hinge with no axial twist. First-person grip checks also require the back of the hand to face the owner and preserve fist dimensions. GPU captures remain necessary to judge the silhouette and the hand/weapon relationship.

From the GooseTools project, with JDK 25 and the sibling Whoiskiller resource pack:

```powershell
.\gradlew.bat --offline --console=plain -I src/sakuraRegressionTest/launch.gradle test build exportSakuraLaunch
python scripts/validate_sakura.py
```

The runner binds the server to loopback on an ephemeral port and hides only its own test windows. It uses the sibling `mods/` Punchy JARs and `resourcepacks/Fluid & Foolish 1.0.1.zip`. Output is in a fresh `build/sakura-native-validation/run-*` directory. The result files and GPU captures must be inspected; a successful build alone is insufficient.

Coverage: local front/back view vs remote/detached/spectator; vanilla/Punchy countdown refresh vs actual slot switches; actual Minecraft attacks select the same two native sword clips for iron, Sakura and charging Sakura; both view modes run charge/approved dash/recovery; no owner animation in corpse snapshots; slot cancellation. The earlier client stays connected until both finish, so observer assertions cannot pass merely because the other client disappeared. The fixture dismisses the optional Punchy first-run tutorial before testing input.

`SakuraIaidoTimelineTest` separately checks server approval, rejected charge recovery, duplicate motion notifications, interruption and blade-tip clearance before the cutting rotation. Left-handed visuals and shader combinations still need their corresponding visual checks; the fixture does not claim those from its default right-handed captures.

To repeat only the rendering phase with Iris 1.11.7, compatible Sodium 0.9.2 and the local Complementary Reimagined r5.9.3 ZIP, set `SAKURA_QUICK=true` and `SAKURA_IRIS=true` before running the Python script. This supplemental run does not replace the full native-attack regression above.
