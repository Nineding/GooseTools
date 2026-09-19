package com.goosethings.tools.client.vision;

import net.minecraft.client.renderer.fog.FogData;
import org.joml.Vector4f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisionFogStateTest {

    @BeforeEach
    void setUp() {
        VisionFogState.apply(false, 12.0F, 16.0F, false);
        for (int i = 0; i < 20; i++) {
            VisionFogState.tick(null);
        }
    }

    @Test
    void shouldBlockSkyWhenVisionIsActive() {
        assertFalse(VisionFogState.shouldBlockSky());

        VisionFogState.apply(true, 1.0F, 2.0F, true);
        for (int i = 0; i < 30; i++) {
            VisionFogState.tick(null);
        }

        assertTrue(VisionFogState.shouldBlockSky());
    }

    @Test
    void blackoutSetsFogColorAndSkyToBlackWithoutRestrictingTerrainFogDistance() {
        VisionFogState.apply(true, 1.0F, 2.0F, true);
        for (int i = 0; i < 30; i++) {
            VisionFogState.tick(null);
        }

        FogData fog = new FogData();
        fog.environmentalStart = 100.0F;
        fog.environmentalEnd = 150.0F;
        fog.renderDistanceStart = 120.0F;
        fog.renderDistanceEnd = 160.0F;
        fog.skyEnd = 200.0F;
        fog.cloudEnd = 200.0F;
        fog.color = new Vector4f(0.8F, 0.9F, 1.0F, 1.0F); // Daytime sky white/blue

        VisionFogState.applyTo(fog, null);

        // Fog color must be pure black so clear color and textBackground fog don't turn white
        assertEquals(0.0F, fog.color.x(), 0.001F);
        assertEquals(0.0F, fog.color.y(), 0.001F);
        assertEquals(0.0F, fog.color.z(), 0.001F);
        assertEquals(1.0F, fog.color.w(), 0.001F);

        // Sky and clouds must be clamped down so sky pass or clouds are blocked
        assertEquals(2.0F, fog.skyEnd, 0.05F);
        assertEquals(2.0F, fog.cloudEnd, 0.05F);

        // Horizontal cylinder must NOT clamp environmental and renderDistance fog,
        // preserving visibility of floor and ceiling within the cylinder
        assertEquals(100.0F, fog.environmentalStart, 0.001F);
        assertEquals(150.0F, fog.environmentalEnd, 0.001F);
        assertEquals(120.0F, fog.renderDistanceStart, 0.001F);
        assertEquals(160.0F, fog.renderDistanceEnd, 0.001F);
    }

    @Test
    void sphericalLimitedVisionSetsAllFogParameters() {
        VisionFogState.apply(true, 12.0F, 16.0F, false);
        for (int i = 0; i < 30; i++) {
            VisionFogState.tick(null);
        }

        FogData fog = new FogData();
        fog.environmentalStart = 100.0F;
        fog.environmentalEnd = 150.0F;
        fog.renderDistanceStart = 120.0F;
        fog.renderDistanceEnd = 160.0F;
        fog.skyEnd = 200.0F;
        fog.cloudEnd = 200.0F;
        fog.color = new Vector4f(0.8F, 0.9F, 1.0F, 1.0F);

        VisionFogState.applyTo(fog, null);

        assertEquals(0.0F, fog.color.x(), 0.001F);
        assertEquals(0.0F, fog.color.y(), 0.001F);
        assertEquals(0.0F, fog.color.z(), 0.001F);
        assertEquals(1.0F, fog.color.w(), 0.001F);

        assertEquals(12.0F, fog.environmentalStart, 0.05F);
        assertEquals(16.0F, fog.environmentalEnd, 0.05F);
        assertEquals(12.0F, fog.renderDistanceStart, 0.05F);
        assertEquals(16.0F, fog.renderDistanceEnd, 0.05F);
        assertEquals(16.0F, fog.skyEnd, 0.05F);
        assertEquals(16.0F, fog.cloudEnd, 0.05F);
    }
}
