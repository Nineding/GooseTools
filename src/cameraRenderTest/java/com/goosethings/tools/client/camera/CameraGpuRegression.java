package com.goosethings.tools.client.camera;

import com.goosethings.tools.GooseTools;
import com.goosethings.tools.camera.CameraBlockFrame;
import com.goosethings.tools.camera.CameraDefinition;
import com.goosethings.tools.camera.CameraLimits;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector4f;

import java.nio.file.Files;
import java.util.Arrays;

/** End-to-end block model -> mesh -> GPU bake -> cache copy -> pixel readback, without a save. */
public final class CameraGpuRegression implements ClientModInitializer {
    private CameraSceneRenderer renderer;
    private CameraScene scene;
    private int ticks;
    private int cachedFrames;
    private boolean capturing;

    @Override
    public void onInitializeClient() {
        if (Boolean.getBoolean("goosetools.cameraRenderTest")) {
            ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        }
    }

    private void tick(Minecraft mc) {
        if (!mc.isGameLoadFinished() || capturing) return;
        try {
            if (renderer == null) {
                scene = scene();
                renderer = new CameraSceneRenderer("gpu-regression");
            }
            // No world is opened. Supply deterministic full-bright illumination in this test client.
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(
                    mc.gameRenderer.lightmap().texture(), new Vector4f(1, 1, 1, 1));
            renderer.update(scene);
            var ready = CameraSceneRenderer.class.getDeclaredField("terrainCacheReady");
            ready.setAccessible(true);
            if (!ready.getBoolean(renderer)) {
                if (++ticks > 400) throw new IllegalStateException("Camera terrain never became ready");
                return;
            }
            // Exercise repeated copies after the one-shot terrain mesh has already been released.
            if (++cachedFrames < 5) return;
            capturing = true;
            var targetField = CameraSceneRenderer.class.getDeclaredField("target");
            targetField.setAccessible(true);
            Screenshot.takeScreenshot((RenderTarget) targetField.get(renderer), image -> {
                try (image) {
                    var output = mc.gameDirectory.toPath().resolve("camera-gpu.png");
                    image.writeToFile(output);
                    int red = 0, green = 0;
                    for (int pixel : image.getPixels()) {
                        int r = pixel >> 16 & 255, g = pixel >> 8 & 255, b = pixel & 255;
                        if (r > 50 && r > g * 1.5 && r > b * 1.5) red++;
                        if (g > 50 && g > r * 1.5 && g > b * 1.5) green++;
                    }
                    // Red wall must draw, with the nearer green block visibly occluding it.
                    int overlap = image.getPixel(415, 270);
                    int overlapR = overlap >> 16 & 255, overlapG = overlap >> 8 & 255;
                    boolean nearOccludesFar = overlapG > 50 && overlapG > overlapR * 1.5;
                    boolean passed = red > 10_000 && green > 1_000 && nearOccludesFar;
                    String result = (passed ? "PASS" : "FAIL") + " red=" + red + " green=" + green
                            + " nearOccludesFar=" + nearOccludesFar + " cachedFrames=" + cachedFrames;
                    Files.writeString(mc.gameDirectory.toPath().resolve("result.txt"), result);
                    GooseTools.LOGGER.info("Camera GPU regression {} (image: {})", result, output);
                } catch (Exception failure) {
                    GooseTools.LOGGER.error("Camera GPU regression readback failed", failure);
                } finally {
                    mc.execute(() -> { renderer.close(); mc.stop(); });
                }
            });
        } catch (Exception failure) {
            capturing = true;
            GooseTools.LOGGER.error("Camera GPU regression failed", failure);
            mc.stop();
        }
    }

    private static CameraScene scene() {
        var scene = new CameraScene(new CameraDefinition("test", "minecraft:overworld", 0, 1.5, 0, 0, 0));
        int[] states = new int[CameraLimits.CELLS];
        int[] lights = new int[CameraLimits.CELLS];
        Arrays.fill(states, Block.getId(Blocks.AIR.defaultBlockState()));
        Arrays.fill(lights, 0xf000f0);
        int red = Block.getId(BuiltInRegistries.BLOCK.getValue(
                Identifier.withDefaultNamespace("red_concrete")).defaultBlockState());
        int green = Block.getId(BuiltInRegistries.BLOCK.getValue(
                Identifier.withDefaultNamespace("lime_concrete")).defaultBlockState());
        for (int y = 0; y < 4; y++) {
            for (int x = -4; x < 4; x++) {
                states[CameraBlockFrame.index(x + 64, y + 16, 70)] = red;
            }
        }
        for (int y = 0; y < 3; y++) {
            states[CameraBlockFrame.index(64, y + 16, 67)] = green;
        }
        scene.blocks = new CameraBlockFrame(-64, -16, -64, states, lights, new int[CameraLimits.CELLS]);
        scene.blockVersion = 1;
        scene.skyColor = 0xff304060;
        return scene;
    }
}
