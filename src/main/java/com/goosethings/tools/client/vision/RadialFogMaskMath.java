package com.goosethings.tools.client.vision;

/** Pure calculations used by the shader-independent radial fog mask. */
final class RadialFogMaskMath {
    private RadialFogMaskMath() {
    }

    static float layerRadius(float clearRadius, float fullFogRadius, int layer, int layerCount) {
        validateLayer(layer, layerCount);
        float fraction = (float) layer / layerCount;
        return clearRadius + (fullFogRadius - clearRadius) * fraction;
    }

    static float layerAlpha(float strength, int layer, int layerCount) {
        validateLayer(layer, layerCount);
        float clampedStrength = Math.clamp(strength, 0.0F, 1.0F);
        float previousOpacity = clampedStrength * (layer - 1.0F) / layerCount;
        float targetOpacity = clampedStrength * layer / layerCount;
        if (previousOpacity >= 1.0F) {
            return 1.0F;
        }
        return Math.clamp((targetOpacity - previousOpacity) / (1.0F - previousOpacity), 0.0F, 1.0F);
    }

    private static void validateLayer(int layer, int layerCount) {
        if (layerCount <= 0 || layer <= 0 || layer > layerCount) {
            throw new IllegalArgumentException("layer must be within 1..layerCount");
        }
    }
}
