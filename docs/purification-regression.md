# Eagleton purification regression

`./gradlew test build` checks password validation, server tick charging, click gaps, forged timestamps, geometry and all mirror solutions.

The isolated GUI check requires the Whoiskiller workspace (datapack, names resource pack, task marker configuration and host mods):

```
./gradlew runPurificationRegressionTest -PpurificationFixtureRoot=/absolute/path/to/Whoiskiller/workspace
```

The run creates a fresh test world and never opens the real map. It sends real block interaction packets, operates the native Screen with mouse and keyboard, tests close/retry, binding, navigation, the exact chamber foot bounds, inventory.14, serial 1200-tick disable and cooldown phases, meeting gates, bomb/temporary Cupid cleanup and five-second charging. Duck entry uses the actual taskgui item offer, selected hotbar slot, carrot right-click score and ggd:rccheck dispatch, covering the full task entry chain. It writes result.txt and four screenshots to build/purification-regression-test.

The static dialog and predicate registries are copied before world creation. The fixture enables only the GUI and purification load/tick functions. Test classes are excluded from the runtime JAR.

Charging counts intervals of 100–500 ms between real accepted clicks. Longer gaps pause without clearing phaseAt; the first click after a pause resumes without counting idle time. Server ticks cannot advance charge or complete it. Unit tests cover the exact pause boundary, gaps with and without intervening server ticks, repeated pause/resume until five seconds, forged timestamps and bursts. The native GUI regression pauses input for 1.2 seconds after earning progress, verifies that progress remains, then resumes and completes.

Dedicated-server smoke must additionally load the installable JAR with the full map datapack and GooseThings. Validate original and borrowed Esper sessions independently: purify the host, verify only the matching possessor dies, and verify the corpse remains at the retained body position. Check original and borrowed Cupid cleanup against two marked players while preserving formal Lovers tags. The new death is an environment death and must not credit a duck kill or first knife achievement.
