# Camera GPU regression

Run `gradlew runCameraRenderTest` with Java 25 and a graphics-capable desktop session.
The task starts its own client in `build/camera-render-test`, opens no world, renders
a fixed block scene through the production `CameraSceneRenderer`, reads the 960x540
GPU target back, and exits. It fails if no successful result file is produced.

The red wall must be visible and a nearer green column must occlude it at the
overlapping pixel. The captured image is taken after several cached frames, when
the original mesh has already been released. This exercises block models, indexed
draw arguments, reversed-Z projection, terrain baking, and cached colour/depth copies.

Outputs: `build/camera-render-test/camera-gpu.png`, `result.txt`, and `logs/latest.log`.
The test client supplies full-bright light without opening a world. It does not
test network delivery, live actors, the final in-world monitor surface, or shader
pack compatibility. The test mod is in a separate source set and is not included
in the published GooseTools JAR.
