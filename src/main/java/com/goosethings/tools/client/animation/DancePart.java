package com.goosethings.tools.client.animation;

import org.joml.Quaternionf;

/** Preview-only articulation. Cleared on every normal humanoid setup. */
public interface DancePart {
    void goosetools$bend(Quaternionf rotation, float joint);
}
