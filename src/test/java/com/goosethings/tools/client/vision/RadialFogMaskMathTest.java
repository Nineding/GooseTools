package com.goosethings.tools.client.vision;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RadialFogMaskMathTest {
    @Test
    void placesFinalLayerAtFullFogRadius() {
        assertEquals(16.0F, RadialFogMaskMath.layerRadius(12.0F, 16.0F, 12, 12));
    }

    @Test
    void placesMiddleLayerHalfwayThroughFadeSpan() {
        assertEquals(14.0F, RadialFogMaskMath.layerRadius(12.0F, 16.0F, 6, 12));
    }

    @Test
    void accumulatedLayerAlphaMatchesLinearFogOpacity() {
        float remainingColor = 1.0F;
        for (int layer = 1; layer <= 12; layer++) {
            remainingColor *= 1.0F - RadialFogMaskMath.layerAlpha(1.0F, layer, 12);
            assertEquals(1.0F - layer / 12.0F, remainingColor, 0.00001F);
        }
    }

    @Test
    void transitionStrengthLimitsOuterOpacity() {
        float remainingColor = 1.0F;
        for (int layer = 1; layer <= 12; layer++) {
            remainingColor *= 1.0F - RadialFogMaskMath.layerAlpha(0.5F, layer, 12);
        }
        assertEquals(0.5F, remainingColor, 0.00001F);
    }

    @Test
    void rejectsInvalidLayerIndex() {
        assertThrows(IllegalArgumentException.class,
                () -> RadialFogMaskMath.layerRadius(12.0F, 16.0F, 0, 12));
    }
}
