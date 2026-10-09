# Professional GUI regression

Run with Java 25 from the repository root:

```powershell
./gradlew.bat runProfessionRegressionTest --offline
./gradlew.bat runProfessionRegressionTest -PwithInvMove --offline
```

The optional fixture uses the existing local InvMove and Cloth Config JARs from
the parent workspace. They are never included in the release or required by it.

Each run creates an isolated creative flat test save. The test opens all four
professional tasks through real commands, derives values from the public problem
data, and solves sixteen stages with rendered hit regions, mouse/card/diagram
dragging, actual numeric character input, Ctrl+A, Enter, Shift and arrow keys.
It checks rejected inputs, IQ stable time, both cooling outages, localized
handbooks, results, clean replay and exactly one completion event per task.

Every stage/result is rendered at GUI scales 1, 2 and 3 and every handbook is
captured. `build/profession-regression-test/` and
`build/profession-invmove-test/` contain PNGs, `result.txt`, saves and logs.
The optional run also checks InvMove's late overwrite, inventory restoration
and an already disabled configuration. Both runs verify admin close,
replacement, meeting, dimension change and death cleanup.

This source set and its mod entry point are excluded from the runtime JAR.
